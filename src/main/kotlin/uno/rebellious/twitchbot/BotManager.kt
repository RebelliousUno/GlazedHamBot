package uno.rebellious.twitchbot

import com.gikk.twirk.Twirk
import com.gikk.twirk.TwirkBuilder
import com.gikk.twirk.events.TwirkListener
import com.gikk.twirk.types.users.TwitchUser
import com.gikk.twirk.types.users.TwitchUserBuilder
import com.github.kittinunf.fuel.Fuel
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.subjects.BehaviorSubject
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import uno.rebellious.twitchbot.command.CommandManager
import uno.rebellious.twitchbot.database.Channel
import uno.rebellious.twitchbot.database.DatabaseDAO
import uno.rebellious.twitchbot.model.ChattersResponse
import uno.rebellious.twitchbot.model.Settings
import java.time.Instant.now
import java.util.*

object BotManager {

    private val scanner: Observable<String> = Scanner(System.`in`).toObservable().share()
    private val SETTINGS = Settings()
    val lastFMUrl =
        "http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=${SETTINGS.lastFMUser}&api_key=${SETTINGS.lastFMAPI}&format=json&limit=1"
    private var threadList = HashMap<String, Thread>()


    val database = DatabaseDAO()

    fun startCurrencyCheckerForChannel(channel: Channel, pollTime: Int) {
        val url = "http://tmi.twitch.tv/group/user/${channel.channel}/chatters"

        GlobalScope.launch {
            while (true) {
                Fuel.get(url).responseString { _, _, result ->
                    val resultJson = result.get()
                    val chattersResponse = Gson().fromJson(resultJson, ChattersResponse::class.java)
                    val details = database.getCurrencyDetailsForChannel(channel.channel)
                    if (details != null) {
                        database.updateCurrencyForUsers(channel.channel, chattersResponse.chatters.viewers, 1.0)
                        database.updateCurrencyForUsers(channel.channel, chattersResponse.chatters.moderators, details.modMult)
                        database.updateCurrencyForUsers(channel.channel, chattersResponse.chatters.vips, details.vipMult)
                        database.updateCurrencyForUsers(channel.channel, chattersResponse.chatters.broadcaster, 1.0)
                        database.updateCurrencyForUsers(channel.channel, chattersResponse.chatters.staff, 1.0)
                        database.updateCurrencyForUsers(channel.channel, chattersResponse.chatters.admins, 1.0)
                        database.updateCurrencyForUsers(channel.channel, chattersResponse.chatters.global_mods, 1.0)
                    }
                }
                delay(pollTime * 1000.toLong())
            }
        }
    }

    fun startTwirkForChannel(channel: Channel) {
        val twirkThread = Thread(Runnable {
            val shouldStop = BehaviorSubject.create<Boolean>()
            shouldStop.onNext(false)
            val nick = if (channel.nick.isBlank()) SETTINGS.nick else channel.nick
            val password = if (channel.token.isBlank()) SETTINGS.password else channel.token

            val twirk = TwirkBuilder("#${channel.channel}", nick, password)
                .setVerboseMode(true)
                .build()
            twirk.connect()
            twirk.addIrcListener(CommandManager(twirk, channel))
            twirk.addIrcListener(getOnDisconnectListener(twirk))

            scanner
                .takeUntil { it == ".quit" }
                .subscribe {
                    if (it == ".quit") {
                        println("Quitting $channel")
                        twirk.close()
                    } else {
                        twirk.channelMessage(it)
                    }
                }
        })
        twirkThread.name = channel.channel
        twirkThread.start()
        threadList[channel.channel] = twirkThread
    }

    fun stopTwirkForChannel(channel: String) {
        val thread = threadList[channel]
        thread?.interrupt()
    }

    private fun getOnDisconnectListener(twirk: Twirk): TwirkListener? {
        return UnoBotBase(twirk)
    }
}