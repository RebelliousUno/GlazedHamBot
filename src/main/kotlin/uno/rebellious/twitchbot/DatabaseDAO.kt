package uno.rebellious.twitchbot

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement
import java.util.*

class DatabaseDAO : IDatabase {
    override fun addQuoteForChannel(channel: String, date: Date, person: String, quote: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun delQuoteForChannel(channel: String, quoteId: Int) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun editQuoteForChannel(channel: String, quoteId: Int, date: Date, person: String, quote: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getQuoteForChannelById(channel: String, quoteId: Int): String {
        val sql = "SELECT * from quotes where ID = ?"
        val statement = connectionList[channel]?.prepareStatement(sql)
        statement?.setInt(1, quoteId)
        val resultSet = statement?.executeQuery()
        if (resultSet?.next()!!) {
            /*"create table if not exists quotes (" +
                "ID INTEGER PRIMARY KEY, " +
                "quote text, " +
                "subject text, " +
                "timestamp INTEGER)"*/
            var id = resultSet.getInt("ID")
            var quote = resultSet.getString("quote")
            var subject = resultSet.getString("subject")
            var timestamp = resultSet.getInt("timestamp")
            return "Quote $id: \"$quote\" - $subject - $timestamp"
        } else {
            return "No quote with ID $quoteId found"
        }
    }

    override fun getRandomQuoteForChannel(channel: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findQuoteByAuthor(channel: String, author: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun findQuoteByKeyword(channel: String, keyword: String): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private var settingsDB: Connection? = null
    private var connectionList: HashMap<String, Connection> = HashMap()

    init {
        connectSettings() //Connect to settings DB
        setupSettings() //Set up Settings DB
        var channelList = getListOfChannels()
        connect(channelList)
        setupAllChannels()
    }

    fun connectSettings() {
        settingsDB = DriverManager.getConnection("jdbc:sqlite:settings.db")
    }

    fun getListOfChannels(): Array<String> {
        var statement = settingsDB?.createStatement()!!
        statement.queryTimeout = 30
        var channelSelect = "select * from channels"
        var results = statement.executeQuery(channelSelect)
        val list = ArrayList<String>()
        while (results.next()) {
            var channel = results.getString("channel")
            list.add(channel)
        }
        return list.toTypedArray()
    }

    private fun setupSettings() {
        var statement: Statement = settingsDB?.createStatement()!!
        statement.queryTimeout = 30

        val channelList = "create table if not exists channels (" +
                "channel text, prefix text DEFAULT '!')"
        statement.executeUpdate(channelList)
        if (getListOfChannels().isEmpty()) { // Set up default Channel
            addChannel("glazedhambot", "!")
        }
    }

    fun connect(channels: Array<String>) {
        channels.forEach {
            val con = DriverManager.getConnection("jdbc:sqlite:${it.toLowerCase()}.db")
            connectionList[it] = con
        }
    }

    fun connect(channel: String) {
        val con = DriverManager.getConnection("jdbc:sqlite:${channel.toLowerCase()}.db")
        connectionList[channel] = con
    }

    private fun setupAllChannels() {
        connectionList.forEach {
            setup(it.value)
        }
    }

    private fun setup(connection: Connection) {
        val statement: Statement = connection.createStatement()!!
        statement.queryTimeout = 30

        val responsesTableSql = "create table if not exists responses (" +
                "command text," +
                "response text)"

        val quotesTableSql = "create table if not exists quotes (" +
                "ID INTEGER PRIMARY KEY, " +
                "quote text, " +
                "subject text, " +
                "timestamp INTEGER)"
        statement.executeUpdate(responsesTableSql)
        statement.executeUpdate(quotesTableSql)
    }

    override fun getPrefixForChannel(channel: String): String {
        val sql = "Select prefix from channels where channel = ?"
        val preparedStatement = settingsDB?.prepareStatement(sql)
        preparedStatement?.setString(1, channel)
        val results = preparedStatement?.executeQuery()
        return if (results?.next()!!) {
            results.getString("prefix")
        } else {
            "!" //Default to "!"
        }
    }

    override fun setPrefixForChannel(channel: String, prefix: String) {
        val sql = "UPDATE channels set prefix = ? where channel = ?"
        val preparedStatement = settingsDB?.prepareStatement(sql)
        preparedStatement?.setString(1, prefix)
        preparedStatement?.setString(2, channel)
        preparedStatement?.executeUpdate()
    }

    override fun findResponse(channel: String, command: String): String {
        val connection = connectionList[channel]
        val sql = "Select response from responses where command = ?"
        val preparedStatement = connection?.prepareStatement(sql)
        preparedStatement?.setString(1, command)
        val resultSet = preparedStatement?.executeQuery()

        return if (resultSet?.next()!!) {
            resultSet.getString("response")
        } else {
            ""
        }
    }

    fun channelExists(channel: String): Boolean? {
        val sql = "Select * from channels WHERE channel = ?"
        val preparedStatement = settingsDB?.prepareStatement(sql)
        preparedStatement?.setString(1, channel)
        val results = preparedStatement?.executeQuery()
        return results?.next()
    }

    override fun addChannel(newChannel: String, prefix: String) {
        val exists = channelExists(newChannel)
        if (exists != null && !exists) {
            val sql = "INSERT INTO channels(channel, prefix) VALUES (?, ?)"
            val preparedStatement = settingsDB?.prepareStatement(sql)
            preparedStatement?.setString(1, newChannel)
            preparedStatement?.setString(2, prefix)
            preparedStatement?.executeUpdate()
            connect(newChannel)
        }
    }

    override fun leaveChannel(channel: String) {
        val sql = "DELETE FROM channels WHERE channel = ?"
        val preparedStatement = settingsDB?.prepareStatement(sql)
        preparedStatement?.setString(1, channel)
        preparedStatement?.executeUpdate()
    }

    override fun setResponse(channel: String, command: String, response: String) {
        val connection = connectionList[channel]
        val exists = findResponse(channel, command)
        val sql = if (exists == "") {
            "INSERT INTO responses(response, command) VALUES (?, ?)"
        } else {
            "UPDATE responses SET response = ? WHERE command = ?"
        }
        val preparedStatement = connection?.prepareStatement(sql)
        preparedStatement?.setString(1, response)
        preparedStatement?.setString(2, command)
        preparedStatement?.executeUpdate()
    }

    override fun removeResponse(channel: String, command: String) {
        val connection = connectionList[channel]
        val sql = "DELETE FROM responses WHERE command = ?"
        val preparedStatement = connection?.prepareStatement(sql)
        preparedStatement?.setString(1, command)
        preparedStatement?.executeUpdate()
    }

    override fun getAllCommandList(channel: String): ArrayList<String> {
        val connection = connectionList[channel]
        val sql = "SELECT command FROM responses"
        val preparedStatement = connection?.prepareStatement(sql)
        val results = preparedStatement?.executeQuery()
        val returnList = ArrayList<String>()
        while(results?.next()!!) returnList.add(results.getString("command"))
        return returnList
    }
}