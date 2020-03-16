package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.events.TwirkListener
import com.gikk.twirk.types.twitchMessage.TwitchMessage
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.twitchbot.BotManager
import uno.rebellious.twitchbot.database.Channel
import java.util.*

class CommandManager(private val twirk: Twirk, private val channel: Channel) : CommandList(), TwirkListener {

    private val commands = ArrayList<CommandList>()

    private var prefix = "!"
    private val database = BotManager.database

    init {
        prefix = channel.prefix

        commands.add(QuoteCommands(prefix, twirk, channel.channel, database))
        commands.add(CounterCommands(prefix, twirk, channel.channel, database))
        commands.add(ResponseCommands(prefix, twirk, channel.channel, database))
        commands.add(AdminCommands(prefix, twirk, channel.channel, database))
        commands.add(MiscCommands(prefix, twirk, channel.channel, database))

        commands.forEach {
            commandList.addAll(it.commandList)
        }
        commandList.add(commandListCommand())
        commandList.add(helpCommand())

        twirk.channelMessage("Starting up for ${channel.channel} - prefix is ${channel.prefix}")
    }

    private fun helpCommand(): Command {
        return Command(
            prefix,
            "help",
            "Usage: ${prefix}help cmd - to get help for a particular command",
            Permission(false, false, false)
        ) { twitchUser: TwitchUser, content: List<String> ->
            if (content.size > 1) {
                twirk.channelMessage(commandList.firstOrNull { command ->
                    command.command == content[1] && command.canUseCommand(
                        twitchUser
                    )
                }?.helpString)
            } else {
                twirk.channelMessage("Usage: ${prefix}help cmd - to get help for a particular command")
            }
        }
    }

    private fun countCommand(): Command {
        return Command(prefix, "", "", Permission(false, false, false)) { _: TwitchUser, content: List<String> ->
            twirk.channelMessage(database.getCounterForChannel(channel.channel, content[0].substring(1)))
        }
    }

    private fun responseCommand(): Command {
        return Command(prefix, "", "", Permission(false, false, false)) { _: TwitchUser, content: List<String> ->
            twirk.channelMessage(database.findResponse(channel.channel, content[0].substring(1)))
        }
    }

    private fun commandListCommand(): Command {
        return Command(
            prefix,
            "cmdlist",
            "Usage: ${prefix}cmdlist - lists the commands for this channel",
            Permission(false, false, false)
        ) { twitchUser: TwitchUser, content: List<String> ->
            val dbCommands = database
                .getAllCommandList(channel.channel)
                .map { command -> prefix + command }
                .sorted()
            val quoteCmds = commands
                .first { it is QuoteCommands }
                .commandList
                .filter { it.canUseCommand(twitchUser) }
                .map { command -> command.prefix + command.command }
                .sorted()
            val adminCmds = commands
                .first { it is AdminCommands }
                .commandList
                .filter { it.canUseCommand(twitchUser) }
                .map { command -> command.prefix + command.command }
                .sorted()
            val miscCmds = commands
                .first { it is MiscCommands }
                .commandList
                .filter { it.canUseCommand(twitchUser) }
                .map { command -> command.prefix + command.command }
                .sorted()
            val responseCmds = commands
                .first { it is ResponseCommands }
                .commandList
                .filter { it.canUseCommand(twitchUser) }
                .map { command -> command.prefix + command.command }
                .sorted()
            val countersCommands = database
                .showCountersForChannel(channel.channel, false)
                .map { it.split(":")[0] }
                .map { command -> prefix + command }
                .sorted()
            val counterCmds = commands
                .first { it is CounterCommands }
                .commandList
                .filter { it.canUseCommand(twitchUser) }
                .map { command -> command.prefix + command.command }
                .sorted()
            var commandlist = ""
            val cmdlist = content.joinToString()
            var commandList = ""
            cmdlist.toLowerCase().apply {
                if (contains("quote")) commandList += "Quotes: $quoteCmds "
                if (contains("response")) commandList += "Responses: $dbCommands $responseCmds "
                if (contains("counter")) commandList += "Counters: $countersCommands $counterCmds "
                if (contains("misc")) commandList += "Misc: $miscCmds "
                if (contains("admin")) commandList += "Admin: $adminCmds "
                if (commandList.isBlank())
                    commandList =
                        "Quotes: $quoteCmds, Responses: $dbCommands $responseCmds, Counters: $countersCommands $counterCmds, Misc: $miscCmds,  Admin: $adminCmds"
                twirk.channelMessage(commandList.trim())
            }
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