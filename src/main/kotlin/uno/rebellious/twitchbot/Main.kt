/**
 * Created by rebel on 16/07/2017.
 */
package uno.rebellious.twitchbot

import com.gikk.twirk.Twirk
import com.gikk.twirk.TwirkBuilder
import com.gikk.twirk.enums.USER_TYPE
import com.gikk.twirk.events.TwirkListener
import com.gikk.twirk.events.TwirkListenerBaseImpl
import com.gikk.twirk.types.twitchMessage.TwitchMessage
import com.gikk.twirk.types.users.TwitchUser
import com.github.kittinunf.fuel.Fuel
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import java.io.IOException
import java.util.*



val props = Properties()
val scanner: Scanner = Scanner(System.`in`)
val SETTINGS = Settings()
val channel: String = "#" + SETTINGS.channel
val lastFMUrl = "http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=rebelliousuno&api_key=${SETTINGS.lastFMAPI}&format=json&limit=1"
val twirk: Twirk = TwirkBuilder(channel, SETTINGS.nick, SETTINGS.password)
        .setVerboseMode(true)
        .build()

val database = DatabaseDAO()

fun main(args: Array<String>) {

    twirk.connect()

    twirk.addIrcListener(PatternCommand(twirk))
    twirk.addIrcListener(getOnDisconnectListener(twirk))

    var line: String
    do {
        line = scanner.nextLine()
        twirk.channelMessage(line)
    } while (line != ".quit")

    scanner.close()
    twirk.close()
}

fun getOnDisconnectListener(twirk: Twirk): TwirkListener? {
    return UnoBotBase(twirk)
}

class UnoBotBase constructor(val twirk: Twirk) : TwirkListenerBaseImpl() {
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

class PatternCommand constructor(val twirk: Twirk) : TwirkListenerBaseImpl() {
    private val gson = Gson()
    private var jackboxCode = "NO ROOM CODE SET"

    override fun onPrivMsg(sender: TwitchUser, message: TwitchMessage) {
        val content: String = message.getContent().trim()
        if (!content.startsWith('!')) return

        val splitContent = content.split(' ', ignoreCase = true, limit = 3)
        val command = splitContent[0].toLowerCase(Locale.ENGLISH)

        when {
            command.startsWith("!song") -> Fuel.get(lastFMUrl).responseString { _, _, result ->
                val resultJson: String = result.get().replace("#", "")
                val json = gson.fromJson<LastFMResponse>(resultJson)
                val artist = json.recenttracks.track[0].artist.text
                val track = json.recenttracks.track[0].name
                val album = json.recenttracks.track[0].album.text
                twirk.channelMessage("RebelliousUno last listened to $track by $artist from the album $album")
            }
            command.startsWith("!cmdlist") -> {
                val dbCommands = database.getAllCommandList()
                dbCommands.add("!song")
                dbCommands.add("!jack")
                twirk.channelMessage("Command List: " + dbCommands)
                if (sender.isMod || sender.userType == USER_TYPE.OWNER) {
                    twirk.channelMessage("Mods Only: !jackset, !addcmd, !editcmd, !delcmd")
                }
            }
            command.startsWith("!addcmd") || command.startsWith("!editcmd")
                    && splitContent.size > 2 && (sender.isMod || sender.userType == USER_TYPE.OWNER) -> {
                val newCommand = splitContent[1].toLowerCase(Locale.ENGLISH)
                val newResponse = splitContent[2]
                database.setResponse(newCommand, newResponse)
            }
            command.startsWith("!delcmd")
                    && splitContent.size > 1 && (sender.isMod || sender.userType == USER_TYPE.OWNER) -> {
                val removeCommand = splitContent[1].toLowerCase(Locale.ENGLISH)
                database.removeResponse(removeCommand)
            }
            command.startsWith("!help") && splitContent.size > 1 -> when {
                splitContent[1].contains("song") -> twirk.channelMessage("!song - shows most recently played song")
                splitContent[1].contains("jack") -> {
                    twirk.channelMessage("!jack - shows current audience code for Jackbox TV games")
                    if (sender.isMod || sender.userType == USER_TYPE.OWNER) {
                        twirk.channelMessage("!jackset CODE - Mod Only - sets the jackbox code to CODE")
                    }
                }
                splitContent[1].contains("!addcmd") && (sender.isMod || sender.userType == USER_TYPE.OWNER) -> twirk.channelMessage("!addcmd newcmd The Message To Send - Mod Only - Creates a new GlazedHamBot response")
                splitContent[1].contains("!editcmd") && (sender.isMod || sender.userType == USER_TYPE.OWNER) -> twirk.channelMessage("!editcmd cmd The Message To Send - Mod Only - Updates a GlazedHamBot response")
                splitContent[1].contains("!delcmd") && (sender.isMod || sender.userType == USER_TYPE.OWNER) -> twirk.channelMessage("!delcmd cmd - Mod Only - Deletes a GlazedHamBot response")
            }
            command.startsWith("!help") -> twirk.channelMessage("Type !help followed by the command to get more help about that command.  !cmdlist shows the current commands")
            command.startsWith("!jackset") && (sender.isMod || sender.userType == USER_TYPE.OWNER) -> {
                jackboxCode = splitContent[1].substring(0, 4).toUpperCase()
                twirk.channelMessage("Jackbox Code Set to $jackboxCode you can get the link by typing !jack into chat")
            }
            command.startsWith("!jack") -> twirk.channelMessage("You can join in the audience by going to http://jackbox.tv and using the room code $jackboxCode")
            command.startsWith("!") -> twirk.channelMessage(database.findResponse(splitContent[0].substring(1)))
        }
    }

}
