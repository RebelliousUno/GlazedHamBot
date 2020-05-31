package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.twitchbot.BotManager.pastebin
import uno.rebellious.twitchbot.command.model.Permission
import uno.rebellious.twitchbot.database.DatabaseDAO
import uno.rebellious.twitchbot.model.Waypoint
import uno.rebellious.twitchbot.model.WaypointCoordinate
import uno.rebellious.twitchbot.model.WaypointOrder
import uno.rebellious.twitchbot.model.waypointToString
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.regex.Pattern

class WaypointCommands(
    private val prefix: String,
    private val twirk: Twirk,
    private val channel: String,
    private val database: DatabaseDAO
) : CommandList() {
    init {
        commandList.add(waypointCommand())
    }

    private fun waypointCommand(): Command {
        return Command(
            prefix,
            "waypoint",
            "Usage: ${prefix}waypoint",
            Permission.ANYONE
        ) { user: TwitchUser, content: List<String> ->
            val command = when (if (content.size > 1) content[1] else "") {
                "add" -> addWaypointCommand()
                "delete" -> deleteWaypointCommand()
                "list" -> listWaypointCommand()
                "find" -> findWaypointCommand()
                "distance" -> distanceWaypointCommand()
                else -> null
            }
            if (command?.canUseCommand(user) == true) {
                command.action(user, content.drop(1)) //remove the first element which would be !waypoint
            }
        }
    }

    private fun addWaypointCommand(): Command {
        return Command(
            prefix,
            "waypoint add",
            "Usage: ${prefix}waypoint",
            Permission.MOD_ONLY
        ) { _: TwitchUser, content: List<String> ->
            var errorMessage: String? = null
            if (content.size > 1) {
                val split = content[1].split(",").map { it.trim() }
                if (split.size == 4) {
                    val waypointCoordinate = try {
                        val x = split[0].toInt()
                        val y = split[1].toInt()
                        val z = split[2].toInt()
                        WaypointCoordinate(x, y, z)
                    } catch (nfe: NumberFormatException) {
                        errorMessage = "Invalid Coordinate ${split.subList(0, 3)}"
                        null
                    }
                    if (waypointCoordinate != null) {
                        val waypoint = Waypoint(split[3], waypointCoordinate)
                        val waypointId = database.addWaypoint(channel, waypoint)
                        if (waypointId > 0)
                            twirk.channelMessage("Waypoint: ${waypoint.waypointToString()} added as $waypointId")
                        else
                            twirk.channelMessage("Waypoint: ${waypoint.waypointToString()} not added")
                    }
                } else {
                    errorMessage = "(missing name)Usage: ${prefix}waypoint add x, y, z, name"
                }
            } else {
                errorMessage = "(missing commands)Usage: ${prefix}waypoint add x, y, z, name"
            }
            if (errorMessage != null) {
                twirk.channelMessage(errorMessage)
            }
        }
    }

    private fun deleteWaypointCommand(): Command {
        return Command(
            prefix,
            "waypoint delete",
            "Usage: ${prefix}waypoint",
            Permission.MOD_ONLY
        ) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                val id = content[1].toIntOrNull()
                if (id != null) {
                    database.deleteWaypointById(channel, id)
                } else {
                    database.deleteWaypointByName(channel, content[1])
                }
            } else {
                twirk.channelMessage("!waypoint delete id/name")
            }
        }
    }

    private fun listWaypointCommand(): Command {
        return Command(
            prefix,
            "waypoint list",
            "Usage: ${prefix}waypoint",
            Permission.ANYONE
        ) { _: TwitchUser, content: List<String> ->
            val orderString = if (content.size == 2) {
                content[1]
            } else {
                "id"
            }
            val orderBy = when (orderString.trim()) {
                "name" -> WaypointOrder.NAME
                "x" -> WaypointOrder.X
                "y" -> WaypointOrder.Y
                "z" -> WaypointOrder.Z
                "id" -> WaypointOrder.ID
                else -> WaypointOrder.ID
            }
            val waypoints = database.listWaypoints(channel, orderBy)
            val waypointString = pastebin.parseWaypoints(waypoints)
            val paste = pastebin.createPaste("$channel: Waypoints", waypointString)
            twirk.channelMessage("Waypoint List: $paste")
        }
    }

    private fun findWaypointCommand(): Command {
        return Command(
            prefix,
            "waypoint find",
            "Usage: ${prefix}waypoint",
            Permission.ANYONE
        ) { _: TwitchUser, content: List<String> ->
            //content find ....
            //content.size == 2?
            var errorMessage: String? = null
            if (content.size != 2) {
                errorMessage = "${prefix}waypoint find name/id"
            } else {
                val pattern = Pattern.compile("(\\s*-?\\d+\\s*),(\\s*-?\\d+\\s*),(\\s*-?\\d+\\s*)")
                val id = content[1].toIntOrNull()
                var distance: Double? = null
                val waypoint = when {
                    content[1].matches(pattern.toRegex()) -> {
                        val coords = content[1].split(',').map { it.trim().toInt() }
                        twirk.channelMessage("coords $coords")
                        val waypointAndDistance =
                            database.findWaypointByCoords(channel, WaypointCoordinate(coords[0], coords[1], coords[2]))
                        distance = waypointAndDistance.first
                        waypointAndDistance.second
                    }
                    id == null -> {
                        database.findWaypointByName(channel, content[1])
                    }
                    else -> {
                        database.findWaypointById(channel, id)
                    }
                }
                if (waypoint != null) {
                    if (distance != null) {
                        twirk.channelMessage(
                            "Closest Waypoint: ${waypoint.waypointToString()} Distance: ${BigDecimal(
                                distance
                            ).setScale(2, RoundingMode.HALF_EVEN)}"
                        )
                    } else {
                        twirk.channelMessage("Waypoint: ${waypoint.waypointToString()}")
                    }
                } else {
                    errorMessage = "Waypoint not found"
                }
            }
            if (errorMessage != null)
                twirk.channelMessage(errorMessage)
        }
    }

    private fun distanceWaypointCommand(): Command {
        return Command(
            prefix,
            "waypoint distance",
            "Usage: ${prefix}waypoint",
            Permission.ANYONE
        ) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                val pattern = Pattern.compile("[\\w\\s]+,(\\s*-?\\d+\\s*),(\\s*-?\\d+\\s*),(\\s*-?\\d+\\s*)")
                if (content[1].matches(pattern.toRegex())) {
                    val waypointList = database.listWaypoints(channel, WaypointOrder.NAME)
                    val split = content[1].split(',').map { it.trim() }
                    val waypoint = waypointList.find { it.waypoint == split[0] }
                    val distance = waypoint?.distanceToWaypoint(
                        WaypointCoordinate(
                            split[1].toInt(),
                            split[2].toInt(),
                            split[3].toInt()
                        )
                    )
                    if (distance != null) {
                        twirk.channelMessage(
                            "Distance to ${waypoint.waypointToString()}: ${BigDecimal(distance).setScale(
                                2,
                                RoundingMode.HALF_EVEN
                            )}"
                        )
                    } else {
                        twirk.channelMessage("Waypoint not found")
                    }
                } else {
                    twirk.channelMessage("!waypoint distance name, x, y, z ")
                }
            } else {
                twirk.channelMessage("!waypoint distance name, x, y, z ")
            }
        }
    }
}