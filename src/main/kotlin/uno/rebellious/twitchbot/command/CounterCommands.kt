package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.twitchbot.database.DatabaseDAO

class CounterCommands(private val prefix: String, private val twirk: Twirk, private val channel: String, private val database: DatabaseDAO): CommandList() {
    init {
        commandList.add(createCounterCommand())
        commandList.add(addCountCommand())
        commandList.add(removeCountCommand())
        commandList.add(resetCountCommand())
        commandList.add(listCountersCommand())
        commandList.add(deleteCounterCommand())
        commandList.add(resetAllCountersCommand())
        commandList.add(meanCounterListCommand())
    }

    private fun meanCounterListCommand(): Command {
        val helpString = "Usage: ${prefix}meanCounterList - Gets the average counts per stream"
        return Command(prefix, "meancounterlist", helpString, Permission(false, false, false)) { _: TwitchUser, _: List<String> ->
            val list = database.showCountersForChannel(channel, true)
                .map { it.split(":") }
                .map { Pair(it[0], Integer.parseInt(it[1].split("/")[0].trim())) }
                .toMap()
            val streamCounter = list["stream"]
            if (streamCounter != null && streamCounter > 0) {
                val meanValues = list
                    .filter { it.key != "stream" }
                    .mapValues{ it.value / streamCounter.toDouble() }
                    .map { "${it.key}: ${it.value}" }
                twirk.channelMessage(meanValues.toString())
            }
        }
    }

    private fun resetAllCountersCommand(): Command {
        val helpString = "Usage: ${prefix}resetAllCounters - resets today's count for all counters"
        return Command(prefix, "resetallcounters", helpString, Permission(false, true, false)) { _: TwitchUser, _: List<String> ->
            database.showCountersForChannel(channel, true)
                .map { it.split(":")[0] }
                .forEach {
                    database.resetTodaysCounterForChannel(channel, it)
                }
            twirk.channelMessage(database.showCountersForChannel(channel, true).toString())
        }
    }

    private fun deleteCounterCommand(): Command {
        val helpString = ""
        return Command(prefix, "deletecounter", helpString, Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            if (content.size == 2) {
                database.removeCounterForChannel(channel, content[1])
            } else {
                twirk.channelMessage(helpString)
            }
        }
    }

    private fun resetCountCommand(): Command {
        val helpString = "Usage ${prefix}resetCount count - resets today's count for a counter"
        return Command(prefix, "resetcount", helpString, Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            if (content.size == 2) {
                database.resetTodaysCounterForChannel(channel, content[1])
            } else {
                twirk.channelMessage(helpString)
            }
        }
    }

    private fun listCountersCommand(): Command {
        val helpString = ""
        return Command(prefix, "counterlist", helpString, Permission(false, false, false)) { _: TwitchUser, _: List<String> ->
            twirk.channelMessage(database.showCountersForChannel(channel, false).toString())
        }
    }

    private fun removeCountCommand(): Command {
        val helpString = "Usage: ${prefix}removecount counterName [amount]- eg. ${prefix}removecount fall or ${prefix}removecount fall 2"
        return Command(prefix, "removecount", helpString, Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            val counter = content[1]
            try {
                val by = if (content.size == 3) {
                    Integer.parseInt(content[2])
                } else {
                    1
                }
                if (by > 0) {
                    database.incrementCounterForChannel(channel, counter, -by)
                    twirk.channelMessage(database.getCounterForChannel(channel, counter))
                }
                else
                    twirk.channelMessage("${content[2]} is not a valid number to decrement by")
            } catch (e: NumberFormatException) {
                twirk.channelMessage("${content[2]} is not a valid number to decrement by")
            }
        }
    }

    private fun addCountCommand(): Command {
        val helpString = "Usage: ${prefix}addcount counterName [amount]- eg. ${prefix}addcount fall or ${prefix}addcount fall 2"
        return Command(prefix, "addcount", helpString, Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            val counter = content[1]
            try {
                val by = if (content.size == 3) {
                    Integer.parseInt(content[2])
                } else {
                    1
                }
                if (by > 0) {
                    database.incrementCounterForChannel(channel, counter, by)
                    twirk.channelMessage(database.getCounterForChannel(channel, counter))
                }
                else twirk.channelMessage("${content[2]} is not a valid number to increment by")
            } catch (e: NumberFormatException) {
                twirk.channelMessage("${content[2]} is not a valid number to increment by")
            }
        }
    }

    private fun createCounterCommand(): Command {
        val helpString = "Usage: ${prefix}createCounter counterName singular plural - eg. ${prefix}createCounter fall fall falls"
        return Command(prefix, "createcounter", helpString, Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            if (content.size == 3) {
                val counter = content[1]
                val singular = content[2].split(" ")[0]
                val plural = content[2].split(" ")[1]
                database.createCounterForChannel(channel, counter, singular, plural)
            } else {
                twirk.channelMessage(helpString)
            }
        }
    }
}
