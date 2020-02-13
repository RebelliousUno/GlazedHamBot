package uno.rebellious.twitchbot.model

import java.time.LocalDateTime

data class SpotifyToken(val authCode: String, val accessToken: String?, val refreshToken: String?, val expiryTime: LocalDateTime?)