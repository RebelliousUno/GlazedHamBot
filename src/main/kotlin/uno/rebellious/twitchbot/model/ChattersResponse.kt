package uno.rebellious.twitchbot.model

data class ChattersResponse(val _links: Any, val chatter_count: Int, val chatters: Chatters)

data class Chatters(
    val broadcaster: List<String>,
    val vips: List<String>,
    val moderators: List<String>,
    val staff: List<String>,
    val admins: List<String>,
    val global_mods: List<String>,
    val viewers: List<String>
)