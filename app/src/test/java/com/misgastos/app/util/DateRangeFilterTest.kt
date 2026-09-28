package com.misgastos.app.util

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DateRangeFilterTest {

    private fun calendarOf(year: Int, month: Int, day: Int, hour: Int = 12): Calendar =
        Calendar.getInstance().apply {
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

    @Test
    fun `rangeFor ALL devuelve null para no filtrar`() {
        assertNull(DateUtils.rangeFor(DateRangeFilter.ALL, System.currentTimeMillis()))
    }

    @Test
    fun `rangeFor WEEK cubre los ultimos 7 dias`() {
        val now = calendarOf(2026, Calendar.SEPTEMBER, 18).timeInMillis
        val range = DateUtils.rangeFor(DateRangeFilter.WEEK, now)!!

        assertEquals(now, range.last)
        assertEquals(now - (7L * 24 * 60 * 60 * 1000), range.first)

        val start = Calendar.getInstance().apply { timeInMillis = range.first }
        assertEquals(11, start.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.SEPTEMBER, start.get(Calendar.MONTH))
    }

    @Test
    fun `rangeFor MONTH resta un mes calendar`() {
        val now = calendarOf(2026, Calendar.SEPTEMBER, 18).timeInMillis
        val range = DateUtils.rangeFor(DateRangeFilter.MONTH, now)!!

        assertEquals(now, range.last)
        val start = Calendar.getInstance().apply { timeInMillis = range.first }
        assertEquals(18, start.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.AUGUST, start.get(Calendar.MONTH))
        assertEquals(12, start.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun `rangeFor MONTH maneja cambio de ano`() {
        val now = calendarOf(2026, Calendar.JANUARY, 15).timeInMillis
        val range = DateUtils.rangeFor(DateRangeFilter.MONTH, now)!!

        val start = Calendar.getInstance().apply { timeInMillis = range.first }
        assertEquals(15, start.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.DECEMBER, start.get(Calendar.MONTH))
        assertEquals(2025, start.get(Calendar.YEAR))
    }

    @Test
    fun `rangeFor YEAR resta un ano`() {
        val now = calendarOf(2026, Calendar.SEPTEMBER, 18).timeInMillis
        val range = DateUtils.rangeFor(DateRangeFilter.YEAR, now)!!

        assertEquals(now, range.last)
        val start = Calendar.getInstance().apply { timeInMillis = range.first }
        assertEquals(18, start.get(Calendar.DAY_OF_MONTH))
        assertEquals(Calendar.SEPTEMBER, start.get(Calendar.MONTH))
        assertEquals(2025, start.get(Calendar.YEAR))
    }

    @Test
    fun `rangeFor YEAR maneja 29 de febrero en ano bisiesto`() {
        val now = calendarOf(2028, Calendar.MARCH, 1).timeInMillis
        val range = DateUtils.rangeFor(DateRangeFilter.YEAR, now)!!

        val start = Calendar.getInstance().apply { timeInMillis = range.first }
        assertEquals(Calendar.MARCH, start.get(Calendar.MONTH))
        assertEquals(2027, start.get(Calendar.YEAR))
        assertTrue(range.first < range.last)
    }

    @Test
    fun `los rangos siempre incluyen la fecha actual`() {
        val now = System.currentTimeMillis()
        DateRangeFilter.entries
            .mapNotNull { DateUtils.rangeFor(it, now) }
            .forEach { range ->
                assertTrue(now in range)
            }
    }
}
