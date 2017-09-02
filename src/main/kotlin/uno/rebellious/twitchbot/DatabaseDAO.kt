package uno.rebellious.twitchbot

import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

class DatabaseDAO : IDatabase {

    private var connection: Connection? = null

    init {
        connect()
        setup()
    }

    fun connect() {
        connection = DriverManager.getConnection("jdbc:sqlite:sample.db")
    }

    fun disconnect() {
        connection?.close()
    }

    private fun setup() {
        var statement: Statement = connection?.createStatement()!!
        statement.queryTimeout = 30

        val responsesTableSql = "create table if not exists responses (" +
                "command text," +
                "response text)"
        statement.executeUpdate(responsesTableSql)
    }

    override fun findResponse(command: String): String {
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

    override fun setResponse(command: String, response: String) {
        val exists = findResponse(command)
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

    override fun removeResponse(command: String) {
        val sql = "DELETE FROM responses WHERE command = ?"
        val preparedStatement = connection?.prepareStatement(sql)
        preparedStatement?.setString(1, command)
        preparedStatement?.executeUpdate()
    }

    override fun getAllCommandList(): ArrayList<String> {
        val sql = "SELECT command FROM responses"
        val preparedStatement = connection?.prepareStatement(sql)
        val results = preparedStatement?.executeQuery()
        val returnList = ArrayList<String>()
        while(results?.next()!!) returnList.add("!${results.getString("command")}")
        return returnList
    }
}