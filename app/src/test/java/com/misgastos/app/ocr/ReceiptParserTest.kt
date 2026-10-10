package com.misgastos.app.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReceiptParserTest {

    @Test
    fun `parse ticket espanol con total y fecha`() {
        val raw = """
            SUPERMERCADO EL PUENTE
            C/ Mayor 12
            18/09/2026
            Pan de molde     1,85 €
            Leche entera     1,20 €
            TOTAL            3,05 €
        """.trimIndent()

        val parsed = ReceiptParser.parse(raw)

        assertEquals(3.05, parsed.total!!, 0.001)
        assertEquals("18/09/2026", parsed.date)
        assertEquals(2, parsed.lineItems.size)
        assertEquals("Pan de molde", parsed.lineItems[0].name)
        assertEquals(1.85, parsed.lineItems[0].price, 0.001)
    }

    @Test
    fun `parse usa plantilla de comercio para el total`() {
        val raw = """
            MERCADONA
            02/10/2026
            Total compra: 12,50 €
            Importe total: 11,00 €
        """.trimIndent()

        val template = ReceiptTemplate(totalKeyword = "importe total")
        val parsed = ReceiptParser.parse(raw, template)

        assertEquals(11.00, parsed.total!!, 0.001)
    }

    @Test
    fun `parse ticket sin total devuelve el mayor importe`() {
        val raw = """
            TIENDA LA ESQUINA
            4,20 €
            9,99 €
        """.trimIndent()

        val parsed = ReceiptParser.parse(raw)

        assertEquals(9.99, parsed.total!!, 0.001)
    }

    @Test
    fun `detectar palabra clave del total`() {
        val raw = "TOTAL A PAGAR    25,90 €"
        assertEquals("total a pagar", ReceiptParser.detectTotalKeyword(raw))
    }

    @Test
    fun `detectar formato de fecha`() {
        assertEquals("dd/MM/yyyy", ReceiptParser.detectDateFormat("18/09/2026"))
        assertEquals("dd-MM-yyyy", ReceiptParser.detectDateFormat("18-09-2026"))
        assertNull(ReceiptParser.detectDateFormat(null))
    }

    @Test
    fun `detectar comercio omite lineas con ruido`() {
        val raw = "C/ Mayor 12\nTEL 955123456\nPANADERIA SANTA ANA\nTOTAL 5,00 €"
        val parsed = ReceiptParser.parse(raw)

        assertEquals("PANADERIA SANTA ANA", parsed.merchant)
    }

    @Test
    fun `lineas de ruido no se interpretan como productos`() {
        val raw = """
            MERCADONA
            tarjeta 12,50 €
            TOTAL 12,50 €
        """.trimIndent()

        val parsed = ReceiptParser.parse(raw)

        assertTrue(parsed.lineItems.isEmpty())
    }

    @Test
    fun `texto vacio produce recibo nulo`() {
        val parsed = ReceiptParser.parse("")

        assertNull(parsed.total)
        assertNull(parsed.date)
        assertNull(parsed.merchant)
        assertTrue(parsed.lineItems.isEmpty())
    }    @Test
    fun `cantidad prefija y precio unitario en formato 3 columnas`() {
        val raw = """
            MERCADONA, S.A.
            Descripci\u00f3n P. Unit Imp (\u20ac)
            PAN DE PUEBLO 1,55 1,55
            1 5 BOCADILLOS 1,70 1,70
            2 RUEDA VERDEJO 2,00 4,00
            6 AGUA MINERAL NATURAL 1,25 7,50
            TOTAL 20,85
        """.trimIndent()
        val parsed = ReceiptParser.parse(raw)
        assertEquals(4, parsed.lineItems.size)
        val bocadillos = parsed.lineItems.first { it.name.contains("BOCADILLOS") }
        assertEquals("5 BOCADILLOS", bocadillos.name)
        assertEquals(1.0, bocadillos.quantity, 0.001)
        assertEquals(1.70, bocadillos.price, 0.001)
        val agua = parsed.lineItems.first { it.name.contains("AGUA") }
        assertEquals(6.0, agua.quantity, 0.001)
        assertEquals(1.25, agua.unitPrice!!, 0.001)
        assertEquals(7.50, agua.price, 0.001)
        val verdejo = parsed.lineItems.first { it.name.contains("VERDEJO") }
        assertEquals(2.0, verdejo.quantity, 0.001)
        assertEquals(2.00, verdejo.unitPrice!!, 0.001)
        assertEquals(4.00, verdejo.price, 0.001)
        val pan = parsed.lineItems.first { it.name.contains("PAN DE PUEBLO") }
        assertEquals(1.0, pan.quantity, 0.001)
        assertEquals(1.55, pan.price, 0.001)
    }
}
