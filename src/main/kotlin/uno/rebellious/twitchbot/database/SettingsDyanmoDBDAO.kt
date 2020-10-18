package uno.rebellious.twitchbot.database

import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.*
import uno.rebellious.twitchbot.BotManager


class SettingsDyanmoDBDAO : ISettings {

    private val tableName = "${BotManager.env}_settings"

    private fun dbClient(): DynamoDbClient {
        return DynamoDbClient.builder()
            .region(Region.US_EAST_2)
            .credentialsProvider(AwsCredentialsProvider { BotManager.awsCredentials })
            .build()
    }

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

        val ddb = dbClient()

        try {
            val result = ddb.createTable(request)
            migrateFromOldDB()
            println(result.tableDescription().tableName())

        } catch (e: DynamoDbException) {
            System.err.println(e.message)
        }
    }

    private fun migrateFromOldDB() {
        val ddb = dbClient()
        val c = SettingsDAO(HashMap()).getListOfChannels()
        c.forEach {
            val h = mapOf(
                "channel" to AttributeValue.builder().s(it.channel).build(),
                "nick" to AttributeValue.builder().s(it.nick).build(),
                "prefix" to AttributeValue.builder().s(it.prefix).build(),
                "token" to AttributeValue.builder().s(it.token).build()
            )
            val request = PutItemRequest.builder().tableName(tableName).item(h).build()
            ddb.putItem(request)
        }
    }

    override fun leaveChannel(channel: String) {
        val ddb = dbClient()
        val request = deleteItemRequest(channel)
        ddb.deleteItem(request)
    }


    override fun addChannel(newChannel: String, prefix: String) {
        // Check if the channel exists
        val ddb = dbClient()

        val exists = ddb.getItem(itemRequest(newChannel)).hasItem()

        if (!exists) {
            println("Not Exists")

            val h = mapOf(
                "channel" to AttributeValue.builder().s(newChannel).build(),
                "prefix" to AttributeValue.builder().s(prefix).build(),
                "nick" to AttributeValue.builder().s("").build(),
                "token" to AttributeValue.builder().s("").build()
            )
            val request = PutItemRequest.builder().tableName(tableName).item(h).build()
            ddb.putItem(request)
        } else {
            println("Already Exists")
        }
    }

    override fun getPrefixForChannel(channel: String): String {
        val ddb = dbClient()
        val request = itemRequest(channel)
        val response = ddb.getItem(request)
        return if (response.hasItem()) {
            response.item()["prefix"]?.s()
        } else {
            "???"
        } ?: "????"
    }

    override fun setPrefixForChannel(channel: String, prefix: String) {
        val ddb = dbClient()
        val request = updateItemRequest(channel, prefix)
        ddb.updateItem(request)
    }

    private fun updateItemRequest(channel: String, prefix: String): UpdateItemRequest {
        return UpdateItemRequest.builder().tableName(tableName)
            .key(mapOf("channel" to AttributeValue.builder().s(channel).build()))
            .attributeUpdates(
                mapOf(
                    "prefix" to AttributeValueUpdate.builder().value(AttributeValue.builder().s(prefix).build())
                        .action(AttributeAction.PUT).build()
                )
            )
            .build()
    }

    override fun getListOfChannels(): Array<Channel> {
        val arr = arrayListOf<Channel>()
        val ddb = dbClient()
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


    private fun deleteItemRequest(channel: String) = DeleteItemRequest.builder().tableName(tableName)
        .key(mapOf("channel" to AttributeValue.builder().s(channel).build())).build()

    private fun itemRequest(channel: String) = GetItemRequest.builder().tableName(tableName)
        .key(mapOf("channel" to AttributeValue.builder().s(channel).build())).build()


}