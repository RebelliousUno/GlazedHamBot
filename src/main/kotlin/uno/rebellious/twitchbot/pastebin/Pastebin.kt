package uno.rebellious.twitchbot.pastebin

import com.github.kittinunf.fuel.httpPost
import com.github.kittinunf.result.Result
import uno.rebellious.twitchbot.model.Quote

class Pastebin(devKey: String?, userKey: String?) {

    private val expiry = Pair("api_paste_expire_date", "1W")
    private val apiOption = Pair("api_option", "paste")
    private val apiDevKey = Pair("api_dev_key", devKey)
    private val apiUserKey = Pair("api_user_key", userKey)

    private val path = "https://pastebin.com/api/api_post.php"


    fun parseQuotes(quotes: List<Quote>): String {
        val quoteString = StringBuilder().append("Quotes\n")
        with(quoteString) {
            quotes.forEach {
                append("${it.id}: ${it.quote} - ${it.author} - ${it.date.toLocalDate()} ")
                append("\n")
            }
        }
        return quoteString.toString()
    }

    fun createPaste(pasteTitle: String, pasteText: String): String {
        val title = Pair("api_paste_name", pasteTitle)
        val text = Pair("api_paste_code", pasteText)
        val body = listOf(expiry, apiDevKey, apiOption, apiUserKey, title, text)

        val (_, _, result) = path.httpPost(body).responseString()
        return when (result) {
            is Result.Failure -> {
                "Cannot Make Paste"
            }
            is Result.Success -> {
                result.get()
            }
        }
    }

}