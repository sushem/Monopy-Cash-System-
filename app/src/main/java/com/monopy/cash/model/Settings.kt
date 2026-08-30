package com.monopy.cash.model

import org.json.JSONArray
import org.json.JSONObject

/** A currency the board can be played in. */
data class CurrencyOption(val symbol: String, val label: String)

val Currencies: List<CurrencyOption> = listOf(
    CurrencyOption("$", "US Dollar"),
    CurrencyOption("₹", "Indian Rupee"),
    CurrencyOption("€", "Euro"),
    CurrencyOption("£", "Pound Sterling"),
    CurrencyOption("¥", "Japanese Yen"),
    CurrencyOption("A$", "Australian Dollar"),
    CurrencyOption("C$", "Canadian Dollar"),
    CurrencyOption("R$", "Brazilian Real"),
    CurrencyOption("kr", "Krona"),
    CurrencyOption("CHF", "Swiss Franc"),
    CurrencyOption("M", "Monopoly money")
)

val DEFAULT_QUICK_AMOUNTS = listOf(50L, 100L, 500L)

/** Everything the player can change under Settings. */
data class Settings(
    val currencySymbol: String = Currencies.first().symbol,
    val quickAmounts: List<Long> = DEFAULT_QUICK_AMOUNTS
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("currency", currencySymbol)
        put("quickAmounts", JSONArray().also { array -> quickAmounts.forEach { array.put(it) } })
    }

    companion object {
        /** More than this and the chip row stops being a shortcut. */
        const val MAX_QUICK_AMOUNTS = 8

        fun fromJson(json: JSONObject): Settings {
            val array = json.optJSONArray("quickAmounts") ?: JSONArray()
            val amounts = (0 until array.length()).map { array.optLong(it) }.filter { it > 0 }
            return Settings(
                currencySymbol = json.optString("currency", Currencies.first().symbol)
                    .ifBlank { Currencies.first().symbol },
                quickAmounts = amounts.sorted()
            )
        }
    }
}
