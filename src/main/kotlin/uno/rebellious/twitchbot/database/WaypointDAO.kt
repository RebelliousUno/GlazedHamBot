package uno.rebellious.twitchbot.database

import uno.rebellious.twitchbot.model.Waypoint
import uno.rebellious.twitchbot.model.WaypointCoordinate
import uno.rebellious.twitchbot.model.WaypointOrder
import java.sql.Connection

class WaypointDAO(private val connectionList: HashMap<String, Connection>) : IWaypoint {
    fun setupWaypoints(connection: Connection) {
        val waypointTableSQL = """
            CREATE TABLE IF NOT EXISTS "waypoints" (
            	"id"	INTEGER PRIMARY KEY AUTOINCREMENT,
            	"name"	TEXT,
            	"x"	INTEGER,
            	"y"	INTEGER,
            	"z"	INTEGER,
            	"deleted"	INTEGER DEFAULT 0
            );
        """.trimIndent()
        connection.createStatement()?.apply {
            queryTimeout = 30
            executeUpdate(waypointTableSQL)
        }
    }

    override fun addWaypoint(channel: String, waypoint: Waypoint): Int {
        val sql = "insert into waypoints(name, x, y, z) values(?, ?, ?, ?)"
        return connectionList[channel]?.prepareStatement(sql)?.run {
            setString(1, waypoint.waypoint)
            setInt(2, waypoint.coordinate.x)
            setInt(3, waypoint.coordinate.y)
            setInt(4, waypoint.coordinate.z)
            executeUpdate()
            val id = generatedKeys
            if (id.next())
                id.getInt(1)
            else 0
        } ?: 0
    }

    override fun deleteWaypointByName(channel: String, waypoint: String) {
        TODO("Not yet implemented")
    }

    override fun deleteWaypointById(channel: String, id: Int) {
        TODO("Not yet implemented")
    }

    override fun listWaypoints(channel: String, waypointOrder: WaypointOrder): String {
        TODO("Not yet implemented")
    }

    override fun findWaypointByName(channel: String, waypoint: String): Waypoint {
        TODO("Not yet implemented")
    }

    override fun findWaypointById(channel: String, id: Int): Waypoint {
        TODO("Not yet implemented")
    }

    override fun findWaypointByCoords(channel: String, coordinate: WaypointCoordinate): Waypoint {
        TODO("Not yet implemented")
    }

    override fun distanceToWaypoint(channel: String, coordinate: WaypointCoordinate): Int {
        TODO("Not yet implemented")
    }
}