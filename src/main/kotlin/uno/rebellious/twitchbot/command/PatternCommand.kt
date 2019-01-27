package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.events.TwirkListener
import com.gikk.twirk.types.twitchMessage.TwitchMessage
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.twitchbot.BotManager
import java.util.*

class PatternCommand (private val twirk: Twirk, private val channel: String): CommandList(), TwirkListener {

    private val commands = ArrayList<CommandList>()

    private var prefix = "!"
    private val database = BotManager.database
    init {
        prefix = database.getPrefixForChannel(channel)

        commands.add(QuoteCommands(prefix, twirk, channel, database))
        commands.add(CounterCommands(prefix, twirk, channel, database))
        commands.add(ResponseCommands(prefix, twirk, channel, database))
        commands.add(AdminCommands(prefix, twirk, channel, database))
        commands.add(MiscCommands(prefix, twirk, channel, database))

        commands.forEach {
            commandList.addAll(it.commandList)
        }
        commandList.add(commandListCommand())


        twirk.channelMessage("Starting up for $channel - prefix is $prefix")
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

    private fun commandListCommand(): Command {
        return Command(prefix, "cmdlist", "Usage: ${prefix}cmdlist - lists the commands for this channel", Permission(false, false, false)) { twitchUser: TwitchUser, _: List<String> ->
            val dbCommands = database
                    .getAllCommandList(channel)
                    .map { command -> prefix + command }
                    .sorted()
            val quoteCmds = commands
                    .first { it is QuoteCommands }
                    .commandList
                    .filter{it.canUseCommand(twitchUser)}
                    .map { command -> command.prefix + command.command }
                    .sorted()
            val adminCmds = commands
                    .first { it is AdminCommands }
                    .commandList
                    .filter{it.canUseCommand(twitchUser)}
                    .map { command -> command.prefix + command.command }
                    .sorted()
            val miscCmds = commands
                    .first { it is MiscCommands }
                    .commandList
                    .filter{it.canUseCommand(twitchUser)}
                    .map { command -> command.prefix + command.command }
                    .sorted()
            val responseCmds = commands
                    .first { it is ResponseCommands }
                    .commandList
                    .filter{it.canUseCommand(twitchUser)}
                    .map { command -> command.prefix + command.command }
                    .sorted()
            val counterCmds = commands
                    .first { it is CounterCommands }
                    .commandList
                    .filter { it.canUseCommand(twitchUser) }
                    .map { command -> command.prefix + command.command }
                    .sorted()
            twirk.channelMessage("Quotes: $quoteCmds, Responses: $dbCommands $responseCmds, Counters: $counterCmds, Misc: $miscCmds,  Admin: $adminCmds")
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