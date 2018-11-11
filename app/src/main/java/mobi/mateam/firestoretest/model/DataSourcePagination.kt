package mobi.mateam.firestoretest.model

import android.icu.util.Calendar
import android.os.Build
import com.github.ajalt.timberkt.Timber
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject

class DataSourcePagination(val tag: String, private val pageSize: Long = 2) {

    private val publisher = BehaviorSubject.create<List<Model>>()
    private val dataSource = ModelDataSource(tag, pageSize)

    private var lastReceivedModelTimestamp: Long = 0

    private val disposables = CompositeDisposable()

    fun getModels(): Flowable<List<Model>> {
        return publisher
                .toFlowable(BackpressureStrategy.LATEST)
                .distinctUntilChanged()
                .doOnNext { Timber.d { "getModels() for tag [$tag]: post model list $it" } }
    }


    fun loadMore(oldestPostedTime: Long?) {

        if (hasEnoughItemsInCache(oldestPostedTime)) {
            Timber.d { "loadMore for tag [$tag], oldestPostedTime [oldestPostedTime]: network call is not needed, cache contains enough items for next page" }
            return
        }

        if (lastReceivedModelTimestamp == 0L) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                lastReceivedModelTimestamp = Calendar.getInstance().timeInMillis
            }
        }

        if (dataSource.endReached) {
            Timber.d{"The end was reached for tag [$tag]"}
        } else {
            val disposable = dataSource
                    .getModels(lastReceivedModelTimestamp)
                    .subscribeBy(onNext = {
                        Timber.d { "loadMore for tag [$tag] received $it for tag $tag and timeAfter : $lastReceivedModelTimestamp" }
                        publisher.onNext(it)
                        lastReceivedModelTimestamp = it.minBy { it.timestamp }?.timestamp ?: lastReceivedModelTimestamp
                    })

            disposables.add(disposable)
        }

        Timber.d { "loadMore: Online Listeners[for tag $tag],  size  = [${disposables.size()}]" }
    }


    private fun hasEnoughItemsInCache(oldestPostedTime: Long?): Boolean {
        if (oldestPostedTime == null) {
            Timber.d { "hasEnoughItemsInCache for tag [$tag] (oldestPostedTime: $oldestPostedTime): Return [false]" }
            return false
        }
        return if (lastReceivedModelTimestamp > oldestPostedTime) {
            Timber.d { "hasEnoughItemsInCache for tag [$tag] (oldestPostedTime: $oldestPostedTime) , lastReceivedModelTimestamp = [$lastReceivedModelTimestamp]: requested for older items. Return [false]" }
            false
        } else {
            val oldestInCacheSize = dataSource.cache.toList().filter { it.timestamp < oldestPostedTime }.size
            Timber.d { "hasEnoughItemsInCache for tag [$tag] (oldestPostedTime: $oldestPostedTime): cache contains [$oldestInCacheSize] older items, return result [${oldestInCacheSize >= pageSize}]" }
            oldestInCacheSize >= pageSize
        }
    }

    fun dispose() {
        dataSource.dispose()
        disposables.dispose()
        publisher.onComplete()
    }
}