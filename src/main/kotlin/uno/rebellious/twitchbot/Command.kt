package uno.rebellious.twitchbot

import com.gikk.twirk.types.users.TwitchUser

class Command (var prefix: String, val command: String, val helpString: String, val permissions: Permission, val action: (List<String>) -> Any){
    fun canUseCommand(sender: TwitchUser): Boolean {
        println(sender.toString())
        if (permissions.isOwnerOnly && !sender.isOwner) return false
        if (permissions.isModOnly && !(sender.isMod || sender.isOwner)) return false
        if (permissions.isSubOnly && !(sender.isOwner || sender.isMod || sender.isSub)) return false
        return true
    }
}