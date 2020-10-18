/**
 * Created by rebel on 16/07/2017.
 */
package uno.rebellious.twitchbot

fun main() {
    val channelList = BotManager.database.getListOfChannels()
    channelList.forEach { channel ->
        BotManager.startTwirkForChannel(channel)
    }
}

