package uno.rebellious.twitchbot.database

import java.sql.Connection
import java.sql.DriverManager

class DatabaseDAO(
    private var connectionList: HashMap<String, Connection> = HashMap(),
    private val countersDAO: CountersDAO = CountersDAO(connectionList),
    private val responsesDAO: ResponsesDynamoDBDAO = ResponsesDynamoDBDAO(),
    private val quotesDAO: QuotesDAO = QuotesDAO(connectionList),
    private val settingsDAO: SettingsDyanmoDBDAO = SettingsDyanmoDBDAO(),
    private val spotifyDAO: SpotifyDynamoDBDAO = SpotifyDynamoDBDAO(),
    private val waypointDAO: WaypointDAO = WaypointDAO(connectionList)
) : ICounters by countersDAO, IQuotes by quotesDAO, ISettings by settingsDAO, IResponse by responsesDAO,
    ISpotify by spotifyDAO, IWaypoint by waypointDAO {

    init {
        setupSettings() //Set up Settings DB
        val channelList = settingsDAO.getListOfChannels()
        connect(channelList)
        setupAllChannels()
        setupSpotifyForAllChannels(channelList)
        responsesDAO.createTablesForChannels(channelList)
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

    private fun setupSpotifyForAllChannels(channels: Array<Channel>) {
        spotifyDAO.createTableForChannel(channels)
    }

    private fun setupAllChannels() {

        connectionList.forEach {

            quotesDAO.createQuotesTable(it.value)
            countersDAO.setupCounters(it.value)
            waypointDAO.setupWaypoints(it.value)
        }
    }
}