package uno.rebellious.twitchbot.database

import software.amazon.awssdk.services.dynamodb.model.*
import uno.rebellious.twitchbot.BotManager
import uno.rebellious.twitchbot.model.Response
import uno.rebellious.twitchbot.model.Waypoint
import uno.rebellious.twitchbot.model.WaypointCoordinate
import uno.rebellious.twitchbot.model.WaypointOrder
import java.sql.Connection
import java.sql.DriverManager
import java.util.*

class WaypointDBDAO(): IWaypoint {



    private val tableName = "${BotManager.env}_waypoint"
    private val keyField = "channel"
    private val sortField = "waypoint"

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
        val wpDAO = WaypointDAO(channel)
        wpDAO.listWaypoints(channel, WaypointOrder.NAME)
            .forEach {
                addWaypoint(channel, it)
            }
    }


    override fun addWaypoint(channel: String, waypoint: Waypoint): Int {
        val ddb = DynamoDBHelper.client
        val item = mapOf(keyField to channel,
        sortField to waypoint.waypoint,
        "x" to waypoint.coordinate.x,
        "y" to waypoint.coordinate.y,
        "z" to waypoint.coordinate.z).mapValues { DynamoDBHelper.attributeValue(it.value) }
        val request = PutItemRequest.builder().item(item).tableName(tableName).build()
        ddb.putItem(request)
        return 0
    }

    override fun deleteWaypointByName(channel: String, waypoint: String) {
        TODO("Not yet implemented")
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