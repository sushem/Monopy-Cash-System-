package com.monopy.cash.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

private val numberFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)
private val timeFormat = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())

/** The currency symbol chosen in Settings, available to every screen. */
val LocalCurrency = compositionLocalOf { "$" }

/** Multi-character symbols read better with a space: "CHF 1,500" but "$1,500". */
private fun prefix(symbol: String) = if (symbol.length > 1) "$symbol " else symbol

/** "$1,500" / "-$250" */
fun formatMoney(amount: Long, symbol: String): String {
    val sign = if (amount < 0) "-" else ""
    return sign + prefix(symbol) + numberFormat.format(amount.absoluteValue)
}

/** "+$200" / "-$200" — used in the transaction history. */
fun formatSigned(amount: Long, symbol: String): String {
    val sign = if (amount < 0) "-" else "+"
    return sign + prefix(symbol) + numberFormat.format(amount.absoluteValue)
}

/** Formats using the currency currently in force. */
@Composable
@ReadOnlyComposable
fun money(amount: Long): String = formatMoney(amount, LocalCurrency.current)

@Composable
@ReadOnlyComposable
fun signedMoney(amount: Long): String = formatSigned(amount, LocalCurrency.current)

fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

fun Int.toComposeColor(): Color = Color(this)

/** Black or white, whichever stays readable on top of this colour. */
fun Color.contrastingContentColor(): Color =
    if ((red * 0.299f + green * 0.587f + blue * 0.114f) > 0.6f) Color.Black else Color.White
