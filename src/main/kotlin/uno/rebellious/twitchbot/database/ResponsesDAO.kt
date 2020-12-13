package uno.rebellious.twitchbot.database

import uno.rebellious.twitchbot.model.Response
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

class ResponsesDAO(private val connectionList: HashMap<String, Connection>) : IResponse {

    constructor(channel: String) : this(HashMap()) {
        connectionList[channel] = DriverManager.getConnection("jdbc:sqlite:${channel.toLowerCase()}.db")
    }

    override fun getAllCommandList(channel: String): ArrayList<String> {
        val connection = connectionList[channel]
        val sql = "SELECT command FROM responses"
        val returnList = ArrayList<String>()
        connection?.prepareStatement(sql)?.run {
            executeQuery()
        }?.apply {
            while (next()) returnList.add(getString("command"))
        }
        return returnList
    }

    override fun findResponse(channel: String, command: Response): Response {
        val connection = connectionList[channel]
        val sql = "Select response from responses where command = ?"
        return connection?.prepareStatement(sql)?.run {
            setString(1, command.command)
            executeQuery()
        }?.run {
            if (next()) {
                Response(command.command, getString("response"))
            } else {
                Response("")
            }
        } ?: Response("")
    }

    override fun setResponse(channel: String, response: Response) {
        val connection = connectionList[channel]
        val exists = findResponse(channel, response)
        val sql = if (exists.isBlank()) {
            "INSERT INTO responses(response, command) VALUES (?, ?)"
        } else {
            "UPDATE responses SET response = ? WHERE command = ?"
        }
        connection?.prepareStatement(sql)?.apply {
            setString(1, response.response)
            setString(2, response.command)
            executeUpdate()
        }
    }

    override fun removeResponse(channel: String, command: Response) {
        val connection = connectionList[channel]
        val sql = "DELETE FROM responses WHERE command = ?"
        connection?.prepareStatement(sql)?.apply {
            setString(1, command.command)
            executeUpdate()
        }
    }

    fun createResponseTable(connection: Connection) {
        val responsesTableSql = "create table if not exists responses (" +
                "command text," +
                "response text)"
        connection.createStatement().apply {
            executeUpdate(responsesTableSql)
        }
    }
}
