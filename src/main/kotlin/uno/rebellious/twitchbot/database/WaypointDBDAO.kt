package uno.rebellious.twitchbot.database

import software.amazon.awssdk.services.dynamodb.model.*
import uno.rebellious.twitchbot.BotManager
import uno.rebellious.twitchbot.model.Waypoint
import uno.rebellious.twitchbot.model.WaypointCoordinate
import uno.rebellious.twitchbot.model.WaypointOrder
import java.util.*

class WaypointDBDAO() : IWaypoint {


    private val tableName = "${BotManager.env}_waypoint"
    private val keyField = "channel"
    private val sortField = "id"

    fun createTablesForChannels(channels: Array<Channel>) {
        val request = CreateTableRequest.builder()
            .attributeDefinitions(
                listOf(
                    AttributeDefinition.builder().attributeName(keyField).attributeType(ScalarAttributeType.S).build(),
                    AttributeDefinition.builder().attributeName(sortField).attributeType(ScalarAttributeType.N).build()
                )
            ).keySchema(
                KeySchemaElement.builder().attributeName(keyField).keyType(KeyType.HASH).build(),
                KeySchemaElement.builder().attributeName(sortField).keyType(KeyType.RANGE).build()
            )
            .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(1).writeCapacityUnits(1).build())
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
        val wpDAO = WaypointDAO(channel)
        putWaypointId(channel, 0)
        wpDAO.listWaypoints(channel, WaypointOrder.NAME)
            .forEach {
                addWaypoint(channel, it)
            }
        val lastID = wpDAO.listWaypoints(channel, WaypointOrder.ID).lastOrNull()?.id ?: 0
        setWaypointId(channel, lastID)
    }

    //Should only be used during set up
    private fun putWaypointId(channel: String, id: Int) {
        val ddb = DynamoDBHelper.client
        val item = mapOf(keyField to channel, sortField to -1, "val" to id).mapValues(DynamoDBHelper::attributeValue)
        val request = PutItemRequest.builder().tableName(tableName).item(item).build()
        ddb.putItem(request)
    }

    private fun setWaypointId(channel: String, id: Int) {
        val ddb = DynamoDBHelper.client
        val item = mapOf(keyField to channel, sortField to -1).mapValues(DynamoDBHelper::attributeValue)

        val updateExpression = "SET #val = #val + :inc"
        val expressionAttributeValues = mapOf(":inc" to DynamoDBHelper.attributeValue(1))
        val expressionNames = mapOf("#val" to "val")

        val request =
            DynamoDBHelper.updateItemRequest(
                tableName,
                item,
                updateExpression,
                expressionNames,
                expressionAttributeValues
            )
        ddb.updateItem(request)
    }
    private fun getWaypointId(channel: String): Int {
        val ddb = DynamoDBHelper.client
        val item =  mapOf("channel" to channel, "id" to -1).mapValues(DynamoDBHelper::attributeValue)
        val request = DynamoDBHelper.itemRequest(tableName, item)
        return ddb.getItem(request).item().get("val")?.n()?.toInt() ?: -2
    }

    override fun addWaypoint(channel: String, waypoint: Waypoint): Int {
        val ddb = DynamoDBHelper.client
        val waypointId = getWaypointId(channel)+1
        if (waypointId < 0) return -1
        val item = mapOf(
            keyField to channel,
            sortField to waypointId ,
            "waypoint" to waypoint.waypoint,
            "x" to waypoint.coordinate.x,
            "y" to waypoint.coordinate.y,
            "z" to waypoint.coordinate.z
        ).mapValues(DynamoDBHelper::attributeValue)

        val request = PutItemRequest.builder().item(item).tableName(tableName).build()
        ddb.putItem(request)
        setWaypointId(channel, waypointId)
        return waypointId
    }

    override fun deleteWaypointByName(channel: String, waypoint: String) {
        val ddb = DynamoDBHelper.client
        val item = mapOf(keyField to channel, sortField to waypoint)
        val request = DynamoDBHelper.deleteItemRequest(channel, item)
        ddb.deleteItem(request)
    }

    override fun deleteWaypointById(channel: String, id: Int) {
        TODO("Not yet implemented")
    }

    override fun listWaypoints(channel: String, orderBy: WaypointOrder): List<Waypoint> {
        TODO("Not yet implemented")
    }

    override fun findWaypointById(channel: String, id: Int, deleted: Boolean): Waypoint? {
        TODO("Not yet implemented")
    }

    override fun findWaypointByCoords(
        channel: String,
        coordinate: WaypointCoordinate,
        deleted: Boolean
    ): Pair<Double, Waypoint> {
        TODO("Not yet implemented")
    }

    override fun findWaypointByName(channel: String, waypoint: String, deleted: Boolean): Waypoint? {
        TODO("Not yet implemented")
    }
}