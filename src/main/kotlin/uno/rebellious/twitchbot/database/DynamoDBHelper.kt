package uno.rebellious.twitchbot.database

import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest

object DynamoDBHelper {

    fun deleteItemRequest(channel: String, tableName: String): DeleteItemRequest = DeleteItemRequest.builder().tableName(tableName)
        .key(mapOf("channel" to attributeValue(channel))).build()

    fun itemRequest(channel: String, tableName: String): GetItemRequest = GetItemRequest.builder().tableName(tableName)
        .key(mapOf("channel" to attributeValue(channel))).build()

    fun attributeValue(value: String): AttributeValue {
        return AttributeValue.builder().s(value).build()
    }

}