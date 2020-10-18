package glazedhambot.tests.dao

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import uno.rebellious.twitchbot.database.ResponsesDAO
import uno.rebellious.twitchbot.model.Response
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestResponseDAO {
    lateinit var responsesDAO: ResponsesDAO
    private var connectionList: HashMap<String, Connection> = HashMap()
    lateinit var con: Connection
    private val channel = "test"

    @BeforeEach
    fun setupTestDB() {
        con = DriverManager.getConnection("jdbc:sqlite:$channel.db")
        connectionList[channel] = con
        responsesDAO = ResponsesDAO(connectionList)
        responsesDAO.createResponseTable(connectionList[channel]!!)
    }

    @AfterEach
    fun clearDownTestDB() {
        con.close()
        con = DriverManager.getConnection("jdbc:sqlite:$channel.db")
        val dropTableSQL = "drop table IF EXISTS responses"
        con.createStatement()?.execute(dropTableSQL)
        con.close()
    }

    @Test
    fun testDatabaseSetup() {
        val nameList = ArrayList(listOf("responses"))
        val resultList = ArrayList<String>()
        con.createStatement()
            .executeQuery("SELECT name FROM SQLITE_MASTER")?.run {
                while (next()) {
                    resultList.add(getString(1))
                }
            }
        assert(resultList.containsAll(nameList))
    }


    @Test
    fun findResponse() {
        val cmd = "cmd"
        val response = "This is a test response"
        responsesDAO.setResponse(channel, Response(cmd, response))
        val result = responsesDAO.findResponse(channel, Response(cmd))
        assertEquals(response, result.response)
    }

    @Test
    fun findMissingResponse() {
        val cmd = "cmd"
        val response = "This is a test response"
        responsesDAO.setResponse(channel, Response(cmd, response))
        val result = responsesDAO.findResponse(channel, Response("test"))
        assertTrue(result.isBlank())
    }


    @Test
    fun setResponse() {
        val cmd = "cmd"
        val response = "This is a test response"
        responsesDAO.setResponse(channel, Response(cmd, response))
        val result = responsesDAO.findResponse(channel, Response(cmd))
        assertEquals(response, result.response)
    }

    @Test
    fun updateResponse() {
        val cmd = "cmd"
        val response = "This is a test response"
        val responseEdit = "$response edit"
        responsesDAO.setResponse(channel, Response(cmd, response))
        responsesDAO.setResponse(channel, Response(cmd, responseEdit))
        val result = responsesDAO.findResponse(channel, Response(cmd))
        assertEquals(responseEdit, result.response)
    }

    @Test
    fun removeResponse() {
        val cmd = "cmd"
        val response = "This is a test response"
        responsesDAO.setResponse(channel, Response(cmd, response))
        responsesDAO.removeResponse(channel, Response(cmd))
        val result = responsesDAO.findResponse(channel, Response(cmd))
        assertTrue(result.isBlank())
    }

}