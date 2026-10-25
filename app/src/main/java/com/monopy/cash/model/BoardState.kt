package com.monopy.cash.model

import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything about the game in progress that is not cash: who stands where, who owns
 * what, and whose turn it is. Serialised whole, so the host can hand the same picture
 * to every device on the table.
 */
data class BoardState(
    val started: Boolean = false,
    /** Player account ids, in turn order. */
    val order: List<String> = emptyList(),
    val turn: Int = 0,
    val positions: Map<String, Int> = emptyMap(),
    val owners: Map<Int, String> = emptyMap(),
    val houses: Map<Int, Int> = emptyMap(),
    /** Player id to the number of turns still to serve. */
    val jailed: Map<String, Int> = emptyMap(),
    val dice: List<Int> = emptyList(),
    val doublesRun: Int = 0,
    /** Has the player whose turn it is already rolled? */
    val rolled: Boolean = false,
    /** Tile the current player may buy right now, if any. */
    val awaitingBuy: Int? = null,
    /** सुवर्ण संधी card drawn this turn, by id. */
    val lastCard: Int? = null,
    val log: List<String> = emptyList()
) {
    val currentPlayerId: String?
        get() = order.getOrNull(if (order.isEmpty()) 0 else turn % order.size)

    fun positionOf(playerId: String): Int = positions[playerId] ?: 0

    fun ownerOf(tileIndex: Int): String? = owners[tileIndex]

    fun housesOn(tileIndex: Int): Int = houses[tileIndex] ?: 0

    fun isJailed(playerId: String): Boolean = (jailed[playerId] ?: 0) > 0

    /** How many tiles of a group this player holds. */
    fun ownedInGroup(playerId: String, group: String?): Int =
        BoardData.groupTiles(group).count { owners[it.index] == playerId }

    fun ownsWholeGroup(playerId: String, group: String?): Boolean {
        val tiles = BoardData.groupTiles(group)
        return tiles.isNotEmpty() && tiles.all { owners[it.index] == playerId }
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("started", started)
        put("order", JSONArray().also { array -> order.forEach { array.put(it) } })
        put("turn", turn)
        put("positions", JSONObject().also { obj -> positions.forEach { obj.put(it.key, it.value) } })
        put("owners", JSONObject().also { obj -> owners.forEach { obj.put(it.key.toString(), it.value) } })
        put("houses", JSONObject().also { obj -> houses.forEach { obj.put(it.key.toString(), it.value) } })
        put("jailed", JSONObject().also { obj -> jailed.forEach { obj.put(it.key, it.value) } })
        put("dice", JSONArray().also { array -> dice.forEach { array.put(it) } })
        put("doublesRun", doublesRun)
        put("rolled", rolled)
        put("awaitingBuy", awaitingBuy ?: JSONObject.NULL)
        put("lastCard", lastCard ?: JSONObject.NULL)
        put("log", JSONArray().also { array -> log.forEach { array.put(it) } })
    }

    companion object {
        /** Keep the table log short enough to scroll. */
        const val MAX_LOG = 60

        fun fromJson(json: JSONObject): BoardState {
            fun stringList(key: String): List<String> {
                val array = json.optJSONArray(key) ?: JSONArray()
                return (0 until array.length()).map { array.optString(it) }
            }

            fun intList(key: String): List<Int> {
                val array = json.optJSONArray(key) ?: JSONArray()
                return (0 until array.length()).map { array.optInt(it) }
            }

            fun stringToInt(key: String): Map<String, Int> {
                val obj = json.optJSONObject(key) ?: JSONObject()
                return obj.keys().asSequence().associateWith { obj.optInt(it) }
            }

            fun intToString(key: String): Map<Int, String> {
                val obj = json.optJSONObject(key) ?: JSONObject()
                return obj.keys().asSequence().mapNotNull { key ->
                    key.toIntOrNull()?.let { it to obj.optString(key) }
                }.toMap()
            }

            fun intToInt(key: String): Map<Int, Int> {
                val obj = json.optJSONObject(key) ?: JSONObject()
                return obj.keys().asSequence().mapNotNull { key ->
                    key.toIntOrNull()?.let { it to obj.optInt(key) }
                }.toMap()
            }

            return BoardState(
                started = json.optBoolean("started", false),
                order = stringList("order"),
                turn = json.optInt("turn"),
                positions = stringToInt("positions"),
                owners = intToString("owners"),
                houses = intToInt("houses"),
                jailed = stringToInt("jailed"),
                dice = intList("dice"),
                doublesRun = json.optInt("doublesRun"),
                rolled = json.optBoolean("rolled", false),
                awaitingBuy = if (json.isNull("awaitingBuy")) null else json.optInt("awaitingBuy"),
                lastCard = if (json.isNull("lastCard")) null else json.optInt("lastCard"),
                log = stringList("log")
            )
        }
    }
}
