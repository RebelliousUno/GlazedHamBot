package uno.rebellious.twitchbot.command.model

data class Permission(val isOwnerOnly: Boolean, val isModOnly: Boolean, val isSubOnly: Boolean) {
    companion object {
        val OWNER_ONLY = Permission(true, false, false)
        val MOD_ONLY = Permission(false, true, false)
        val SUB_ONLY = Permission(false, false, true)
        val ANYONE = Permission(false, false, false)
    }
}
