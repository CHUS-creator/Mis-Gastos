package com.misgastos.ocr.http

import com.misgastos.ocr.api.OcrException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/** POST JSON mínimo sobre HttpURLConnection (sin dependencias externas). */
object Http {

    const val TIMEOUT_MS = 30_000

    fun postJson(
        url: String,
        payload: String,
        headers: Map<String, String> = emptyMap(),
    ): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Content-Type", "application/json")
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
            doOutput = true
        }
        return try {
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw OcrException("HTTP $code: ${text.take(300)}")
            }
            text
        } catch (e: OcrException) {
            throw e
        } catch (e: IOException) {
            throw OcrException("Error de red: ${e.message}", e)
        } finally {
            conn.disconnect()
        }
    }
}

/** Escapado de cadenas para construir JSON a mano (sin org.json en JVM puro). */
object Json {
    fun escape(value: String): String = buildString(value.length + 8) {
        for (ch in value) {
            when (ch) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (ch < ' ') append("\\u%04x".format(ch.code)) else append(ch)
            }
        }
    }

    fun string(value: String): String = "\"${escape(value)}\""
}
