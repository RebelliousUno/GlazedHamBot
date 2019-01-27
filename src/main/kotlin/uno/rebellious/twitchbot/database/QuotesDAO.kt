package uno.rebellious.twitchbot.database

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

internal class QuotesDAO(private val connectionList: HashMap<String, Connection>) : IQuotes {

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

    fun createQuotesTable(connection: Connection) {
        val quotesTableSql = "create table if not exists quotes (" +
                "ID INTEGER PRIMARY KEY, " +
                "quote text, " +
                "subject text, " +
                "timestamp INTEGER)"
        connection.createStatement().apply {
            executeUpdate(quotesTableSql)
        }
    }

}
