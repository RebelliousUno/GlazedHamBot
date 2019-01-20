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
import io.reactivex.subjects.BehaviorSubject
import java.io.IOException
import java.lang.NumberFormatException
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
        val shouldStop = BehaviorSubject.create<Boolean>()
        shouldStop.onNext(false)
        val twirk = TwirkBuilder("#$channel", SETTINGS.nick, SETTINGS.password)
            .setVerboseMode(true)
            .build()

        twirk.connect()

        twirk.addIrcListener(PatternCommand(twirk, channel))
        twirk.addIrcListener(getOnDisconnectListener(twirk))

        scanner
                .takeUntil {
                    it == ".quit"
                }
            .subscribe {
                if (it == ".quit") {
                    println("Quitting $channel")
                    twirk.close()
                } else {
                    twirk.channelMessage(it)
                }
            }
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
    private var responseCommands = ArrayList<Command>()
    private var counterCommands = ArrayList<Command>()
    private var miscCommands = ArrayList<Command>()
    private var adminCommands = ArrayList<Command>()
    private var prefix = "!"
    init {
        prefix = database.getPrefixForChannel(channel)
        if (channel == "rebelliousuno") miscCommands.add(songCommand())
        if (channel == "glazedhambot") adminCommands.add(addChannelCommand())
        adminCommands.add(leaveChannelCommand())
        adminCommands.add(listChannelsCommand())
        responseCommands.add(addCommand())
        responseCommands.add(editCommand())
        responseCommands.add(delCommand())
        miscCommands.add(commandListCommand())
        adminCommands.add(setPrefixCommand())
        counterCommands.add(createCounterCommand())
        counterCommands.add(addCountCommand())
        counterCommands.add(removeCountCommand())
        counterCommands.add(resetCountCommand())
        counterCommands.add(listCountersCommand())
        counterCommands.add(deleteCounterCommand())

        if (channel == "rebelliousuno") miscCommands.add(jackSetCommand())
        if (channel == "rebelliousuno") miscCommands.add(jackCommand())
        miscCommands.add(helpCommand())
        commandList.add(quoteCommand())
        commandList.addAll(adminCommands)
        commandList.addAll(responseCommands)
        commandList.addAll(counterCommands)
        commandList.addAll(miscCommands)


        twirk.channelMessage("Starting up for $channel - prefix is $prefix")
    }

    private fun deleteCounterCommand(): Command {
        val helpString = ""
        return Command(prefix, "deletecounter", helpString, Permission(false, true, false)) {
            if (it.size == 2) {
                database.removeCounterForChannel(channel, it[1])
            } else {
                twirk.channelMessage(helpString)
            }
        }
    }

    private fun resetCountCommand(): Command {
        val helpString = "Usage ${prefix}resetCount count - resets today's count for a counter"
        return Command(prefix, "resetcount", helpString, Permission(false, true, false)) {
            if (it.size == 2) {
                database.resetTodaysCounterForChannel(channel, it[1])
            } else {
                twirk.channelMessage(helpString)
            }
        }
    }

    private fun listCountersCommand(): Command {
        val helpString = ""
        return Command(prefix, "counterlist", helpString, Permission(false, true, false)) {
            twirk.channelMessage(database.showCountersForChannel(channel).toString())
        }
    }

    private fun removeCountCommand(): Command {
        val helpString = "Usage: ${prefix}removecount counterName [amount]- eg. ${prefix}removecount fall or ${prefix}removecount fall 2"
        return Command(prefix, "removecount", helpString, Permission(false, true, false)) {
            val counter = it[1]
            try {
                val by = if (it.size == 3) {
                    Integer.parseInt(it[2])
                } else {
                    1
                }
                if (by > 0)
                    database.incrementCounterForChannel(channel, counter, by)
                else
                    twirk.channelMessage("${it[2]} is not a valid number to decrement by")
            } catch (e: NumberFormatException) {
                twirk.channelMessage("${it[2]} is not a valid number to decrement by")
            }
        }
    }

    private fun addCountCommand(): Command {
        val helpString = "Usage: ${prefix}addcount counterName [amount]- eg. ${prefix}addcount fall or ${prefix}addcount fall 2"
        return Command(prefix, "addcount", helpString, Permission(false, true, false)) {
            val counter = it[1]
            try {
                val by = if (it.size == 3) {
                    Integer.parseInt(it[2])
                } else {
                    1
                }
                if (by > 0)
                    database.incrementCounterForChannel(channel, counter, by)
                else twirk.channelMessage("${it[2]} is not a valid number to increment by")
            } catch (e: NumberFormatException) {
                twirk.channelMessage("${it[2]} is not a valid number to increment by")
            }
        }
    }

    private fun createCounterCommand(): Command {
        val helpString = "Usage: ${prefix}createCounter counterName singular plural - eg. ${prefix}createCounter fall fall falls"
        return Command(prefix, "createcounter", helpString, Permission(false, true, false)) {
            if (it.size == 3) {
                val counter = it[1]
                val singular = it[2].split(" ")[0]
                val plural = it[2].split(" ")[1]
                database.createCounterForChannel(channel, counter, singular, plural)
            } else {
                twirk.channelMessage(helpString)
            }
        }
    }


    private fun quoteCommand(): Command {
        return Command(prefix, "quote", "", Permission(false, false, false)) {
            var message: String
            if (it.size > 1) {
                message = try {
                    val id = Integer.parseInt(it[1])
                    database.getQuoteForChannelById(channel, id)
                } catch (nfe: NumberFormatException) {
                    val quote1 = database.findQuoteByAuthor(channel, it[1])
                    val quote2 = database.findQuoteByKeyword(channel, it[1])
                    if (!quote1.isEmpty()) {
                        quote1
                    } else {
                        quote2
                    }
                }
            } else {
                message = database.getRandomQuoteForChannel(channel)
            }
            twirk.channelMessage(message)
        }
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

    private fun countCommand(): Command {
        return Command(prefix, "", "", Permission(false, false, false)) {
            twirk.channelMessage(database.getCounterForChannel(channel, it[0].substring(1)))
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
            val dbCommands = database.getAllCommandList(channel).map {command ->
                prefix + command
            }.sorted()
            val adminCmds = adminCommands.map { command ->
                command.prefix + command.command
            }.sorted()
            val miscCmds = miscCommands.map { command ->
                command.prefix + command.command
            }.sorted()
            val responseCmds = responseCommands.map { command ->
                command.prefix + command.command
            }.sorted()
            val counterCmds = counterCommands.map { command ->
                command.prefix + command.command
            }.sorted()
            twirk.channelMessage("Responses: $dbCommands $responseCmds, Counters: $counterCmds, Misc: $miscCmds,  Admin: $adminCmds")
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
                ?.action?.invoke(splitContent) ?: run {
                  countCommand().action.invoke(splitContent)
            responseCommand().action.invoke(splitContent)
        }
    }
}
