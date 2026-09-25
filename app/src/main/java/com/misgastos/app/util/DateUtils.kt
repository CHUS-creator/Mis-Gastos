package com.misgastos.app.util

import com.misgastos.app.R

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class DateRangeFilter(val labelRes: Int) {
    ALL(R.string.filter_period_all),
    WEEK(R.string.filter_period_week),
    MONTH(R.string.filter_period_month),
    YEAR(R.string.filter_period_year),
}

object DateUtils {
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val monthFmt = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

    fun formatDate(timestamp: Long): String = dateFmt.format(Date(timestamp))

    fun parseDate(text: String): Long? =
        runCatching { dateFmt.parse(text.trim())?.time }.getOrNull()

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

    fun rangeFor(filter: DateRangeFilter, now: Long = System.currentTimeMillis()): LongRange? =
        when (filter) {
            DateRangeFilter.ALL -> null
            DateRangeFilter.WEEK -> {
                val start = now - WEEK_MS
                start..now
            }
            DateRangeFilter.MONTH -> {
                val cal = Calendar.getInstance().apply {
                    time = Date(now)
                    add(Calendar.MONTH, -1)
                }
                cal.timeInMillis..now
            }
            DateRangeFilter.YEAR -> {
                val cal = Calendar.getInstance().apply {
                    time = Date(now)
                    add(Calendar.YEAR, -1)
                }
                cal.timeInMillis..now
            }
        }

    private const val WEEK_MS = 7L * 24 * 60 * 60 * 1000
}
