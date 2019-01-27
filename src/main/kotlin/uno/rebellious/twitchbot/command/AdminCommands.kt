package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.twitchbot.BotManager
import uno.rebellious.twitchbot.database.DatabaseDAO
import java.util.*

class AdminCommands(private var prefix: String, private val twirk: Twirk, private val channel: String, private val database: DatabaseDAO) : CommandList() {

    init {
        if (channel == "glazedhambot") commandList.add(addChannelCommand())
        commandList.add(leaveChannelCommand())
        commandList.add(listChannelsCommand())
        commandList.add(setPrefixCommand())
    }

    private fun listChannelsCommand(): Command {
        return Command(prefix, "listchannels", "Usage: ${prefix}listchannels - Lists all the channels the bot is in", Permission(false, true, false)) { _: TwitchUser, _: List<String> ->
            val channelList = database.getListOfChannels()
            twirk.channelMessage("GlazedHamBot is present in $channelList")
        }
    }

    private fun setPrefixCommand(): Command {
        return Command(prefix, "setprefix", "Usage: '${prefix}setprefix !' - Sets the prefix for commands to '!'", Permission(true, false, false)) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                prefix = content[1]
                database.setPrefixForChannel(channel, prefix)
                commandList.forEach { cmd ->
                    cmd.prefix = prefix
                }
            }
        }
    }

    private fun addChannelCommand(): Command {
        return Command(prefix, "addchannel",
                "Usage: ${prefix}addchannel channeltoAdd - Add a GlazedHamBot to a channel",
                Permission(false, false, false)
        ) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                val newChannel = content[1].toLowerCase(Locale.ENGLISH)
                database.addChannel(newChannel)
                BotManager.startTwirkForChannel(newChannel)
            }
        }
    }

    private fun leaveChannelCommand(): Command {
        return Command(prefix, "hamleave", "Usage: ${prefix}hamleave - Asks the bot to leave the channel (Mod only)",
                Permission(false, true, false)) { _: TwitchUser, _: List<String> ->
            database.leaveChannel(channel)
            twirk.channelMessage("Leaving $channel")
            BotManager.stopTwirkForChannel(channel)
        }
    }
}
