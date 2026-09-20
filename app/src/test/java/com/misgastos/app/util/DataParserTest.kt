package com.misgastos.app.util

import com.misgastos.app.data.entity.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataParserTest {

    @Test
    fun `parseCsv importa transacciones validas`() {
        val csv = "date,type,amount,category,description,merchant,source\n" +
            "15/09/2026,EXPENSE,12.50,Supermercado,Compra semanal,Mercadona,SCAN\n" +
            "16/09/2026,INCOME,1500.00,Salario,Nómina,,MANUAL\n"

        val bundle = DataParser.parseCsv(csv)

        assertTrue(bundle.warnings.isEmpty())
        assertEquals(2, bundle.transactions.size)

        val first = bundle.transactions[0]
        assertEquals(TransactionType.EXPENSE, first.type)
        assertEquals(12.50, first.amount, 1e-9)
        assertEquals("Supermercado", first.category)
        assertEquals("Compra semanal", first.description)
        assertEquals("Mercadona", first.merchant)

        val second = bundle.transactions[1]
        assertEquals(TransactionType.INCOME, second.type)
        assertEquals("", second.merchant)
    }

    @Test
    fun `parseCsv maneja campos entrecomillados con comas`() {
        val csv = "date,type,amount,category,description,merchant,source\n" +
            "15/09/2026,EXPENSE,10.00,\"Super,mercado\",\"Compra con \"\"comillas\"\"\",Lidl,MANUAL\n"

        val bundle = DataParser.parseCsv(csv)

        assertEquals(1, bundle.transactions.size)
        assertEquals("Super,mercado", bundle.transactions[0].category)
        assertEquals("Compra con \"comillas\"", bundle.transactions[0].description)
    }

    @Test
    fun `parseCsv ignora filas invalidas y avisa`() {
        val csv = "date,type,amount,category,description,merchant,source\n" +
            "fecha-mala,EXPENSE,10.00,Super,desc,Lidl,MANUAL\n" +
            "15/09/2026,TIPO_RARO,10.00,Super,desc,Lidl,MANUAL\n" +
            "15/09/2026,EXPENSE,no-numero,Super,desc,Lidl,MANUAL\n" +
            "15/09/2026,EXPENSE,10.00,Super,desc,Lidl,MANUAL\n"

        val bundle = DataParser.parseCsv(csv)

        assertEquals(1, bundle.transactions.size)
        assertEquals(3, bundle.warnings.size)
    }

    @Test
    fun `parseCsv rechaza cabecera sin columnas obligatorias`() {
        val csv = "col1,col2\n1,2"
        val bundle = DataParser.parseCsv(csv)
        assertTrue(bundle.transactions.isEmpty())
        assertTrue(bundle.warnings.first().contains("Faltan columnas"))
    }

    @Test
    fun `parseCsv acepta columnas en otro orden`() {
        val csv = "type,amount,date,category\nEXPENSE,5.50,20/09/2026,Café"
        val bundle = DataParser.parseCsv(csv)
        assertEquals(1, bundle.transactions.size)
        assertEquals(TransactionType.EXPENSE, bundle.transactions[0].type)
        assertEquals("Café", bundle.transactions[0].category)
    }

    @Test
    fun `parseCsv archivo vacio devuelve warning`() {
        val bundle = DataParser.parseCsv("")
        assertTrue(bundle.transactions.isEmpty())
        assertTrue(bundle.warnings.isNotEmpty())
    }

    @Test
    fun `parseCsv acepta GASTO y INGRESO como tipos en espanol`() {
        val csv = "date,type,amount,category\n" +
            "15/09/2026,GASTO,10.00,Super\n" +
            "16/09/2026,INGRESO,100.00,Salario\n"

        val bundle = DataParser.parseCsv(csv)

        assertEquals(2, bundle.transactions.size)
        assertEquals(TransactionType.EXPENSE, bundle.transactions[0].type)
        assertEquals(TransactionType.INCOME, bundle.transactions[1].type)
    }

    @Test
    fun `parseJson importa transacciones con lineItems`() {
        val json = "[{\"date\":\"15/09/2026\",\"type\":\"EXPENSE\",\"amount\":\"12.50\"," +
            "\"category\":\"Supermercado\",\"description\":\"Compra\",\"merchant\":\"Lidl\",\"source\":\"SCAN\"," +
            "\"lineItems\":[{\"name\":\"Leche\",\"price\":\"1.19\",\"quantity\":\"1.00\"}," +
            "{\"name\":\"Pan\",\"price\":\"0.90\",\"quantity\":\"2.00\"}]}," +
            "{\"date\":\"16/09/2026\",\"type\":\"INCOME\",\"amount\":\"1500.00\"," +
            "\"category\":\"Salario\",\"description\":\"\",\"merchant\":\"\",\"source\":\"MANUAL\"}]"

        val bundle = DataParser.parseJson(json)

        assertTrue(bundle.warnings.isEmpty())
        assertEquals(2, bundle.transactions.size)

        val first = bundle.transactions[0]
        assertEquals(2, first.lineItems.size)
        assertEquals("Leche", first.lineItems[0].name)
        assertEquals(1.19, first.lineItems[0].price, 1e-9)
        assertEquals(0.90, first.lineItems[1].price, 1e-9)
        assertEquals(2.0, first.lineItems[1].quantity, 1e-9)
    }

    @Test
    fun `parseJson ignora transacciones con datos invalidos`() {
        val json = "[{\"date\":\"fecha-mala\",\"type\":\"EXPENSE\",\"amount\":\"12.50\",\"category\":\"Super\"}," +
            "{\"date\":\"16/09/2026\",\"type\":\"EXPENSE\",\"amount\":\"12.50\",\"category\":\"Super\"}]"

        val bundle = DataParser.parseJson(json)

        assertEquals(1, bundle.transactions.size)
        assertEquals(1, bundle.warnings.size)
    }

    @Test
    fun `parseJson rechaza texto que no es array`() {
        val bundle = DataParser.parseJson("{\"date\":\"15/09/2026\"}")
        assertTrue(bundle.transactions.isEmpty())
        assertTrue(bundle.warnings.isNotEmpty())
    }

    @Test
    fun `parseJson con caracteres escapados`() {
        val json = "[{\"date\":\"15/09/2026\",\"type\":\"EXPENSE\",\"amount\":\"10.00\"," +
            "\"category\":\"Super\",\"description\":\"Café \\\"especial\\\"\",\"merchant\":\"Lidl\"}]"

        val bundle = DataParser.parseJson(json)

        assertEquals(1, bundle.transactions.size)
        assertEquals("Café \"especial\"", bundle.transactions[0].description)
    }

    @Test
    fun `parseType acepta variantes y rechaza desconocidas`() {
        assertEquals(TransactionType.INCOME, DataFormat.parseType("income"))
        assertEquals(TransactionType.EXPENSE, DataFormat.parseType("Gasto"))
        assertEquals(null, DataFormat.parseType("xyz"))
    }

    @Test
    fun `parseAmount maneja formato europeo y rechaza negativos`() {
        assertEquals(12.5, DataFormat.parseAmount("12,50")!!, 1e-9)
        assertEquals(12.5, DataFormat.parseAmount("12.50 €")!!, 1e-9)
        assertEquals(null, DataFormat.parseAmount("-5.00"))
        assertEquals(null, DataFormat.parseAmount("abc"))
    }

    @Test
    fun `csv roundtrip conserva los datos basicos`() {
        val escaped = DataFormat.csvEscape("a,b\"c\nd")
        val fields = DataFormat.splitCsvLine(escaped)
        assertEquals("a,b\"c\nd", DataFormat.csvUnescape(fields[0]))
    }
}
