package uno.rebellious.twitchbot

import com.gikk.twirk.SETTINGS
import software.amazon.awssdk.auth.credentials.AwsCredentials
import uno.rebellious.twitchbot.model.Settings

data class DBCredentials(val accessKeyId: String, val secretAccessKey: String): AwsCredentials {
    override fun accessKeyId(): String {
        return accessKeyId
    }

    override fun secretAccessKey(): String {
        return secretAccessKey
    }

}