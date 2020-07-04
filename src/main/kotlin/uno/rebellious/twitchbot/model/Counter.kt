package uno.rebellious.twitchbot.model

data class Counter(
    val command: String,
    val singular: String = "",
    val plural: String = "",
    val today: Int = 0,
    val total: Int = 0
) {
    val totalString
        get() = "$command: $today/$total"
    val outputString
        get() = "There ${if (today == 1) "has" else "have"} been $today ${if (today == 1) singular else plural} today. Total $plural: $total"

    fun isValidCreateCounter() = command.isNotBlank() && singular.isNotBlank() && plural.isNotBlank()
    fun isEmpty() = command.isEmpty() && singular.isEmpty() && plural.isEmpty()

    override fun equals(other: Any?): Boolean {
        return other is Counter && other.command == this.command && other.singular == this.singular && other.plural == this.plural
    }

    override fun hashCode(): Int {
        return command.hashCode() + singular.hashCode() + plural.hashCode()
    }
}