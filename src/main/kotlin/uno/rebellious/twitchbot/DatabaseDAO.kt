package uno.rebellious.twitchbot

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

class DatabaseDAO : IDatabase {
    override fun createCounterForChannel(
        channel: String,
        counter: String,
        responseSingular: String,
        responsePlural: String
    ) {
        val sql = "INSERT INTO counters(command, today, total, singular, plural) VALUES (?, ?, ?, ?, ?)"
        val statement = connectionList[channel]?.prepareStatement(sql)
        statement?.setString(1, counter)
        statement?.setInt(2, 0)
        statement?.setInt(3, 0)
        statement?.setString(4, responseSingular)
        statement?.setString(5, responsePlural)
        statement?.executeUpdate()
    }

    override fun showCountersForChannel(channel: String): List<String> {
        val sql = "SELECT * from counters"
        val statement = connectionList[channel]?.createStatement()
        val results = statement?.executeQuery(sql)
        val counters = ArrayList<String>()
        results?.let {
            while (it.next()) {
                counters.add("${it.getString("command")}: ${it.getString("today")}/${it.getString("total")}")
            }
        }
        return counters
    }

    override fun removeCounterForChannel(channel: String, counter: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun incrementCounterForChannel(channel: String, counter: String, by: Int) {
        val todaySql = "UPDATE counters SET today = today + ?, total = total + ? WHERE command like ?"
        connectionList[channel]
        val statement1 = connectionList[channel]?.prepareStatement(todaySql)
        statement1?.setInt(1, by)
        statement1?.setInt(2, by)
        statement1?.setString(3, counter)
        statement1?.executeUpdate()
    }

    override fun getCounterForChannel(channel: String, counter: String): String {
        val sql = "SELECT * FROM counters where command like ?"
        val statement = connectionList[channel]?.prepareStatement(sql)
        statement?.setString(1, counter)
        val resultSet = statement?.executeQuery()
        resultSet?.let {
            if (it.next()) {
                val today = it.getInt("today")
                val total = it.getInt("total")
                val singular = it.getString("singular")
                val plural = it.getString("plural")
                return "There ${if (today == 1) "has" else "have"} been $today ${if (today == 1) singular else plural} today. Total $plural: $total"
            }
        }
        return ""
    }

    override fun resetTodaysCounterForChannel(channel: String, counter: String) {
        val sql = "UPDATE counters SET today = 0 where command like ?"
        val statement = connectionList[channel]?.prepareStatement(sql)
        statement?.setString(1, counter)
        statement?.executeUpdate()
    }

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

    private fun connectSettings() {
        settingsDB = DriverManager.getConnection("jdbc:sqlite:settings.db")
    }

    fun getListOfChannels(): Array<String> {
        val statement = settingsDB?.createStatement()
        statement?.queryTimeout = 30
        val channelSelect = "select * from channels"
        val results = statement?.executeQuery(channelSelect)
        val list = ArrayList<String>()
        results?.let {
            while (it.next()) {
                val channel = it.getString("channel")
                list.add(channel)
            }
        }
        return list.toTypedArray()
    }

    private fun setupSettings() {
        val statement = settingsDB?.createStatement()
        statement?.queryTimeout = 30

        val channelList = "create table if not exists channels (" +
                "channel text, prefix text DEFAULT '!')"
        statement?.executeUpdate(channelList)
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

        val statement = connection.createStatement()!!
        statement.queryTimeout = 30

        val counterTableSQL = "create table if not exists counters (command text, today int, total int, singular text, plural text)"
        statement.executeUpdate(counterTableSQL)
    }

    private fun setup(connection: Connection) {
        val statement = connection.createStatement()!!
        statement.queryTimeout = 30

        val responsesTableSql = "create table if not exists responses (" +
                "command text," +
                "response text)"
        statement.executeUpdate(responsesTableSql)
    }

    override fun getPrefixForChannel(channel: String): String {
        val sql = "Select prefix from channels where channel = ?"
        val preparedStatement = settingsDB?.prepareStatement(sql)
        preparedStatement?.setString(1, channel)
        val results = preparedStatement?.executeQuery()

        return results?.let {
            if (it.next()) {
                it.getString("prefix")
            } else {
                "!" //Default to "!"
            }
        } ?: "!"
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

    private fun channelExists(channel: String): Boolean? {
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