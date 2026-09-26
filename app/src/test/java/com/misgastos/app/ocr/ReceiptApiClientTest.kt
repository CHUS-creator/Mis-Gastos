package com.misgastos.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptApiClientTest {

    @Test
    fun `parsear respuesta json limpia`() {
        val content = """
            {"merchant": "MERCADONA, S.A.", "address": "CL GRAN CANARIA 6", "date": "22/09/2026", "total": 20.85,
             "lineItems": [{"name": "AGUA MINERAL", "quantity": 6, "unitPrice": 0.29, "price": 1.75}]}
        """.trimIndent()
        val result = ReceiptApiClient.parseJson(content)
        assertEquals("MERCADONA, S.A.", result.merchant)
        assertEquals("CL GRAN CANARIA 6", result.address)
        assertEquals("22/09/2026", result.date)
        assertEquals(20.85, result.total!!, 0.001)
        assertEquals(1, result.lineItems.size)
        assertEquals("AGUA MINERAL", result.lineItems[0].name)
        assertEquals(1.75, result.lineItems[0].price, 0.001)
        assertEquals(6.0, result.lineItems[0].quantity, 0.001)
        assertEquals(0.29, result.lineItems[0].unitPrice!!, 0.001)
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
        assertNull(result.address)
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
    fun `unitPrice y quantity rellenan price faltante`() {
        val content = """
            {"merchant": "MERCADONA", "total": 4.64,
             "lineItems": [{"name": "AGUA MINERAL", "quantity": 2, "unitPrice": 1.16}]}
        """.trimIndent()
        val result = ReceiptApiClient.parseJson(content)
        assertEquals(1, result.lineItems.size)
        assertEquals(2.32, result.lineItems[0].price, 0.001)
        assertEquals(2.0, result.lineItems[0].quantity, 0.001)
        assertEquals(1.16, result.lineItems[0].unitPrice!!, 0.001)
    }

    @Test
    fun `quantity se deduce de price y unitPrice`() {
        val content = """
            {"merchant": "ALDI", "total": 4.5,
             "lineItems": [{"name": "LECHE", "unitPrice": 0.95, "price": 2.85}]}
        """.trimIndent()
        val result = ReceiptApiClient.parseJson(content)
        assertEquals(1, result.lineItems.size)
        assertEquals(3.0, result.lineItems[0].quantity, 0.01)
        assertEquals(2.85, result.lineItems[0].price, 0.001)
    }

    @Test
    fun `quantity como texto con coma se parsea`() {
        val content = """
            {"merchant": "LIDL", "total": 3.9,
             "lineItems": [{"name": "PAN", "quantity": "1,5", "unitPrice": 2.0, "price": 3.0}]}
        """.trimIndent()
        val result = ReceiptApiClient.parseJson(content)
        assertEquals(1, result.lineItems.size)
        assertEquals(1.5, result.lineItems[0].quantity, 0.001)
        assertEquals(3.0, result.lineItems[0].price, 0.001)
    }

    @Test
    fun `fechas se normalizan a dia mes anio`() {
        assertEquals("22/09/2026", ReceiptApiClient.normalizeDate("22/09/2026"))
        assertEquals("02/09/2026", ReceiptApiClient.normalizeDate("2/9/2026"))
        assertEquals("22/09/2026", ReceiptApiClient.normalizeDate("22-09-26"))
        assertNull(ReceiptApiClient.normalizeDate(null))
        assertEquals("22/09/2026", ReceiptApiClient.normalizeDate(" 22/09/2026 "))
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
