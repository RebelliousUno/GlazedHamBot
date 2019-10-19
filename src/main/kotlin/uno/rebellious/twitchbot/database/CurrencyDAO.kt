package uno.rebellious.twitchbot.database

import com.gikk.twirk.types.users.TwitchUser
import java.sql.Connection
import java.util.*

class CurrencyDAO(private val connectionList: HashMap<String, Connection>) : ICurrency {

    fun setupCurrency(connection: Connection) {
        setupCurrencyTable(connection)
        setupCurrencyAccountTable(connection)
    }

    private fun setupCurrencyAccountTable(connection: Connection) {
        val accountTableSql = """CREATE TABLE if not exists "currencyAccount" (
	        "user"	    TEXT,
	        "currency"	NUMERIC DEFAULT 0
        )
        """.trimIndent()
        connection.createStatement().apply {
            queryTimeout = 30
            executeUpdate(accountTableSql)
        }
    }

    private fun setupCurrencyTable(connection: Connection) {
        val currencyTableSql = """CREATE TABLE if not exists "currencyDetails" (
            "name"	TEXT,
            "subMult"	NUMERIC DEFAULT 1.0,
            "vipMult"	NUMERIC DEFAULT 1.0,
            "modMult"	NUMERIC DEFAULT 1.0
        )""".trimIndent()
        connection.createStatement()?.apply {
            queryTimeout = 30
            executeUpdate(currencyTableSql)
        }

        val count = connection.createStatement().executeQuery("select count(*) from currencyDetails").getInt(1)
        if (count == 0) {
            val firstLine = "insert into currencyDetails(name) values ('Points')"
            connection.createStatement().executeUpdate(firstLine)
        }
    }


    override fun startCurrencyGame(channel: String, user: TwitchUser) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun joinCurrencyGame(channel: String, user: TwitchUser) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getUsersInCurrencyGame(channel: String, user: TwitchUser): List<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun updateCurrencyForUsers(channel: String, users: List<String>, multiplier: Double) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCurrencyName(channel: String): String {
        val sql = "select name from currencyDetails"
        val con = connectionList[channel]
        return con?.createStatement()?.executeQuery(sql)?.getString(1) ?: ""
    }

    override fun setCurrencyName(channel: String, currency: String) {
        val sql = "update currencyDetails SET name = ?"
        val con = connectionList[channel]
        con?.prepareStatement(sql)?.apply {
            setString(1, currency)
            executeUpdate()
        }
    }

    override fun getCurrencyForUser(channel: String, user: TwitchUser): Double {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}