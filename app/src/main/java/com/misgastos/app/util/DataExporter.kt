package com.misgastos.app.util

import android.content.Context
import android.net.Uri
import com.misgastos.app.data.entity.Transaction
import com.misgastos.app.data.entity.TransactionType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DataExporter {

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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

    fun exportJson(context: Context, uri: Uri, transactions: List<Transaction>): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.bufferedWriter().use { writer ->
                    writer.write("[")
                    transactions.forEachIndexed { index, tx ->
                        if (index > 0) writer.write(",")
                        writer.write(toJsonObject(tx))
                    }
                    writer.write("]")
                }
            } ?: return false
            true
        }.getOrDefault(false)
    }

    private fun toCsvRow(tx: Transaction): String {
        val cols = listOf(
            dateFmt.format(Date(tx.date)),
            if (tx.type == TransactionType.INCOME) "INCOME" else "EXPENSE",
            String.format(Locale.US, "%.2f", tx.amount),
            csvEscape(tx.category),
            csvEscape(tx.description),
            csvEscape(tx.merchant),
            tx.source.name,
        )
        return cols.joinToString(",") + "\n"
    }

    private fun csvEscape(value: String): String {
        val needsQuotes = value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }
        val escaped = value.replace("\"", "\"\"")
        return if (needsQuotes) "\"$escaped\"" else escaped
    }

    private fun toJsonObject(tx: Transaction): String {
        fun jsonStr(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\""
        return buildString {
            append("{")
            append("\"date\":").append(jsonStr(dateFmt.format(Date(tx.date)))).append(",")
            append("\"type\":").append(jsonStr(tx.type.name)).append(",")
            append("\"amount\":").append(String.format(Locale.US, "%.2f", tx.amount)).append(",")
            append("\"category\":").append(jsonStr(tx.category)).append(",")
            append("\"description\":").append(jsonStr(tx.description)).append(",")
            append("\"merchant\":").append(jsonStr(tx.merchant)).append(",")
            append("\"source\":").append(jsonStr(tx.source.name))
            append("}")
        }
    }
}
