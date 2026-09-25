package com.misgastos.app.ocr

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Suite de regresión sobre el corpus de tickets reales.
 *
 * El corpus (app/src/test/resources/receipts/) contiene el texto OCR de
 * recibos reales y un manifiesto con la verdad esperada por ticket:
 *   archivo | total | fecha | comercio | num_lineas
 *
 * Ver manifiesto para el significado de los valores "-" y negativos.
 */
class ReceiptCorpusTest {

    private data class CorpusEntry(
        val file: File,
        val expectedTotal: Double?,
        val expectedDate: String?,
        val expectedMerchant: String?,
        val expectedLineItems: Int?,
    )

    private fun corpusDir(): File {
        val resource = javaClass.classLoader?.getResource("receipts/manifest.txt")
        if (resource != null) {
            return File(resource.toURI()).parentFile
        }
        val dir = File("src/test/resources/receipts")
        if (dir.isDirectory) {
            return dir
        }
        throw IllegalStateException("Corpus de tickets no encontrado (classpath ni src/test/resources/receipts)")
    }

    private fun loadManifest(): List<CorpusEntry> {
        val manifest = corpusDir().resolve("manifest.txt").readText()
        return manifest.lines()
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val parts = line.split("|")
                require(parts.size == 5) { "Línea de manifiesto inválida: $line" }
                CorpusEntry(
                    file = corpusDir().resolve(parts[0].trim()),
                    expectedTotal = parts[1].trim().takeIf { it != "-" }?.replace(",", ".")?.toDoubleOrNull(),
                    expectedDate = parts[2].trim().takeIf { it != "-" },
                    expectedMerchant = parts[3].trim().takeIf { it != "-" },
                    expectedLineItems = parts[4].trim().toIntOrNull()?.takeIf { it >= 0 },
                )
            }
    }

    private fun rawText(entry: CorpusEntry): String = entry.file.readText()

    @Test
    fun `corpus completo presente`() {
        val entries = loadManifest()
        assertTrue("El corpus debe tener tickets", entries.isNotEmpty())
        entries.forEach { entry ->
            assertTrue("Falta el ticket ${entry.file.name}", entry.file.isFile)
        }
    }

    @Test
    fun `totales correctos en todo el corpus`() {
        val failures = mutableListOf<String>()
        for (entry in loadManifest()) {
            val expected = entry.expectedTotal ?: continue
            val parsed = ReceiptParser.parse(rawText(entry))
            if (parsed.total != expected) {
                failures.add("${entry.file.name}: esperado $expected, obtenido ${parsed.total}")
            }
        }
        assertTrue("Fallos de total:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test
    fun `fechas correctas en todo el corpus`() {
        val failures = mutableListOf<String>()
        for (entry in loadManifest()) {
            val expected = entry.expectedDate ?: continue
            val parsed = ReceiptParser.parse(rawText(entry))
            if (parsed.date != expected) {
                failures.add("${entry.file.name}: esperado $expected, obtenido ${parsed.date}")
            }
        }
        assertTrue("Fallos de fecha:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test
    fun `comercios correctos en todo el corpus`() {
        val failures = mutableListOf<String>()
        for (entry in loadManifest()) {
            val expected = entry.expectedMerchant ?: continue
            val parsed = ReceiptParser.parse(rawText(entry))
            val actual = parsed.merchant.orEmpty()
                .lowercase()
                .replace(Regex("\\s+"), " ")
                .trim()
            if (!actual.contains(expected)) {
                failures.add("${entry.file.name}: esperado '$expected', obtenido '${parsed.merchant}'")
            }
        }
        assertTrue("Fallos de comercio:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test
    fun `lineas de producto dentro del rango en todo el corpus`() {
        val failures = mutableListOf<String>()
        for (entry in loadManifest()) {
            val expected = entry.expectedLineItems ?: continue
            val parsed = ReceiptParser.parse(rawText(entry))
            val size = parsed.lineItems.size
            if (size < 1 || size > expected) {
                failures.add("${entry.file.name}: esperadas 1..$expected, obtenidas $size")
            }
        }
        assertTrue("Fallos de líneas de producto:\n${failures.joinToString("\n")}", failures.isEmpty())
    }

    @Test
    fun `el parser nunca produce lineas de producto con precio no positivo`() {
        for (entry in loadManifest()) {
            val parsed = ReceiptParser.parse(rawText(entry))
            parsed.lineItems.forEach { item ->
                assertTrue("${entry.file.name}: precio no positivo en '${item.name}'", item.price > 0.0)
            }
        }
    }
}
