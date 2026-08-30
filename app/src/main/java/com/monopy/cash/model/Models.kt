package com.monopy.cash.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A single money movement recorded against an account. */
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    /** Positive when money came in, negative when money went out. */
    val amount: Long,
    val note: String,
    /** How this entry moved the account's loan counter, so undo can reverse both. */
    val loanDelta: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("amount", amount)
        put("note", note)
        put("loanDelta", loanDelta)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(json: JSONObject) = Transaction(
            id = json.optString("id", UUID.randomUUID().toString()),
            amount = json.optLong("amount"),
            note = json.optString("note"),
            loanDelta = json.optLong("loanDelta"),
            timestamp = json.optLong("timestamp", System.currentTimeMillis())
        )
    }
}

/** A property a player owns, plus the houses standing on it. */
data class Asset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val houses: Int = 0
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("houses", houses)
    }

    companion object {
        /** Four houses trade up to a hotel, so five is the top of the ladder. */
        const val MAX_HOUSES = 5

        fun fromJson(json: JSONObject) = Asset(
            id = json.optString("id", UUID.randomUUID().toString()),
            name = json.optString("name", "Property"),
            houses = json.optInt("houses", 0).coerceIn(0, MAX_HOUSES)
        )
    }
}

/** A player or the bank. Both hold cash, so both are accounts. */
data class Account(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val colorArgb: Int,
    val balance: Long,
    val isBank: Boolean = false,
    /** Cash borrowed from the bank and not yet paid back. Kept apart from [balance]. */
    val loan: Long = 0,
    val assets: List<Asset> = emptyList(),
    val transactions: List<Transaction> = emptyList()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("color", colorArgb)
        put("balance", balance)
        put("isBank", isBank)
        put("loan", loan)
        put("assets", JSONArray().also { array -> assets.forEach { array.put(it.toJson()) } })
        put("transactions", JSONArray().also { array -> transactions.forEach { array.put(it.toJson()) } })
    }

    companion object {
        const val BANK_ID = "bank"

        fun fromJson(json: JSONObject): Account {
            val array = json.optJSONArray("transactions") ?: JSONArray()
            val transactions = (0 until array.length()).map { Transaction.fromJson(array.getJSONObject(it)) }
            val assetArray = json.optJSONArray("assets") ?: JSONArray()
            val assets = (0 until assetArray.length()).map { Asset.fromJson(assetArray.getJSONObject(it)) }
            return Account(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Player"),
                colorArgb = json.optInt("color", PlayerColors.first()),
                balance = json.optLong("balance"),
                isBank = json.optBoolean("isBank", false),
                loan = json.optLong("loan"),
                assets = assets,
                transactions = transactions
            )
        }
    }
}

/** Token colours players can pick from. */
val PlayerColors: List<Int> = listOf(
    0xFFE53935.toInt(), // red
    0xFF1E88E5.toInt(), // blue
    0xFF43A047.toInt(), // green
    0xFFFB8C00.toInt(), // orange
    0xFF8E24AA.toInt(), // purple
    0xFF00ACC1.toInt(), // teal
    0xFFD81B60.toInt(), // pink
    0xFF6D4C41.toInt(), // brown
    0xFF3949AB.toInt(), // indigo
    0xFF7CB342.toInt(), // lime
    0xFFF4511E.toInt(), // deep orange
    0xFF546E7A.toInt()  // slate
)

/** Standard Monopoly bank float and per-player starting cash. */
const val DEFAULT_BANK_CASH = 20580L
const val DEFAULT_STARTING_CASH = 1500L
