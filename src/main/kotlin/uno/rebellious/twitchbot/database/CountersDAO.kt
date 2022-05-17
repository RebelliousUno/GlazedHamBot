package uno.rebellious.twitchbot.database

import uno.rebellious.twitchbot.model.Counter
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

class CountersDAO(private val connectionList: HashMap<String, Connection>) : ICounters {

    constructor(channel: String) : this(HashMap()) {
        connectionList[channel] = DriverManager.getConnection("jdbc:sqlite:${channel.lowercase(Locale.getDefault())}.db")
    }

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
        counter: Counter
    ) {
        val sql = "INSERT INTO counters(command, today, total, singular, plural) VALUES (?, ?, ?, ?, ?)"
        if (!counter.isValidCreateCounter()) return
        connectionList[channel]?.prepareStatement(sql)?.apply {
            setString(1, counter.command)
            setInt(2, 0)
            setInt(3, 0)
            setString(4, counter.singular)
            setString(5, counter.plural)
            executeUpdate()
        }
    }

    override fun showCountersForChannel(channel: String, includeStream: Boolean): List<Counter> {
        val sql =
            if (includeStream) "SELECT * from counters" else "SELECT * from counters where command NOT LIKE 'stream'"
        val counters = ArrayList<Counter>()
        connectionList[channel]?.createStatement()?.run {
            executeQuery(sql)
        }?.run {
            while (next()) {
                counters += Counter(
                    command = getString("command"),
                    singular = getString("singular"),
                    plural = getString("plural"),
                    today = getInt("today"),
                    total = getInt("total")
                )
            }
        }
        return counters
    }

    override fun removeCounterForChannel(channel: String, counter: Counter) {
        val sql = "DELETE FROM counters WHERE command like ?"
        connectionList[channel]?.prepareStatement(sql)?.apply {
            setString(1, counter.command)
            executeUpdate()
        }
    }

    override fun incrementCounterForChannel(channel: String, counter: Counter, by: Int) {
        val todaySql = "UPDATE counters SET today = today + ?, total = total + ? WHERE command like ?"
        connectionList[channel]
        connectionList[channel]?.prepareStatement(todaySql)?.apply {
            setInt(1, by)
            setInt(2, by)
            setString(3, counter.command)
            executeUpdate()
        }
    }

    override fun getCounterForChannel(channel: String, counter: Counter, consistentRead: Boolean): Counter {
        val sql = "SELECT * FROM counters where command like ?"
        connectionList[channel]?.prepareStatement(sql)?.run {
            setString(1, counter.command)
            executeQuery()
        }?.run {
            if (next()) {
                return Counter(
                    command = counter.command,
                    today = getInt("today"),
                    total = getInt("total"),
                    singular = getString("singular"),
                    plural = getString("plural")
                )
            }
        }
        return Counter("")
    }

    override fun resetTodaysCounterForChannel(channel: String, counter: Counter) {
        val sql = "UPDATE counters SET today = 0 where command like ?"
        connectionList[channel]?.prepareStatement(sql)?.apply {
            setString(1, counter.command)
            executeUpdate()
        }
    }
}