package glazedhambot.tests.dao


import org.junit.jupiter.api.*
import uno.rebellious.twitchbot.database.CountersDAO
import uno.rebellious.twitchbot.model.Counter
import java.sql.Connection
import java.sql.DriverManager
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestCountersDAO {


    lateinit var countersDAO: CountersDAO
    private var connectionList: HashMap<String, Connection> = HashMap()
    lateinit var con: Connection
    private val channel = "test"

    @BeforeEach
    fun setupTestDB() {
        con = DriverManager.getConnection("jdbc:sqlite:$channel.db")
        connectionList[channel] = con
        countersDAO = CountersDAO(connectionList)
        countersDAO.setupCounters(connectionList[channel]!!)
    }

    @AfterEach
    fun clearDownTestDB() {
        con.close()
        con = DriverManager.getConnection("jdbc:sqlite:$channel.db")
        val dropTableSQL = "drop table IF EXISTS counters"
        con.createStatement()?.execute(dropTableSQL)
        con.close()
    }

    @Test
    fun testDatabaseSetup() {
        val nameList = ArrayList(listOf("counters"))
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
    fun testAddCounter() {
        val counter = Counter(command = "test", plural = "plural", singular = "single")
        countersDAO.createCounterForChannel(channel, counter)
        val result = countersDAO.getCounterForChannel(channel, Counter("test"))
        assertEquals(counter.command, result.command)
        assertEquals(counter.singular, result.singular)
        assertEquals(counter.plural, result.plural)
        assertEquals(0, result.total)
        assertEquals(0, result.today)
    }

    @Test
    fun testDeleteCounter() {
        val counter = Counter(command = "test", plural = "plural", singular = "single")
        countersDAO.createCounterForChannel(channel, counter)
        val r1 = countersDAO.getCounterForChannel(channel, counter)
        countersDAO.removeCounterForChannel(channel, counter)
        val r2 = countersDAO.getCounterForChannel(channel, counter)
        assertFalse(r1.isEmpty())
        assertTrue(r2.isEmpty())
    }

    @Test
    fun testIncrementCounter() {
        val counter = Counter(command = "test", plural = "plural", singular = "single")
        countersDAO.createCounterForChannel(channel, counter)
        val r1 = countersDAO.getCounterForChannel(channel, counter)
        countersDAO.incrementCounterForChannel(channel, counter)
        val r2 = countersDAO.getCounterForChannel(channel, counter)
        countersDAO.incrementCounterForChannel(channel, counter, 2)
        val r3 = countersDAO.getCounterForChannel(channel, counter)

        assertEquals(0, r1.today)
        assertEquals(0, r1.total)
        assertEquals(1, r2.today)
        assertEquals(1, r2.total)
        assertEquals(3, r3.today)
        assertEquals(3, r3.total)
    }

    @Test
    fun testDecrementCounter() {
        val counter = Counter(command = "test", plural = "plural", singular = "single")
        countersDAO.createCounterForChannel(channel, counter)
        val r1 = countersDAO.getCounterForChannel(channel, counter)
        countersDAO.incrementCounterForChannel(channel, counter, 5)
        val r2 = countersDAO.getCounterForChannel(channel, counter)
        countersDAO.incrementCounterForChannel(channel, counter, -1)
        val r3 = countersDAO.getCounterForChannel(channel, counter)

        assertEquals(0, r1.today)
        assertEquals(0, r1.total)
        assertEquals(5, r2.today)
        assertEquals(5, r2.total)
        assertEquals(4, r3.today)
        assertEquals(4, r3.total)
    }

    @Test
    fun resetCounters() {
        val counter = Counter(command = "test", plural = "plural", singular = "single")
        countersDAO.createCounterForChannel(channel, counter)
        countersDAO.incrementCounterForChannel(channel, counter, 5)
        val r1 = countersDAO.getCounterForChannel(channel, counter)
        countersDAO.resetTodaysCounterForChannel(channel, counter)
        val r2 = countersDAO.getCounterForChannel(channel, counter)
        countersDAO.incrementCounterForChannel(channel, counter, 5)
        val r3 = countersDAO.getCounterForChannel(channel, counter)
        assertEquals(5, r1.today)
        assertEquals(5, r1.total)
        assertEquals(0, r2.today)
        assertEquals(5, r2.total)
        assertEquals(5, r3.today)
        assertEquals(10, r3.total)
    }

    @Test
    fun testGetCounter() {
        val counter = Counter(command = "test", plural = "plural", singular = "single")
        countersDAO.createCounterForChannel(channel, counter)
        val r1 = countersDAO.getCounterForChannel(channel, counter)
        assertEquals(counter.command, r1.command)
        assertEquals(counter.singular, r1.singular)
        assertEquals(counter.plural, r1.plural)
        assertEquals(0, r1.total)
        assertEquals(0, r1.today)

    }

    @Test
    fun testGetAllCounter() {
        val counter1 = Counter(command = "test1", plural = "plural1", singular = "single1")
        countersDAO.createCounterForChannel(channel, counter1)
        val counter2 = Counter(command = "test2", plural = "plural2", singular = "single2")
        countersDAO.createCounterForChannel(channel, counter2)
        val counter3 = Counter(command = "test3", plural = "plural3", singular = "single3")
        countersDAO.createCounterForChannel(channel, counter3)
        val stream = Counter(command = "stream", plural = "streams", singular = "stream")
        countersDAO.createCounterForChannel(channel, stream)

        val result = countersDAO.showCountersForChannel(channel, false)
        assertEquals(3, result.size)
        assertTrue(result.containsAll(listOf(counter1, counter2, counter3)))
        val withStream = countersDAO.showCountersForChannel(channel, true)
        assertEquals(4, withStream.size)
        assertTrue(withStream.containsAll(listOf(counter1, counter2, counter3, stream)))
    }
}