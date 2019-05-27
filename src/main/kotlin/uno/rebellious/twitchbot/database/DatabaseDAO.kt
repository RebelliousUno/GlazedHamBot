package uno.rebellious.twitchbot.database

import java.sql.Connection
import java.sql.DriverManager
import java.time.LocalDate
import java.util.*

class DatabaseDAO : IDatabase {

    private var connectionList: HashMap<String, Connection> = HashMap()
    private val countersDAO = CountersDAO(connectionList)
    private val responsesDAO = ResponsesDAO(connectionList)
    private val quotesDAO = QuotesDAO(connectionList)
    private val settingsDAO = SettingsDAO(connectionList)

    init {
        setupSettings() //Set up Settings DB
        val channelList = settingsDAO.getListOfChannels()
        connect(channelList)
        setupAllChannels()
    }

    override fun createCounterForChannel(
        channel: String,
        counter: String,
        responseSingular: String,
        responsePlural: String
    ) = countersDAO.createCounterForChannel(channel, counter, responseSingular, responsePlural)

    override fun showCountersForChannel(channel: String): List<String> = countersDAO.showCountersForChannel(channel)

    override fun removeCounterForChannel(channel: String, counter: String) =
            countersDAO.removeCounterForChannel(channel, counter)

    override fun incrementCounterForChannel(channel: String, counter: String, by: Int) =
            countersDAO.incrementCounterForChannel(channel, counter, by)

    override fun getCounterForChannel(channel: String, counter: String): String =
            countersDAO.getCounterForChannel(channel, counter)

    override fun resetTodaysCounterForChannel(channel: String, counter: String) =
            countersDAO.resetTodaysCounterForChannel(channel, counter)

    override fun addQuoteForChannel(channel: String, date: LocalDate, person: String, quote: String): Int =
            quotesDAO.addQuoteForChannel(channel, date, person, quote)

    override fun delQuoteForChannel(channel: String, quoteId: Int) = quotesDAO.delQuoteForChannel(channel, quoteId)

    override fun undeleteQuoteForChannel(channel: String, quoteId: Int) = quotesDAO.undeleteQuoteForChannel(channel, quoteId)

    override fun editQuoteForChannel(channel: String, quoteId: Int, date: LocalDate?, person: String, quote: String) =
            quotesDAO.editQuoteForChannel(channel, quoteId, date, person, quote)

    override fun getQuoteForChannelById(channel: String, quoteId: Int): String =
            quotesDAO.getQuoteForChannelById(channel, quoteId)

    override fun getRandomQuoteForChannel(channel: String): String = quotesDAO.getRandomQuoteForChannel(channel)

    override fun findQuoteByAuthor(channel: String, author: String): String =
            quotesDAO.findQuoteByAuthor(channel, author)

    override fun findQuoteByKeyword(channel: String, keyword: String): String =
            quotesDAO.findQuoteByKeyword(channel, keyword)

    private fun setupSettings() {
        settingsDAO.createChannelsTable()
        if (settingsDAO.getListOfChannels().isEmpty()) { // Set up default Channel
            addChannel("glazedhambot", "!")
        }
    }

    private fun connect(channels: Array<Channel>) {
        channels.forEach {
            val con = DriverManager.getConnection("jdbc:sqlite:${it.channel.toLowerCase()}.db")
            connectionList[it.channel] = con
        }
    }

    private fun setupAllChannels() {
        connectionList.forEach {
            responsesDAO.createResponseTable(it.value)
            quotesDAO.createQuotesTable(it.value)
            countersDAO.setupCounters(it.value)
        }
    }

    override fun getPrefixForChannel(channel: String): String  = settingsDAO.getPrefixForChannel(channel)

    override fun setPrefixForChannel(channel: String, prefix: String) = settingsDAO.setPrefixForChannel(channel, prefix)

    override fun findResponse(channel: String, command: String): String = responsesDAO.findResponse(channel, command)

    override fun addChannel(newChannel: String, prefix: String) = settingsDAO.addChannel(newChannel, prefix)

    override fun leaveChannel(channel: String) = settingsDAO.leaveChannel(channel)

    override fun setResponse(channel: String, command: String, response: String) =
            responsesDAO.setResponse(channel, command, response)

    override fun removeResponse(channel: String, command: String) = responsesDAO.removeResponse(channel, command)

    override fun getAllCommandList(channel: String): ArrayList<String> = settingsDAO.getAllCommandList(channel)

    override fun getListOfChannels(): Array<Channel> = settingsDAO.getListOfChannels()
}