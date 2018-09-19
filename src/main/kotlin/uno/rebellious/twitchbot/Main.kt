/**
 * Created by rebel on 16/07/2017.
 */
package uno.rebellious.twitchbot

import com.gikk.twirk.Twirk
import com.gikk.twirk.TwirkBuilder
import com.gikk.twirk.enums.USER_TYPE
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
import kotlin.properties.Delegates


val props = Properties()
val scanner = Scanner(System.`in`).toObservable().share()
val SETTINGS = Settings()
val channel: String = "#" + SETTINGS.channel
val lastFMUrl = "http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=${SETTINGS.lastFMUser}&api_key=${SETTINGS.lastFMAPI}&format=json&limit=1"
//val twirkConnections= HashMap<String, Twirk>()
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

        var disposableScanner = scanner
            .subscribe {
                twirk.channelMessage(it)
            }
        var interupted = false
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
    var thread = threadList[channel]
    thread?.interrupt()
}

fun getOnDisconnectListener(twirk: Twirk): TwirkListener? {
    return UnoBotBase(twirk)
}

class UnoBotBase constructor(val twirk: Twirk) : TwirkListener {
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

class PatternCommand constructor(val twirk: Twirk, val channel: String) : TwirkListener {
    private val gson = Gson()
    private var jackboxCode = "NO ROOM CODE SET"

    override fun onPrivMsg(sender: TwitchUser, message: TwitchMessage) {
        val content: String = message.getContent().trim()
        if (!content.startsWith('!')) return

        val splitContent = content.split(' ', ignoreCase = true, limit = 3)
        val command = splitContent[0].toLowerCase(Locale.ENGLISH)

        when {
            command.startsWith("!song") && channel=="RebelliousUno".toLowerCase() -> Fuel.get(lastFMUrl).responseString { _, _, result ->
                val resultJson: String = result.get().replace("#", "")
                val json = gson.fromJson<LastFMResponse>(resultJson)
                val artist = json.recenttracks.track[0].artist.text
                val track = json.recenttracks.track[0].name
                val album = json.recenttracks.track[0].album.text
                twirk.channelMessage("$channel last listened to $track by $artist from the album $album")
            }
            command.startsWith("!addchannel")
                    && (channel.toLowerCase() == "glazedhambot")
                    && (splitContent.size > 1) -> {
                val newChannel = splitContent[1].toLowerCase(Locale.ENGLISH)
                database.addChannel(newChannel)
                startTwirkForChannel(newChannel)
            }
            command.startsWith("!hamleave") && (sender.userType == USER_TYPE.OWNER || sender.displayName.toLowerCase() == "rebelliousuno") -> {
                database.leaveChannel(channel)
                twirk.channelMessage("Leaving $channel")
                stopTwirkForChannel(channel)
            }

            command.startsWith("!listchannels") && sender.isMod -> {
                val channelList = database.getListOfChannels()
                twirk.channelMessage("GlazedHamBot is present in $channelList")
            }

            command.startsWith("!cmdlist") -> {
                val dbCommands = database.getAllCommandList(channel)
                dbCommands.add("!song")
                dbCommands.add("!jack")
                twirk.channelMessage("Command List: " + dbCommands)
                if (sender.isMod || sender.isOwner) {
                    twirk.channelMessage("Mods Only: !jackset, !addcmd, !editcmd, !delcmd")
                }
            }
            command.startsWith("!addcmd") || command.startsWith("!editcmd")
                    && splitContent.size > 2 && (sender.isMod || sender.isOwner) -> {
                val newCommand = splitContent[1].toLowerCase(Locale.ENGLISH)
                val newResponse = splitContent[2]
                database.setResponse(channel, newCommand, newResponse)
            }
            command.startsWith("!delcmd")
                    && splitContent.size > 1 && (sender.isMod || sender.isOwner) -> {
                val removeCommand = splitContent[1].toLowerCase(Locale.ENGLISH)
                database.removeResponse(channel, removeCommand)
            }
            command.startsWith("!help") && splitContent.size > 1 -> when {
                splitContent[1].contains("song") -> twirk.channelMessage("!song - shows most recently played song")
                splitContent[1].contains("jack") -> {
                    twirk.channelMessage("!jack - shows current audience code for Jackbox TV games")
                    if (sender.isMod || sender.isOwner) {
                        twirk.channelMessage("!jackset CODE - Mod Only - sets the jackbox code to CODE")
                    }
                }
                splitContent[1].contains("!addcmd") && (sender.isMod || sender.isOwner) -> twirk.channelMessage("!addcmd newcmd The Message To Send - Mod Only - Creates a new GlazedHamBot response")
                splitContent[1].contains("!editcmd") && (sender.isMod || sender.isOwner) -> twirk.channelMessage("!editcmd cmd The Message To Send - Mod Only - Updates a GlazedHamBot response")
                splitContent[1].contains("!delcmd") && (sender.isMod || sender.isOwner) -> twirk.channelMessage("!delcmd cmd - Mod Only - Deletes a GlazedHamBot response")
            }
            command.startsWith("!help") -> twirk.channelMessage("Type !help followed by the command to get more help about that command.  !cmdlist shows the current commands")
            command.startsWith("!jackset") && (sender.isMod || sender.userType == USER_TYPE.OWNER) -> {
                jackboxCode = splitContent[1].substring(0, 4).toUpperCase()
                twirk.channelMessage("Jackbox Code Set to $jackboxCode you can get the link by typing !jack into chat")
            }
            command.startsWith("!jack") -> twirk.channelMessage("You can join in the audience by going to http://jackbox.tv and using the room code $jackboxCode")
            command.startsWith("!") -> twirk.channelMessage(database.findResponse(channel, splitContent[0].substring(1)))
        }
    }

}
