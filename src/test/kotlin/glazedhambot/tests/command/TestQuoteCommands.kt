package glazedhambot.tests.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import glazedhambot.tests.TestHelpers
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import uno.rebellious.twitchbot.command.QuoteCommands
import uno.rebellious.twitchbot.database.DatabaseDAO
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class TestQuoteCommands {

    lateinit var quoteCommands: QuoteCommands
    lateinit var mockQuotesDAO: DatabaseDAO
    lateinit var mockTwirk: Twirk
    lateinit var mockTwitchUser: TwitchUser
    val channel = "test"
    private val clock = Clock.fixed(Instant.parse("2020-07-11T11:00:00Z"), ZoneId.of("Europe/London"))

    @BeforeEach
    fun setup() {
        mockTwirk = Mockito.mock(Twirk::class.java)
        mockQuotesDAO = Mockito.mock(DatabaseDAO::class.java)
        mockTwitchUser = Mockito.mock(TwitchUser::class.java)

        quoteCommands = QuoteCommands("!", mockTwirk, channel, mockQuotesDAO, clock)
    }

    /*
            commandList.add(quoteListCommand())
        commandList.add(quoteCommand())
        commandList.add(addQuoteCommand())
        commandList.add(editQuoteCommand())
        commandList.add(deleteQuoteCommand())
        commandList.add(undeleteQuoteCommand())
     */


    @Test
    fun testAddQuoteWithDateCommand() {
//        !addquote This is the quote | this is the person | this is the date
        val cmd = "!addquote This is the quote | Uno | 2020-07-01".split(" ", limit = 3)
        val command = quoteCommands.commandList.first { it.command == "addquote" }
        `when`(mockQuotesDAO.addQuoteForChannel(anyString(), TestHelpers.any(), anyString(), anyString())).thenReturn(1)
        command.action(mockTwitchUser, cmd)
        verify(mockQuotesDAO).addQuoteForChannel(channel, LocalDate.parse("2020-07-01"), "Uno", "This is the quote")
        verify(mockTwirk).channelMessage("Added with ID 1 : This is the quote - Uno on 2020-07-01")
    }

    @Test
    fun testAddQuoteWithoutDateCommand() {

//        !addquote This is the quote | this is the person | this is the date
        val cmd = "!addquote This is the quote | Uno  ".trim().split(" ", limit = 3)
        val command = quoteCommands.commandList.first { it.command == "addquote" }
        `when`(
            mockQuotesDAO.addQuoteForChannel(
                channel,
                LocalDate.now(clock),
                "Uno",
                "This is the quote"
            )
        ).thenReturn(1)
        command.action(mockTwitchUser, cmd)
        verify(mockQuotesDAO).addQuoteForChannel(channel, LocalDate.parse("2020-07-11"), "Uno", "This is the quote")
        verify(mockTwirk).channelMessage("Added with ID 1 : This is the quote - Uno on 2020-07-11")
    }
}

