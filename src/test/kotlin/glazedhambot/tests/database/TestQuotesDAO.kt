package glazedhambot.tests.database

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import uno.rebellious.twitchbot.database.QuotesDAO
import java.sql.Connection
import java.sql.DriverManager
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestQuotesDAO {

    lateinit var quotesDAO: QuotesDAO
    private var connectionList: HashMap<String, Connection> = HashMap()
    lateinit var con: Connection
    private val channel = "test"
    private val clock = Clock.fixed(Instant.parse("2020-07-11T11:00:00Z"), ZoneId.of("UTC"))

    @BeforeEach
    fun setupTestDB() {
        con = DriverManager.getConnection("jdbc:sqlite:$channel.db")
        connectionList[channel] = con
        quotesDAO = QuotesDAO(connectionList, clock)
        quotesDAO.createQuotesTable(connectionList[channel]!!)
    }

    @AfterEach
    fun clearDownTestDB() {
        con.close()
        con = DriverManager.getConnection("jdbc:sqlite:$channel.db")
        val dropTableSQL = "drop table IF EXISTS quotes"
        con.createStatement()?.execute(dropTableSQL)
        con.close()
    }

    @Test
    fun testDatabaseSetup() {
        val nameList = ArrayList(listOf("quotes"))
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
    fun testAddQuoteForChannel() {
        val id = quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "Uno", "This is a test quote")
        assertEquals(1, id)
    }

    @Test
    fun testGetQuoteForChannelById() {
        val id = quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "Uno", "This is a test quote")
        val quote = quotesDAO.getQuoteForChannelById(channel, id)
        val expected = "Quote 1: \"This is a test quote\" - Uno - 11-Jul-2020"
        assertEquals(expected, quote)
    }

    @Test
    fun testGetQuoteForChannelByIdNotFound() {
        quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "Uno", "This is a test quote")
        val quote = quotesDAO.getQuoteForChannelById(channel, 2000)
        val expected = "Quote not found"
        assertEquals(expected, quote)
    }

    @Test
    fun testDelQuoteForChannel() {
        val id = quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "Uno", "This is a test quote")
        quotesDAO.delQuoteForChannel(channel, id)
        val quote = quotesDAO.getQuoteForChannelById(channel, id)
        val expected = "Quote not found"
        assertEquals(expected, quote)
    }

    @Test
    fun testEditQuoteForChannelNewDate() {
        val id = quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "Uno", "This is a test quote")
        //    override fun editQuoteForChannel(channel: String, quoteId: Int, date: LocalDate?, person: String, quote: String) {
        quotesDAO.editQuoteForChannel(
            channel,
            id,
            LocalDate.now(clock).minusDays(1),
            "James",
            "This is an edited test quote"
        )
        val quote = quotesDAO.getQuoteForChannelById(channel, id)

        val expected = "Quote 1: \"This is an edited test quote\" - James - 10-Jul-2020"
        assertEquals(expected, quote)

    }

    @Test
    fun testEditQuoteForChannelSameDate() {
        val id = quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "Uno", "This is a test quote")
        quotesDAO.editQuoteForChannel(channel, id, null, "Uno", "This is an edited test quote")
        val quote = quotesDAO.getQuoteForChannelById(channel, id)
        val expected = "Quote 1: \"This is an edited test quote\" - Uno - 11-Jul-2020"
        assertEquals(expected, quote)

    }

    fun testGetRandomQuoteFOrChannel() {
    }

    @Test
    fun testFindQuoteByAuthor() {
        val id = quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "Uno", "This is a Uno quote")
        val id2 = quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "James", "This is a James quote")
        val unoQuote = quotesDAO.findQuoteByAuthor(channel, "Uno")
        val jamesQuote = quotesDAO.findQuoteByAuthor(channel, "James")
        val unoExpected = "Quote 1: \"This is a Uno quote\" - Uno - 11-Jul-2020"
        val jamesExpected = "Quote 2: \"This is a James quote\" - James - 11-Jul-2020"
        assertEquals(unoExpected, unoQuote)
        assertEquals(jamesExpected, jamesQuote)
    }

    @Test
    fun testFindQuoteByKeyword() {
        quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "Uno", "This is a quote about butts")
        quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "James", "This is a quote about sheep")
        val buttsQuote = quotesDAO.findQuoteByKeyword(channel, "butts")
        val sheepQuote = quotesDAO.findQuoteByKeyword(channel, "sheep")
        val buttsExpected = "Quote 1: \"This is a quote about butts\" - Uno - 11-Jul-2020"
        val sheepExpected = "Quote 2: \"This is a quote about sheep\" - James - 11-Jul-2020"
        assertEquals(buttsExpected, buttsQuote)
        assertEquals(sheepExpected, sheepQuote)
    }

    @Test
    fun testUndeleteQuoteForChannel() {
        val id = quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "Uno", "This is a quote about butts")
        quotesDAO.delQuoteForChannel(channel, id)
        quotesDAO.undeleteQuoteForChannel(channel, id)
        val buttsQuote = quotesDAO.getQuoteForChannelById(channel, id)
        val buttsExpected = "Quote 1: \"This is a quote about butts\" - Uno - 11-Jul-2020"
        assertEquals(buttsExpected, buttsQuote)

    }

    @Test
    fun testGetAllQuotesForChannel() {
        val id = quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "Uno", "This is a quote about butts")
        val id2 = quotesDAO.addQuoteForChannel(channel, LocalDate.now(clock), "James", "This is a quote about sheep")
        val quotes = quotesDAO.getAllQuotesForChannel(channel)
        assertEquals(2, quotes.size)
    }
}