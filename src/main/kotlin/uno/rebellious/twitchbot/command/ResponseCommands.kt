package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.twitchbot.database.DatabaseDAO
import java.util.*

class ResponseCommands(private val prefix: String, private val twirk: Twirk, private val channel: String, private val database: DatabaseDAO): CommandList() {
    init {
        commandList.add(addCommand())
        commandList.add(editCommand())
        commandList.add(delCommand())
    }

    private fun delCommand(): Command {
        return Command(prefix, "delcmd", "Usage: ${prefix}delcmd cmd - deletes the command 'cmd' (Mod Only - Custom commands only)", Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            val removeCommand = content[1].toLowerCase(Locale.ENGLISH)
            database.removeResponse(channel, removeCommand)
        }
    }

    private fun addCommand(): Command {
        return Command(prefix, "addcmd", "Usage: ${prefix}addcmd cmd Response Text- Adds the command 'cmd' with the text 'Response Text' (Mod Only - Custom commands only)", Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            if (content.size > 2) {
                val newCommand = content[1].toLowerCase(Locale.ENGLISH)
                val newResponse = content[2]
                database.setResponse(channel, newCommand, newResponse)
            }
        }
    }

    private fun editCommand(): Command {
        return Command(prefix, "editcmd", "Usage: ${prefix}editcmd cmd Response Text- Edits the command 'cmd' with the text 'Response Text' (Mod Only - Custom commands only)", Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            if (content.size > 2) {
                val newCommand = content[1].toLowerCase(Locale.ENGLISH)
                val newResponse = content[2]
                database.setResponse(channel, newCommand, newResponse)
            }
        }
    }

}
