package mobi.mateam.firestoretest.model

import android.icu.util.Calendar
import android.os.Build
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import org.jetbrains.anko.AnkoLogger
import org.jetbrains.anko.debug

class DataSourcePagination(val tag: String) {
    private val log = AnkoLogger(this.javaClass)

    val publisher = BehaviorSubject.create<List<Model>>()
    private val pageSize: Long = 2
    private val dataSource = ModelDataSource(pageSize)
    var lastReceivedModelTimestamp: Long = 0
    var lastRequestedModelTimestamp: Long = 0
    val disposables = CompositeDisposable()

    fun getModels(): Flowable<List<Model>> {
        return publisher
            .toFlowable(BackpressureStrategy.LATEST)
            .distinctUntilChanged()
            .doOnNext { log.debug { "getModels(): post model list $it" } }
    }


    fun loadNextPage() {
        if (lastReceivedModelTimestamp == 0L) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                lastReceivedModelTimestamp = Calendar.getInstance().timeInMillis
            }
        }

        val disposable = dataSource
            .getModels(tag, lastReceivedModelTimestamp)
            .subscribeBy(onNext = {
                log.debug { "loadNextPage received $it for tag $tag and timeAfter : $lastRequestedModelTimestamp" }
                publisher.onNext(it)
                lastRequestedModelTimestamp = lastReceivedModelTimestamp
                lastReceivedModelTimestamp = it.minBy { it.timestamp }?.timestamp ?: lastReceivedModelTimestamp
            })

        disposables.add(disposable)
    }

    fun dispose() {
        dataSource.dispose()
        disposables.dispose()
        publisher.onComplete()
    }
}