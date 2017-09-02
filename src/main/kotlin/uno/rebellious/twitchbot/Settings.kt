/**
 * Created by rebel on 16/07/2017.
 */

package uno.rebellious.twitchbot

import java.util.*

class Settings {
    val props = Properties()
    private var settings: DataSettings?
    init {
        props.load(this.javaClass.classLoader.getResourceAsStream("settings.properties"))
        settings = DataSettings(props.getProperty("nick"), props.getProperty("password"), props.getProperty("lastfm"), props.getProperty("channel"))
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
    val channel: String?
        get() {
            return settings?.CHANNEL
        }
}

data class DataSettings(val MY_NICK: String, val MY_PASS: String, val LAST_FM_API: String, val CHANNEL: String)
