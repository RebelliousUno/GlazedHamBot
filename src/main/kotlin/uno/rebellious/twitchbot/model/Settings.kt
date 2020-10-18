/**
 * Created by rebel on 16/07/2017.
 */

package uno.rebellious.twitchbot.model

import java.util.*

class Settings {
    private val props = Properties()
    private var settings: DataSettings?

    init {
        props.load(this.javaClass.classLoader.getResourceAsStream("settings.properties"))
        settings = DataSettings(
            props.getProperty("nick"),
            props.getProperty("password"),
            props.getProperty("lastfm"),
            props.getProperty("channel"),
            props.getProperty("lastFmUser"),
            props.getProperty("spotifyOAuth"),
            props.getProperty("client.id"),
            props.getProperty("client.secret"),
            props.getProperty("pastebin_dev"),
            props.getProperty("pastebin_user"),
            props.getProperty("aws.accessKeyId"),
            props.getProperty("aws.secretAccessKey")
        )
    }

    val pastebinDev = settings?.PASTEBIN_DEV
    val pastebinUser = settings?.PASTEBIN_USER

    val spotifyOAuthToken = settings?.SPOTIFY_OAUTH

    val nick = settings?.MY_NICK

    val password = settings?.MY_PASS

    val lastFMAPI = settings?.LAST_FM_API
    val lastFMUser = settings?.LAST_FM_USER
    val clientId = settings?.CLIENT_ID
    val clientSecret = settings?.CLIENT_SECRET
    val awsAccessKeyId = settings?.AWS_ACCESS_KEY_ID
    val awsAccessKey = settings?.AWS_ACCESS_KEY
}

data class DataSettings(
    val MY_NICK: String,
    val MY_PASS: String,
    val LAST_FM_API: String,
    val CHANNEL: String,
    val LAST_FM_USER: String,
    val SPOTIFY_OAUTH: String?,
    val CLIENT_ID: String,
    val CLIENT_SECRET: String,
    val PASTEBIN_DEV: String,
    val PASTEBIN_USER: String,
    val AWS_ACCESS_KEY_ID: String,
    val AWS_ACCESS_KEY: String
)
