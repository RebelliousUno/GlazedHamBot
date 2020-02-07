package uno.rebellious.twitchbot.model

import com.jayway.jsonpath.JsonPath

class SpotifyResponse(val json: String) {

    val ctx = JsonPath.parse(json)

    val track = ctx.read<String>("$.item.album.name")
    val album = ctx.read<String>("$.item.artists[0].name")
    val artist = ctx.read<String>("$.item.name")

}