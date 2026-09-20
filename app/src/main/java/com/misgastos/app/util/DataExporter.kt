package com.misgastos.app.util

import android.content.Context
import android.net.Uri
import com.misgastos.app.data.entity.LineItem
import com.misgastos.app.data.entity.Transaction
import com.misgastos.app.data.entity.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExportLineItem(
    val name: String,
    val price: Double,
    val quantity: Double,
)

data class ExportTransaction(
    val dateText: String,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val description: String,
    val merchant: String,
    val source: String,
    val lineItems: List<ExportLineItem> = emptyList(),
)

data class DataBundle(
    val transactions: List<ExportTransaction> = emptyList(),
    val warnings: List<String> = emptyList(),
)

object DataFormat {

    val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.US)

    fun formatDate(timestamp: Long): String = dateFmt.format(Date(timestamp))

    fun parseDate(text: String): Long? =
        runCatching { dateFmt.parse(text.trim())?.time }.getOrNull()

    fun parseType(text: String): TransactionType? = when (text.trim().uppercase(Locale.US)) {
        "INCOME", "INGRESO" -> TransactionType.INCOME
        "EXPENSE", "GASTO" -> TransactionType.EXPENSE
        else -> null
    }

    fun parseAmount(text: String): Double? =
        text.trim()
            .replace("€", "", ignoreCase = true)
            .replace(Regex("\\s+"), "")
            .replace(",", ".")
            .toDoubleOrNull()
            ?.takeIf { it >= 0.0 }

    fun csvEscape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    fun csvUnescape(value: String): String {
        val trimmed = value.trim()
        if (trimmed.length >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.removePrefix("\"").removeSuffix("\"").replace("\"\"", "\"")
        }
        return value
    }

    fun splitCsvLine(line: String): List<String> {
        val fields = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    fields.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
            i++
        }
        fields.add(current.toString())
        return fields
    }

    fun jsonEscape(s: String): String =
        "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t") + "\""

    fun jsonStringOf(json: String, key: String): String? {
        val keyIdx = json.indexOf("\"$key\"")
        if (keyIdx < 0) return null
        var i = json.indexOf(':', keyIdx)
        if (i < 0) return null
        i++
        while (i < json.length && json[i].isWhitespace()) i++
        if (i >= json.length || json[i] != '"') return null
        val sb = StringBuilder()
        i++
        while (i < json.length) {
            val c = json[i]
            if (c == '\\' && i + 1 < json.length) {
                when (val next = json[i + 1]) {
                    '"' -> sb.append('"')
                    '\\' -> sb.append('\\')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    't' -> sb.append('\t')
                    '/' -> sb.append('/')
                    'u' -> {
                        if (i + 5 < json.length) {
                            val hex = json.substring(i + 2, i + 6)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 4
                            }
                        }
                    }
                    else -> sb.append(next)
                }
                i += 2
            } else if (c == '"') {
                return sb.toString()
            } else {
                sb.append(c)
                i++
            }
        }
        return null
    }

    fun jsonNumberListOf(json: String, key: String): List<String> {
        val searchFrom = if (key.isBlank()) 0 else json.indexOf("\"$key\"")
        if (key.isNotBlank() && searchFrom < 0) return emptyList()
        var i = json.indexOf('[', searchFrom)
        if (i < 0) return emptyList()
        i++
        val start = i
        var depth = 1
        while (i < json.length && depth > 0) {
            when (json[i]) {
                '[' -> depth++
                ']' -> depth--
            }
            i++
        }
        if (depth != 0) return emptyList()
        val inner = json.substring(start, i - 1)
        val items = mutableListOf<String>()
        var objStart = -1
        var objDepth = 0
        var j = 0
        while (j < inner.length) {
            when (inner[j]) {
                '{' -> {
                    if (objDepth == 0) objStart = j
                    objDepth++
                }
                '}' -> {
                    objDepth--
                    if (objDepth == 0 && objStart >= 0) {
                        items.add(inner.substring(objStart, j + 1))
                        objStart = -1
                    }
                }
            }
            j++
        }
        return items
    }
}

object DataParser {

    fun parseCsv(text: String): DataBundle {
        val lines = text.lineSequence()
            .map { it.trimEnd('\r') }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return DataBundle(warnings = listOf("Archivo vacío"))

        val header = DataFormat.splitCsvLine(lines.first()).map { it.trim().lowercase(Locale.US) }
        val required = listOf("date", "type", "amount", "category")
        val missing = required.filter { it !in header }
        if (missing.isNotEmpty()) {
            return DataBundle(warnings = listOf("Faltan columnas: ${missing.joinToString(", ")}"))
        }
        val idx = header.withIndex().associate { (i, col) -> col to i }
        val dateIdx = idx.getValue("date")
        val typeIdx = idx.getValue("type")
        val amountIdx = idx.getValue("amount")
        val categoryIdx = idx.getValue("category")
        val descIdx = idx["description"] ?: -1
        val merchantIdx = idx["merchant"] ?: -1
        val sourceIdx = idx["source"] ?: -1

        val transactions = mutableListOf<ExportTransaction>()
        val warnings = mutableListOf<String>()
        for (lineNo in 1 until lines.size) {
            val fields = DataFormat.splitCsvLine(lines[lineNo])
            val date = DataFormat.parseDate(fields.getOrNull(dateIdx) ?: "")
            val type = DataFormat.parseType(fields.getOrNull(typeIdx) ?: "")
            val amount = DataFormat.parseAmount(fields.getOrNull(amountIdx) ?: "")
            if (date == null || type == null || amount == null) {
                warnings.add("Línea ${lineNo + 1}: ignorada por datos inválidos")
                continue
            }
            transactions.add(
                ExportTransaction(
                    dateText = fields.getOrNull(dateIdx).orEmpty().trim(),
                    type = type,
                    amount = amount,
                    category = DataFormat.csvUnescape(fields.getOrNull(categoryIdx).orEmpty()).trim(),
                    description = if (descIdx >= 0) DataFormat.csvUnescape(fields.getOrNull(descIdx).orEmpty()).trim() else "",
                    merchant = if (merchantIdx >= 0) DataFormat.csvUnescape(fields.getOrNull(merchantIdx).orEmpty()).trim() else "",
                    source = if (sourceIdx >= 0) fields.getOrNull(sourceIdx).orEmpty().trim().uppercase(Locale.US) else "MANUAL",
                ),
            )
        }
        return DataBundle(transactions, warnings)
    }

    fun parseJson(text: String): DataBundle {
        val trimmed = text.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return DataBundle(warnings = listOf("Formato JSON no reconocido"))
        }
        val objects = DataFormat.jsonNumberListOf(trimmed, "")
        if (objects.isEmpty()) return DataBundle(warnings = listOf("Sin transacciones en el archivo"))

        val transactions = mutableListOf<ExportTransaction>()
        val warnings = mutableListOf<String>()
        objects.forEachIndexed { index, obj ->
            val dateText = DataFormat.jsonStringOf(obj, "date") ?: ""
            val type = DataFormat.parseType(DataFormat.jsonStringOf(obj, "type") ?: "")
            val amountText = DataFormat.jsonStringOf(obj, "amount")
            val amount = DataFormat.parseAmount(amountText ?: "")
            val date = DataFormat.parseDate(dateText)
            if (date == null || type == null || amount == null) {
                warnings.add("Transacción ${index + 1}: ignorada por datos inválidos")
                return@forEachIndexed
            }
            val items = DataFormat.jsonNumberListOf(obj, "lineItems").mapNotNull { item ->
                val name = DataFormat.jsonStringOf(item, "name") ?: return@mapNotNull null
                val price = DataFormat.parseAmount(DataFormat.jsonStringOf(item, "price") ?: "") ?: return@mapNotNull null
                val quantity = DataFormat.parseAmount(DataFormat.jsonStringOf(item, "quantity") ?: "") ?: 1.0
                ExportLineItem(name = name, price = price, quantity = quantity.takeIf { it > 0.0 } ?: 1.0)
            }
            transactions.add(
                ExportTransaction(
                    dateText = dateText,
                    type = type,
                    amount = amount,
                    category = DataFormat.jsonStringOf(obj, "category").orEmpty().trim(),
                    description = DataFormat.jsonStringOf(obj, "description").orEmpty().trim(),
                    merchant = DataFormat.jsonStringOf(obj, "merchant").orEmpty().trim(),
                    source = (DataFormat.jsonStringOf(obj, "source") ?: "MANUAL").uppercase(Locale.US),
                    lineItems = items,
                ),
            )
        }
        return DataBundle(transactions, warnings)
    }
}

object DataExporter {

    fun exportCsv(context: Context, uri: Uri, transactions: List<Transaction>): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.bufferedWriter().use { writer ->
                    writer.write("date,type,amount,category,description,merchant,source")
                    writer.newLine()
                    transactions.forEach { tx -> writer.write(toCsvRow(tx)) }
                }
            } ?: return false
            true
        }.getOrDefault(false)
    }

    fun exportJson(
        context: Context,
        uri: Uri,
        transactions: List<Transaction>,
        lineItemsByTransaction: (Long) -> List<LineItem> = { emptyList() },
    ): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.bufferedWriter().use { writer ->
                    writer.write("[")
                    transactions.forEachIndexed { index, tx ->
                        if (index > 0) writer.write(",")
                        writer.write(toJsonObject(tx, lineItemsByTransaction(tx.id)))
                    }
                    writer.write("]")
                }
            } ?: return false
            true
        }.getOrDefault(false)
    }

    private fun toCsvRow(tx: Transaction): String {
        val cols = listOf(
            DataFormat.formatDate(tx.date),
            if (tx.type == TransactionType.INCOME) "INCOME" else "EXPENSE",
            String.format(Locale.US, "%.2f", tx.amount),
            DataFormat.csvEscape(tx.category),
            DataFormat.csvEscape(tx.description),
            DataFormat.csvEscape(tx.merchant),
            tx.source.name,
        )
        return cols.joinToString(",") + "\n"
    }

    private fun toJsonObject(tx: Transaction, items: List<LineItem>): String {
        fun s(v: String): String = DataFormat.jsonEscape(v)
        return buildString {
            append("{")
            append("\"date\":").append(s(DataFormat.formatDate(tx.date))).append(",")
            append("\"type\":").append(s(tx.type.name)).append(",")
            append("\"amount\":").append(String.format(Locale.US, "%.2f", tx.amount)).append(",")
            append("\"category\":").append(s(tx.category)).append(",")
            append("\"description\":").append(s(tx.description)).append(",")
            append("\"merchant\":").append(s(tx.merchant)).append(",")
            append("\"source\":").append(s(tx.source.name))
            if (items.isNotEmpty()) {
                append(",\"lineItems\":[")
                items.forEachIndexed { i, item ->
                    if (i > 0) append(",")
                    append("{\"name\":").append(s(item.name)).append(",")
                    append("\"price\":").append(String.format(Locale.US, "%.2f", item.price)).append(",")
                    append("\"quantity\":").append(String.format(Locale.US, "%.2f", item.quantity))
                    append("}")
                }
                append("]")
            }
            append("}")
        }
    }
}
