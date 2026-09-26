package com.misgastos.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptApiClientTest {

    @Test
    fun `parsear respuesta json limpia`() {
        val content = """
            {"merchant": "MERCADONA, S.A.", "date": "22/09/2026", "total": 20.85,
             "lineItems": [{"name": "AGUA MINERAL", "price": 1.25}]}
        """.trimIndent()
        val result = ReceiptApiClient.parseJson(content)
        assertEquals("MERCADONA, S.A.", result.merchant)
        assertEquals("22/09/2026", result.date)
        assertEquals(20.85, result.total!!, 0.001)
        assertEquals(1, result.lineItems.size)
        assertEquals("AGUA MINERAL", result.lineItems[0].name)
        assertEquals(1.25, result.lineItems[0].price, 0.001)
    }

    @Test
    fun `parsear respuesta con markdown`() {
        val content = "```json\n{\"merchant\": \"ALDI\", \"total\": 14.26}\n```"
        val result = ReceiptApiClient.parseJson(content)
        assertEquals("ALDI", result.merchant)
        assertEquals(14.26, result.total!!, 0.001)
        assertNull(result.date)
        assertTrue(result.lineItems.isEmpty())
    }

    @Test
    fun `lineas con precio no positivo se descartan`() {
        val content = """
            {"merchant": "X", "total": 5.0,
             "lineItems": [{"name": "A", "price": 2.0}, {"name": "B", "price": 0}, {"name": "", "price": 1.0}]}
        """.trimIndent()
        val result = ReceiptApiClient.parseJson(content)
        assertEquals(1, result.lineItems.size)
        assertEquals("A", result.lineItems[0].name)
    }

    @Test
    fun `campos ausentes devuelven null`() {
        val content = """{"lineItems": []}"""
        val result = ReceiptApiClient.parseJson(content)
        assertNull(result.merchant)
        assertNull(result.date)
        assertNull(result.total)
        assertTrue(result.lineItems.isEmpty())
    }

    @Test(expected = ReceiptApiException::class)
    fun `respuesta no json lanza excepcion`() {
        ReceiptApiClient.parseJson("Lo siento, no puedo procesar eso")
    }

    @Test
    fun `lineas con campos en orden distinto se parsean`() {
        val content = """
            {"merchant": "MERCADONA", "date": "22/09/2026", "total": 20.85,
             "lineItems": [{"price": 1.25, "name": "AGUA MINERAL"},
                          {"name": "PAN", "quantity": 2, "price": 1.10}]}
        """.trimIndent()
        val result = ReceiptApiClient.parseJson(content)
        assertEquals(2, result.lineItems.size)
        assertEquals("AGUA MINERAL", result.lineItems[0].name)
        assertEquals(1.25, result.lineItems[0].price, 0.001)
        assertEquals("PAN", result.lineItems[1].name)
        assertEquals(1.10, result.lineItems[1].price, 0.001)
    }

    @Test
    fun `precios como texto con coma se parsean`() {
        val content = """
            {"merchant": "ALDI", "total": "14,26",
             "lineItems": [{"name": "AZUCAR", "price": "0,95"}]}
        """.trimIndent()
        val result = ReceiptApiClient.parseJson(content)
        assertEquals(14.26, result.total!!, 0.001)
        assertEquals(1, result.lineItems.size)
        assertEquals(0.95, result.lineItems[0].price, 0.001)
    }

    @Test
    fun `proveedor local lanza excepcion`() {
        val config = ReceiptApiConfig(provider = ReceiptApiProvider.LOCAL)
        try {
            kotlinx.coroutines.runBlocking { ReceiptApiClient.extract(config, "texto") }
            throw AssertionError("Debería haber lanzado ReceiptApiException")
        } catch (e: ReceiptApiException) {
        }
    }
}
