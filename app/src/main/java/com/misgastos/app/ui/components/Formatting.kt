package com.misgastos.app.ui.components

import java.text.NumberFormat
import java.util.Locale

fun formatMoney(value: Double): String {
    val fmt = NumberFormat.getCurrencyInstance(Locale.getDefault())
    return fmt.format(value)
}

@Suppress("unused")
fun formatSigned(value: Double): String =
    (if (value >= 0) "+" else "") + formatMoney(value)
