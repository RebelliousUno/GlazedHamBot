package uno.rebellious.twitchbot.model

data class ChattersResponse(val _links: Any, val chatter_count: Int, val chatters: Chatters)

data class Chatters(
    val broadcaster: ArrayList<String>,
    val vips: ArrayList<String>,
    val moderators: ArrayList<String>,
    val staff: ArrayList<String>,
    val admins: ArrayList<String>,
    val global_mods: ArrayList<String>,
    val viewers: ArrayList<String>
)