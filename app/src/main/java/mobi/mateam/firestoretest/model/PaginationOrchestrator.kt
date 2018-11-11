package mobi.mateam.firestoretest.model

import com.github.ajalt.timberkt.Timber
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import io.reactivex.subjects.BehaviorSubject
import java.util.concurrent.TimeUnit

class PaginationOrchestrator(val allTags: List<String>, val pageSize: Int = 2) {

    val repostTrigger = BehaviorSubject.create<List<String>>()
    private var selectedTags: List<String> = allTags
    private val dataSources = mutableMapOf<String, DataSourcePagination>()
    var limit = 0
    private var oldestPostedTime: Long? = null

    init {
        for (tag in allTags) {
            val dataSourcePagination = DataSourcePagination(tag)
            dataSources[tag] = dataSourcePagination
            dataSourcePagination.loadMore(oldestPostedTime)
        }
    }


    fun getModels(): Flowable<List<Model>> {
        val flowableList = dataSources.values.map { it.getModels() }

        return flowableList
                .combineLatest { it.flatten() }
                .combineLatest(getResetTrigger())
                .map { (models, selectedTags) ->
                    models
                            .distinct()
                            .filter { model -> isContainsSelectedTag(model.tags, selectedTags) }
                            .sortedByDescending { model -> model.timestamp }
                            .take(limit)
                }
                .doOnNext { Timber.d { "combined models list posted $it" } }
                .doOnNext { oldestPostedTime = it.lastOrNull()?.timestamp }

    }

    private fun getResetTrigger() = repostTrigger
            .toFlowable(BackpressureStrategy.LATEST)
            .debounce(300, TimeUnit.MILLISECONDS)


    private fun isContainsSelectedTag(modelTags: List<String>, selectedTags: List<String>): Boolean {
        for (modelTag in modelTags) {
            if (modelTag in selectedTags) {
                return true
            }
        }

        return false
    }

    fun onFilterChanged(vararg tags: String) {
        this.selectedTags = tags.toList()
        limit = pageSize
        repostTrigger.onNext(selectedTags)
    }

    fun loadMore() {
        limit += pageSize
        for (tag in selectedTags) {
            dataSources[tag]?.loadMore(oldestPostedTime)
        }
        repostTrigger.onNext(selectedTags)
    }

    fun dispose() {
        dataSources.forEach { it.value.dispose() }
        dataSources.clear()
    }
}