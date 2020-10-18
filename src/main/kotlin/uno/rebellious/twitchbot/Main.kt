/**
 * Created by rebel on 16/07/2017.
 */
package uno.rebellious.twitchbot

import uno.rebellious.twitchbot.database.SettingsDyanmoDBDAO

fun main(args: Array<String>) {
    val channelList = BotManager.database.getListOfChannels()
    channelList.forEach { channel ->
        BotManager.startTwirkForChannel(channel)
    }

//    val s = SettingsDyanmoDBDAO()
//    with(s) {
//        createChannelsTable()
//        addChannel("rebelliousuno", "*")
//        addChannel("someNewChannel", "!")
//        leaveChannel("someNewChannel")
//        setPrefixForChannel("rebelliousuno", "@")
//        println(getPrefixForChannel("rebelliousuno") == "@")
//        println(getPrefixForChannel("newChannel") == "???")
//        println(getPrefixForChannel("blank") == "????")
//        val channels = getListOfChannels()
//        channels.forEach {
//            println(it)
//        }
//
//    }
}


