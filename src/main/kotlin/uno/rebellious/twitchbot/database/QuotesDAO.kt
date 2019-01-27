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

    companion object {
        const val QUOTE_NOT_FOUND = "Quote not found"
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

    override fun undeleteQuoteForChannel(channel: String, quoteId: Int) {
        setDeleteStatusForQuote(channel, quoteId, false)
    }

    override fun delQuoteForChannel(channel: String, quoteId: Int) {
        setDeleteStatusForQuote(channel, quoteId, true)
    }

    private fun setDeleteStatusForQuote(channel: String, quoteId: Int, deleted: Boolean) {
        val sql = "update quotes SET deleted = ? where ID = ?"
        connectionList[channel]?.prepareStatement(sql)?.apply {
            setBoolean(1, deleted)
            setInt(2, quoteId)
            executeUpdate()
        }
    }

    override fun editQuoteForChannel(channel: String, quoteId: Int, date: LocalDate?, person: String, quote: String) {
        val sqls = arrayListOf<String>()
        var idCount = 1
        var timestampId = 0
        var subjectId = 0
        var newQuoteId = 0
        var timestamp: Timestamp? = null
        if (date != null) {
            timestamp = Timestamp.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant())
            sqls.add("timestamp = ?")
            timestampId = idCount
            idCount ++
        }
        if (!person.isBlank()) {
            sqls.add("subject = ?")
            subjectId = idCount
            idCount ++
        }
        if (!quote.isBlank()) {
            sqls.add("quote = ?")
            newQuoteId = idCount
            idCount ++
        }

        val sql = "UPDATE quotes SET ${sqls.joinToString(", ")} WHERE ID = ?"

        if (idCount > 1 ) connectionList[channel]?.prepareStatement(sql)?.apply {
            if (timestampId > 0 && timestamp != null) setTimestamp(timestampId, timestamp)
            if (subjectId > 0) setString(subjectId, person)
            if (newQuoteId > 0) setString(newQuoteId, quote)
            setInt(idCount, quoteId)
            executeUpdate()
        }
    }

    override fun getQuoteForChannelById(channel: String, quoteId: Int): String {
        val sql = "SELECT * from quotes where ID = ? AND deleted = ?"
        val statement = connectionList[channel]?.prepareStatement(sql)?.apply {
            setInt(1, quoteId)
            setBoolean(2, false)
        }
        return getQuotesFromStatement(statement)
    }

    override fun getRandomQuoteForChannel(channel: String): String {
        val sql = "SELECT * from quotes where deleted = ? ORDER BY Random() LIMIT 1"
        val statement = connectionList[channel]?.prepareStatement(sql)?.apply { setBoolean(1, false)
            getQuotesFromStatement(this)
        }
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
                QUOTE_NOT_FOUND
            }
        } ?: QUOTE_NOT_FOUND

    }

    override fun findQuoteByAuthor(channel: String, author: String): String {
        val sql = "SELECT * from quotes where deleted = ? AND instr(subject,?) ORDER BY Random() LIMIT 1"
        val statement = connectionList[channel]?.prepareStatement(sql)?.apply {
            setBoolean(1, false)
            setString(2, author)
        }
        return getQuotesFromStatement(statement)
    }

    override fun findQuoteByKeyword(channel: String, keyword: String): String {
        val sql = "SELECT * from quotes where deleted = ? AND instr(quote, ?) ORDER BY Random() LIMIT 1"
        val statement = connectionList[channel]?.prepareStatement(sql)?.apply {
            setBoolean(1, false)
            setString(2, keyword)
        }
        return getQuotesFromStatement(statement)
    }

    fun createQuotesTable(connection: Connection) {
        val quotesTableSql = "create table if not exists quotes (" +
                "ID INTEGER PRIMARY KEY, " +
                "quote text, " +
                "subject text, " +
                "timestamp INTEGER, " +
                "deleted INTEGER DEFAULT 0)"
        connection.createStatement().apply {
            executeUpdate(quotesTableSql)
        }
    }

}
