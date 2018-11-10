package mobi.mateam.firestoretest.model

import io.reactivex.Flowable
import io.reactivex.rxkotlin.combineLatest
import org.jetbrains.anko.AnkoLogger

class PaginationOrchestrator(val tags: List<String>) {
    private val log = AnkoLogger(this.javaClass)

    val dataSources = mutableMapOf<String, DataSourcePagination>()

    init {
        for (tag in tags) {
            val dataSourcePagination = DataSourcePagination(tag)
            dataSources[tag] = dataSourcePagination
        }

    }

    fun getModels(): Flowable<List<Model>> {
        val flowableList = dataSources.values.map { it.getModels() }

        return flowableList
            .combineLatest { it.flatten() }
            .map { it.distinct() }
            .map { list -> list.sortedByDescending { it.timestamp } }
            .distinctUntilChanged()

    }

    fun nextPage(vararg tags: String) {
        for (tag in tags) {
            dataSources[tag]?.loadNextPage()
        }
    }
}