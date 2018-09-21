/**
 * Created by rebel on 16/07/2017.
 */
package uno.rebellious.twitchbot

import com.gikk.twirk.Twirk
import com.gikk.twirk.TwirkBuilder
import com.gikk.twirk.events.TwirkListener
import com.gikk.twirk.types.twitchMessage.TwitchMessage
import com.gikk.twirk.types.users.TwitchUser
import com.github.kittinunf.fuel.Fuel
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import java.io.IOException
import java.util.*

val scanner: Observable<String> = Scanner(System.`in`).toObservable().share()
val SETTINGS = Settings()
val lastFMUrl = "http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=${SETTINGS.lastFMUser}&api_key=${SETTINGS.lastFMAPI}&format=json&limit=1"
var threadList = HashMap<String, Thread>()
val database = DatabaseDAO()

fun main(args: Array<String>) {
    val channelList = database.getListOfChannels()
    channelList.forEach {channel ->
        startTwirkForChannel(channel)
    }
}

fun startTwirkForChannel(channel: String) {
    val twirkThread = Thread(Runnable {
        val twirk = TwirkBuilder("#$channel", SETTINGS.nick, SETTINGS.password)
            .setVerboseMode(true)
            .build()

        twirk.connect()

        twirk.addIrcListener(PatternCommand(twirk, channel))
        twirk.addIrcListener(getOnDisconnectListener(twirk))

        val disposableScanner = scanner
            .subscribe {
                twirk.channelMessage(it)
            }
        var interupted: Boolean
        do {
            interupted = Thread.currentThread().isInterrupted
            if (interupted) {
                println("$channel interupted")
                twirk.close()
                disposableScanner.dispose()
            }
        } while (!interupted)
    })
    twirkThread.name = channel
    twirkThread.start()
    threadList[channel] = twirkThread
}

fun stopTwirkForChannel(channel: String) {
    val thread = threadList[channel]
    thread?.interrupt()
}

fun getOnDisconnectListener(twirk: Twirk): TwirkListener? {
    return UnoBotBase(twirk)
}

class UnoBotBase constructor(private val twirk: Twirk) : TwirkListener {
    override fun onDisconnect() {
        try {
            if (!twirk.connect())
                twirk.close()
        } catch (e: IOException) {
            twirk.close()
        } catch (e: InterruptedException) {
        }
    }
}

class PatternCommand constructor(private val twirk: Twirk, private val channel: String) : TwirkListener {
    private val gson = Gson()
    private var jackboxCode = "NO ROOM CODE SET"
    private var commandList = ArrayList<Command>()
    private var prefix = "!"
    init {
        prefix = database.getPrefixForChannel(channel)
        if (channel == "rebelliousuno") commandList.add(songCommand())
        if (channel == "glazedhambot") commandList.add(addChannelCommand())
        commandList.add(leaveChannelCommand())
        commandList.add(listChannelsCommand())
        commandList.add(addCommand())
        commandList.add(editCommand())
        commandList.add(delCommand())
        commandList.add(commandListCommand())
        commandList.add(setPrefixCommand())
        if (channel == "rebelliousuno") commandList.add(jackSetCommand())
        if (channel == "rebelliousuno") commandList.add(jackCommand())
        commandList.add(helpCommand())

        twirk.channelMessage("Starting up for $channel - prefix is $prefix")
    }

    private fun helpCommand(): Command {
        return Command(prefix, "help" , "Usage: ${prefix}help cmd - to get help for a particular command", Permission(false, false, false)) {
            if (it.size > 1) {
                twirk.channelMessage(commandList.firstOrNull { command -> command.command == it[1] }?.helpString)
            } else {
                twirk.channelMessage("Usage: ${prefix}help cmd - to get help for a particular command")
            }
        }
    }

    private fun responseCommand(): Command {
        return Command(prefix, "", "", Permission(false, false, false)) {
            twirk.channelMessage(database.findResponse(channel, it[0].substring(1)))
        }
    }

    private fun setPrefixCommand(): Command {
        return Command(prefix, "setprefix", "Usage: '${prefix}setprefix !' - Sets the prefix for commands to '!'", Permission(true, false, false)) {
            if (it.size > 1) {
                prefix = it[1]
                database.setPrefixForChannel(channel, prefix)
                commandList.forEach {
                    it.prefix = prefix
                }
            }
        }
    }

    private fun commandListCommand(): Command {
        return Command(prefix, "cmdlist", "Usage: ${prefix}cmdlist - lists the commands for this channel", Permission(false, false, false)) {
            val dbCommands = database.getAllCommandList(channel).map {
                prefix + it
            } as ArrayList<String>
            val configuredCommands = commandList.map {
                it.prefix + it.command
            }
            dbCommands.addAll(configuredCommands)
            twirk.channelMessage("Command List: $dbCommands")
        }
    }

    private fun delCommand(): Command {
        return Command(prefix, "delcmd", "Usage: ${prefix}delcmd cmd - deletes the command 'cmd' (Mod Only - Custom commands only)", Permission(false, true, false)) {
            val removeCommand = it[1].toLowerCase(Locale.ENGLISH)
            database.removeResponse(channel, removeCommand)
        }
    }

    private fun addCommand(): Command {
        return Command(prefix, "addcmd", "Usage: ${prefix}addcmd cmd Response Text- Adds the command 'cmd' with the text 'Response Text' (Mod Only - Custom commands only)", Permission(false, true, false)) {
            if (it.size > 2) {
                val newCommand = it[1].toLowerCase(Locale.ENGLISH)
                val newResponse = it[2]
                database.setResponse(channel, newCommand, newResponse)
            }
        }
    }

    private fun editCommand(): Command {
        return Command(prefix, "editcmd", "Usage: ${prefix}editcmd cmd Response Text- Edits the command 'cmd' with the text 'Response Text' (Mod Only - Custom commands only)", Permission(false, true, false)) {
            if (it.size > 2) {
                val newCommand = it[1].toLowerCase(Locale.ENGLISH)
                val newResponse = it[2]
                database.setResponse(channel, newCommand, newResponse)
            }
        }
    }

    private fun addChannelCommand(): Command {
        return Command(prefix, "addchannel",
            "Usage: ${prefix}addchannel channeltoAdd - Add a GlazedHamBot to a channel",
            Permission(false, false, false)
        ) {
            if (it.size > 1) {
                val newChannel = it[1].toLowerCase(Locale.ENGLISH)
                database.addChannel(newChannel)
                startTwirkForChannel(newChannel)
            }
        }
    }

    private fun leaveChannelCommand(): Command {
        return Command(prefix, "hamleave", "Usage: ${prefix}hamleave - Asks the bot to leave the channel (Mod only)",
            Permission(false, true, false)) {
            database.leaveChannel(channel)
            twirk.channelMessage("Leaving $channel")
            stopTwirkForChannel(channel)
        }
    }

    private fun songCommand(): Command {
        return Command(prefix, "song",
            "Usage: ${prefix}song - The last song listened to by $channel",
            Permission(false, false, false)) {
            Fuel.get(lastFMUrl).responseString { _, _, result ->
                    val resultJson: String = result.get().replace("#", "")
                    val json = gson.fromJson<LastFMResponse>(resultJson)
                    val artist = json.recenttracks.track[0].artist.text
                    val track = json.recenttracks.track[0].name
                    val album = json.recenttracks.track[0].album.text
                    twirk.channelMessage("$channel last listened to $track by $artist from the album $album")
            }}
    }

    private fun listChannelsCommand(): Command {
        return Command(prefix, "listchannels", "Usage: ${prefix}listchannels - Lists all the channels the bot is in", Permission(false, true, false)) {
            val channelList = database.getListOfChannels()
            twirk.channelMessage("GlazedHamBot is present in $channelList")
        }
    }

    private fun jackSetCommand(): Command {
        return Command(prefix, "jackset", "Usage: ${prefix}jackset ROOM - Sets the jackbox code to ROOM", Permission(false, true, false)) {
            if (it.size > 1) {
                jackboxCode = it[1].substring(0,4).toUpperCase()
                twirk.channelMessage("Jackbox Code Set to $jackboxCode you can get the link by typing ${prefix}jack into chat")
            }
        }
    }

    private fun jackCommand(): Command {
        return Command(prefix, "jack", "Usage: ${prefix}jack - Gets the jackbox code for the current game", Permission(false, false, false)) {
            twirk.channelMessage("Jackbox Code Set to $jackboxCode you can get the link by typing ${prefix}jack into chat")
        }
    }

    override fun onPrivMsg(sender: TwitchUser, message: TwitchMessage) {
        val content: String = message.content.trim()
        if (!content.startsWith(prefix)) return

        val splitContent = content.split(' ', ignoreCase = true, limit = 3)
        val command = splitContent[0].toLowerCase(Locale.ENGLISH)

        commandList
                .filter { "${it.prefix}${it.command}".startsWith(command) }
                .firstOrNull { it.canUseCommand(sender) }
                ?.action?.invoke(splitContent) ?: responseCommand().action.invoke(splitContent)
    }
}
