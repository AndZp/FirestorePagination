package mobi.mateam.firestoretest

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.method.ScrollingMovementMethod
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import durdinapps.rxfirebase2.RxFirestore
import io.reactivex.BackpressureStrategy
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.activity_main.*
import mobi.mateam.firestoretest.model.DataGenerator
import mobi.mateam.firestoretest.model.DataSourcePagination
import mobi.mateam.firestoretest.model.PaginationOrchestrator
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug


class MainActivity : AppCompatActivity() {
    private val log = AnkoLogger(this.javaClass)
    val publisher = BehaviorSubject.create<Array<String>>()


    val paginator = DataSourcePagination("A")
    var tags = arrayOf("A", "B")
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView.movementMethod = ScrollingMovementMethod()


        val paginationOrcestrator = PaginationOrchestrator(listOf("A", "B"))

        btnLoad.setOnClickListener {
            paginationOrcestrator.nextPage(*tags)
            textView.text = textView.text.toString() + "Next page clicked \n"
        }

        btnA.setOnClickListener {
            tags = arrayOf("A")
            textView.text = textView.text.toString() + "Tags changed A \n"
            publisher.onNext(tags)
        }
        btnB.setOnClickListener {
            tags = arrayOf("B")
            textView.text = textView.text.toString() + "Tags changed B \n"
            publisher.onNext(tags)

        }

        btnAll.setOnClickListener {
            tags = arrayOf("A", "B")
            textView.text = textView.text.toString() + "Tags changed ALL \n"
            publisher.onNext(tags)

        }


        paginationOrcestrator
            .getModels()
            .combineLatest(publisher.toFlowable(BackpressureStrategy.MISSING).startWith(tags))
            .map { pair -> pair.first.filter { isContainsSelectedTag(it.tags, pair.second) } }
            .subscribeBy(
                onNext = {
                    log.debug { "List received $it" }
                    textView.text = textView.text.toString() + "List received with size ${it.size} \n"

                    it.forEach {
                        textView.text = textView.text.toString() + "$it \n"

                    }

                },
                onError = { Log.e("AAAAA", "Error A", it) })

    }

    private fun isContainsSelectedTag(modelTags: List<String>, selectedTags: Array<String>): Boolean {
        for (modelTag in modelTags) {
            if (modelTag in selectedTags) {
                return true
            }
        }

        return false
    }

    private fun uploadModelsForTest() {
        val db = FirebaseFirestore.getInstance()
        val listA = DataGenerator.generateModels(0, 5, listOf("A"))
        val listB = DataGenerator.generateModels(6, 5, listOf("B"))
        //val listAB = DataGenerator.generateModels(6, 5, listOf("B"))


        for (model in listB) {
            RxFirestore
                .addDocument(db.collection("models"), model)
                .subscribeBy(
                    onSuccess = { Log.d("AAAAA", "OnSuccess A") },
                    onError = { Log.e("AAAAA", "Error A", it) })
        }
    }
}
