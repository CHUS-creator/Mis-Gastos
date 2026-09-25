package com.misgastos.app.ocr

import java.text.SimpleDateFormat
import java.util.Locale

object ReceiptParser {

    private val genericTotalKeywords = listOf(
        "total a pagar", "a pagar", "importe total", "total tarjeta",
        "total efectivo", "total factura", "total pagado",
        "suma total", "total €", "total a", "total (impuestos incl",
        "total", "importe",
    )

    private val numericOnlyRegex = Regex("^[\\d\\s.,]+\\s*€?$")

    private val decimalStartRegex = Regex("^\\d+[.,]\\d")

    private val dateRegexes = listOf(
        Regex("(\\d{2}[/.-]\\d{2}[/.-]\\d{4})"),
        Regex("(\\d{1,2}\\s+de\\s+[a-záéíóú]+\\s+de\\s+\\d{4})", RegexOption.IGNORE_CASE),
        Regex("(\\d{4}[/.-]\\d{2}[/.-]\\d{2})"),
        Regex("(\\d{2}[/.-]\\d{2}[/.-]\\d{2})"),
    )

    private val dateFormats = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy", "yyyy/MM/dd", "dd/MM/yy")

    private val numberRegex = Regex("(?<!\\d)(\\d{1,8}[.,]\\d{2,3})\\b\\s*€?")

    private val noiseLines = listOf(
        "iva", "igic", "cif", "nif", "tlf", "tel", "teléf", "telefono", "teléfono",
        "www", "http", "gracias", "gràcies", "merci", "thank",
        "c/ ", "avda", "av.", "pol.", "polígono", "calle",
        "c.p.", "cp ", "€/kg", "€/ud", "ud.", "kg.", "tarjeta", "efectivo",
        "cambio", "entregado", "entrega", "devolución", "devolucion", "vuelto", "vuelta", "apto", "operación",
        "nº op", "aut.", "autoriz", "referencia", "lote", "desc.",
        "impuesto", "base", "copia", "mastercard", "impresion", "impresión",
        "super reducido", "tfno", "hora", "vendedor", "surt.",
        "producto", "descripcion", "descripción", "precio", "cantidad", "unidad",
        "fecha", "eur", "pvp", "saldo", "euro", "cobrado", "cuenta", "ahorra",
        "por tu compra", "comerciante minor", "imp.", "unit",
    )

    private val itemNoiseLines = noiseLines - "eur"

    private val currencyTokenRegex = Regex("\\b(?:EUR|EUP)\\b", RegexOption.IGNORE_CASE)

    private val payKeywords = listOf("importe", "entregado", "cobrado", "tarjeta", "pago", "visa")

    private val lineItemRegex = Regex("^(.+?)\\s+(\\d{1,8}[.,]\\d{2,3})\\s*€?$")

    private val spacedLettersRegex = Regex("(?:[A-Za-zÁÉÍÓÚÜÑáéíóúñü] ){2,}[A-Za-zÁÉÍÓÚÜÑáéíóúñü]")

    fun parse(rawText: String, template: ReceiptTemplate? = null): ParsedReceipt {
        val lines = rawText.lines()
            .map { it.replace("|", " ") }
            .map { it.replace(Regex("\\s+"), " ") }
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

    private fun parseAmount(text: String): Double? {
        val cleaned = text.replace("€", "", ignoreCase = true).trim()
        val normalized = when {
            cleaned.contains(",") && cleaned.contains(".") ->
                cleaned.replace(".", "", ignoreCase = false).replace(",", ".", ignoreCase = false)
            cleaned.contains(",") -> cleaned.replace(",", ".", ignoreCase = false)
            else -> cleaned
        }
        return normalized.toDoubleOrNull()
    }

    private fun findTotal(lines: List<String>, template: ReceiptTemplate?): Double? {
        val templateKeyword = template?.totalKeyword?.takeIf { it.isNotBlank() }
        if (templateKeyword != null) {
            findTotalByKeyword(lines, templateKeyword, requireStart = true)?.let { return it }
        }
        for (kw in genericTotalKeywords) {
            val value = findTotalByKeyword(lines, kw, requireStart = false, allowNextLine = false)
            if (value != null) return value
        }
        for (kw in genericTotalKeywords) {
            val value = findTotalByKeyword(lines, kw, requireStart = false, allowNextLine = true)
            if (value != null) return value
        }
        val allNumbers = lines.mapNotNull { numberRegex.find(it)?.let { m -> parseAmount(m.value) } }
        return allNumbers.maxOrNull()
    }

    private fun findTotalByKeyword(
        lines: List<String>,
        keyword: String,
        requireStart: Boolean,
        allowNextLine: Boolean = true,
    ): Double? {
        for ((index, line) in lines.withIndex()) {
            val lower = line.lowercase(Locale.getDefault())
            if (lower.contains("subtotal")) continue
            val matches = if (requireStart) lower.startsWith(keyword) else lower.contains(keyword)
            if (matches) {
                numberRegex.find(line)?.let { return parseAmount(it.value) }
                if (allowNextLine) {
                    lines.getOrNull(index + 1)?.let { next ->
                        if (numericOnlyRegex.matches(next.trim())) {
                            numberRegex.find(next)?.let { return parseAmount(it.value) }
                        }
                    }
                }
            }
        }
        return null
    }

    private val mangledDateRegexes = listOf(
        Regex("(\\d{2})\\s+(\\d{2})[.](\\d{4})"),
        Regex("(\\d{2})[.](\\d{2})\\s+(\\d{4})"),
    )

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
        for (line in lines) {
            for (regex in mangledDateRegexes) {
                regex.find(line)?.let { m ->
                    val g = m.groupValues
                    return "${g[1]}/${g[2]}/${g[3]}"
                }
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
        val candidates = mutableListOf<String>()
        for (line in lines) {
            val lower = line.lowercase(Locale.getDefault())
            if (noiseLines.any { lower.contains(it) }) continue
            if (genericTotalKeywords.any { lower.startsWith(it) }) continue
            if (numberRegex.containsMatchIn(line)) continue
            if (line.length !in 3..40) continue
            if ((line == line.uppercase(Locale.getDefault())) && (line.length > 25)) continue
            if (line.count { it.isLetter() } < 3) continue
            if (line.first().isDigit()) continue
            candidates.add(line)
            if (candidates.size == 2) break
        }
        val first = candidates.firstOrNull() ?: return null
        val second = candidates.getOrNull(1)
        if (second != null) {
            val f = collapseSpacedLetters(first).trim(' ', '#', '*', '.', ',', '-')
            val s = collapseSpacedLetters(second).trim(' ', '#', '*', '.', ',', '-')
            val firstIsAddress = f.contains(Regex("\\d{5}")) || f.lowercase().contains("puerto del")
            val secondIsLogo = (s == s.uppercase(Locale.getDefault())) && (s.length in 3..10)
            val firstIsLogo = (f == f.uppercase(Locale.getDefault())) && (f.length in 3..10)
            if (secondIsLogo && !firstIsLogo && firstIsAddress) return s
        }
        return collapseSpacedLetters(first).trim(' ', '#', '*', '.', ',', '-')
    }

    private fun collapseSpacedLetters(line: String): String =
        spacedLettersRegex.replace(line) { it.value.replace(" ", "") }

    private fun findLineItems(lines: List<String>, total: Double?): List<ParsedLineItem> {
        val items = mutableListOf<ParsedLineItem>()
        for (raw in lines) {
            val line = currencyTokenRegex.replace(raw, " ")
                .replace(Regex("\\s+"), " ")
                .trim()
            if (line.isBlank()) continue
            val lower = line.lowercase(Locale.getDefault())
            if (itemNoiseLines.any { lower.contains(it) }) continue
            if (lower.startsWith("total") || lower.startsWith("suma") || lower.startsWith("subtotal")) continue
            val match = lineItemRegex.matchEntire(line)
            if (match != null) {
                var name = match.groupValues[1].trim()
                val price = parseAmount(match.groupValues[2]) ?: continue
                numberRegex.find(name)?.let { name = name.substring(0, it.range.first).trim() }
                if (name.isBlank()) continue
                if (price <= 0.0) continue
                if ((total != null) && (price == total) && (name.length <= 8)) continue
                if (payKeywords.any { name.lowercase(Locale.getDefault()).startsWith(it) }) continue
                if (decimalStartRegex.containsMatchIn(name)) continue
                items.add(ParsedLineItem(name = name, price = price))
            } else {
                val numbers = numberRegex.findAll(line).toList()
                if (numbers.size < 2) continue
                var name = line.substring(0, numbers.first().range.first).trim()
                val price = parseAmount(numbers.last().value) ?: continue
                numberRegex.find(name)?.let { name = name.substring(0, it.range.first).trim() }
                if (price <= 0.0) continue
                if (name.count { it.isLetter() } < 2) continue
                if ((total != null) && (price == total)) continue
                items.add(ParsedLineItem(name = name, price = price))
            }
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
