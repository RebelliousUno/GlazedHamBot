package uno.rebellious.twitchbot.model

/**
 * Created by rebel on 16/07/2017.
 */

data class LastFMResponse(val recenttracks: RecentTrack)

data class RecentTrack(val track: List<Track>, val attr: LastFMAttr)

data class Track(
    val artist: Artist,
    val name: String,
    val streamable: String,
    val mbid: String,
    val album: Album,
    val url: String,
    val image: List<LastFMImage>,
    val date: LastFMDate
)

data class Artist(val text: String, val mbid: String)

data class Album(val text: String, val mbid: String)

data class LastFMImage(val text: String, val size: String)

data class LastFMDate(val uts: String, val text: String)

data class LastFMAttr(
    val user: String,
    val page: String,
    val perPage: String,
    val totalPages: String,
    val total: String
)