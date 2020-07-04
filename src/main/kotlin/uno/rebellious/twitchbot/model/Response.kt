package uno.rebellious.twitchbot.model

data class Response(val command: String, val response: String = "") {
    fun isBlank(): Boolean {
        return command.isBlank() || response.isBlank()
    }
}