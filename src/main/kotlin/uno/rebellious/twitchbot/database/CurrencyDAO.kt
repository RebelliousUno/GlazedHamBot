package uno.rebellious.twitchbot.database

import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.twitchbot.model.CurrencyDetails
import java.sql.Connection
import java.util.*

class CurrencyDAO(private val connectionList: HashMap<String, Connection>) : ICurrency {


    fun setupCurrency(connection: Connection) {
        setupCurrencyTable(connection)
        setupCurrencyAccountTable(connection)
    }

    private fun setupCurrencyAccountTable(connection: Connection) {
        val accountTableSql = """CREATE TABLE if not exists "currencyAccount" (
	"user"	TEXT,
	"currency"	NUMERIC DEFAULT 0,
	PRIMARY KEY("user")
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

    override fun getCurrencyDetailsForChannel(channel: String): CurrencyDetails? {
        val con = connectionList[channel]
        val sql = "select * from currencyDetails"
        val results = con?.createStatement()?.executeQuery(sql)
        return results?.run {
            if (next())
                CurrencyDetails(getString(1), getDouble(2), getDouble(3), getDouble(4))
            else null
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

    override fun updateCurrencyForUsers(channel: String, users: ArrayList<String>, multiplier: Double) {
        /**
         * any users that aren't in the db add them.
         *  getListOfUsers in DB compare to list of users
         * for each user in the list increment points by 1
         */
        val listOfAll = getListOfAccounts(channel)
        val listToAdd = users.filter { !listOfAll.contains(it) }.toCollection(ArrayList())
        if (listToAdd.isNotEmpty()){
            createAccountForUsers(channel, listToAdd)
        }
        if (listOfAll.isNotEmpty()) {
            updateCurrencyValuesForUsers(channel, users, multiplier)
        }
    }

    private fun updateCurrencyValuesForUsers(channel: String, users: ArrayList<String>, multiplier: Double) {
        val con = connectionList[channel]
        val sql = "update currencyAccount set currency = currency+(1*?) where user = ?"
        con?.autoCommit = false
        users.forEach {
            val statement = con?.prepareStatement(sql)
            statement?.setDouble(1, multiplier)
            statement?.setString(2, it)
            statement?.executeUpdate()
        }
        con?.commit()
        con?.autoCommit = true
    }

    private fun createAccountForUsers(channel: String, userList: List<String>) {
        val sql = "insert into currencyAccount(user) values(?)"
        val connection = connectionList[channel]
        connection?.autoCommit = false
        userList.forEach {
            val statement = connection?.prepareStatement(sql)
            statement?.setString(1, it)
            statement?.executeUpdate()
        }
        connection?.commit()
        connection?.autoCommit = true
    }

    private fun getListOfAccounts(channel: String): ArrayList<String> {
        val sql = "select user from currencyAccount"
        val users = connectionList[channel]?.createStatement()?.executeQuery(sql)
        val result = ArrayList<String>()
        while (users?.next() == true) {
            result.add(users.getString(1))
        }
        return result
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
        val sql = "select currency from currencyAccount where user = ?"
        val con = connectionList[channel]
        val results = con?.prepareStatement(sql)?.run {
            setString(1, user.userName)
            executeQuery()
        }
        return if (results?.next() == true) {
            results.getDouble(1)
        } else {
            0.0
        }
    }
}