package com.misgastos.app.ocr

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

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

enum class ReceiptApiProvider {
    LOCAL, MISTRAL, GEMINI
}

data class ReceiptApiConfig(
    val provider: ReceiptApiProvider = ReceiptApiProvider.LOCAL,
    val apiKey: String = "",
)

data class ReceiptApiResult(
    val merchant: String?,
    val address: String?,
    val date: String?,
    val total: Double?,
    val lineItems: List<ParsedLineItem> = emptyList(),
)

class ReceiptApiException(message: String, cause: Throwable? = null) : Exception(message, cause)

object ReceiptApiClient {

    private const val MISTRAL_URL = "https://api.mistral.ai/v1/chat/completions"
    private const val MISTRAL_MODEL = "mistral-small-latest"
    private const val GEMINI_BASE_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/"
    private const val GEMINI_PRIMARY_MODEL = "gemini-flash-latest"
    private const val GEMINI_FALLBACK_MODEL = "gemini-flash-lite-latest"
    private const val TIMEOUT_MS = 30_000
    private const val MAX_ATTEMPTS = 3
    private const val RETRY_DELAY_MS = 1_000L

    private val prompt = """
        Eres un extractor de datos de tickets de compra (recibos) españoles.
        Devuelve SOLO un objeto JSON válido, sin markdown ni explicaciones:
        {"merchant": string|null, "address": string|null, "date": string|null, "total": number|null,
         "lineItems": [{"name": string, "quantity": number, "unitPrice": number, "price": number}]}
        El texto proviene de un OCR: puede tener mayúsculas/minúsculas inconsistentes,
        abreviaturas (C/, AV., CTRA., NIF, TFNO.), comas o puntos como separador decimal
        y ruido. Sé tolerante y normaliza.
        - merchant: nombre del comercio; corrige errores evidentes del OCR
          (p. ej. "nercadona" -> "MERCADONA, S.A."). Null si no aparece.
        - address: dirección del establecimiento tal como aparece en el ticket
          (p. ej. "CL GRAN CANARIA 6, 35626 ESQUINZO"). Null si no aparece.
        - date: fecha del ticket en formato dd/MM/yyyy (null si no aparece).
        - total: importe final pagado con IVA/IGIC incluido, número con punto decimal.
        - lineItems: líneas de producto. Para cada una:
          - name: descripción del producto, corrigiendo errores evidentes del OCR.
          - quantity: cantidad comprada (número; por defecto 1 si el ticket no la indica).
          - unitPrice: precio por unidad con punto decimal.
          - price: importe total de la línea (quantity x unitPrice) con punto decimal.
          Lista vacía si el ticket no detalla productos.
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
        val response = try {
            post("$GEMINI_BASE_URL$GEMINI_PRIMARY_MODEL:generateContent?key=$apiKey", null, body.toString())
        } catch (e: ReceiptApiException) {
            if (e.message?.startsWith("HTTP 503") == true) {
                post("$GEMINI_BASE_URL$GEMINI_FALLBACK_MODEL:generateContent?key=$apiKey", null, body.toString())
            } else {
                throw e
            }
        }
        val content = JSONObject(response)
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
        return parseJson(content)
    }

    private fun post(url: String, apiKey: String?, payload: String): String {
        var lastError: ReceiptApiException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                if (apiKey != null) setRequestProperty("Authorization", "Bearer $apiKey")
                setRequestProperty("Content-Type", "application/json")
                doOutput = true
            }
            try {
                conn.outputStream.use { it.write(payload.toByteArray(Charsets.UTF_8)) }
                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
                if (code in 200..299) return text
                val error = ReceiptApiException("HTTP $code: ${text.take(300)}")
                if (code == 503 || code == 429) {
                    lastError = error
                    if (attempt < MAX_ATTEMPTS - 1) {
                        Thread.sleep(RETRY_DELAY_MS * (1L shl attempt))
                        return@repeat
                    }
                } else {
                    throw error
                }
            } catch (e: IOException) {
                throw ReceiptApiException("Error de red: ${e.message}", e)
            } finally {
                conn.disconnect()
            }
        }
        throw lastError ?: ReceiptApiException("HTTP 503: sin respuesta")
    }

    internal fun parseJson(content: String): ReceiptApiResult {
        val cleaned = content.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val block = jsonBlockRegex.find(cleaned)?.value
            ?: throw ReceiptApiException("Respuesta no es JSON válido: ${cleaned.take(200)}")
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
                val item = buildLineItem(name, quantity, unitPrice, price)
                if (item != null) items.add(item)
            }
        }
        return ReceiptApiResult(
            merchant = merchantFieldRegex.find(block)?.groupValues?.get(1)?.trim()?.ifBlank { null },
            address = addressFieldRegex.find(block)?.groupValues?.get(1)?.trim()?.ifBlank { null },
            date = dateFieldRegex.find(block)?.groupValues?.get(1)?.trim()?.ifBlank { null },
            total = totalRegex.find(block)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull(),
            lineItems = items,
        )
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

    internal fun normalizeDate(text: String?): String? {
        val raw = text?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val match = Regex("""^(\d{1,2})[/\-.](\d{1,2})[/\-.](\d{2,4})$""").find(raw) ?: return raw
        val (d, m, y) = match.destructured
        val day = d.padStart(2, '0')
        val month = m.padStart(2, '0')
        val year = if (y.length == 2) "20$y" else y
        return "$day/$month/$year"
    }
}
