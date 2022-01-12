package uno.rebellious.twitchbot.database

import software.amazon.awssdk.services.dynamodb.model.*
import uno.rebellious.twitchbot.BotManager
import uno.rebellious.twitchbot.model.Counter
import java.util.*

class CountersDynamoDBDAO : ICounters {

    private val tableName = "${BotManager.env}_counters"
    private val keyField = "channel"
    private val sortField = "counter"

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
            ddb.createTable(request)
            channels.forEach {
                migrateFromOldDB(it.channel)
            }
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

        CountersDAO(channel).showCountersForChannel(channel, true)
            .map {
                CountersDAO(channel).getCounterForChannel(channel, it)
            }.forEach {
                createCounterForChannel(channel, it)
            }
    }

    override fun createCounterForChannel(channel: String, counter: Counter) {
        val ddb = DynamoDBHelper.client
        val item = mapOf(
            keyField to channel,
            sortField to counter.command.lowercase(Locale.ENGLISH),
            "singular" to counter.singular,
            "plural" to counter.plural,
            "today" to counter.today,
            "total" to counter.total
        ).mapValues { DynamoDBHelper.attributeValue(it) }
        val request = PutItemRequest.builder().item(item).tableName(tableName).build()
        ddb.putItem(request)
    }

    override fun removeCounterForChannel(channel: String, counter: Counter) {
        val ddb = DynamoDBHelper.client
        val request =
            DynamoDBHelper.deleteItemRequest(tableName, mapOf("channel" to channel, "counter" to counter.command))
        val response = ddb.deleteItem(request)
    }

    override fun incrementCounterForChannel(channel: String, counter: Counter, by: Int) {
        val updateExpression = "SET #today = #today + :num, #total = #total + :num"
        val expressionAttributeValues = mapOf(":num" to DynamoDBHelper.attributeValue(by))
        val expressionNames = mapOf("#today" to "today", "#total" to "total")
        val ddb = DynamoDBHelper.client
        val request =
            DynamoDBHelper.updateItemRequest(
                tableName,
                mapOf("channel" to channel, "counter" to counter.command),
                updateExpression,
                expressionNames,
                expressionAttributeValues
            )
        ddb.updateItem(request)
    }

    override fun getCounterForChannel(channel: String, counter: Counter): Counter {
        val ddb = DynamoDBHelper.client
        val request = DynamoDBHelper.itemRequest(tableName, mapOf("channel" to channel, "counter" to counter.command))
        val response = ddb.getItem(request)
        return if (response.hasItem()) {
            with(response.item()) {
                Counter(
                    this["counter"]?.s() ?: counter.command,
                    this["singular"]?.s() ?: "",
                    this["plural"]?.s() ?: "",
                    this["today"]?.n()?.toInt() ?: 0,
                    this["total"]?.n()?.toInt() ?: 0
                )
            }
        } else {
            Counter("")
        }
    }

    override fun resetTodaysCounterForChannel(channel: String, counter: Counter) {
        val updateExpression = "SET #today = :num"
        val expressionAttributeValues = mapOf(":num" to DynamoDBHelper.attributeValue(0))
        val expressionNames = mapOf("#today" to "today")
        val ddb = DynamoDBHelper.client
        val request =
            DynamoDBHelper.updateItemRequest(
                tableName,
                mapOf("channel" to channel, "counter" to counter.command),
                updateExpression,
                expressionNames,
                expressionAttributeValues
            )
        ddb.updateItem(request)
    }

    override fun showCountersForChannel(channel: String, includeStream: Boolean): List<Counter> {
        val ddb = DynamoDBHelper.client
        val scan = QueryRequest.builder().tableName(tableName).keyConditionExpression("channel = :channel_name")
            .expressionAttributeValues(mapOf(":channel_name" to DynamoDBHelper.attributeValue(channel))).build()
        val results = ddb.query(scan)
        val list = ArrayList<Counter>()
        if (results.hasItems()) {
            results.items().forEach {
                list.add(
                    Counter(
                        it["counter"]?.s() ?: "",
                        it["singular"]?.s() ?: "",
                        it["plural"]?.s() ?: "",
                        it["today"]?.n()?.toInt() ?: 0,
                        it["total"]?.n()?.toInt() ?: 0
                    )
                )
            }
        }
        return list
    }
}