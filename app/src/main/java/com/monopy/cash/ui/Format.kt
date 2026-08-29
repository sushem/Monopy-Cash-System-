package com.monopy.cash.ui

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue

private val numberFormat: NumberFormat = NumberFormat.getIntegerInstance(Locale.US)
private val timeFormat = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())

/** "$1,500" / "-$250" */
fun formatMoney(amount: Long): String {
    val sign = if (amount < 0) "-" else ""
    return sign + "$" + numberFormat.format(amount.absoluteValue)
}

/** "+$200" / "-$200" — used in the transaction history. */
fun formatSigned(amount: Long): String {
    val sign = if (amount < 0) "-" else "+"
    return sign + "$" + numberFormat.format(amount.absoluteValue)
}

fun formatTime(timestamp: Long): String = timeFormat.format(Date(timestamp))

fun Int.toComposeColor(): Color = Color(this)

/** Black or white, whichever stays readable on top of this colour. */
fun Color.contrastingContentColor(): Color =
    if ((red * 0.299f + green * 0.587f + blue * 0.114f) > 0.6f) Color.Black else Color.White
