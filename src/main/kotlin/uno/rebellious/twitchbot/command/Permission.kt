package uno.rebellious.twitchbot.command

data class Permission(val isOwnerOnly: Boolean, val isModOnly: Boolean, val isSubOnly: Boolean) {
    companion object {
        val ANYONE = Permission(false, false, false)
        val OWNER = Permission(true, false, false)
        val SUB = Permission(false, false, true)
        val MOD = Permission(false, true, false)
    }
}




