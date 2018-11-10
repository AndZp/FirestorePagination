package mobi.mateam.firestoretest.model

import com.google.firebase.firestore.*
import durdinapps.rxfirebase2.RxFirestore
import io.reactivex.Flowable
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug

class ModelDataSource(val limit: Long) {
    private val log = AnkoLogger(this.javaClass)

    val db = FirebaseFirestore.getInstance()
    val cache = CacheHelper<Model>()


    fun getModels(tag: String, startAfterTime: Long): Flowable<List<Model>> {

        val query: Query = db
            .collection("models")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .whereArrayContains("tags", tag)
            .whereLessThan("timestamp", startAfterTime)
            .limit(limit)

        return RxFirestore
            .observeQueryRef(query)
            .map { handleSnapshot(it) }

    }

    private fun handleSnapshot(snapshot: QuerySnapshot): List<Model> {
        log.debug { "handleSnapshot: is snapshot from cache ${snapshot.metadata.isFromCache}" }
        for (documentChange in snapshot.documentChanges) {
            when (documentChange.type) {
                DocumentChange.Type.ADDED -> onItemAdded(documentChange.document)
                DocumentChange.Type.MODIFIED -> onItemChanged(documentChange.document)
                DocumentChange.Type.REMOVED -> onItemRemoved(documentChange.document)
            }

        }
        return cache.toList()
    }

    private fun onItemAdded(document: QueryDocumentSnapshot) {
        val model = document.toObject(Model::class.java)
        model.let { cache.put(it) }
        log.debug { "onItemAdded $model, is document from cache ${document.metadata.isFromCache}" }
    }

    private fun onItemChanged(document: QueryDocumentSnapshot) {
        val model = document.toObject(Model::class.java)
        model.let { cache.put(it) }
        log.debug { "onItemChanged $model" }
    }

    private fun onItemRemoved(document: QueryDocumentSnapshot) {
        val model = document.toObject(Model::class.java)
        model.let { cache.remove(it) }
        log.debug { "onItemRemoved $model" }
    }

    fun dispose(){
        cache.clear()
    }
}