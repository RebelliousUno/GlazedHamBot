package uno.rebellious.twitchbot

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

class DatabaseDAO : IDatabase {

    //private var connection: Connection? = null
    private var settingsDB: Connection? = null
    private var connectionList: HashMap<String, Connection> = HashMap()

    init {
        connectSettings() //Connect to settings DB
        setupSettings() //Set up Settings DB
        var channelList = getListOfChannels()
        println(channelList)
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
                "channel text)"
        statement.executeUpdate(channelList)
        if (getListOfChannels().isEmpty()) { // Set up default Channel
            addChannel("GlazedHamBot")
        }
    }

    fun connect(channels: Array<String>) {
        channels.forEach {
            val con = DriverManager.getConnection("jdbc:sqlite:${it.toLowerCase()}.db")
            connectionList[it] = con
        }

    }

    fun disconnect() {
        connectionList.forEach {
            it.value.close()
        }
    }

    private fun setupAllChannels() {
        connectionList.forEach {
            setup(it.value)
        }
    }

    private fun setup(connection: Connection) {
        var statement: Statement = connection.createStatement()!!
        statement.queryTimeout = 30

        val responsesTableSql = "create table if not exists responses (" +
                "command text," +
                "response text)"
        statement.executeUpdate(responsesTableSql)
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

    override fun addChannel(newChannel: String) {
        val exists = channelExists(newChannel)
        if (exists != null && !exists) {
            val sql = "INSERT INTO channels(channel) VALUES (?)"
            val preparedStatement = settingsDB?.prepareStatement(sql)
            preparedStatement?.setString(1, newChannel)
            preparedStatement?.executeUpdate()
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
        while(results?.next()!!) returnList.add("!${results.getString("command")}")
        return returnList
    }
}