package glazedhambot.tests.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import uno.rebellious.twitchbot.command.ResponseCommands
import uno.rebellious.twitchbot.database.DatabaseDAO
import uno.rebellious.twitchbot.model.Response

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TestResponseCommands {
    lateinit var responseCommands: ResponseCommands
    lateinit var mockResponseDAO: DatabaseDAO
    lateinit var mockTwirk: Twirk
    lateinit var mockTwitchUser: TwitchUser
    val channel = "test"

    @BeforeEach
    fun setup() {
        mockTwirk = Mockito.mock(Twirk::class.java)
        mockResponseDAO = Mockito.mock(DatabaseDAO::class.java)
        mockTwitchUser = Mockito.mock(TwitchUser::class.java)

        responseCommands = ResponseCommands("!", mockTwirk, channel, mockResponseDAO)
    }

    @Test
    fun testContainsAllCommands() {
        val commandList = listOf(
            "addcmd",
            "editcmd",
            "delcmd"
        )
        Assertions.assertTrue(commandList.containsAll(responseCommands.commandList.map { it.command }))
    }

    @Test
    fun testAddResponse() {
        val command = responseCommands.commandList.first { it.command == "addcmd" }
        val commandString = "!addcmd cmd This is a test command".split(" ", limit = 3)
        command.action(mockTwitchUser, commandString)
        verify(mockResponseDAO).setResponse(channel, Response("cmd", "This is a test command"))
    }

    @Test
    fun testAddBadResponse() {
        val command = responseCommands.commandList.first { it.command == "addcmd" }
        val commandString = "!addcmd cmd".split(" ", limit = 3)
        command.action(mockTwitchUser, commandString)
        verifyNoInteractions(mockResponseDAO)
    }

    @Test
    fun testEditResponse() {
        val command = responseCommands.commandList.first { it.command == "editcmd" }
        val commandString = "!editcmd cmd This is a test command".split(" ", limit = 3)
        command.action(mockTwitchUser, commandString)
        verify(mockResponseDAO).setResponse(channel, Response("cmd", "This is a test command"))
    }

    @Test
    fun testDeleteResponse() {
        val command = responseCommands.commandList.first { it.command == "delcmd" }
        val commandString = "!delcmd cmd".split(" ", limit = 3)
        command.action(mockTwitchUser, commandString)
        verify(mockResponseDAO).removeResponse(channel, Response("cmd"))

    }
}