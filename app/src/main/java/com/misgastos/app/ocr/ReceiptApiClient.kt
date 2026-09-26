package com.misgastos.app.ocr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

private val merchantFieldRegex = Regex("""\"merchant\"\s*:\s*\"([^\"]*)\"""")
private val dateFieldRegex = Regex("""\"date\"\s*:\s*\"([^\"]*)\"""")
private val totalRegex = Regex("""\"total\"\s*:\s*(-?\d+(?:\.\d+)?)""")
private val lineItemRegex = Regex("""\"name\"\s*:\s*\"([^\"]*)\"\s*,\s*\"price\"\s*:\s*(-?\d+(?:\.\d+)?)""")
private val jsonBlockRegex = Regex("\\{.*}", RegexOption.DOT_MATCHES_ALL)

enum class ReceiptApiProvider {
    LOCAL, MISTRAL, GEMINI
}

data class ReceiptApiConfig(
    val provider: ReceiptApiProvider = ReceiptApiProvider.LOCAL,
    val apiKey: String = "",
)

data class ReceiptApiResult(
    val merchant: String?,
    val date: String?,
    val total: Double?,
    val lineItems: List<ParsedLineItem> = emptyList(),
)

class ReceiptApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

object ReceiptApiClient {

    private const val MISTRAL_URL = "https://api.mistral.ai/v1/chat/completions"
    private const val MISTRAL_MODEL = "mistral-small-latest"
    private const val GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    private const val GEMINI_MODEL = "gemini-2.0-flash"
    private const val TIMEOUT_MS = 30_000

    private val prompt = """
        Eres un extractor de datos de tickets de compra (recibos) españoles.
        Devuelve SOLO un objeto JSON válido, sin markdown ni explicaciones:
        {"merchant": string|null, "date": string|null, "total": number|null, "lineItems": [{"name": string, "price": number}]}
        - merchant: nombre del comercio; corrige errores evidentes del OCR
          (p. ej. "nercadona" -> "MERCADONA, S.A.").
        - date: fecha en formato dd/MM/yyyy (null si no aparece).
        - total: importe final pagado con IVA/IGIC incluido, número con punto decimal.
        - lineItems: líneas de producto con el precio pagado por línea; lista vacía si no hay.
        No inventes valores que no aparezcan en el texto.
    """.trimIndent()

    suspend fun extract(config: ReceiptApiConfig, rawText: String): ReceiptApiResult =
        withContext(Dispatchers.IO) {
            when (config.provider) {
                ReceiptApiProvider.MISTRAL -> callMistral(config.apiKey, rawText)
                ReceiptApiProvider.GEMINI -> callGemini(config.apiKey, rawText)
                ReceiptApiProvider.LOCAL ->
                    throw ReceiptApiException("Proveedor LOCAL: usar ReceiptParser")
            }
        }

    private fun callMistral(apiKey: String, rawText: String): ReceiptApiResult {
        val body = JSONObject()
            .put("model", MISTRAL_MODEL)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", prompt))
                    .put(JSONObject().put("role", "user").put("content", rawText)),
            )
            .put("temperature", 0)
            .put("response_format", JSONObject().put("type", "json_object"))
        val response = post(MISTRAL_URL, apiKey, body.toString())
        val content = JSONObject(response)
            .getJSONArray("choices")
            .getJSONObject(0)
            .getJSONObject("message")
            .getString("content")
        return parseJson(content)
    }

    private fun callGemini(apiKey: String, rawText: String): ReceiptApiResult {
        val body = JSONObject()
            .put(
                "contents",
                JSONArray().put(
                    JSONObject().put(
                        "parts",
                        JSONArray()
                            .put(JSONObject().put("text", "$prompt\n\nTicket:\n$rawText")),
                    ),
                ),
            )
            .put(
                "generationConfig",
                JSONObject()
                    .put("temperature", 0)
                    .put("responseMimeType", "application/json"),
            )
        val url = URL("$GEMINI_URL?key=$apiKey")
        val response = post(url, body.toString())
        val content = JSONObject(response)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
        return parseJson(content)
    }

    private fun post(url: String, apiKey: String, payload: String): String {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }
        try {
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw ReceiptApiException("HTTP $code: ${text.take(300)}")
            }
            return text
        } catch (e: IOException) {
            throw ReceiptApiException("Error de red: ${e.message}", e)
        } finally {
            conn.disconnect()
        }
    }

    private fun post(url: URL, payload: String): String {
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }
        try {
            conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw ReceiptApiException("HTTP $code: ${text.take(300)}")
            }
            return text
        } catch (e: IOException) {
            throw ReceiptApiException("Error de red: ${e.message}", e)
        } finally {
            conn.disconnect()
        }
    }

    internal fun parseJson(content: String): ReceiptApiResult {
        val cleaned = content.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val block = jsonBlockRegex.find(cleaned)?.value
            ?: throw ReceiptApiException("Respuesta no es JSON válido: ${cleaned.take(200)}")
        val items = mutableListOf<ParsedLineItem>()
        lineItemRegex.findAll(block).forEach { m ->
            val name = m.groupValues[1].trim()
            val price = m.groupValues[2].toDoubleOrNull() ?: return@forEach
            if (name.isNotBlank() && price > 0.0) {
                items.add(ParsedLineItem(name = name, price = price))
            }
        }
        return ReceiptApiResult(
            merchant = merchantFieldRegex.find(block)?.groupValues?.get(1)?.trim()?.ifBlank { null },
            date = dateFieldRegex.find(block)?.groupValues?.get(1)?.trim()?.ifBlank { null },
            total = totalRegex.find(block)?.groupValues?.get(1)?.toDoubleOrNull(),
            lineItems = items,
        )
    }
}
