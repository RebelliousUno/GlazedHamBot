package uno.rebellious.twitchbot.model

import java.time.LocalDateTime

data class Quote(val id: Int, val quote: String, val author: String, val date: LocalDateTime)