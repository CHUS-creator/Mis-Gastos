package com.misgastos.app.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object DateUtils {
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    fun formatDate(timestamp: Long): String = dateFmt.format(Date(timestamp))

    fun formatMonth(timestamp: Long): String =
        monthFmt.format(Date(timestamp)).replaceFirstChar { it.uppercase() }

    fun monthRange(timestamp: Long): LongRange {
        val cal = Calendar.getInstance().apply {
            time = Date(timestamp)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        cal.add(Calendar.MILLISECOND, -1)
        return start..cal.timeInMillis
    }

    @Suppress("unused")
    fun currentMonthRange(): LongRange = monthRange(System.currentTimeMillis())
}
