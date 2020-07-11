package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.twitchbot.BotManager.pastebin
import uno.rebellious.twitchbot.command.model.Permission
import uno.rebellious.twitchbot.database.DatabaseDAO
import uno.rebellious.twitchbot.database.QuotesDAO
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeParseException


class QuoteCommands(
    private val prefix: String,
    private val twirk: Twirk,
    val channel: String,
    val database: DatabaseDAO,
    val clock: Clock = Clock.systemDefaultZone()
) : CommandList() {
    init {
        commandList.add(quoteListCommand())
        commandList.add(quoteCommand())
        commandList.add(addQuoteCommand())
        commandList.add(editQuoteCommand())
        commandList.add(deleteQuoteCommand())
        commandList.add(undeleteQuoteCommand())
    }

    private fun quoteListCommand(): Command {
        val helpString = ""
        return Command(
            prefix,
            "quotelist",
            helpString,
            Permission.MOD_ONLY
        ) { _: TwitchUser, _: List<String> ->
            val quoteList = database.getAllQuotesForChannel(channel)
            val quotesString = pastebin.parseQuotes(quoteList)
            val quotesURL = pastebin.createPaste("Punch A Quotes", quotesString)
            twirk.channelMessage("Quote List: $quotesURL")
        }
    }

    private fun deleteQuoteCommand(): Command {
        val helpString = "Usage: ${prefix}delquote quoteid - Deletes quote quoteid"
        return Command(
            prefix,
            "delquote",
            helpString,
            Permission.MOD_ONLY
        ) { _: TwitchUser, content: List<String> ->
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
        return Command(
            prefix,
            "undelquote",
            helpString,
            Permission.MOD_ONLY
        ) { _: TwitchUser, content: List<String> ->
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
        val helpString =
            "Usage: ${prefix}addquote QUOTE | PERSON | [YYYY-MM-DD] - eg. Adds a quote for Person on Date (optional defaults to today)"
        return Command(
            prefix,
            "addquote",
            helpString,
            Permission.MOD_ONLY
        ) { _: TwitchUser, content: List<String> ->
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
                    LocalDate.now(clock)
                if (date != null) {
                    val id = database.addQuoteForChannel(channel, date, person, quote)
                    twirk.channelMessage("Added with ID $id : $quote - $person on $date")
                }
            }
        }
    }

    private fun editQuoteCommand(): Command {
        val helpString =
            "Usage: ${prefix}editquote QUOTEID [QUOTE] | [PERSON] | [YYYY-MM-DD] - eg. Edits quote QUOTEID - Sections of the quote are optional but needs both ||"
        return Command(
            prefix,
            "editquote",
            helpString,
            Permission.MOD_ONLY
        ) { _: TwitchUser, content: List<String> ->
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
                } catch (e: java.lang.NumberFormatException) {
                    twirk.channelMessage("${content[1]} is not a number")
                }
            }
        }
    }

    private fun quoteCommand(): Command {
        val helpString =
            "Usage: ${prefix}quote [SEARCH TERM] - Searches for a quote where Search Term is either quote ID, Author or a keyword"
        return Command(
            prefix,
            "quote",
            helpString,
            Permission.ANYONE
        ) { _: TwitchUser, content: List<String> ->
            val message: String
            if (content.size > 1) {
                message = try {
                    val id = Integer.parseInt(content[1])
                    database.getQuoteForChannelById(channel, id)
                } catch (nfe: NumberFormatException) {
                    val searchPhrase = content.subList(1, content.size).joinToString(" ")
                    val byAuthor = database.findQuoteByAuthor(channel, searchPhrase)
                    val byKeyword = database.findQuoteByKeyword(channel, searchPhrase)
                    if (!byAuthor.run { isEmpty() || this == QuotesDAO.QUOTE_NOT_FOUND }) {
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
}