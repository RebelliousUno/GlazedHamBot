package uno.rebellious.twitchbot.database

import java.sql.Connection
import java.util.*

class CountersDAO(private val connectionList: HashMap<String, Connection>) : ICounters {

    fun setupCounters(connection: Connection) {
        val counterTableSQL =
            "create table if not exists counters (command text, today int, total int, singular text, plural text)"
        connection.createStatement()?.apply {
            queryTimeout = 30
            executeUpdate(counterTableSQL)
        }
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

    override fun showCountersForChannel(channel: String, includeStream: Boolean): List<String> {
        val sql =
            if (includeStream) "SELECT * from counters" else "SELECT * from counters where command NOT LIKE 'stream'"
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
}