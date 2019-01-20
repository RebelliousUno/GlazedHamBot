package uno.rebellious.twitchbot.database

import java.sql.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import java.util.Date

class DatabaseDAO : IDatabase {
    private var settingsDB: Connection? = null
    private var connectionList: HashMap<String, Connection> = HashMap()

    init {
        connectSettings() //Connect to settings DB
        setupSettings() //Set up Settings DB
        val channelList = getListOfChannels()
        println(channelList)
        connect(channelList)
        setupAllChannels()
    }

    override fun createCounterForChannel(
        channel: String,
        counter: String,
        responseSingular: String,
        responsePlural: String
    ) {
        val sql = "INSERT INTO counters(command, today, total, singular, plural) VALUES (?, ?, ?, ?, ?)"
        connectionList[channel]?.prepareStatement(sql)?.apply {
            setString(1, counter)
            setInt(2, 0)
            setInt(3, 0)
            setString(4, responseSingular)
            setString(5, responsePlural)
            executeUpdate()
        }
    }

    override fun showCountersForChannel(channel: String): List<String> {
        val sql = "SELECT * from counters"
        val counters = ArrayList<String>()
        connectionList[channel]?.createStatement()?.run {
            executeQuery(sql)
        }?.run {
            while (next()) {
                counters += "${getString("command")}: ${getString("today")}/${getString("total")}"
            }
        }
        return counters
    }

    override fun removeCounterForChannel(channel: String, counter: String) {
        val sql = "DELETE FROM counters WHERE command like ?"
        connectionList[channel]?.prepareStatement(sql)?.apply {
            setString(1, counter)
            executeUpdate()
        }
    }

    override fun incrementCounterForChannel(channel: String, counter: String, by: Int) {
        val todaySql = "UPDATE counters SET today = today + ?, total = total + ? WHERE command like ?"
        connectionList[channel]
        connectionList[channel]?.prepareStatement(todaySql)?.apply {
            setInt(1, by)
            setInt(2, by)
            setString(3, counter)
            executeUpdate()
        }
    }

    override fun getCounterForChannel(channel: String, counter: String): String {
        val sql = "SELECT * FROM counters where command like ?"
        connectionList[channel]?.prepareStatement(sql)?.run {
            setString(1, counter)
            executeQuery()
        }?.run {
            if (next()) {
                val today = getInt("today")
                val total = getInt("total")
                val singular = getString("singular")
                val plural = getString("plural")
                return "There ${if (today == 1) "has" else "have"} been $today ${if (today == 1) singular else plural} today. Total $plural: $total"
            }
        }
        return ""
    }

    override fun resetTodaysCounterForChannel(channel: String, counter: String) {
        val sql = "UPDATE counters SET today = 0 where command like ?"
        connectionList[channel]?.prepareStatement(sql)?.apply {
            setString(1, counter)
            executeUpdate()
        }
    }
    override fun addQuoteForChannel(channel: String, date: LocalDate, person: String, quote: String): Int {
        val timestamp = Timestamp.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
        val sql = "INSERT into quotes (quote, subject, timestamp) VALUES (?, ?, ?)"
        return connectionList[channel]?.prepareStatement(sql)?.run {
            setString(1, quote)
            setString(2, person)
            setTimestamp(3, timestamp)
            executeUpdate()
            val id = generatedKeys
            if (id.next())
                id.getInt(1)
            else 0
        } ?: 0
    }

    override fun delQuoteForChannel(channel: String, quoteId: Int) {
        /*"create table if not exists quotes (" +
  "ID INTEGER PRIMARY KEY, " +
  "quote text, " +
  "subject text, " +
  "timestamp INTEGER)"*/

        //TODO: Should probably either audit this or just set to deleted or not
        val sql = "delete from quotes where ID = ?"
        connectionList[channel]?.prepareStatement(sql)?.apply {
            setInt(1, quoteId)
            executeUpdate()
        }
    }

    override fun editQuoteForChannel(channel: String, quoteId: Int, date: Date, person: String, quote: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getQuoteForChannelById(channel: String, quoteId: Int): String {
        val sql = "SELECT * from quotes where ID = ?"
        val statement = connectionList[channel]?.prepareStatement(sql)?.apply {
            setInt(1, quoteId)
        }
        return getQuotesFromStatement(statement)
    }

    override fun getRandomQuoteForChannel(channel: String): String {
        val sql = "SELECT * from quotes ORDER BY Random() LIMIT 1"
        val statement = connectionList[channel]?.prepareStatement(sql)
        return getQuotesFromStatement(statement)
    }

    private fun getQuotesFromStatement(statement: PreparedStatement?): String {
        return statement?.executeQuery()?.run {
            if (next()) {
                val id = getInt("ID")
                val quote = getString("quote")
                val subject = getString("subject")
                val timestamp = getTimestamp("timestamp").toLocalDateTime().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                "Quote $id: \"$quote\" - $subject - $timestamp"
            } else {
                "Quote not found"
            }
        } ?: "Quote not found"

    }

    override fun findQuoteByAuthor(channel: String, author: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findQuoteByKeyword(channel: String, keyword: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun connectSettings() {
        settingsDB = DriverManager.getConnection("jdbc:sqlite:settings.db")
    }

    fun getListOfChannels(): Array<String> {
        val channelSelect = "select * from channels"
        val list = ArrayList<String>()
        settingsDB?.createStatement()?.run {
            queryTimeout = 30
            executeQuery(channelSelect)
        }?.apply {
            while (next()) {
                val channel = getString("channel")
                list.add(channel)
            }
        }
        return list.toTypedArray()
    }

    private fun setupSettings() {
        val channelList = "create table if not exists channels (" +
                "channel text, prefix text DEFAULT '!')"

        settingsDB?.createStatement()?.apply {
            queryTimeout = 30
            executeUpdate(channelList)
        }
        if (getListOfChannels().isEmpty()) { // Set up default Channel
            addChannel("glazedhambot", "!")
        }
    }

    private fun connect(channels: Array<String>) {
        channels.forEach {
            val con = DriverManager.getConnection("jdbc:sqlite:${it.toLowerCase()}.db")
            connectionList[it] = con
        }
    }

    private fun connect(channel: String) {
        val con = DriverManager.getConnection("jdbc:sqlite:${channel.toLowerCase()}.db")
        connectionList[channel] = con
    }

    private fun setupAllChannels() {
        connectionList.forEach {
            setup(it.value)
            setupCounters(it.value)
        }
    }

    private fun setupCounters(connection: Connection) {
        val counterTableSQL = "create table if not exists counters (command text, today int, total int, singular text, plural text)"
        connection.createStatement()?.apply {
            queryTimeout = 30
            executeUpdate(counterTableSQL)
        }
    }

    private fun setup(connection: Connection) {
        val responsesTableSql = "create table if not exists responses (" +
                "command text," +
                "response text)"

        val quotesTableSql = "create table if not exists quotes (" +
                "ID INTEGER PRIMARY KEY, " +
                "quote text, " +
                "subject text, " +
                "timestamp INTEGER)"

        connection.createStatement()?.apply {
            queryTimeout = 30
            executeUpdate(responsesTableSql)
            executeUpdate(quotesTableSql)
        }
    }

    override fun getPrefixForChannel(channel: String): String {
        val sql = "Select prefix from channels where channel = ?"
        return settingsDB?.prepareStatement(sql)?.run {
            setString(1, channel)
            executeQuery()
        }?.run {
            if (next()) {
                getString("prefix")
            } else {
                "!" //Default to "!"
            }
        } ?: "!"
    }

    override fun setPrefixForChannel(channel: String, prefix: String) {
        val sql = "UPDATE channels set prefix = ? where channel = ?"
        settingsDB?.prepareStatement(sql)?.apply {
            setString(1, prefix)
            setString(2, channel)
            executeUpdate()
        }
    }

    override fun findResponse(channel: String, command: String): String {
        val connection = connectionList[channel]
        val sql = "Select response from responses where command = ?"
        return connection?.prepareStatement(sql)?.run {
            setString(1, command)
            executeQuery()
        }?.run {
            if (next()) {
                getString("response")
            } else {
                ""
            }
        } ?: ""
    }

    private fun channelExists(channel: String): Boolean? {
        val sql = "Select * from channels WHERE channel = ?"
        return settingsDB?.prepareStatement(sql)?.run {
            setString(1, channel)
            executeQuery()
        }?.run {next()} ?: false
    }

    override fun addChannel(newChannel: String, prefix: String) {
        val exists = channelExists(newChannel)
        if (exists != null && !exists) {
            val sql = "INSERT INTO channels(channel, prefix) VALUES (?, ?)"
            settingsDB?.prepareStatement(sql)?.apply {
                setString(1, newChannel)
                setString(2, prefix)
                executeUpdate()
                connect(newChannel)
            }
        }
    }

    override fun leaveChannel(channel: String) {
        val sql = "DELETE FROM channels WHERE channel = ?"
        settingsDB?.prepareStatement(sql)?.apply {
            setString(1, channel)
            executeUpdate()
        }
    }

    override fun setResponse(channel: String, command: String, response: String) {
        val connection = connectionList[channel]
        val exists = findResponse(channel, command)
        val sql = if (exists == "") {
            "INSERT INTO responses(response, command) VALUES (?, ?)"
        } else {
            "UPDATE responses SET response = ? WHERE command = ?"
        }
        connection?.prepareStatement(sql)?.apply {
            setString(1, response)
            setString(2, command)
            executeUpdate()
        }
    }

    override fun removeResponse(channel: String, command: String) {
        val connection = connectionList[channel]
        val sql = "DELETE FROM responses WHERE command = ?"
        connection?.prepareStatement(sql)?.apply {
            setString(1, command)
            executeUpdate()
        }
    }

    override fun getAllCommandList(channel: String): ArrayList<String> {
        val connection = connectionList[channel]
        val sql = "SELECT command FROM responses"
        val returnList = ArrayList<String>()
        connection?.prepareStatement(sql)?.run {
            executeQuery()
        }?.apply {
            while(next()) returnList.add(getString("command"))
        }
        return returnList
    }
}