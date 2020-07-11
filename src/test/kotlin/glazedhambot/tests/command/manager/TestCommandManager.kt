package glazedhambot.tests.command.manager

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.twitchMessage.TwitchMessage
import com.gikk.twirk.types.users.TwitchUser
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import uno.rebellious.twitchbot.command.manager.CommandManager
import uno.rebellious.twitchbot.database.Channel

class TestCommandManager {

    lateinit var commandManager: CommandManager
    lateinit var mockTwirk: Twirk
    lateinit var mockSender: TwitchUser
    lateinit var mockMessage: TwitchMessage

    val channel = Channel("test", "!", "GlazedHamBot", "")

    @BeforeEach
    fun setUp() {
        mockTwirk = mock(Twirk::class.java)
        mockSender = mock(TwitchUser::class.java)
        mockMessage = mock(TwitchMessage::class.java)
        commandManager = CommandManager(mockTwirk, channel)
        clearInvocations(mockTwirk)
    }

    @Test
    fun testCommandManagerSetUp() {
        commandManager = CommandManager(mockTwirk, channel)
        verify(mockTwirk).channelMessage("Starting up for test - prefix is !")
    }


    @Test
    fun testOnPrivMessageWithoutPrefix() {
        `when`(mockMessage.content).thenReturn("Not a real message")
        commandManager.onPrivMsg(mockSender, mockMessage)
        verifyNoInteractions(mockTwirk)
    }
}