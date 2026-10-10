package com.misgastos.app.ocr

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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

/**
 * Custom exceptions for API errors with specific HTTP status codes
 */
open class ReceiptApiException(
    message: String,
    cause: Throwable? = null,
    val statusCode: Int? = null
) : Exception(message, cause)

class ReceiptApiAuthenticationException(message: String, cause: Throwable? = null) : 
    ReceiptApiException(message, cause, 401)

class ReceiptApiRateLimitException(message: String, cause: Throwable? = null) : 
    ReceiptApiException(message, cause, 429)

class ReceiptApiInvalidModelException(message: String, cause: Throwable? = null) : 
    ReceiptApiException(message, cause, 400)

class ReceiptApiContextOverflowException(message: String, cause: Throwable? = null) : 
    ReceiptApiException(message, cause, 413)

class ReceiptApiNetworkException(message: String, cause: Throwable? = null) : 
    ReceiptApiException(message, cause, 0)

class ReceiptApiServerException(message: String, cause: Throwable? = null, statusCode: Int? = null) : 
    ReceiptApiException(message, cause, statusCode ?: 500)

object ReceiptApiClient {

    private const val TAG = "ReceiptApiClient"
    private const val MISTRAL_URL = "https://api.mistral.ai/v1/chat/completions"
    private const val MISTRAL_MODEL = "mistral-small-latest"
    private const val GEMINI_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    private const val GEMINI_MODEL = "gemini-2.0-flash"
    private const val TIMEOUT_MS = 30_000
    
    // Exponential backoff configuration
    private const val MAX_RETRIES = 3
    private const val INITIAL_BACKOFF_MS = 1000L
    private const val MAX_BACKOFF_MS = 30000L
    
    // Context length limits
    private const val MAX_CONTEXT_TOKENS = 32000
    private const val MAX_PROMPT_TOKENS = 8000
    
    // Valid models for each provider
    private val VALID_MISTRAL_MODELS = setOf(
        "mistral-tiny-latest",
        "mistral-small-latest", 
        "mistral-medium-latest",
        "mistral-large-latest",
        "codestral-latest"
    )
    private val VALID_GEMINI_MODELS = setOf(
        "gemini-2.0-flash",
        "gemini-2.0-pro",
        "gemini-1.5-flash-latest",
        "gemini-1.5-pro-latest"
    )

    private val prompt = """"
        Eres un extractor de datos de tickets de compra (recibos) españoles.
        Devuelve SOLO un objeto JSON valido, sin markdown ni explicaciones:
        {"merchant": string|null, "address": string|null, "date": string|null, "total": number|null,
         "lineItems": [{"name": string, "quantity": number, "unitPrice": number, "price": number}]}
        El texto proviene de un OCR: puede tener mayusculas/minusculas inconsistentes,
        abreviaturas (C/, AV., CTRA., NIF, TFNO.), comas o puntos como separador decimal
        y ruido. Se tolerante y normaliza.
        - merchant: nombre del comercio; corrige errores evidentes del OCR
          (p. ej. "nercadona" -> "MERCADONA, S.A."). Null si no aparece.
        - address: direccion del establecimiento tal como aparece en el ticket
          (p. ej. "CL GRAN CANARIA 6, 35626 ESQUINZO"). Null si no aparece.
        - date: fecha del ticket en formato dd/MM/yyyy (null si no aparece).
        - total: importe final pagado con IVA/IGIC incluido, numero con punto decimal.
        - lineItems: lineas de producto. Para cada una:
          - name: descripcion del producto, corrigiendo errores evidentes del OCR.
          - quantity: cantidad comprada (numero; por defecto 1 si el ticket no la indica).
          - unitPrice: precio por unidad con punto decimal.
          - price: importe total de la linea (quantity x unitPrice) con punto decimal.
          Lista vacia si el ticket no detalla productos.
        No inventes valores que no aparezcan en el texto.
    """.trimIndent()

    suspend fun extract(config: ReceiptApiConfig, rawText: String): ReceiptApiResult =
        withContext(Dispatchers.IO) {
            // Validate API key before making request
            if (config.provider != ReceiptApiProvider.LOCAL && config.apiKey.isBlank()) {
                throw ReceiptApiAuthenticationException(
                    "API key is required for ${config.provider} provider. Please configure your API key."
                )
            }
            
            // Validate text length to prevent context overflow
            if (rawText.length > MAX_CONTEXT_TOKENS * 4) {
                throw ReceiptApiContextOverflowException(
                    "Text is too long (${rawText.length} chars). Maximum allowed: ${MAX_CONTEXT_TOKENS * 4}. Please shorten the content."
                )
            }
            
            // Validate model if specified in config
            if (config.provider != ReceiptApiProvider.LOCAL) {
                val isValidModel = when (config.provider) {
                    ReceiptApiProvider.MISTRAL -> MISTRAL_MODEL in VALID_MISTRAL_MODELS
                    ReceiptApiProvider.GEMINI -> GEMINI_MODEL in VALID_GEMINI_MODELS
                    else -> true
                }
                if (!isValidModel) {
                    throw ReceiptApiInvalidModelException(
                        "Invalid model for ${config.provider}. Valid models: ${VALID_MISTRAL_MODELS.joinToString()}"
                    )
                }
            }
            
            when (config.provider) {
                ReceiptApiProvider.MISTRAL -> callMistralWithRetry(config.apiKey, rawText)
                ReceiptApiProvider.GEMINI -> callGeminiWithRetry(config.apiKey, rawText)
                ReceiptApiProvider.LOCAL ->
                    throw ReceiptApiException("Proveedor LOCAL: usar ReceiptParser")
            }
        }

    /**
     * Call Mistral API with exponential backoff retry logic
     */
    private suspend fun callMistralWithRetry(apiKey: String, rawText: String): ReceiptApiResult {
        var lastException: ReceiptApiException? = null
        var backoffMs = INITIAL_BACKOFF_MS
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                return callMistral(apiKey, rawText)
            } catch (e: ReceiptApiRateLimitException) {
                lastException = e
                Log.w(TAG, "Rate limit hit (attempt ${attempt + 1}/$MAX_RETRIES). Retrying in ${backoffMs}ms...")
                delay(backoffMs)
                backoffMs = minOf(backoffMs * 2, MAX_BACKOFF_MS)
            } catch (e: ReceiptApiAuthenticationException) {
                throw e
            } catch (e: ReceiptApiException) {
                if (e.statusCode in listOf(500, 502, 503, 504)) {
                    lastException = e
                    Log.w(TAG, "Transient error (${e.statusCode}) on attempt ${attempt + 1}/$MAX_RETRIES. Retrying in ${backoffMs}ms...")
                    delay(backoffMs)
                    backoffMs = minOf(backoffMs * 2, MAX_BACKOFF_MS)
                } else {
                    throw e
                }
            }
        }
        
        throw lastException ?: ReceiptApiException("Failed after $MAX_RETRIES attempts")
    }

    /**
     * Call Gemini API with exponential backoff retry logic
     */
    private suspend fun callGeminiWithRetry(apiKey: String, rawText: String): ReceiptApiResult {
        var lastException: ReceiptApiException? = null
        var backoffMs = INITIAL_BACKOFF_MS
        
        repeat(MAX_RETRIES) { attempt ->
            try {
                return callGemini(apiKey, rawText)
            } catch (e: ReceiptApiRateLimitException) {
                lastException = e
                Log.w(TAG, "Rate limit hit (attempt ${attempt + 1}/$MAX_RETRIES). Retrying in ${backoffMs}ms...")
                delay(backoffMs)
                backoffMs = minOf(backoffMs * 2, MAX_BACKOFF_MS)
            } catch (e: ReceiptApiAuthenticationException) {
                throw e
            } catch (e: ReceiptApiException) {
                if (e.statusCode in listOf(500, 502, 503, 504)) {
                    lastException = e
                    Log.w(TAG, "Transient error (${e.statusCode}) on attempt ${attempt + 1}/$MAX_RETRIES. Retrying in ${backoffMs}ms...")
                    delay(backoffMs)
                    backoffMs = minOf(backoffMs * 2, MAX_BACKOFF_MS)
                } else {
                    throw e
                }
            }
        }
        
        throw lastException ?: ReceiptApiException("Failed after $MAX_RETRIES attempts")
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
            
            when (code) {
                401 -> throw ReceiptApiAuthenticationException(
                    "HTTP 401 Unauthorized: Invalid or expired API key. Check MISTRAL_API_KEY"
                )
                400 -> throw ReceiptApiInvalidModelException(
                    "HTTP 400 Bad Request: Invalid model or request format. Model: $MISTRAL_MODEL"
                )
                429 -> throw ReceiptApiRateLimitException(
                    "HTTP 429 Too Many Requests: Rate limit exceeded. Please wait and retry."
                )
                413 -> throw ReceiptApiContextOverflowException(
                    "HTTP 413 Payload Too Large: Context length exceeded. Text length: ${payload.length}"
                )
                in 400..499 -> throw ReceiptApiException(
                    "HTTP $code Client Error: ${text.take(300)}",
                    statusCode = code
                )
                in 500..599 -> throw ReceiptApiException(
                    "HTTP $code Server Error: ${text.take(300)}",
                    statusCode = code
                )
                !in 200..299 -> throw ReceiptApiException(
                    "HTTP $code: ${text.take(300)}",
                    statusCode = code
                )
            }
            return text
        } catch (e: IOException) {
            throw ReceiptApiNetworkException("Network error: ${e.message}. Please check your internet connection.", e)
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
            
            when (code) {
                401 -> throw ReceiptApiAuthenticationException(
                    "HTTP 401 Unauthorized: Invalid or expired API key. Check GEMINI_API_KEY"
                )
                400 -> throw ReceiptApiInvalidModelException(
                    "HTTP 400 Bad Request: Invalid model or request format. Model: $GEMINI_MODEL"
                )
                429 -> throw ReceiptApiRateLimitException(
                    "HTTP 429 Too Many Requests: Rate limit exceeded. Please wait and retry."
                )
                413 -> throw ReceiptApiContextOverflowException(
                    "HTTP 413 Payload Too Large: Context length exceeded. Text length: ${payload.length}"
                )
                in 400..499 -> throw ReceiptApiException(
                    "HTTP $code Client Error: ${text.take(300)}",
                    statusCode = code
                )
                in 500..599 -> throw ReceiptApiException(
                    "HTTP $code Server Error: ${text.take(300)}",
                    statusCode = code
                )
                !in 200..299 -> throw ReceiptApiException(
                    "HTTP $code: ${text.take(300)}",
                    statusCode = code
                )
            }
            return text
        } catch (e: IOException) {
            throw ReceiptApiNetworkException("Network error: ${e.message}. Please check your internet connection.", e)
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
            ?: throw ReceiptApiException("Invalid JSON response: ${cleaned.take(200)}")
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
        val match = Regex("""^(\d{1,2})[/\\-.](\d{1,2})[/\\-.](\d{2,4})$""").find(raw) ?: return raw
        val (d, m, y) = match.destructured
        val day = d.padStart(2, '0')
        val month = m.padStart(2, '0')
        val year = if (y.length == 2) "20$y" else y
        return "$day/$month/$year"
    }
}
