package uno.rebellious.twitchbot.database

import software.amazon.awssdk.services.dynamodb.model.*
import uno.rebellious.twitchbot.BotManager
import uno.rebellious.twitchbot.database.DynamoDBHelper.attributeValue
import uno.rebellious.twitchbot.model.SpotifyToken
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SpotifyDynamoDBDAO : ISpotify {

    private val tableName = "${BotManager.env}_spotify"

    fun createTableForChannel(channelList: Array<Channel>) {
        val request = CreateTableRequest.builder()
            .attributeDefinitions(
                listOf(
                    AttributeDefinition.builder().attributeName("channel").attributeType(ScalarAttributeType.S).build()
                )
            ).keySchema(KeySchemaElement.builder().attributeName("channel").keyType(KeyType.HASH).build())
            .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(5).writeCapacityUnits(5).build())
            .tableName(tableName)
            .build()

        val ddb = DynamoDBHelper.dbClient()

        try {
            val result = ddb.createTable(request)
            println("Spotify Table Created")
            println("Migrate from Old DB")
            channelList.forEach {
                migrateFromOldDB(it.channel)
            }


            println(result.tableDescription().tableName())

        } catch (e: DynamoDbException) {
            System.err.println(e.message)

        }
    }

    private fun migrateFromOldDB(channel: String) {
        val ddb = DynamoDBHelper.dbClient()
        var describe = false
        var i = 0
        while (!describe && i < 20) { // Need to extract this to the aws helper... later
            describe = ddb.describeTable {
                it.tableName(tableName).build()
            }.table().tableStatus() == TableStatus.ACTIVE
            Thread.sleep(500) // Wait half a second before checking the table again
            i++ // Only try 20 times
        }

        val c = SpotifyDAO(channel).getTokensForChannel(channel)
        c?.let {
            val h = mapOf(
                "channel" to attributeValue(channel),
                "authCode" to attributeValue(it.authCode),
                "accessToken" to attributeValue(it.accessToken ?: ""),
                "refreshToken" to attributeValue(it.refreshToken ?: ""),
                "expiryTime" to attributeValue(
                    it.expiryTime?.format(DateTimeFormatter.ISO_DATE_TIME) ?: ""
                )
            )

            val request = PutItemRequest.builder().tableName(tableName).item(h).build()
            ddb.putItem(request)
        }
    }

    override fun setTokensForChannel(
        channel: String,
        accessToken: String,
        refreshToken: String,
        expiryTime: LocalDateTime
    ) {
        val ddb = DynamoDBHelper.dbClient()
        val request = updateItemRequest(channel, accessToken, refreshToken, expiryTime)
        ddb.updateItem(request)
    }

    override fun getTokensForChannel(channel: String): SpotifyToken? {
            val ddb = DynamoDBHelper.dbClient()
            val request = DynamoDBHelper.itemRequest(channel, tableName)
            val response = ddb.getItem(request)
            return if (response.hasItem()) {
                with(response.item()) {
                    SpotifyToken(
                        authCode = get("authCode")?.s() ?: "",
                        accessToken = get("accessToken")?.s(),
                        refreshToken = get("refreshToken")?.s(),
                        expiryTime = LocalDateTime.parse(get("expiryTime")?.s())
                    )                }
            } else {
                null
            }
    }


    private fun updateItemRequest(
        channel: String,
        accessToken: String,
        refreshToken: String,
        expiryTime: LocalDateTime
    ): UpdateItemRequest {
        return UpdateItemRequest.builder().tableName(tableName)
            .key(mapOf("channel" to AttributeValue.builder().s(channel).build()))
            .attributeUpdates(mapOf(
                "accessToken" to AttributeValueUpdate.builder().value(attributeValue(accessToken)),
                "refreshToken" to AttributeValueUpdate.builder().value(attributeValue(refreshToken)),
                "expiryTime" to AttributeValueUpdate.builder().value(attributeValue(expiryTime))
            ).mapValues { it.value.action(AttributeAction.PUT).build() })
            .build()
    }


}