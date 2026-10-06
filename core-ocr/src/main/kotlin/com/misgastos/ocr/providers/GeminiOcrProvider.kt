package com.misgastos.ocr.providers

import com.misgastos.ocr.api.OcrProvider
import com.misgastos.ocr.api.OcrReceipt
import com.misgastos.ocr.api.OcrException
import com.misgastos.ocr.api.OcrRequest
import com.misgastos.ocr.api.ProviderField
import com.misgastos.ocr.api.required
import com.misgastos.ocr.http.Http
import com.misgastos.ocr.http.Json
import com.misgastos.ocr.json.ReceiptJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiOcrProvider : OcrProvider {

    override val id = "gemini"
    override val displayName = "Google Gemini"
    override val configSchema = listOf(
        ProviderField(
            id = "apiKey",
            label = "Clave de API de Google AI Studio",
            secret = true,
            placeholder = "https://aistudio.google.com/apikey",
        ),
        ProviderField(
            id = "model",
            label = "Modelo",
            required = false,
            defaultValue = "gemini-2.0-flash",
        ),
    )

    override suspend fun extract(
        request: OcrRequest,
        config: Map<String, String>,
    ): OcrReceipt = withContext(Dispatchers.IO) {
        val apiKey = config.required("apiKey", id)
        val model = config["model"]?.takeIf { it.isNotBlank() } ?: "gemini-2.0-flash"
        val text = ExtractionPrompt.text + "\n\nTicket:\n" + request.rawText
        val body = """
            {"contents": [{"parts": [{"text": ${Json.string(text)}}]}],
             "generationConfig": {"temperature": 0, "responseMimeType": "application/json"}}
        """.trimIndent()
        val response = Http.postJson(
            url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent",
            payload = body,
            headers = mapOf("x-goog-api-key" to apiKey),
        )
        ReceiptJsonParser.parse(extractContent(response))
    }

    private fun extractContent(response: String): String {
        val regex = Regex("""\"text\"\s*:\s*\"((?:[^"\\]|\\.)*)\"""")
        return regex.find(response)?.groupValues?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
            ?: throw OcrException(
                "Respuesta de Gemini sin contenido: ${response.take(200)}",
            )
    }
}
