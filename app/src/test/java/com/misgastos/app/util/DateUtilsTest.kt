package com.misgastos.app.util

import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {

    @Test
    fun `formatDate produce ddMMyyyy con separador de barra`() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 18, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        assertEquals("18/09/2026", DateUtils.formatDate(cal.timeInMillis))
    }

    @Test
    fun `parseDate y formatDate son inversas`() {
        val parsed = DateUtils.parseDate("01/03/2026")
        assertEquals("01/03/2026", DateUtils.formatDate(parsed!!))
    }

    @Test
    fun `parseDate rechaza texto invalido`() {
        assertEquals(null, DateUtils.parseDate("no es fecha"))
        assertEquals(null, DateUtils.parseDate(""))
    }

    @Test
    fun `monthRange cubre desde el primer dia hasta el ultimo del mes`() {
        val midMonth = Calendar.getInstance().apply {
            set(2026, Calendar.FEBRUARY, 15, 10, 30, 0)
        }.timeInMillis

        val range = DateUtils.monthRange(midMonth)
        val start = Calendar.getInstance().apply { timeInMillis = range.first }
        val end = Calendar.getInstance().apply { timeInMillis = range.last }

        assertEquals(1, start.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, start.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, start.get(Calendar.MINUTE))
        assertEquals(Calendar.FEBRUARY, start.get(Calendar.MONTH))

        assertEquals(28, end.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, end.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, end.get(Calendar.MINUTE))
    }

    @Test
    fun `monthRange maneja febrero en ano bisiesto`() {
        val midMonth = Calendar.getInstance().apply {
            set(2028, Calendar.FEBRUARY, 15, 0, 0, 0)
        }.timeInMillis

        val range = DateUtils.monthRange(midMonth)
        val end = Calendar.getInstance().apply { timeInMillis = range.last }

        assertEquals(29, end.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun `formatMonth capitaliza la primera letra`() {
        val ts = Calendar.getInstance().apply {
            set(2026, Calendar.SEPTEMBER, 18, 0, 0, 0)
        }.timeInMillis

        val label = DateUtils.formatMonth(ts)

        assertTrue(label.isNotEmpty())
        assertEquals(label, label.replaceFirstChar { it.uppercase() })
    }
}
