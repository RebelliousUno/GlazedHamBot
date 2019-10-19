package uno.rebellious.twitchbot.command

import com.gikk.twirk.Twirk
import com.gikk.twirk.types.users.TwitchUser
import uno.rebellious.twitchbot.database.DatabaseDAO

class CurrencyCommands(
    private val prefix: String,
    private val twirk: Twirk,
    private val channel: String,
    private val database: DatabaseDAO
) : CommandList() {
    init {
        commandList.add(getCurrencyNameCommand())
        commandList.add(setCurrencyNameCommand())
        commandList.add(getCurrencyCommand())
//        commandList.add(spendCurrencyCommand())
//        commandList.add(startCurrencyGameCommand())
//        commandList.add(joinCurrencyGameCommand())
    }

    private fun getCurrencyCommand(): Command {
        val currencyName = database.getCurrencyName(channel)
        return Command(prefix, currencyName.toLowerCase(), "", Permission.ANYONE) { user: TwitchUser, _ ->
            val currency = database.getCurrencyForUser(channel, user)
            twirk.channelMessage("${user.userName} you currently have $currency $currencyName")

        }
    }

    private fun getCurrencyNameCommand(): Command {
        return Command(prefix, "currencyname", "", Permission.MOD) { _: TwitchUser, _: List<String> ->
            val currencyName = database.getCurrencyName(channel)
            if (currencyName.isNotBlank())
                twirk.channelMessage("Currency name for $channel is $currencyName")
            else twirk.channelMessage("No currency in place")
        }
    }
    private fun setCurrencyNameCommand(): Command {
        return Command(prefix, "setcurrencyname", "", Permission.MOD) { _: TwitchUser, values: List<String> ->
            if (values.size>1) {
                val name = values[1]
                database.setCurrencyName(channel, name)
            }
        }
    }

    private fun spendCurrencyCommand(): Command {
        TODO("Not Yet Implemented")
    }

    private fun startCurrencyGameCommand(): Command {
        TODO("Not Yet Implemented")
    }

    private fun joinCurrencyGameCommand(): Command {
        TODO("Not Yet Implemented")
    }

}
