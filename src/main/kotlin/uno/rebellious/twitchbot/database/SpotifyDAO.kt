package uno.rebellious.twitchbot.database

import uno.rebellious.twitchbot.model.SpotifyToken
import java.sql.Connection
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.*

class SpotifyDAO(private val connectionList: HashMap<String, Connection>) : ISpotify {

    private fun getConnectionForChannel(channel: String) = connectionList[channel]

    override fun setTokensForChannel(
        channel: String,
        accessToken: String,
        refreshToken: String,
        expiryTime: LocalDateTime
    ) {
        val con = getConnectionForChannel(channel)

        val sql = "update spotifysettings set refreshToken = ?, accessToken = ?, expires = ? where ROWID = 1"
        val statement = con?.prepareStatement(sql)
        statement?.run {
            setString(1, refreshToken)
            setString(2, accessToken)
            setTimestamp(3, Timestamp.valueOf(expiryTime))
            executeUpdate()

        }
    }

    override fun getTokensForChannel(channel: String): SpotifyToken? {
        val sql = "select * from spotifysettings limit 1"
        val con = getConnectionForChannel(channel)
        val statement = con?.createStatement()
        val result = statement?.executeQuery(sql)
        return result?.run {
            if (next())
                SpotifyToken(
                    authCode = getString("authCode").orEmpty(),
                    accessToken = getString("accessToken"),
                    refreshToken = getString("refreshToken"),
                    expiryTime = getTimestamp("expires")?.toLocalDateTime()
                )
            else null
        }
    }
}