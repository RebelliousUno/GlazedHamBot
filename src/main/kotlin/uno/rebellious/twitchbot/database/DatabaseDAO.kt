package uno.rebellious.twitchbot.database

import java.sql.Connection
import java.sql.DriverManager

class DatabaseDAO(
    private var connectionList: HashMap<String, Connection> = HashMap(),
    private val countersDAO: CountersDAO = CountersDAO(connectionList),
    private val responsesDAO: ResponsesDAO = ResponsesDAO(connectionList),
    private val quotesDAO: QuotesDAO = QuotesDAO(connectionList),
    private val settingsDAO: SettingsDAO = SettingsDAO(connectionList),
    private val spotifyDAO: SpotifyDAO = SpotifyDAO(connectionList),
    private val waypointDAO: WaypointDAO = WaypointDAO(connectionList)
) : ICounters by countersDAO, IQuotes by quotesDAO, ISettings by settingsDAO, IResponse by responsesDAO,
    ISpotify by spotifyDAO, IWaypoint by waypointDAO {

    init {
        setupSettings() //Set up Settings DB
        val channelList = settingsDAO.getListOfChannels()
        connect(channelList)
        setupAllChannels()
    }

    private fun setupSettings() {
        settingsDAO.createChannelsTable()
        if (settingsDAO.getListOfChannels().isEmpty()) { // Set up default Channel
            addChannel("glazedhambot", "!")
        }
    }

    private fun connect(channels: Array<Channel>) {
        channels.forEach {
            val con = DriverManager.getConnection("jdbc:sqlite:${it.channel.toLowerCase()}.db")
            connectionList[it.channel] = con
        }
    }

    private fun setupAllChannels() {
        connectionList.forEach {
            responsesDAO.createResponseTable(it.value)
            quotesDAO.createQuotesTable(it.value)
            countersDAO.setupCounters(it.value)
            waypointDAO.setupWaypoints(it.value)
        }
    }
}