package uno.rebellious.twitchbot

import software.amazon.awssdk.auth.credentials.AwsCredentials

data class DBCredentials(val accessKeyId: String, val secretAccessKey: String) : AwsCredentials {
    override fun accessKeyId(): String {
        return accessKeyId
    }

    override fun secretAccessKey(): String {
        return secretAccessKey
    }

}