package uno.rebellious.twitchbot.database

import software.amazon.awssdk.services.dynamodb.model.*
import uno.rebellious.twitchbot.BotManager
import uno.rebellious.twitchbot.model.Response
import java.util.*

class ResponsesDynamoDBDAO : IResponse {

    private val tableName = "${BotManager.env}_response"
    private val keyField = "channel"
    private val sortField = "command"

    fun createTablesForChannels(channels: Array<Channel>) {
        val request = CreateTableRequest.builder()
            .attributeDefinitions(
                listOf(
                    AttributeDefinition.builder().attributeName(keyField).attributeType(ScalarAttributeType.S).build(),
                    AttributeDefinition.builder().attributeName(sortField).attributeType(ScalarAttributeType.S).build()
                )
            ).keySchema(
                KeySchemaElement.builder().attributeName(keyField).keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName(sortField).keyType(KeyType.RANGE).build()
            )
            .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(5).writeCapacityUnits(5).build())
            .tableName(tableName)
            .build()

        val ddb = DynamoDBHelper.client

        try {
            val result = ddb.createTable(request)
            println("Spotify Table Created")
            println("Migrate from Old DB")
            channels.forEach {
                migrateFromOldDB(it.channel)
            }
            println(result.tableDescription().tableName())

        } catch (e: DynamoDbException) {
            System.err.println(e.message)

        }
    }

    private fun migrateFromOldDB(channel: String) {
        val ddb = DynamoDBHelper.client
        var describe = false
        var i = 0
        while (!describe && i < 20) { // Need to extract this to the aws helper... later
            describe = ddb.describeTable {
                it.tableName(tableName).build()
            }.table().tableStatus() == TableStatus.ACTIVE
            Thread.sleep(500) // Wait half a second before checking the table again
            i++ // Only try 20 times
        }

        val c = ResponsesDAO(channel).getAllCommandList(channel)
        c.forEach {
            val response = ResponsesDAO(channel).findResponse(channel, Response(it))
            val h = mapOf(
                "channel" to DynamoDBHelper.attributeValue(channel),
                "command" to DynamoDBHelper.attributeValue(response.command),
                "response" to DynamoDBHelper.attributeValue(response.response)
            )
            val request = PutItemRequest.builder().tableName(tableName).item(h).build()
            ddb.putItem(request)
        }
    }


    override fun findResponse(channel: String, command: Response): Response {
        val ddb = DynamoDBHelper.client
        val request = DynamoDBHelper.itemRequest(tableName, mapOf("channel" to channel, "command" to command.command))
        val response = ddb.getItem(request)
        return if (response.hasItem()) {
            with(response.item()) {
                Response(this["command"]?.s() ?: "", this["response"]?.s() ?: "")
            }
        } else {
            command
        }
    }

    override fun setResponse(channel: String, response: Response) {
        val ddb = DynamoDBHelper.client
        val item = mapOf(
            keyField to channel,
            sortField to response.command,
            "response" to response.response
        ).mapValues { DynamoDBHelper.attributeValue(it.value) }
        val request = PutItemRequest.builder().item(item).tableName(tableName).build()
        ddb.putItem(request)
    }

    override fun removeResponse(channel: String, command: Response) {
        val ddb = DynamoDBHelper.client
        val item =
            DynamoDBHelper.deleteItemRequest(tableName, mapOf("channel" to channel, "command" to command.command))
        ddb.deleteItem(item)
    }

    override fun getAllCommandList(channel: String): ArrayList<String> {
        val ddb = DynamoDBHelper.client
        val scan = QueryRequest.builder().tableName(tableName)
            .keyConditionExpression("channel = :channel_name")
            .expressionAttributeValues(mapOf(":channel_name" to DynamoDBHelper.attributeValue(channel))).build()
        val results = ddb.query(scan)
        val list = ArrayList<String>()
        if (results.hasItems()) {
            results.items().forEach {
                list.add(it["command"]?.s() ?: "")
            }
        }
        return list
    }
}