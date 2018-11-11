package mobi.mateam.firestoretest.model

import com.github.ajalt.timberkt.Timber
import com.google.firebase.firestore.*
import durdinapps.rxfirebase2.RxFirestore
import io.reactivex.Flowable

class ModelDataSource(val tag: String, val limit: Long) {
    val db = FirebaseFirestore.getInstance()
    val cache = CacheHelper<Model>()
    var endReached: Boolean = false

    fun getModels(startAfterTime: Long): Flowable<List<Model>> {

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
        Timber.d { "handleSnapshot for tag [$tag]: is snapshot from cache [${snapshot.metadata.isFromCache}]" }

        if (snapshot.isEmpty) {
            Timber.d { "handleSnapshot for tag [$tag]: is snapshot empty [${snapshot.isEmpty}]" }
            endReached = true
        } else {
            for (documentChange in snapshot.documentChanges) {
                when (documentChange.type) {
                    DocumentChange.Type.ADDED -> onItemAdded(documentChange.document)
                    DocumentChange.Type.MODIFIED -> onItemChanged(documentChange.document)
                    DocumentChange.Type.REMOVED -> onItemRemoved(documentChange.document)
                }
            }
        }
        return cache.toList()
    }

    private fun onItemAdded(document: QueryDocumentSnapshot) {
        val model = document.toObject(Model::class.java)
        model.let { cache.put(it) }
        Timber.d { "onItemAdded for tag [$tag] -  $model, is document from cache ${document.metadata.isFromCache}" }
    }

    private fun onItemChanged(document: QueryDocumentSnapshot) {
        val model = document.toObject(Model::class.java)
        model.let { cache.put(it) }
        Timber.d { "onItemChanged for tag [$tag] -  $model" }
    }

    private fun onItemRemoved(document: QueryDocumentSnapshot) {
        val model = document.toObject(Model::class.java)
        model.let { cache.remove(it) }
        Timber.d { "onItemRemoved for tag [$tag] -  $model" }
    }

    fun dispose() {
        Timber.d { "Disposed called for tag [$tag]" }
        cache.clear()
    }
}