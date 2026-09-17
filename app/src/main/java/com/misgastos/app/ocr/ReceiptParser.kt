package com.misgastos.app.ocr

import java.text.SimpleDateFormat
import java.util.Locale

object ReceiptParser {

    private val totalKeywords = listOf(
        "total", "total a pagar", "importe total", "total tarjeta",
        "total efectivo", "total factura", "total €", "total a",
        "suma total", "importe", "total pagado",
    )

    private val dateRegexes = listOf(
        Regex("(\\d{2}[/.-]\\d{2}[/.-]\\d{4})"),
        Regex("(\\d{1,2}\\s+de\\s+[a-záéíóú]+\\s+de\\s+\\d{4})", RegexOption.IGNORE_CASE),
    )

    private val numberRegex = Regex("(\\d{1,8}[.,]\\d{2})\\s*€?")

    private val noiseLines = listOf(
        "iva", "cif", "nif", "tlf", "tel", "teléf", "telefono", "teléfono",
        "www", "http", "gracias", "gràcies", "merci", "thank",
        "c/ ", "avda", "av.", "pol.", "polígono", "calle",
        "c.p.", "cp ", "€/kg", "€/ud", "ud.", "kg.",
    )

    private val lineItemRegex = Regex("^(.+?)\\s+(\\d{1,8}[.,]\\d{2})\\s*€?$")

    fun parse(rawText: String): ParsedReceipt {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val total = findTotal(lines)
        val date = findDate(lines)
        val merchant = findMerchant(lines)
        val lineItems = findLineItems(lines, total)

        return ParsedReceipt(
            total = total,
            date = date,
            merchant = merchant,
            lineItems = lineItems,
            rawText = rawText,
        )
    }

    private fun parseAmount(text: String): Double? =
        text.replace(".", "", false)
            .replace(",", ".", false)
            .replace("€", "", true)
            .trim()
            .toDoubleOrNull()

    private fun findTotal(lines: List<String>): Double? {
        var bestCandidate: Double? = null
        for (line in lines) {
            val lower = line.lowercase(Locale.getDefault())
            val keyword = totalKeywords.firstOrNull { lower.contains(it) }
            if (keyword != null) {
                val match = numberRegex.find(line)
                val value = match?.let { parseAmount(it.value) }
                if (value != null) {
                    val strength = if (lower.startsWith(keyword)) 3 else 2
                    if (bestCandidate == null || strength >= 2) {
                        bestCandidate = value
                    }
                }
            }
        }
        if (bestCandidate != null) return bestCandidate
        val allNumbers = lines.mapNotNull { numberRegex.find(it)?.let { m -> parseAmount(m.value) } }
        return allNumbers.maxOrNull()
    }

    private fun findDate(lines: List<String>): String? {
        for (line in lines) {
            for (regex in dateRegexes) {
                regex.find(line)?.let { return it.value }
            }
        }
        return null
    }

    private fun findMerchant(lines: List<String>): String? {
        for (line in lines) {
            val lower = line.lowercase(Locale.getDefault())
            if (noiseLines.any { lower.contains(it) }) continue
            if (numberRegex.containsMatchIn(line)) continue
            if (line.length < 3 || line.length > 40) continue
            if (line == line.uppercase(Locale.getDefault()) && line.length > 25) continue
            return line
        }
        return null
    }

    private fun findLineItems(lines: List<String>, total: Double?): List<ParsedLineItem> {
        val items = mutableListOf<ParsedLineItem>()
        for (line in lines) {
            val lower = line.lowercase(Locale.getDefault())
            if (noiseLines.any { lower.contains(it) }) continue
            val match = lineItemRegex.matchEntire(line) ?: continue
            val name = match.groupValues[1].trim()
            val price = parseAmount(match.groupValues[2]) ?: continue
            if (price <= 0.0) continue
            if (total != null && price == total && name.length <= 8) continue
            if (lower.startsWith("total")) continue
            items.add(ParsedLineItem(name = name, price = price))
        }
        return items
    }

    @Suppress("unused")
    fun formatDateForDisplay(dateStr: String): String? {
        val formats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy")
        for (fmt in formats) {
            runCatching {
                val parsed = SimpleDateFormat(fmt, Locale.getDefault()).parse(dateStr)
                parsed?.let { return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) }
            }
        }
        return null
    }
}
