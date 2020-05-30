package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.twitchbot.database.DatabaseDAO
import uno.rebellious.twitchbot.model.Waypoint
import uno.rebellious.twitchbot.model.WaypointCoordinate
import java.lang.NumberFormatException

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
            Permission(false, false, false)
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
            Permission(false, true, false)
        ) { _: TwitchUser, content: List<String> ->

            //parse the command
            //add x, y, z, name
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
                        errorMessage = "Invalid Coordinate ${split.subList(0,3)}"
                        null
                    }
                    if (waypointCoordinate!=null) {
                        val waypoint = Waypoint(split[3], waypointCoordinate)
                        val waypointId = database.addWaypoint(channel, waypoint)
                        if (waypointId > 0)
                            twirk.channelMessage("Waypoint: ${waypoint.waypoint}(${waypoint.coordinate.x}, ${waypoint.coordinate.y}, ${waypoint.coordinate.z}) added as $waypointId")
                        else
                            twirk.channelMessage("Waypoint: ${waypoint.waypoint}(${waypoint.coordinate.x}, ${waypoint.coordinate.y}, ${waypoint.coordinate.z}) not added")
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
            Permission(false, true, false)
        ) { _: TwitchUser, content: List<String> ->
            twirk.channelMessage("deleteWaypointCommand")
        }
    }

    private fun listWaypointCommand(): Command {
        return Command(
            prefix,
            "waypoint list",
            "Usage: ${prefix}waypoint",
            Permission(false, false, false)
        ) { _: TwitchUser, content: List<String> ->
            twirk.channelMessage("listWaypointCommand")
        }
    }


    private fun findWaypointCommand(): Command {
        return Command(
            prefix,
            "waypoint find",
            "Usage: ${prefix}waypoint",
            Permission(false, false, false)
        ) { _: TwitchUser, content: List<String> ->
            twirk.channelMessage("findWaypointCommand")
        }
    }

    private fun distanceWaypointCommand(): Command {
        return Command(
            prefix,
            "waypoint distance",
            "Usage: ${prefix}waypoint",
            Permission(false, false, false)
        ) { _: TwitchUser, content: List<String> ->
            twirk.channelMessage("distanceWaypointCommand")
        }
    }
}