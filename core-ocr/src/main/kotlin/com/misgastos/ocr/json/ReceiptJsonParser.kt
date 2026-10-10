package com.misgastos.ocr.json

import com.misgastos.ocr.api.OcrException
import com.misgastos.ocr.api.OcrReceipt
import com.misgastos.ocr.api.ParsedLineItem

/**
 * Parseo tolerante de la respuesta JSON de un LLM: acepta fences ```json,
 * comas o puntos como separador decimal y campos ausentes.
 * Compartido por todos los proveedores basados en LLM.
 */
object ReceiptJsonParser {

    private val merchantFieldRegex = Regex("""\"merchant\"\s*:\s*\"([^\"]*)\"""")
    private val addressFieldRegex = Regex("""\"address\"\s*:\s*\"([^\"]*)\"""")
    private val dateFieldRegex = Regex("""\"date\"\s*:\s*\"([^\"]*)\"""")
    private val totalRegex = Regex("""\"total\"\s*:\s*\"?(-?\d+(?:[.,]\d+)?)\"?""")
    private val lineItemsArrayRegex =
        Regex("""\"lineItems\"\s*:\s*\[(.*?)]""", RegexOption.DOT_MATCHES_ALL)
    private val lineObjectBodyRegex = Regex("""\{([^{}]*)}""")
    private val lineNameRegex = Regex("""\"name\"\s*:\s*\"([^\"]*)\"""")
    private val lineQuantityRegex = Regex("""\"quantity\"\s*:\s*\"?(-?\d+(?:[.,]\d+)?)\"?""")
    private val lineUnitPriceRegex = Regex("""\"unitPrice\"\s*:\s*\"?(-?\d+(?:[.,]\d+)?)\"?""")
    private val linePriceRegex = Regex("""\"price\"\s*:\s*\"?(-?\d+(?:[.,]\d+)?)\"?""")
    private val jsonBlockRegex = Regex("\\{.*}", RegexOption.DOT_MATCHES_ALL)

    fun parse(content: String): OcrReceipt {
        val cleaned = content.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val block = jsonBlockRegex.find(cleaned)?.value
            ?: throw OcrException("Respuesta no es JSON válido: ${cleaned.take(200)}")
        val items = mutableListOf<ParsedLineItem>()
        lineItemsArrayRegex.find(block)?.groupValues?.get(1)?.let { arrayContent ->
            lineObjectBodyRegex.findAll(arrayContent).forEach { m ->
                val obj = m.groupValues[1]
                val name = lineNameRegex.find(obj)?.groupValues?.get(1)?.trim().orEmpty()
                val quantity = lineQuantityRegex.find(obj)?.groupValues?.get(1)
                    ?.replace(',', '.')?.toDoubleOrNull()
                val unitPrice = lineUnitPriceRegex.find(obj)?.groupValues?.get(1)
                    ?.replace(',', '.')?.toDoubleOrNull()
                val price = linePriceRegex.find(obj)?.groupValues?.get(1)
                    ?.replace(',', '.')?.toDoubleOrNull()
                buildLineItem(name, quantity, unitPrice, price)?.let { items.add(it) }
            }
        }
        return OcrReceipt(
            merchant = merchantFieldRegex.find(block)?.groupValues?.get(1)?.trim()?.ifBlank { null },
            address = addressFieldRegex.find(block)?.groupValues?.get(1)?.trim()?.ifBlank { null },
            date = normalizeDate(dateFieldRegex.find(block)?.groupValues?.get(1)),
            total = totalRegex.find(block)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull(),
            lineItems = items,
        )
    }

    fun normalizeDate(text: String?): String? {
        val raw = text?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val match = Regex("""^(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})$""").find(raw) ?: return raw
        val (d, m, y) = match.destructured
        val day = d.padStart(2, '0')
        val month = m.padStart(2, '0')
        val year = if (y.length == 2) "20$y" else y
        return "$day/$month/$year"
    }

    private fun buildLineItem(
        name: String,
        quantity: Double?,
        unitPrice: Double?,
        linePrice: Double?,
    ): ParsedLineItem? {
        if (name.isBlank()) return null
        val qty = quantity?.takeIf { it > 0.0 }
        val unit = unitPrice?.takeIf { it > 0.0 }
        val total = linePrice?.takeIf { it > 0.0 }
        val resolvedQty = qty
            ?: if (unit != null && total != null && unit > 0.0) (total / unit).takeIf { it > 0.0 } else null
        val resolvedUnit = unit
            ?: if (resolvedQty != null && resolvedQty > 0.0) total?.div(resolvedQty) else null
        val resolvedTotal = total
            ?: if (unit != null && qty != null) unit * qty else null
        if (resolvedTotal == null || resolvedTotal <= 0.0) return null
        return ParsedLineItem(
            name = name,
            price = resolvedTotal,
            quantity = resolvedQty ?: 1.0,
            unitPrice = resolvedUnit,
        )
    }
}
