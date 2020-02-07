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
        settings = DataSettings(props.getProperty("nick"),
            props.getProperty("password"),
            props.getProperty("lastfm"),
            props.getProperty("channel"),
            props.getProperty("lastFmUser"),
            props.getProperty("spotifyOAuth"))
    }

    val spotifyOAuthToken: String?
        get() {
            return settings?.SPOTIFY_OAUTH
        }

    val nick: String?
        get() {
            return settings?.MY_NICK
        }

    val password: String?
        get() {
            return settings?.MY_PASS
        }

    val lastFMAPI: String?
        get() {
            return settings?.LAST_FM_API
        }
    val lastFMUser: String?
        get() {
            return settings?.LAST_FM_USER
        }
}

data class DataSettings(val MY_NICK: String, val MY_PASS: String, val LAST_FM_API: String, val CHANNEL: String, val LAST_FM_USER: String, val SPOTIFY_OAUTH: String?)
