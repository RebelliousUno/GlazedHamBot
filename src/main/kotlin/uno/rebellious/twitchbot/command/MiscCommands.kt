package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.result.Result
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import uno.rebellious.twitchbot.BotManager
import uno.rebellious.twitchbot.database.DatabaseDAO
import uno.rebellious.twitchbot.model.LastFMResponse
import uno.rebellious.twitchbot.model.SpotifyAccessTokenResponse
import uno.rebellious.twitchbot.model.SpotifyRefreshTokenResponse
import uno.rebellious.twitchbot.model.SpotifyResponse
import java.time.LocalDateTime
import java.time.LocalDateTime.now

class MiscCommands(
    private val prefix: String,
    private val twirk: Twirk,
    private val channel: String,
    private val database: DatabaseDAO
) : CommandList() {
    private val gson = Gson()
    private var jackboxCode = "NO ROOM CODE SET"

    init {
        if (channel == "rebelliousuno") commandList.add(lastfmCommand())
        if (channel == "rebelliousuno") commandList.add(jackSetCommand())
        if (channel == "rebelliousuno") commandList.add(jackCommand())
        if (channel == "rebelliousuno" || channel == "james_lrr") commandList.add(spotifyCommand())

        commandList.add(shoutOutCommand())
    }

    private fun shoutOutCommand(): Command {
        return Command(
            prefix, "shoutout", "Usage: ${prefix}shoutout channelname - the name of the channel to shout out",
            Permission(false, true, false)
        ) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                val shoutout = content[1]
                twirk.channelMessage("Hey why don't you check out $shoutout's channel at https://www.twitch.tv/$shoutout they're good people")
            }
        }
    }

    private fun spotifyCommand(): Command {
        return Command(prefix, "spotify", "", Permission(false, false, false)) { _: TwitchUser, _: List<String> ->

            var spotifyToken = database.getTokensForChannel(channel)
            if (spotifyToken == null) {
                twirk.channelMessage("Need to register the channel with RebelliousUno")
                return@Command ""
            }

            if (spotifyToken.accessToken == null) {
                //access token is null
                //get access token
                //refresh spotifyToken
                twirk.channelMessage("No Access Token requesting one")
                val (request, response, result) = Fuel.request(Method.POST, "https://accounts.spotify.com/api/token")
                    .body("grant_type=authorization_code&code=${spotifyToken.authCode}&redirect_uri=http://bot.rebellious.uno/callback")
                    .appendHeader(BotManager.spotifyBasicAuth)
                    .appendHeader("Content-Type" to "application/x-www-form-urlencoded ")
                    .responseString()
                when (result) {
                    is Result.Success -> {
                        println(response)
                        val tokenResponse = SpotifyAccessTokenResponse(result.get())
                        database.setTokensForChannel(
                            channel,
                            accessToken = tokenResponse.accessToken,
                            refreshToken = tokenResponse.refreshToken,
                            expiryTime = now().plusSeconds(tokenResponse.expiry.toLong())
                        )
                    }
                    is Result.Failure -> {
                        twirk.channelMessage("Unable to refresh spotify token")
                        return@Command ""
                    }
                }
                spotifyToken = database.getTokensForChannel(channel)
                println(spotifyToken)
            }

            if (spotifyToken?.refreshToken != null && (spotifyToken.expiryTime == null || ((spotifyToken.expiryTime as LocalDateTime) < now()))) {
                //Access token is expired or has no expiry - refresh it
                val (request, response, result) = Fuel.request(Method.POST, "https://accounts.spotify.com/api/token")
                    .appendHeader(BotManager.spotifyBasicAuth)
                    .appendHeader("Content-Type" to "application/x-www-form-urlencoded ")
                    .body("grant_type=refresh_token&refresh_token=${spotifyToken.refreshToken}")
                    .responseString()
                when (result) {
                    is Result.Success -> {
                        val spotifyRefreshTokenResponse = SpotifyRefreshTokenResponse(result.get())
                        database.setTokensForChannel(
                            channel,
                            accessToken = spotifyRefreshTokenResponse.accessToken,
                            refreshToken = spotifyToken.refreshToken!!,
                            expiryTime = now().plusSeconds(spotifyRefreshTokenResponse.expiry.toLong())
                        )
                    }
                    is Result.Failure -> {
                        twirk.channelMessage("Unable to refresh token")
                        return@Command ""
                    }
                }
                spotifyToken = database.getTokensForChannel(channel)
            }

            Fuel.request(Method.GET, BotManager.spotifyUrl)
                .appendHeader("Authorization" to "Bearer ${spotifyToken?.accessToken}")
                .responseString { s, e, result ->
                    val resultJson = result.get()
                    val spotifyResponse = SpotifyResponse(resultJson)
                    twirk.channelMessage("$channel is listening to ${spotifyResponse.track} by ${spotifyResponse.artist} from the album ${spotifyResponse.album}")
                }
        }
    }

    private fun lastfmCommand(): Command {
        return Command(
            prefix, "song",
            "Usage: ${prefix}song - The last song listened to by $channel",
            Permission(false, false, false)
        ) { _: TwitchUser, _: List<String> ->
            Fuel.get(BotManager.lastFMUrl).responseString { _, _, result ->
                val resultJson: String = result.get().replace("#", "")
                val json = gson.fromJson<LastFMResponse>(resultJson)
                val artist = json.recenttracks.track[0].artist.text
                val track = json.recenttracks.track[0].name
                val album = json.recenttracks.track[0].album.text
                twirk.channelMessage("$channel last listened to $track by $artist from the album $album")
            }
        }
    }

    private fun jackSetCommand(): Command {
        return Command(
            prefix,
            "jackset",
            "Usage: ${prefix}jackset ROOM - Sets the jackbox code to ROOM",
            Permission(false, true, false)
        ) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                jackboxCode = content[1].substring(0, 4).toUpperCase()
                twirk.channelMessage("Jackbox Code Set to $jackboxCode you can get the link by typing ${prefix}jack into chat")
            }
        }
    }

    private fun jackCommand(): Command {
        return Command(
            prefix,
            "jack",
            "Usage: ${prefix}jack - Gets the jackbox code for the current game",
            Permission(false, false, false)
        ) { _: TwitchUser, _: List<String> ->
            twirk.channelMessage("Jackbox Code Set to $jackboxCode you can get the link by typing ${prefix}jack into chat")
        }
    }
}
