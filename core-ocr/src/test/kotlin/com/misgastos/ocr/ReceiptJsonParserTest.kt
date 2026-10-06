package com.misgastos.ocr

import com.misgastos.ocr.api.OcrException
import com.misgastos.ocr.json.ReceiptJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptJsonParserTest {

    @Test
    fun `json limpio se parsea completo`() {
        val json = """
            {"merchant": "MERCADONA, S.A.", "address": "CL GRAN CANARIA 6",
             "date": "14/09/2026", "total": 12.50,
             "lineItems": [
               {"name": "PAN", "quantity": 1, "unitPrice": 0.85, "price": 0.85},
               {"name": "LECHE", "quantity": 2, "unitPrice": "1,20", "price": 2.40}
             ]}
        """.trimIndent()
        val receipt = ReceiptJsonParser.parse(json)
        assertEquals("MERCADONA, S.A.", receipt.merchant)
        assertEquals("14/09/2026", receipt.date)
        assertEquals(12.50, receipt.total!!, 0.001)
        assertEquals(2, receipt.lineItems.size)
        assertEquals(2.40, receipt.lineItems[1].price, 0.001)
        assertEquals(1.2, receipt.lineItems[1].unitPrice!!, 0.001)
    }

    @Test
    fun `json con fence markdown se limpia`() {
        val json = "```json\n{\"merchant\": \"LIDL\", \"total\": \"9,99\"}\n```"
        val receipt = ReceiptJsonParser.parse(json)
        assertEquals("LIDL", receipt.merchant)
        assertEquals(9.99, receipt.total!!, 0.001)
    }

    @Test
    fun `linea incompleta deduce cantidad y precio unitario`() {
        val json = """
            {"total": 5.00, "lineItems": [
              {"name": "AGUA", "quantity": 2, "price": 2.00},
              {"name": "SIN PRECIO"},
              {"name": "", "price": 1.0}
            ]}
        """.trimIndent()
        val receipt = ReceiptJsonParser.parse(json)
        assertEquals(1, receipt.lineItems.size)
        assertEquals(1.0, receipt.lineItems[0].unitPrice!!, 0.001)
    }

    @Test
    fun `fecha con ano de dos digitos se normaliza`() {
        assertEquals("06/04/2026", ReceiptJsonParser.normalizeDate("6/4/26"))
        assertEquals("06/04/2026", ReceiptJsonParser.normalizeDate("06/04/2026"))
        assertNull(ReceiptJsonParser.normalizeDate(null))
    }

    @Test
    fun `contenido sin json lanza OcrException`() {
        try {
            ReceiptJsonParser.parse("Lo siento, no puedo leer el ticket")
            throw AssertionError("Debería haber lanzado OcrException")
        } catch (e: OcrException) {
            assertTrue(e.message!!.contains("JSON"))
        }
    }
}
