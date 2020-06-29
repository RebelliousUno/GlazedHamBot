package uno.rebellious.twitchbot.model

data class Counter(val command: String, val singular: String = "", val plural: String = "", val today: Int = 0, val total: Int = 0) {
    val totalString
        get() = "$command: $today/$total"
    val outputString
        get() = "There ${if (today == 1) "has" else "have"} been $today ${if (today == 1) singular else plural} today. Total $plural: $total"
    fun isValidCreateCounter() = command.isNotBlank() && singular.isNotBlank() && plural.isNotBlank()
}