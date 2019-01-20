package uno.rebellious.twitchbot

import com.gikk.twirk.Twirk
import com.gikk.twirk.TwirkBuilder
import com.gikk.twirk.events.TwirkListener
import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import io.reactivex.subjects.BehaviorSubject
import uno.rebellious.twitchbot.command.PatternCommand
import uno.rebellious.twitchbot.database.DatabaseDAO
import uno.rebellious.twitchbot.model.Settings
import java.util.*

object BotManager {

    private val scanner: Observable<String> = Scanner(System.`in`).toObservable().share()
    private val SETTINGS = Settings()
    val lastFMUrl = "http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=${SETTINGS.lastFMUser}&api_key=${SETTINGS.lastFMAPI}&format=json&limit=1"
    private var threadList = HashMap<String, Thread>()


    val database = DatabaseDAO()

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
        twirkThread.name = channel
        twirkThread.start()
        threadList[channel] = twirkThread
    }

    fun stopTwirkForChannel(channel: String) {
        val thread = threadList[channel]
        thread?.interrupt()
    }

    private fun getOnDisconnectListener(twirk: Twirk): TwirkListener? {
        return UnoBotBase(twirk)
    }
}