package uno.rebellious.twitchbot

import java.util.*

interface IDatabase {
    fun findResponse(channel: String, command: String): String
    fun setResponse(channel: String, command: String, response: String)
    fun removeResponse(channel: String, command: String)
    fun getAllCommandList(channel: String): ArrayList<String>
    fun leaveChannel(channel: String)
    fun addChannel(newChannel: String, prefix: String = "!")
    fun getPrefixForChannel(channel: String): String
    fun setPrefixForChannel(channel: String, prefix: String)
    fun addQuoteForChannel(channel: String, date: Date, person: String, quote: String)
    fun delQuoteForChannel(channel: String, quoteId: Int)
    fun editQuoteForChannel(channel: String, quoteId: Int, date: Date, person: String, quote: String)
    fun getQuoteForChannelById(channel: String, quoteId: Int): String
    fun getRandomQuoteForChannel(channel: String): String
    fun findQuoteByAuthor(channel: String, author: String): String
    fun findQuoteByKeyword(channel: String, keyword: String): String
}