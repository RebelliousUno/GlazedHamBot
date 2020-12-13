package uno.rebellious.twitchbot

import com.gikk.twirk.Twirk
import com.gikk.twirk.events.TwirkListener
import uno.rebellious.twitchbot.database.Channel
import java.io.IOException

class UnoBotBase(private val twirk: Twirk, private val channel: Channel) : TwirkListener {
    override fun onDisconnect() {
        println("$channel Disconnect")
        try {
            if (!twirk.connect()) {
                twirk.close()
                println("$channel closed")
            } else {
                // return if reconnected
                return
            }
        } catch (e: IOException) {
            twirk.close()
            println("$channel closed (IOException)")
        } catch (e: InterruptedException) {
        }
        //All Else has failed...Kill the thread...start it up again.
        BotManager.stopTwirkForChannel(channel.channel)
        BotManager.startTwirkForChannel(channel)
    }
}