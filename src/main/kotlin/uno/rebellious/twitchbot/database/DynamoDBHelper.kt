package uno.rebellious.twitchbot.database

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import uno.rebellious.twitchbot.BotManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DynamoDBHelper {
    val client: DynamoDbClient = createDBClient()

    fun deleteItemRequest(channel: String, tableName: String): DeleteItemRequest =
        DeleteItemRequest.builder().tableName(tableName)
            .key(mapOf("channel" to attributeValue(channel))).build()

    fun deleteItemRequest(tableName: String, map: Map<String, String>): DeleteItemRequest {
        return deleteItemRequest(tableName, map.mapValues(::attributeValue))
    }

    @JvmName("deleteItemRequest1")
    fun deleteItemRequest(tableName: String, map: Map<String, AttributeValue>): DeleteItemRequest {
        return DeleteItemRequest.builder().tableName(tableName).key(map).build()
    }



    fun updateItemRequest(
        tableName: String,
        map: Map<String, String>,
        updateExpression: String,
        expressionNames: Map<String, String>,
        expressionAttributeValues: Map<String, AttributeValue>
    ): UpdateItemRequest {
        return updateItemRequest(tableName, item = map.mapValues { attributeValue(it.value) }, updateExpression, expressionNames, expressionAttributeValues)
    }

    @JvmName("updateItemRequest1")
    fun updateItemRequest(
        tableName: String,
        item: Map<String, AttributeValue>,
        updateExpression: String,
        expressionNames: Map<String, String>,
        expressionAttributeValues: Map<String, AttributeValue>
    ): UpdateItemRequest {
        return UpdateItemRequest.builder().tableName(tableName)
            .key(item)
            .updateExpression(updateExpression)
            .expressionAttributeNames(expressionNames)
            .expressionAttributeValues(expressionAttributeValues).build()
    }

    fun itemRequest(channel: String, tableName: String, consistentRead: Boolean = false): GetItemRequest = GetItemRequest.builder().tableName(tableName)
        .key(mapOf("channel" to attributeValue(channel))).consistentRead(consistentRead).build()

    fun itemRequest(tableName: String, map: Map<String, String>, consistentRead: Boolean = false): GetItemRequest {
        return GetItemRequest.builder().tableName(tableName)
            .key(map.mapValues {
                attributeValue(it.value)
            }).consistentRead(consistentRead).build()
    }

    @JvmName("itemRequest1")
    fun itemRequest(tableName: String, item: Map<String, AttributeValue>, consistentRead: Boolean = false): GetItemRequest {
        return GetItemRequest.builder().tableName(tableName).key(item).consistentRead(consistentRead).build()
    }



    fun <K, V> attributeValue(entry: Map.Entry<K, V>): AttributeValue {
        with(entry.value) {
            return when (this) {
                is String -> attributeValue(this)
                is Int -> attributeValue(this)
                is LocalDateTime -> attributeValue(this)
                else -> attributeValue("")
            }
        }
    }

    fun attributeValue(value: String): AttributeValue = AttributeValue.builder().s(value).build()

    fun attributeValue(value: Int): AttributeValue {
        return AttributeValue.builder().n(value.toString()).build()
    }
    fun attributeValue(value: LocalDateTime): AttributeValue =
        AttributeValue.builder().s(value.format(DateTimeFormatter.ISO_DATE_TIME)).build()

//    fun attributeValue(value: Comparable<*>): AttributeValue = AttributeValue.builder().s(value.toString()).build()


    private fun createDBClient(): DynamoDbClient {
        return DynamoDbClient.builder()
            .region(Region.US_EAST_2)
            .credentialsProvider(AwsCredentialsProvider { BotManager.awsCredentials })
            .build()

    }


}