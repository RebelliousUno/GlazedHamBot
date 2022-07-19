package glazedhambot.tests.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import uno.rebellious.twitchbot.command.CounterCommands
import uno.rebellious.twitchbot.database.DatabaseDAO
import uno.rebellious.twitchbot.model.Counter


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestCounterCommands {

    lateinit var counterCommands: CounterCommands
    lateinit var mockCountersDAO: DatabaseDAO
    lateinit var mockTwirk: Twirk
    lateinit var mockTwitchUser: TwitchUser
    val channel = "test"

    @BeforeEach
    fun setup() {
        mockTwirk = mock(Twirk::class.java)
        mockCountersDAO = mock(DatabaseDAO::class.java)
        mockTwitchUser = mock(TwitchUser::class.java)

        counterCommands = CounterCommands("!", mockTwirk, channel, mockCountersDAO)
    }

    @Test
    fun testContainsAllCommands() {
        val commandList = listOf(
            "createcounter",
            "addcount",
            "removecount",
            "counterlist",
            "resetcount",
            "deletecounter",
            "meancounterlist",
            "resetallcounters",
            "mean"
        )
        assertTrue(commandList.containsAll(counterCommands.commandList.map { it.command }))
    }

    @Test
    fun testResetAllCountersCommand() {
        val command = counterCommands.commandList.first { it.command == "resetallcounters" }
        val commandString = "!resetallcounters".split(" ", limit = 3)
        val expectedCounterList = listOf(
            Counter("fall", "fall", "falls", 0, 5),
            Counter("test", "test", "tests", 0, 3),
            Counter("break", "break", "breaks", 0, 20),
            Counter("dave", "dave", "daves", 0, 10)
        )
        `when`(mockCountersDAO.showCountersForChannel(anyString(), eq(true))).thenReturn(expectedCounterList)
        command.action(mockTwitchUser, commandString)
        expectedCounterList.forEach {
            verify(mockCountersDAO).resetTodaysCounterForChannel(channel, it)
        }
    }

    @Test
    fun testDeleteValidCounterCommand() {
        val command = counterCommands.commandList.first { it.command == "deletecounter" }
        val commandString = "!deletecounter falls".split(" ", limit = 3)
        val counter = Counter("falls")
        command.action(mockTwitchUser, commandString)
        verify(mockCountersDAO).removeCounterForChannel(channel, counter)
    }

    @Test
    fun testDeleteInvalidCounterCommand() {
        val command = counterCommands.commandList.first { it.command == "deletecounter" }
        val commandString = "!deletecounter".split(" ", limit = 3)
        command.action(mockTwitchUser, commandString)
        verify(mockTwirk).channelMessage(command.helpString(emptyList()))
    }


    @Test
    fun testMeanCounterCommand() {
        val command = counterCommands.commandList.first { it.command == "mean" }
        val commandString = "!mean breaks".split(" ", limit = 3)
        val breaks = Counter("breaks", "break", "breaks", 0, 444)
        val streams = Counter("stream", "stream", "streams", 0, 99)
        `when`(mockCountersDAO.showCountersForChannel(anyString(), eq(true))).thenReturn(listOf(breaks, streams))
        command.action(mockTwitchUser, commandString)
        val expectedString = "Mean breaks per stream (444/99) - 4.48"
        verify(mockTwirk, times(1)).channelMessage(expectedString)
    }

    @Test
    fun testMeanInvalidCounterCommand() {
        val command = counterCommands.commandList.first { it.command == "mean" }
        val commandString = "!mean test".split(" ", limit = 3)
        val breaks = Counter("breaks", "break", "breaks", 0, 444)
        val streams = Counter("stream", "stream", "streams", 0, 99)
        `when`(mockCountersDAO.showCountersForChannel(anyString(), eq(true))).thenReturn(listOf(breaks, streams))
        command.action(mockTwitchUser, commandString)
        verify(mockTwirk, times(0)).channelMessage(anyString())
    }


    @Test
    fun testMeanCounterListCommand() {
        val command = counterCommands.commandList.first { it.command == "meancounterlist" }
        val commandString = "!meancounterlist".split(" ", limit = 3)
        val expectedCounterList = listOf(
            Counter("fall", "fall", "falls", 0, 5),
            Counter("test", "test", "tests", 0, 3),
            Counter("break", "break", "breaks", 10, 20),
            Counter("stream", "stream", "streams", 1, 10)
        )
        `when`(mockCountersDAO.showCountersForChannel(anyString(), eq(true))).thenReturn(expectedCounterList)
        command.action(mockTwitchUser, commandString)
        verify(mockTwirk, times(1)).channelMessage("Per Stream (10) - [fall: 0.50, test: 0.30, break: 2.00]")
    }

    @Test
    fun testListCountersCommand() {
        val command = counterCommands.commandList.first { it.command == "counterlist" }
        val commandString = "!counterlist".split(" ", limit = 3)
        val expectedCounterList = listOf(
            Counter("fall", "fall", "falls", 0, 5),
            Counter("test", "test", "tests", 0, 3),
            Counter("break", "break", "breaks", 10, 20)
        )
        `when`(mockCountersDAO.showCountersForChannel(anyString(), eq(false))).thenReturn(expectedCounterList)
        command.action(mockTwitchUser, commandString)
        verify(mockTwirk, times(1)).channelMessage("[fall: 0/5, test: 0/3, break: 10/20]")
    }

    @Test
    fun testResetCountCommand() {
        val command = counterCommands.commandList.first { it.command == "resetcount" }
        val commndString = "!resetcount falls".split(" ", limit = 3)
        val counter = Counter("falls")
        command.action(mockTwitchUser, commndString)
        verify(mockCountersDAO, times(1)).resetTodaysCounterForChannel(channel, counter)
    }

    @Test
    fun testResetCountHelpCommand() {
        val command = counterCommands.commandList.first { it.command == "resetcount" }
        val commndString = "!resetcount".split(" ", limit = 3)
        val counter = Counter("falls")
        command.action(mockTwitchUser, commndString)
        verify(mockCountersDAO, times(0)).resetTodaysCounterForChannel(channel, counter)
        verify(mockTwirk, times(1)).channelMessage(command.helpString(emptyList()))
    }


    @Test
    fun testAddOneCount() {
        val command = counterCommands.commandList.first { it.command == "addcount" }
        val commandString = "!addcount falls".split(" ", limit = 3)
        val counter = Counter("falls")
        val expectedCounter = Counter("falls", "fall", "falls", 1, 1)
        `when`(mockCountersDAO.getCounterForChannel(channel, counter, true)).thenReturn(expectedCounter)

        command.action(mockTwitchUser, commandString)

        verify(mockCountersDAO, times(1)).incrementCounterForChannel(channel, counter, 1)
        verify(mockTwirk, times(1)).channelMessage(expectedCounter.outputString)
    }

    @Test
    fun testAddMultipleCount() {
        val command = counterCommands.commandList.first { it.command == "addcount" }
        val commandString = "!addcount falls 5".split(" ", limit = 3)
        val counter = Counter("falls")
        val expectedCounter = Counter("falls", "fall", "falls", 5, 5)
        `when`(mockCountersDAO.getCounterForChannel(channel, counter, true)).thenReturn(expectedCounter)

        command.action(mockTwitchUser, commandString)

        verify(mockCountersDAO, times(1)).incrementCounterForChannel(channel, counter, 5)
        verify(mockTwirk, times(1)).channelMessage(expectedCounter.outputString)
    }

    @Test
    fun testAddToInvalidCounter() {
        val command = counterCommands.commandList.first { it.command == "addcount" }
        val commandString = "!addcount falls 5".split(" ", limit = 3)
        val counter = Counter("falls")
        val expectedCounter = Counter("")
        `when`(mockCountersDAO.getCounterForChannel(channel, counter, true)).thenReturn(expectedCounter)

        command.action(mockTwitchUser, commandString)

        verify(mockCountersDAO, times(1)).incrementCounterForChannel(channel, counter, 5)
        verify(mockTwirk, times(0)).channelMessage(anyString())
    }

    @ParameterizedTest
    @ValueSource(strings = ["burb", "-10"])
    fun testAddInvalidNumber(count: String) {
        val command = counterCommands.commandList.first { it.command == "addcount" }
        val commandString = "!addcount falls $count".split(" ", limit = 3)
        val counter = Counter("falls")
        val expectedCounter = Counter("falls", "fall", "falls", 5, 5)
        `when`(mockCountersDAO.getCounterForChannel(channel, counter, true)).thenReturn(expectedCounter)

        command.action(mockTwitchUser, commandString)

        verify(mockCountersDAO, times(0)).incrementCounterForChannel(channel, counter, 5)
        verify(mockTwirk, times(1)).channelMessage("$count is not a valid number to increment by")
    }

    @Test
    fun testRemoveOneCount() {
        val command = counterCommands.commandList.first { it.command == "removecount" }
        val commandString = "!removecount falls".split(" ", limit = 3)
        val counter = Counter("falls")
        val expectedCounter = Counter("falls", "fall", "falls", 1, 1)
        `when`(mockCountersDAO.getCounterForChannel(channel, counter, true)).thenReturn(expectedCounter)

        command.action(mockTwitchUser, commandString)

        verify(mockCountersDAO, times(1)).incrementCounterForChannel(channel, counter, -1)
        verify(mockTwirk, times(1)).channelMessage(expectedCounter.outputString)
    }

    @Test
    fun testRemoveMultipleCount() {
        val command = counterCommands.commandList.first { it.command == "removecount" }
        val commandString = "!removecount falls 5".split(" ", limit = 3)
        val counter = Counter("falls")
        val expectedCounter = Counter("falls", "fall", "falls", 5, 5)
        `when`(mockCountersDAO.getCounterForChannel(channel, counter, true)).thenReturn(expectedCounter)

        command.action(mockTwitchUser, commandString)

        verify(mockCountersDAO, times(1)).incrementCounterForChannel(channel, counter, -5)
        verify(mockTwirk, times(1)).channelMessage(expectedCounter.outputString)
    }

    @Test
    fun testRemoveToInvalidCounter() {
        val command = counterCommands.commandList.first { it.command == "removecount" }
        val commandString = "!removecount falls 5".split(" ", limit = 3)
        val counter = Counter("falls")
        val expectedCounter = Counter("")
        `when`(mockCountersDAO.getCounterForChannel(channel, counter, true)).thenReturn(expectedCounter)

        command.action(mockTwitchUser, commandString)

        verify(mockCountersDAO, times(1)).incrementCounterForChannel(channel, counter, -5)
        verify(mockTwirk, times(0)).channelMessage(anyString())
    }

    @ParameterizedTest
    @ValueSource(strings = ["burb", "-10"])
    fun testRemoveInvalidNumber(count: String) {
        val command = counterCommands.commandList.first { it.command == "removecount" }
        val commandString = "!removecount falls $count".split(" ", limit = 3)
        val counter = Counter("falls")
        val expectedCounter = Counter("falls", "fall", "falls", 5, 5)
        `when`(mockCountersDAO.getCounterForChannel(channel, counter, true)).thenReturn(expectedCounter)

        command.action(mockTwitchUser, commandString)

        verify(mockCountersDAO, times(0)).incrementCounterForChannel(channel, counter, by = 5)
        verify(mockTwirk, times(1)).channelMessage("$count is not a valid number to decrement by")
    }


    @Test
    fun testCreateCounterCommand() {
        val command = counterCommands.commandList.first { it.command == "createcounter" }
        val commandString = "!createcounter fall fall falls".split(" ", limit = 3)
        val counter = Counter(command = "fall", singular = "fall", plural = "falls")
        command.action(mockTwitchUser, commandString)
        verify(mockCountersDAO, times(1)).createCounterForChannel(channel, counter)
    }

    @ParameterizedTest
    @ValueSource(strings = ["!createcounter fall fall", "!createcounter fall", "!createcounter"])
    fun testCreateCounterHelpCommand(cmd: String) {
        val command = counterCommands.commandList.first { it.command == "createcounter" }
        val commandString = cmd.split(" ", limit = 3)
        val counter = Counter(command = "fall", singular = "fall", plural = "falls")
        command.action(mockTwitchUser, commandString)
        verify(mockCountersDAO, times(0)).createCounterForChannel(channel, counter)
        verify(mockTwirk, times(1)).channelMessage(command.helpString(emptyList()))
    }
}