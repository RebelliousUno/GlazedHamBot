package uno.rebellious.twitchbot.model

import com.jayway.jsonpath.JsonPath

class SpotifyResponse(val json: String) {

    val ctx = JsonPath.parse(json)

    val album = ctx.read<String>("$.item.album.name")
    val artist = ctx.read<String>("$.item.artists[0].name")
    val track = ctx.read<String>("$.item.name")

}

class SpotifyAccessTokenResponse(val json: String) {
    val ctx = JsonPath.parse(json)

    val accessToken = ctx.read<String>("$.access_token")
    val refreshToken = ctx.read<String>("$.refresh_token")
    val expiry = ctx.read<Int>("$.expires_in")
    //Body : {"access_token":"BQCm_1tNQGnnX8q51VEPyf8IQkS6xL7IWHZ1nAjxPW4zz9qNQCd5goCY-o6Ok7zNbcrtrjM34rwxhVlh-wRqEMPDetLFqikmYGp5g8zzOIUbosya9LeCFZH2gRGGuHt-lgPNmacsWReHDSCgvV6Ti0GGRA","token_type":"Bearer","expires_in":3600,"refresh_token":"AQDLYAfD1SxVrqTWjKGM5UnA7UD6FYkpgUNRFYKRWqp4FLiifvAyuId2fH4IaxZGN1szPMptxLdSLGgAp9CaAXqQGdw_SWjoK6LzU5BCQcta98hP16pGnOzgxaK-t6sTc6I","scope":"user-read-playback-state"}
}

class SpotifyRefreshTokenResponse(val json: String) {
    val ctx = JsonPath.parse(json)
    val accessToken = ctx.read<String>("$.access_token")
    val expiry = ctx.read<Int>("$.expires_in")

}