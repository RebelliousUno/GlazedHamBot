package glazedhambot.tests.commands

import com.gikk.twirk.Twirk
import com.gikk.twirk.TwirkBuilder
import com.gikk.twirk.types.users.TwitchUser
import com.gikk.twirk.types.users.TwitchUserBuilder
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.*
import uno.rebellious.twitchbot.command.CounterCommands
import uno.rebellious.twitchbot.database.DatabaseDAO
import uno.rebellious.twitchbot.model.Counter
import kotlin.test.expect

/*
        commandList.add(createCounterCommand())
        commandList.add(addCountCommand())
        commandList.add(removeCountCommand())
        commandList.add(resetCountCommand())
        commandList.add(listCountersCommand())
        commandList.add(deleteCounterCommand())
        commandList.add(resetAllCountersCommand())
        commandList.add(meanCounterListCommand())
        commandList.add(meanCounterCommand())
 */

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.Alphanumeric::class)
class TestCounterCommands {

    lateinit var counterCommands: CounterCommands

    lateinit var mockCountersDAO: DatabaseDAO
    lateinit var mockTwirk: Twirk
    val channel = "test"
    @BeforeEach
    fun setup() {
        mockTwirk = mock(Twirk::class.java)
        mockCountersDAO = mock(DatabaseDAO::class.java)
        counterCommands = CounterCommands("!", mockTwirk, channel, mockCountersDAO)
    }

    @Test
    fun testContainsAllCommands() {

        val commandList = listOf("createcounter","addcount", "removecount", "counterlist", "resetcount", "deletecounter", "meancounterlist", "resetallcounters", "mean")
        assertTrue(commandList.containsAll(counterCommands.commandList.map { it.command }))

    }

    @Test
    fun testCreateCounterCommand() {
        val command = counterCommands.commandList.first { it.command == "createcounter" }
        val twitchUser = mock(TwitchUser::class.java)
        val commandString = "!createcounter fall fall falls".split(" ", limit = 3)
        val counter = Counter(command = "fall", singular = "fall", plural = "falls")
        command.action(twitchUser, commandString)
        verify(mockCountersDAO, times(1)).createCounterForChannel(channel, counter)
    }

    @ParameterizedTest
    @ValueSource(strings = ["!createcounter fall fall", "!createcounter fall", "!createcounter"])
    fun testCreateCounterHelpCommand(cmd: String) {
        val command = counterCommands.commandList.first { it.command == "createcounter" }
        val twitchUser = mock(TwitchUser::class.java)
        val commandString = cmd.split(" ", limit = 3)
        val counter = Counter(command = "fall", singular = "fall", plural = "falls")
        command.action(twitchUser, commandString)
        verify(mockCountersDAO, times(0)).createCounterForChannel(channel, counter)
        verify(mockTwirk, times(1)).channelMessage(command.helpString)
    }
}