package mobi.mateam.firestoretest.model

data class Model(
    var id: String = "",
    var timestamp: Long = 0,
    var tags: List<String> = listOf()
) : CacheSupport {

    override fun getKeyId(): String {
        return id
    }
}