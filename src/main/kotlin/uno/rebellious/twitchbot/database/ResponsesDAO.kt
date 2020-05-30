package uno.rebellious.twitchbot.database

import java.sql.Connection
import java.util.*

class ResponsesDAO(private val connectionList: HashMap<String, Connection>) : IResponse {

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

    fun createResponseTable(connection: Connection) {
        val responsesTableSql = "create table if not exists responses (" +
                "command text," +
                "response text)"
        connection.createStatement().apply {
            executeUpdate(responsesTableSql)
        }


    }
}
