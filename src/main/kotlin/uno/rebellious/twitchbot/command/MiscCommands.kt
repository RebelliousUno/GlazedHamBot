package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import com.github.kittinunf.fuel.Fuel
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import uno.rebellious.twitchbot.BotManager
import uno.rebellious.twitchbot.database.DatabaseDAO
import uno.rebellious.twitchbot.model.LastFMResponse

class MiscCommands(private val prefix: String, private val twirk: Twirk, private val  channel: String, private val database: DatabaseDAO) : CommandList() {
    private val gson = Gson()
    private var jackboxCode = "NO ROOM CODE SET"

    init {
        if (channel == "rebelliousuno") commandList.add(songCommand())
        if (channel == "rebelliousuno") commandList.add(jackSetCommand())
        if (channel == "rebelliousuno") commandList.add(jackCommand())
    }

    private fun songCommand(): Command {
        return Command(prefix, "song",
                "Usage: ${prefix}song - The last song listened to by $channel",
                Permission(false, false, false)) { _: TwitchUser, _: List<String> ->
            Fuel.get(BotManager.lastFMUrl).responseString { _, _, result ->
                val resultJson: String = result.get().replace("#", "")
                val json = gson.fromJson<LastFMResponse>(resultJson)
                val artist = json.recenttracks.track[0].artist.text
                val track = json.recenttracks.track[0].name
                val album = json.recenttracks.track[0].album.text
                twirk.channelMessage("$channel last listened to $track by $artist from the album $album")
            }}
    }

    private fun jackSetCommand(): Command {
        return Command(prefix, "jackset", "Usage: ${prefix}jackset ROOM - Sets the jackbox code to ROOM", Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                jackboxCode = content[1].substring(0,4).toUpperCase()
                twirk.channelMessage("Jackbox Code Set to $jackboxCode you can get the link by typing ${prefix}jack into chat")
            }
        }
    }

    private fun jackCommand(): Command {
        return Command(prefix, "jack", "Usage: ${prefix}jack - Gets the jackbox code for the current game", Permission(false, false, false)) { _: TwitchUser, _: List<String> ->
            twirk.channelMessage("Jackbox Code Set to $jackboxCode you can get the link by typing ${prefix}jack into chat")
        }
    }
}
