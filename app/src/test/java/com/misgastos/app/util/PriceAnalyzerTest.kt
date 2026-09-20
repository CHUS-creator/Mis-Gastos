package com.misgastos.app.util

import com.misgastos.app.data.dao.ProductPriceRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceAnalyzerTest {

    private fun row(
        name: String,
        price: Double,
        quantity: Double = 1.0,
        merchant: String,
        date: Long = 1000L,
    ) = ProductPriceRow(name = name, price = price, quantity = quantity, merchant = merchant, date = date)

    @Test
    fun `normalizeName minusculiza y colapsa espacios`() {
        assertEquals("leche entera", PriceAnalyzer.normalizeName("  LECHE   Entera "))
    }

    @Test
    fun `normalizeName quita prefijos de cantidad y puntuacion final`() {
        assertEquals("leche", PriceAnalyzer.normalizeName("2 x Leche."))
        assertEquals("pan", PriceAnalyzer.normalizeName("3 Pan,"))
    }

    @Test
    fun `unitPriceOf divide precio entre cantidad`() {
        assertEquals(1.5, PriceAnalyzer.unitPriceOf(row("leche", 3.0, 2.0, "Lidl"))!!, 1e-9)
        assertNull(PriceAnalyzer.unitPriceOf(row("leche", 3.0, 0.0, "Lidl")))
        assertNull(PriceAnalyzer.unitPriceOf(row("leche", -1.0, 1.0, "Lidl")))
    }

    @Test
    fun `compare devuelve vacio con query en blanco`() {
        val rows = listOf(row("leche", 1.0, merchant = "Lidl"))
        assertTrue(PriceAnalyzer.compare(rows, "").isEmpty())
        assertTrue(PriceAnalyzer.compare(rows, "   ").isEmpty())
    }

    @Test
    fun `compare agrupa por comercio y ordena de mas barato a mas caro`() {
        val rows = listOf(
            row("Leche entera", 1.29, merchant = "Mercadona"),
            row("LECHE ENTERA", 1.19, merchant = "Lidl"),
            row("Vino tinto", 3.5, merchant = "Lidl"),
        )
        val result = PriceAnalyzer.compare(rows, "leche")

        assertEquals(2, result.size)
        assertEquals("Lidl", result[0].cheapest?.merchant)
        assertEquals(1.19, result[0].cheapest?.unitPrice!!, 1e-9)
        assertEquals("Mercadona", result[1].cheapest?.merchant)
        assertEquals(0.0, result[0].savingsPerUnit, 1e-9)
        assertEquals(0.10, result[1].savingsPerUnit, 1e-9)
    }

    @Test
    fun `compare usa el precio por unidad cuando hay cantidades`() {
        val rows = listOf(
            row("Leche pack 6", 6.0, quantity = 6.0, merchant = "Mercadona"),
            row("Leche", 1.19, merchant = "Lidl"),
        )
        val result = PriceAnalyzer.compare(rows, "leche")

        assertEquals(2, result.size)
        assertEquals("Mercadona", result[0].cheapest?.merchant)
        assertEquals(1.0, result[0].cheapest?.unitPrice!!, 1e-9)
    }

    @Test
    fun `compare con un solo comercio no calcula ahorro`() {
        val rows = listOf(
            row("Leche", 1.29, merchant = "Mercadona", date = 2000L),
            row("Leche", 1.25, merchant = "Mercadona", date = 1000L),
        )
        val result = PriceAnalyzer.compare(rows, "leche")

        assertEquals(1, result.size)
        assertEquals(0.0, result[0].savingsPerUnit, 1e-9)
        assertEquals(2, result[0].unitPrices.size)
        assertTrue(result[0].unitPrices.first().date >= result[0].unitPrices.last().date)
    }

    @Test
    fun `compare trata comercios en blanco como sin comercio`() {
        val rows = listOf(
            row("Leche", 1.0, merchant = ""),
            row("Leche", 2.0, merchant = "Lidl"),
        )
        val result = PriceAnalyzer.compare(rows, "leche")

        assertEquals(2, result.size)
        assertEquals(PriceAnalyzer.NO_MERCHANT, result[0].cheapest?.merchant)
    }

    @Test
    fun `compare no coincide productos distintos`() {
        val rows = listOf(
            row("Pan de molde", 1.0, merchant = "Lidl"),
        )
        assertTrue(PriceAnalyzer.compare(rows, "leche").isEmpty())
    }

    @Test
    fun `productNames deduplica y descarta vacios`() {
        val rows = listOf(
            row("Leche", 1.0, merchant = "Lidl"),
            row("Leche", 1.1, merchant = "Mercadona"),
            row("", 1.0, merchant = "Lidl"),
            row("Pan", 0.9, merchant = "Lidl"),
        )
        val names = PriceAnalyzer.productNames(rows)
        assertEquals(listOf("Leche", "Pan"), names)
    }
}
