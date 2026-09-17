package com.misgastos.app.ocr

import java.text.SimpleDateFormat
import java.util.Locale

object ReceiptParser {

    private val genericTotalKeywords = listOf(
        "total a pagar", "importe total", "total tarjeta",
        "total efectivo", "total factura", "total pagado",
        "suma total", "total €", "total a", "total", "importe",
    )

    private val dateRegexes = listOf(
        Regex("(\\d{2}[/.-]\\d{2}[/.-]\\d{4})"),
        Regex("(\\d{1,2}\\s+de\\s+[a-záéíóú]+\\s+de\\s+\\d{4})", RegexOption.IGNORE_CASE),
        Regex("(\\d{4}[/.-]\\d{2}[/.-]\\d{2})"),
        Regex("(\\d{2}[/.-]\\d{2}[/.-]\\d{2})"),
    )

    private val dateFormats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy", "yyyy/MM/dd", "dd/MM/yy")

    private val numberRegex = Regex("(\\d{1,8}[.,]\\d{2})\\s*€?")

    private val noiseLines = listOf(
        "iva", "cif", "nif", "tlf", "tel", "teléf", "telefono", "teléfono",
        "www", "http", "gracias", "gràcies", "merci", "thank",
        "c/ ", "avda", "av.", "pol.", "polígono", "calle",
        "c.p.", "cp ", "€/kg", "€/ud", "ud.", "kg.", "tarjeta", "efectivo",
        "cambio", "entregado", "vuelto", "vuelta", "apto", "operación",
        "nº op", "aut.", "autoriz", "referencia", "lote",
    )

    private val lineItemRegex = Regex("^(.+?)\\s+(\\d{1,8}[.,]\\d{2})\\s*€?$")

    fun parse(rawText: String, template: ReceiptTemplate? = null): ParsedReceipt {
        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val total = findTotal(lines, template)
        val date = findDate(lines, template)
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

    fun detectTotalKeyword(rawText: String): String? {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        for (kw in genericTotalKeywords) {
            val hit = lines.firstOrNull { line ->
                val lower = line.lowercase(Locale.getDefault())
                lower.contains(kw) && numberRegex.containsMatchIn(line)
            }
            if (hit != null) return kw
        }
        return null
    }

    fun detectDateFormat(dateStr: String?): String? {
        if (dateStr.isNullOrBlank()) return null
        for (fmt in dateFormats) {
            runCatching { SimpleDateFormat(fmt, Locale.getDefault()).parse(dateStr) }
                .getOrNull()?.let { return fmt }
        }
        return null
    }

    private fun parseAmount(text: String): Double? =
        text.replace(".", "", false)
            .replace(",", ".", false)
            .replace("€", "", true)
            .trim()
            .toDoubleOrNull()

    private fun findTotal(lines: List<String>, template: ReceiptTemplate?): Double? {
        val templateKeyword = template?.totalKeyword?.takeIf { it.isNotBlank() }
        if (templateKeyword != null) {
            val byTemplate = findTotalByKeyword(lines, templateKeyword, requireStart = true)
            if (byTemplate != null) return byTemplate
        }
        for (kw in genericTotalKeywords) {
            val value = findTotalByKeyword(lines, kw, requireStart = false)
            if (value != null) return value
        }
        val allNumbers = lines.mapNotNull { numberRegex.find(it)?.let { m -> parseAmount(m.value) } }
        return allNumbers.maxOrNull()
    }

    private fun findTotalByKeyword(lines: List<String>, keyword: String, requireStart: Boolean): Double? {
        for (line in lines) {
            val lower = line.lowercase(Locale.getDefault())
            val matches = if (requireStart) lower.startsWith(keyword) else lower.contains(keyword)
            if (matches) {
                val match = numberRegex.find(line)
                return match?.let { parseAmount(it.value) }
            }
        }
        return null
    }

    private fun findDate(lines: List<String>, template: ReceiptTemplate?): String? {
        val templateFormat = template?.dateFormat?.takeIf { it.isNotBlank() }
        if (templateFormat != null) {
            val regex = dateFormatToRegex(templateFormat)
            for (line in lines) {
                regex.find(line)?.let { return it.value }
            }
        }
        for (line in lines) {
            for (regex in dateRegexes) {
                regex.find(line)?.let { return it.value }
            }
        }
        return null
    }

    private fun dateFormatToRegex(format: String): Regex {
        val pattern = format
            .replace("yyyy", "\\d{4}")
            .replace("yy", "\\d{2}")
            .replace("dd", "\\d{2}")
            .replace("MM", "\\d{2}")
            .replace("/", "[/.-]")
            .replace("-", "[/.-]")
            .replace(".", "[/.-]")
        return Regex("($pattern)")
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
            if (lower.startsWith("total") || lower.startsWith("suma")) continue
            items.add(ParsedLineItem(name = name, price = price))
        }
        return items
    }

    @Suppress("unused")
    fun formatDateForDisplay(dateStr: String): String? {
        for (fmt in dateFormats) {
            runCatching {
                val parsed = SimpleDateFormat(fmt, Locale.getDefault()).parse(dateStr)
                parsed?.let { return SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(it) }
            }
        }
        return null
    }
}
