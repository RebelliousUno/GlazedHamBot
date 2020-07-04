package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.twitchbot.command.model.Permission
import uno.rebellious.twitchbot.database.DatabaseDAO
import uno.rebellious.twitchbot.model.Counter
import java.math.BigDecimal
import java.math.RoundingMode

class CounterCommands(
    private val prefix: String,
    private val twirk: Twirk,
    private val channel: String,
    private val database: DatabaseDAO
) : CommandList() {
    init {
        commandList.add(createCounterCommand())
        commandList.add(addCountCommand())
        commandList.add(removeCountCommand())
        commandList.add(resetCountCommand())
        commandList.add(listCountersCommand())
        commandList.add(deleteCounterCommand())
        commandList.add(resetAllCountersCommand())
        commandList.add(meanCounterListCommand())
        commandList.add(meanCounterCommand())
    }

    private fun meanCounterCommand(): Command {
        val helpString = "Usage: ${prefix}mean breaks - Gets the mean count for a particular counter"
        return Command(
            prefix,
            "mean",
            helpString,
            Permission.ANYONE
        ) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                val counter = content[1]
                val counterList = counterListMap()
                if (counterList.containsKey(counter) && counterList.containsKey("stream") && counterList["stream"]?.total ?: 0 > 0) {
                    val counterValue = counterList[counter]?.total ?: 0
                    val streamCounter = counterList["stream"]?.total ?: 1
                    val meanValue = counterValue / (streamCounter).toDouble()
                    twirk.channelMessage(
                        "Mean $counter per stream ($counterValue/$streamCounter) - ${BigDecimal(
                            meanValue
                        ).setScale(2, RoundingMode.HALF_EVEN)}"
                    )
                }
            }
        }
    }

    private fun counterListMap(): Map<String, Counter> {
        return database.showCountersForChannel(channel, true)
            .map { Pair(it.command, it) }
            .toMap()
    }

    private fun meanCounterListCommand(): Command {
        val helpString = "Usage: ${prefix}meanCounterList - Gets the average counts per stream"
        return Command(
            prefix,
            "meancounterlist",
            helpString,
            Permission.ANYONE
        ) { _: TwitchUser, _: List<String> ->
            val list = counterListMap()
            val streamCounter = list["stream"]
            if (streamCounter != null && streamCounter.total > 0) {
                val meanValues = list
                    .filter { it.key != "stream" }
                    .mapValues { it.value.total / streamCounter.total.toDouble() }
                    .map { "${it.key}: ${BigDecimal(it.value).setScale(2, RoundingMode.HALF_EVEN)}" }
                twirk.channelMessage("Per Stream (${streamCounter.total}) - $meanValues")
            }
        }
    }

    private fun resetAllCountersCommand(): Command {
        val helpString = "Usage: ${prefix}resetAllCounters - resets today's count for all counters"
        return Command(
            prefix,
            "resetallcounters",
            helpString,
            Permission.MOD_ONLY
        ) { _: TwitchUser, _: List<String> ->
            database.showCountersForChannel(channel, true)
                .forEach {
                    database.resetTodaysCounterForChannel(channel, it)
                }
            twirk.channelMessage(database.showCountersForChannel(channel, true).toString())
        }
    }

    private fun deleteCounterCommand(): Command {
        val helpString = ""
        return Command(
            prefix,
            "deletecounter",
            helpString,
            Permission.MOD_ONLY
        ) { _: TwitchUser, content: List<String> ->
            if (content.size == 2) {
                database.removeCounterForChannel(channel, Counter(command = content[1]))
            } else {
                twirk.channelMessage(helpString)
            }
        }
    }

    private fun resetCountCommand(): Command {
        val helpString = "Usage ${prefix}resetCount count - resets today's count for a counter"
        return Command(
            prefix,
            "resetcount",
            helpString,
            Permission.MOD_ONLY
        ) { _: TwitchUser, content: List<String> ->
            if (content.size == 2) {
                database.resetTodaysCounterForChannel(channel, Counter(command = content[1]))
            } else {
                twirk.channelMessage(helpString)
            }
        }
    }

    private fun listCountersCommand(): Command {
        val helpString = ""
        return Command(
            prefix,
            "counterlist",
            helpString,
            Permission.ANYONE
        ) { _: TwitchUser, _: List<String> ->
            val countersForChannel = database.showCountersForChannel(channel, false)
            twirk.channelMessage(countersForChannel.map { it.totalString }.toString())
        }
    }

    private fun removeCountCommand(): Command {
        val helpString =
            "Usage: ${prefix}removecount counterName [amount]- eg. ${prefix}removecount fall or ${prefix}removecount fall 2"
        return Command(
            prefix,
            "removecount",
            helpString,
            Permission.MOD_ONLY
        ) { _: TwitchUser, content: List<String> ->
            val counter = Counter(command = content[1])
            try {
                val by = if (content.size == 3) {
                    Integer.parseInt(content[2])
                } else {
                    1
                }
                if (by > 0) {
                    database.incrementCounterForChannel(channel, counter, -by)
                    val newCounter = database.getCounterForChannel(channel, counter)
                    if (!newCounter.isEmpty())
                        twirk.channelMessage(newCounter.outputString)
                } else
                    twirk.channelMessage("${content[2]} is not a valid number to decrement by")
            } catch (e: NumberFormatException) {
                twirk.channelMessage("${content[2]} is not a valid number to decrement by")
            }
        }
    }

    private fun addCountCommand(): Command {
        val helpString =
            "Usage: ${prefix}addcount counterName [amount]- eg. ${prefix}addcount fall or ${prefix}addcount fall 2"
        return Command(
            prefix,
            "addcount",
            helpString,
            Permission.MOD_ONLY
        ) { _: TwitchUser, content: List<String> ->
            val counter = Counter(command = content[1])
            try {
                val by = if (content.size == 3) {
                    Integer.parseInt(content[2])
                } else {
                    1
                }
                if (by > 0) {
                    database.incrementCounterForChannel(channel, counter, by)
                    val newCounter = database.getCounterForChannel(channel, counter)
                    if (!newCounter.isEmpty())
                        twirk.channelMessage(newCounter.outputString)
                } else twirk.channelMessage("${content[2]} is not a valid number to increment by")
            } catch (e: NumberFormatException) {
                twirk.channelMessage("${content[2]} is not a valid number to increment by")
            }
        }
    }

    private fun createCounterCommand(): Command {
        val helpString =
            "Usage: ${prefix}createCounter counterName singular plural - eg. ${prefix}createCounter fall fall falls"
        return Command(
            prefix,
            "createcounter",
            helpString,
            Permission.MOD_ONLY
        ) { _: TwitchUser, content: List<String> ->
            if (content.size == 3) {
                try {
                    val counter = Counter(
                        command = content[1],
                        singular = content[2].split(" ")[0],
                        plural = content[2].split(" ")[1]
                    )
                    database.createCounterForChannel(channel, counter)
                } catch (e: IndexOutOfBoundsException) {
                    twirk.channelMessage(helpString)
                }
            } else {
                twirk.channelMessage(helpString)
            }
        }
    }
}
