package uno.rebellious.twitchbot.database

import uno.rebellious.twitchbot.model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

interface IWaypoint {
    fun addWaypoint(channel: String, waypoint: Waypoint): Int
    fun deleteWaypointByName(channel: String, waypoint: String)
    fun deleteWaypointById(channel: String, id: Int)
    fun listWaypoints(channel: String, orderBy: WaypointOrder): List<Waypoint>
    fun findWaypointById(channel: String, id: Int, deleted: Boolean = false): Waypoint?
    fun findWaypointByCoords(
        channel: String,
        coordinate: WaypointCoordinate,
        deleted: Boolean = false
    ): Pair<Double, Waypoint>

    fun findWaypointByName(channel: String, waypoint: String, deleted: Boolean = false): Waypoint?
}

interface ISpotify {
    fun setTokensForChannel(channel: String, accessToken: String, refreshToken: String, expiryTime: LocalDateTime)
    fun getTokensForChannel(channel: String): SpotifyToken?
}

interface IResponse {
    fun findResponse(channel: String, command: Response): Response
    fun setResponse(channel: String, response: Response)
    fun removeResponse(channel: String, command: Response)
    fun getAllCommandList(channel: String): ArrayList<String>

}

interface ISettings {
    fun createChannelsTable()
    fun leaveChannel(channel: String)
    fun addChannel(newChannel: String, prefix: String = "!")
    fun getPrefixForChannel(channel: String): String
    fun setPrefixForChannel(channel: String, prefix: String)
    fun getListOfChannels(): Array<Channel>
}

interface ICounters {
    fun createSumCounterForChannel(channel: String, sumCounterName: String, counters: List<Counter>)
    fun createCounterForChannel(channel: String, counter: Counter)
    fun removeCounterForChannel(channel: String, counter: Counter)
    fun incrementCounterForChannel(channel: String, counter: Counter, by: Int = 1)
    fun getCounterForChannel(channel: String, counter: Counter, consistentRead: Boolean = false): Counter
    fun getSumCounterForChannel(channel: String, sumCounterName: String, consistentRead: Boolean = false): String
    fun resetTodaysCounterForChannel(channel: String, counter: Counter)
    fun showCountersForChannel(channel: String, includeStream: Boolean): List<Counter>
}

interface IQuotes {
    fun addQuoteForChannel(channel: String, date: LocalDate, person: String, quote: String): Int
    fun delQuoteForChannel(channel: String, quoteId: Int)
    fun editQuoteForChannel(channel: String, quoteId: Int, date: LocalDate?, person: String, quote: String)
    fun getQuoteForChannelById(channel: String, quoteId: Int): String
    fun getRandomQuoteForChannel(channel: String): String
    fun findQuoteByAuthor(channel: String, author: String): String
    fun findQuoteByKeyword(channel: String, keyword: String): String
    fun undeleteQuoteForChannel(channel: String, quoteId: Int)
    fun getAllQuotesForChannel(channel: String): List<Quote>
}