package uno.rebellious.twitchbot

import java.lang.reflect.Array

public interface IDatabase {
    fun findResponse(command: String): String
    fun setResponse(command: String, response: String)
    fun removeResponse(command: String)
    fun getAllCommandList(): ArrayList<String>
}