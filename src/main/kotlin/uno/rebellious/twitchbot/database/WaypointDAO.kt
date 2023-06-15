package uno.rebellious.twitchbot.database

import uno.rebellious.twitchbot.model.Waypoint
import uno.rebellious.twitchbot.model.WaypointCoordinate
import uno.rebellious.twitchbot.model.WaypointOrder
import java.sql.Connection
import java.sql.ResultSet

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
        val sql = "Update waypoints set deleted = ? where name = ?"
        connectionList[channel]?.prepareStatement(sql)?.apply {
            setBoolean(1, true)
            setString(2, waypoint)
            executeUpdate()
        }
    }

    override fun deleteWaypointById(channel: String, id: Int) {
        val sql = "Update waypoints set deleted = ? where id = ?"
        connectionList[channel]?.prepareStatement(sql)?.apply {
            setBoolean(1, true)
            setInt(2, id)
            executeUpdate()
        }
    }

    override fun listWaypoints(channel: String, orderBy: WaypointOrder): List<Waypoint> {
        return getAllWaypointsForChannel(channel, false, orderBy)
    }

    private fun findWaypointResult(result: ResultSet?): Waypoint? {
        return result?.run {
            if (next()) {
                val waypoint = getString("name")
                val x = getInt("x")
                val y = getInt("y")
                val z = getInt("z")
                val id = getInt("id")
                Waypoint(waypoint, WaypointCoordinate(x, y, z), id)
            } else {
                null
            }
        }
    }

    override fun findWaypointByName(channel: String, waypoint: String, deleted: Boolean): Waypoint? {
        val sql = "SELECT * from waypoints where name like concat('%',?,'%') AND deleted = ? limit 1"
        return findWaypointResult(connectionList[channel]?.prepareStatement(sql)?.run {
            setString(1, waypoint)
            setBoolean(2, deleted)
            executeQuery()
        })
    }

    override fun findWaypointById(channel: String, id: Int, deleted: Boolean): Waypoint? {
        val sql = "SELECT * from waypoints where id = ? AND deleted = ? limit 1"
        return findWaypointResult(connectionList[channel]?.prepareStatement(sql)?.run {
            setInt(1, id)
            setBoolean(2, deleted)
            executeQuery()
        })
    }

    override fun findWaypointByCoords(
        channel: String,
        coordinate: WaypointCoordinate,
        deleted: Boolean
    ): Pair<Double, Waypoint> {
        val waypoints = getAllWaypointsForChannel(channel, deleted, WaypointOrder.ID)
        return findClosestWaypoint(coordinate, waypoints)
    }

    private fun findClosestWaypoint(coordinate: WaypointCoordinate, waypoints: List<Waypoint>): Pair<Double, Waypoint> {
        val sortedList = waypoints.map { Pair(it.distanceToWaypoint(coordinate), it) }.sortedBy { it.first }
        return sortedList.first()
    }


    private fun getAllWaypointsForChannel(
        channel: String,
        deleted: Boolean,
        orderBy: WaypointOrder
    ): List<Waypoint> {
        val waypointList = ArrayList<Waypoint>()
        val sql = if (!deleted) {
            "Select * from waypoints where deleted = 0 order by ${orderBy.column}"
        } else {
            "Select * from waypoints order by ${orderBy.column}"
        }
        connectionList[channel]?.createStatement()?.run {
            executeQuery(sql)
        }?.run {
            while (next()) {
                waypointList.add(
                    Waypoint(
                        getString("name"),
                        WaypointCoordinate(getInt("x"), getInt("y"), getInt("z")),
                        getInt("id")
                    )
                )
            }
        }
        return waypointList
    }


}