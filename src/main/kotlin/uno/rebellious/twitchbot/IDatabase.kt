package uno.rebellious.twitchbot

interface IDatabase {
    fun findResponse(channel: String, command: String): String
    fun setResponse(channel: String, command: String, response: String)
    fun removeResponse(channel: String, command: String)
    fun getAllCommandList(channel: String): ArrayList<String>
    fun leaveChannel(channel: String)
    fun addChannel(newChannel: String, prefix: String = "!")
    fun getPrefixForChannel(channel: String): String
    fun setPrefixForChannel(channel: String, prefix: String)
    fun createCounterForChannel(channel: String, counter: String, responseSingular: String, responsePlural: String)
    fun removeCounterForChannel(channel: String, counter: String)
    fun incrementCounterForChannel(channel: String, counter: String, by: Int = 1)
    fun getCounterForChannel(channel: String, counter: String): String
    fun resetTodaysCounterForChannel(channel: String, counter: String)
    fun showCountersForChannel(channel: String): List<String>
}