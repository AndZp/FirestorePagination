package mobi.mateam.firestoretest.model

object DataGenerator {


    fun generateModels(startIndex: Int, amount: Int, tags: List<String>): MutableList<MutableMap<String, Any>> {
        val models = mutableListOf<MutableMap<String, Any>>()
        for (i in startIndex..startIndex + amount) {
            models.add(generateModel(i.toString(), i.toLong(), tags))
        }
        return models
    }

    fun generateModel(id: String, timestamp: Long, tags: List<String>): MutableMap<String, Any> {
        return mutableMapOf( "id" to id, "timestamp" to timestamp, "tags" to tags)
    }
}