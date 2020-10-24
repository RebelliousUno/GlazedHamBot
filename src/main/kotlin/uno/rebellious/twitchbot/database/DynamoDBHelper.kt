package uno.rebellious.twitchbot.database

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import uno.rebellious.twitchbot.BotManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DynamoDBHelper {

    fun deleteItemRequest(channel: String, tableName: String): DeleteItemRequest = DeleteItemRequest.builder().tableName(tableName)
        .key(mapOf("channel" to attributeValue(channel))).build()

    fun itemRequest(channel: String, tableName: String): GetItemRequest = GetItemRequest.builder().tableName(tableName)
        .key(mapOf("channel" to attributeValue(channel))).build()

    fun attributeValue(value: String): AttributeValue {
        return AttributeValue.builder().s(value).build()
    }

    fun attributeValue(value: LocalDateTime): AttributeValue {
        return AttributeValue.builder().s(value.format(DateTimeFormatter.ISO_DATE_TIME)).build()
    }


    fun dbClient(): DynamoDbClient {
        return DynamoDbClient.builder()
            .region(Region.US_EAST_2)
            .credentialsProvider(AwsCredentialsProvider { BotManager.awsCredentials })
            .build()
    }

}