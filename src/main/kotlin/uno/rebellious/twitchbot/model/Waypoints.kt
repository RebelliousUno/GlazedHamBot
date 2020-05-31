package uno.rebellious.twitchbot.model

import kotlin.math.pow
import kotlin.math.sqrt

data class WaypointCoordinate(val x: Int, val y: Int, val z: Int)

data class Waypoint(val waypoint: String, val coordinate: WaypointCoordinate, var id: Int = 0) {
    fun distanceToWaypoint(remote: WaypointCoordinate): Double {
        val x2 = ((remote.x) - (this.coordinate.x)).toDouble().pow(2)
        val y2 = ((remote.y) - (this.coordinate.y)).toDouble().pow(2)
        val z2 = ((remote.z) - (this.coordinate.z)).toDouble().pow(2)
        return sqrt(x2 + y2 + z2)
    }
}

fun Waypoint.waypointToString() =
    "${this.id}: ${this.waypoint} (${this.coordinate.x}, ${this.coordinate.y}, ${this.coordinate.z})"

enum class WaypointOrder(val column: String) {
    X("x"), Y("y"), Z("z"), ID("id"), NAME("name")

}