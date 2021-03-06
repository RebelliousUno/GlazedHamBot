package uno.rebellious.twitchbot.database

import software.amazon.awssdk.services.dynamodb.model.*
import uno.rebellious.twitchbot.BotManager


class SettingsDyanmoDBDAO : ISettings {

    private val tableName = "${BotManager.env}_settings"

    override fun createChannelsTable() {
        val request = CreateTableRequest.builder()
            .attributeDefinitions(
                listOf(
                    AttributeDefinition.builder().attributeName("channel").attributeType(ScalarAttributeType.S).build()
                )
            ).keySchema(KeySchemaElement.builder().attributeName("channel").keyType(KeyType.HASH).build())
            .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(5).writeCapacityUnits(5).build())
            .tableName(tableName)
            .build()

        val ddb = DynamoDBHelper.client

        try {
            val result = ddb.createTable(request)
            println("Channels Table Created")
            println("Migrate from Old DB")
            migrateFromOldDB()

            println(result.tableDescription().tableName())

        } catch (e: DynamoDbException) {
            System.err.println(e.message)
        }
    }

    private fun migrateFromOldDB() {
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

        val c = SettingsDAO(HashMap()).getListOfChannels()
        c.forEach {
            val h = mapOf(
                "channel" to DynamoDBHelper.attributeValue(it.channel),
                "nick" to DynamoDBHelper.attributeValue(it.nick),
                "prefix" to DynamoDBHelper.attributeValue(it.prefix),
                "token" to DynamoDBHelper.attributeValue(it.token)
            )
            val request = PutItemRequest.builder().tableName(tableName).item(h).build()
            ddb.putItem(request)
        }
    }

    override fun leaveChannel(channel: String) {
        val ddb = DynamoDBHelper.client
        val request = DynamoDBHelper.deleteItemRequest(channel, tableName)
        ddb.deleteItem(request)
    }


    override fun addChannel(newChannel: String, prefix: String) {
        // Check if the channel exists
        val ddb = DynamoDBHelper.client

        val exists = ddb.getItem(DynamoDBHelper.itemRequest(newChannel, tableName)).hasItem()

        if (!exists) {
            println("Not Exists")

            val h = mapOf(
                "channel" to DynamoDBHelper.attributeValue(newChannel),
                "prefix" to DynamoDBHelper.attributeValue(prefix),
                "nick" to DynamoDBHelper.attributeValue(""),
                "token" to DynamoDBHelper.attributeValue("")
            )
            val request = PutItemRequest.builder().tableName(tableName).item(h).build()
            ddb.putItem(request)
        } else {
            println("Already Exists")
        }
    }

    override fun getPrefixForChannel(channel: String): String {
        val ddb = DynamoDBHelper.client
        val request = DynamoDBHelper.itemRequest(channel, tableName)
        val response = ddb.getItem(request)
        return if (response.hasItem()) {
            response.item()["prefix"]?.s()
        } else {
            "???"
        } ?: "????"
    }

    override fun setPrefixForChannel(channel: String, prefix: String) {
        val ddb = DynamoDBHelper.client
        val request = updateItemRequest(channel, prefix)
        ddb.updateItem(request)
    }

    private fun updateItemRequest(channel: String, prefix: String): UpdateItemRequest {
        return UpdateItemRequest.builder().tableName(tableName)
            .key(mapOf("channel" to AttributeValue.builder().s(channel).build()))
            .attributeUpdates(
                mapOf(
                    "prefix" to AttributeValueUpdate.builder().value(DynamoDBHelper.attributeValue(prefix))
                        .action(AttributeAction.PUT).build()
                )
            )
            .build()
    }

    override fun getListOfChannels(): Array<Channel> {
        val arr = arrayListOf<Channel>()
        val ddb = DynamoDBHelper.client
        val response = ddb.scan(ScanRequest.builder().tableName(tableName).build())
        response.items().forEach {
            val c = Channel(
                it["channel"]?.s() ?: "",
                it["prefix"]?.s() ?: "",
                it["nick"]?.s() ?: "",
                it["token"]?.s() ?: ""
            )
            arr.add(c)
        }
        return arr.toTypedArray()
    }
}