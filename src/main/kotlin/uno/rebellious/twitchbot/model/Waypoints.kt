package uno.rebellious.twitchbot.model

data class WaypointCoordinate(val x: Int, val y: Int, val z: Int)

data class Waypoint(val waypoint: String, val coordinate: WaypointCoordinate)

enum class WaypointOrder {
    X, Y, Z, ID, NAME
}