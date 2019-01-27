package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.events.TwirkListener
import com.gikk.twirk.types.twitchMessage.TwitchMessage
import com.gikk.twirk.types.users.TwitchUser
import com.github.kittinunf.fuel.Fuel
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import uno.rebellious.twitchbot.BotManager
import uno.rebellious.twitchbot.database.QuotesDAO
import uno.rebellious.twitchbot.model.LastFMResponse
import java.time.LocalDate
import java.time.format.DateTimeParseException
import java.util.*

class PatternCommand (private val twirk: Twirk, private val channel: String) : TwirkListener {
    private val gson = Gson()
    private var jackboxCode = "NO ROOM CODE SET"
    private var commandList = ArrayList<Command>()
    private var responseCommands = ArrayList<Command>()
    private var counterCommands = ArrayList<Command>()
    private var miscCommands = ArrayList<Command>()
    private var adminCommands = ArrayList<Command>()
    private var quoteCommands = ArrayList<Command>()
    private var prefix = "!"
    private val database = BotManager.database
    init {
        prefix = database.getPrefixForChannel(channel)
        if (channel == "rebelliousuno") miscCommands.add(songCommand())
        if (channel == "glazedhambot") adminCommands.add(addChannelCommand())
        adminCommands.add(leaveChannelCommand())
        adminCommands.add(listChannelsCommand())
        responseCommands.add(addCommand())
        responseCommands.add(editCommand())
        responseCommands.add(delCommand())
        miscCommands.add(commandListCommand())
        adminCommands.add(setPrefixCommand())
        counterCommands.add(createCounterCommand())
        counterCommands.add(addCountCommand())
        counterCommands.add(removeCountCommand())
        counterCommands.add(resetCountCommand())
        counterCommands.add(listCountersCommand())
        counterCommands.add(deleteCounterCommand())

        if (channel == "rebelliousuno") miscCommands.add(jackSetCommand())
        if (channel == "rebelliousuno") miscCommands.add(jackCommand())
        miscCommands.add(helpCommand())

        quoteCommands.add(quoteCommand())
        quoteCommands.add(addQuoteCommand())
        quoteCommands.add(editQuoteCommand())
        quoteCommands.add(deleteQuoteCommand())
        quoteCommands.add(undeleteQuoteCommand())

        commandList.addAll(adminCommands)
        commandList.addAll(responseCommands)
        commandList.addAll(counterCommands)
        commandList.addAll(miscCommands)
        commandList.addAll(quoteCommands)


        twirk.channelMessage("Starting up for $channel - prefix is $prefix")
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
        return Command(prefix, "counterlist", helpString, Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            twirk.channelMessage(database.showCountersForChannel(channel).toString())
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
                if (by > 0)
                    database.incrementCounterForChannel(channel, counter, by)
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
                if (by > 0)
                    database.incrementCounterForChannel(channel, counter, by)
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

    private fun deleteQuoteCommand(): Command {
        val helpString = "Usage: ${prefix}delquote quoteid - Deletes quote quoteid"
        return Command(prefix, "delquote", helpString, Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {

                try {
                    val id = content[1].toInt()
                    if (id > 0) {
                        database.delQuoteForChannel(channel, id)
                        twirk.channelMessage("Deleted quote $id")
                    } else {
                        twirk.channelMessage("Quote ids are positive integers")
                    }
                } catch (e: NumberFormatException) {
                    twirk.channelMessage("${content[1]} is not an integer")
                }
            }
        }
    }

    private fun undeleteQuoteCommand(): Command {
        val helpString = "Usage: ${prefix}undelquote quoteid - Undeletes quote quoteid"
        return Command(prefix, "undelquote", helpString, Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {

                try {
                    val id = content[1].toInt()
                    if (id > 0) {
                        database.undeleteQuoteForChannel(channel, id)
                        twirk.channelMessage("Undeleted quote $id")
                    } else {
                        twirk.channelMessage("Quote ids are positive integers")
                    }
                } catch (e: NumberFormatException) {
                    twirk.channelMessage("${content[1]} is not an integer")
                }
            }
        }
    }

    private fun addQuoteCommand(): Command {
        val helpString = "Usage: ${prefix}addquote QUOTE | PERSON | [YYYY-MM-DD] - eg. Adds a quote for Person on Date (optional defaults to today)"
        return Command(prefix, "addquote", helpString, Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            // "!addquote This is the quote | this is the person | this is the date"
            if (content.size > 1) {
                val quoteDetails = "${content[1]} ${content[2]}".split("|")
                val quote = quoteDetails[0].trim()
                val person = if (quoteDetails.size > 1) quoteDetails[1].trim() else "Anon"
                val date = if (quoteDetails.size > 2) try {
                    LocalDate.parse(quoteDetails[2].trim())
                } catch (e: DateTimeParseException) {
                    twirk.channelMessage("Could not parse date ${quoteDetails[2]} - Use format YYYY-MM-DD")
                    null
                } else
                 LocalDate.now()
                if (date != null) {
                    val id = database.addQuoteForChannel(channel, date, person, quote)
                    twirk.channelMessage("Added with ID $id : $quote - $person on $date")
                }
            }
        }
    }

    private fun editQuoteCommand(): Command {
        val helpString = "Usage: ${prefix}editquote QUOTEID [QUOTE] | [PERSON] | [YYYY-MM-DD] - eg. Edits quote QUOTEID - Sections of the quote are optional but needs both ||"
        return Command(prefix, "editquote", helpString, Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            if (content.size > 2) {
                try {
                    val quoteID = Integer.parseInt(content[1])
                    val quoteDetails = content[2].split("|")
                    val quote = quoteDetails[0].trim()
                    val person = if (quoteDetails.size > 1) quoteDetails[1].trim() else ""
                    val date = if (quoteDetails.size > 2 && quoteDetails[2].isNotBlank()) try {
                        LocalDate.parse(quoteDetails[2].trim())
                    } catch (e: DateTimeParseException) {
                        null
                    } else null
                    database.editQuoteForChannel(channel, quoteID, date, person, quote)
                    twirk.channelMessage("Edited quote $quoteID")
                } catch (e: java.lang.NumberFormatException){
                    twirk.channelMessage("${content[1]} is not a number")
                }
            }
        }
    }

    private fun quoteCommand(): Command {
        val helpString = "Usage: ${prefix}quote [SEARCH TERM] - Searches for a quote where Search Term is either quote ID, Author or a keyword"
        return Command(prefix, "quote", helpString, Permission(false, false, false)) { _: TwitchUser, content: List<String> ->
            val message: String
            if (content.size > 1) {
                message = try {
                    val id = Integer.parseInt(content[1])
                    database.getQuoteForChannelById(channel, id)
                } catch (nfe: NumberFormatException) {
                    val searchPhrase = content.subList(1, content.size).joinToString(" ")
                    val byAuthor = database.findQuoteByAuthor(channel, searchPhrase)
                    val byKeyword= database.findQuoteByKeyword(channel, searchPhrase)
                    if (!byAuthor.run { isEmpty() || this == QuotesDAO.QUOTE_NOT_FOUND } ) {
                        "Search By Author - $byAuthor"
                    } else {
                        "Search by Keyword - $byKeyword"
                    }
                }
            } else {
                message = database.getRandomQuoteForChannel(channel)
            }
            twirk.channelMessage(message)
        }
    }

    private fun helpCommand(): Command {
        return Command(prefix, "help" , "Usage: ${prefix}help cmd - to get help for a particular command", Permission(false, false, false)) { twitchUser: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                twirk.channelMessage(commandList.firstOrNull { command -> command.command == content[1] && command.canUseCommand(twitchUser) }?.helpString)
            } else {
                twirk.channelMessage("Usage: ${prefix}help cmd - to get help for a particular command")
            }
        }
    }

    private fun countCommand(): Command {
        return Command(prefix, "", "", Permission(false, false, false)) { _: TwitchUser, content: List<String> ->
            twirk.channelMessage(database.getCounterForChannel(channel, content[0].substring(1)))
        }
    }

    private fun responseCommand(): Command {
        return Command(prefix, "", "", Permission(false, false, false)) { _: TwitchUser, content: List<String> ->
            twirk.channelMessage(database.findResponse(channel, content[0].substring(1)))
        }
    }

    private fun setPrefixCommand(): Command {
        return Command(prefix, "setprefix", "Usage: '${prefix}setprefix !' - Sets the prefix for commands to '!'", Permission(true, false, false)) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                prefix = content[1]
                database.setPrefixForChannel(channel, prefix)
                commandList.forEach { cmd ->
                    cmd.prefix = prefix
                }
            }
        }
    }

    private fun commandListCommand(): Command {
        return Command(prefix, "cmdlist", "Usage: ${prefix}cmdlist - lists the commands for this channel", Permission(false, false, false)) { twitchUser: TwitchUser, content: List<String> ->
            val dbCommands = database.getAllCommandList(channel).map { command ->
                prefix + command
            }.sorted()
            val quoteCmds = quoteCommands.filter{it.canUseCommand(twitchUser)}.map { command ->
                command.prefix + command.command
            }.sorted()L
            val adminCmds = adminCommands.filter{it.canUseCommand(twitchUser)}.map { command ->
                command.prefix + command.command
            }.sorted()
            val miscCmds = miscCommands.filter{it.canUseCommand(twitchUser)}.map { command ->
                command.prefix + command.command
            }.sorted()
            val responseCmds = responseCommands.filter{it.canUseCommand(twitchUser)}.map { command ->
                command.prefix + command.command
            }.sorted()
            val counterCmds = counterCommands.filter{it.canUseCommand(twitchUser)}.map { command ->
                command.prefix + command.command
            }.sorted()
            twirk.channelMessage("Quotes: $quoteCmds, Responses: $dbCommands $responseCmds, Counters: $counterCmds, Misc: $miscCmds,  Admin: $adminCmds")
        }
    }

    private fun delCommand(): Command {
        return Command(prefix, "delcmd", "Usage: ${prefix}delcmd cmd - deletes the command 'cmd' (Mod Only - Custom commands only)", Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            val removeCommand = content[1].toLowerCase(Locale.ENGLISH)
            database.removeResponse(channel, removeCommand)
        }
    }

    private fun addCommand(): Command {
        return Command(prefix, "addcmd", "Usage: ${prefix}addcmd cmd Response Text- Adds the command 'cmd' with the text 'Response Text' (Mod Only - Custom commands only)", Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            if (content.size > 2) {
                val newCommand = content[1].toLowerCase(Locale.ENGLISH)
                val newResponse = content[2]
                database.setResponse(channel, newCommand, newResponse)
            }
        }
    }

    private fun editCommand(): Command {
        return Command(prefix, "editcmd", "Usage: ${prefix}editcmd cmd Response Text- Edits the command 'cmd' with the text 'Response Text' (Mod Only - Custom commands only)", Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            if (content.size > 2) {
                val newCommand = content[1].toLowerCase(Locale.ENGLISH)
                val newResponse = content[2]
                database.setResponse(channel, newCommand, newResponse)
            }
        }
    }

    private fun addChannelCommand(): Command {
        return Command(prefix, "addchannel",
                "Usage: ${prefix}addchannel channeltoAdd - Add a GlazedHamBot to a channel",
                Permission(false, false, false)
        ) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                val newChannel = content[1].toLowerCase(Locale.ENGLISH)
                database.addChannel(newChannel)
                BotManager.startTwirkForChannel(newChannel)
            }
        }
    }

    private fun leaveChannelCommand(): Command {
        return Command(prefix, "hamleave", "Usage: ${prefix}hamleave - Asks the bot to leave the channel (Mod only)",
                Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            database.leaveChannel(channel)
            twirk.channelMessage("Leaving $channel")
            BotManager.stopTwirkForChannel(channel)
        }
    }

    private fun songCommand(): Command {
        return Command(prefix, "song",
                "Usage: ${prefix}song - The last song listened to by $channel",
                Permission(false, false, false)) { _: TwitchUser, content: List<String> ->
            Fuel.get(BotManager.lastFMUrl).responseString { _, _, result ->
                val resultJson: String = result.get().replace("#", "")
                val json = gson.fromJson<LastFMResponse>(resultJson)
                val artist = json.recenttracks.track[0].artist.text
                val track = json.recenttracks.track[0].name
                val album = json.recenttracks.track[0].album.text
                twirk.channelMessage("$channel last listened to $track by $artist from the album $album")
            }}
    }

    private fun listChannelsCommand(): Command {
        return Command(prefix, "listchannels", "Usage: ${prefix}listchannels - Lists all the channels the bot is in", Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            val channelList = database.getListOfChannels()
            twirk.channelMessage("GlazedHamBot is present in $channelList")
        }
    }

    private fun jackSetCommand(): Command {
        return Command(prefix, "jackset", "Usage: ${prefix}jackset ROOM - Sets the jackbox code to ROOM", Permission(false, true, false)) { _: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                jackboxCode = content[1].substring(0,4).toUpperCase()
                twirk.channelMessage("Jackbox Code Set to $jackboxCode you can get the link by typing ${prefix}jack into chat")
            }
        }
    }

    private fun jackCommand(): Command {
        return Command(prefix, "jack", "Usage: ${prefix}jack - Gets the jackbox code for the current game", Permission(false, false, false)) { _: TwitchUser, content: List<String> ->
            twirk.channelMessage("Jackbox Code Set to $jackboxCode you can get the link by typing ${prefix}jack into chat")
        }
    }

    override fun onPrivMsg(sender: TwitchUser, message: TwitchMessage) {
        val content: String = message.content.trim()
        if (!content.startsWith(prefix)) return

        val splitContent = content.split(' ', ignoreCase = true, limit = 3)
        val command = splitContent[0].toLowerCase(Locale.ENGLISH)

        commandList
                .filter { command.startsWith("${it.prefix}${it.command}") }
                .firstOrNull { it.canUseCommand(sender) }
                ?.action?.invoke(sender, splitContent) ?: run {
            countCommand().action.invoke(sender, splitContent)
            responseCommand().action.invoke(sender, splitContent)
        }
    }
}