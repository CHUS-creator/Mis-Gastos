package com.misgastos.ocr.providers

import com.misgastos.ocr.api.OcrProvider
import com.misgastos.ocr.api.OcrReceipt
import com.misgastos.ocr.api.OcrRequest
import com.misgastos.ocr.api.ProviderField
import com.misgastos.ocr.api.required
import com.misgastos.ocr.http.Http
import com.misgastos.ocr.http.Json
import com.misgastos.ocr.json.ReceiptJsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MistralOcrProvider : OcrProvider {

    override val id = "mistral"
    override val displayName = "Mistral AI"
    override val configSchema = listOf(
        ProviderField(
            id = "apiKey",
            label = "Clave de API de Mistral",
            secret = true,
            placeholder = "https://console.mistral.ai/api-keys",
        ),
        ProviderField(
            id = "model",
            label = "Modelo",
            required = false,
            defaultValue = "mistral-small-latest",
        ),
    )

    override suspend fun extract(
        request: OcrRequest,
        config: Map<String, String>,
    ): OcrReceipt = withContext(Dispatchers.IO) {
        val apiKey = config.required("apiKey", id)
        val model = config["model"]?.takeIf { it.isNotBlank() } ?: "mistral-small-latest"
        val body = """
            {"model": ${Json.string(model)},
             "messages": [
               {"role": "system", "content": ${Json.string(ExtractionPrompt.text)}},
               {"role": "user", "content": ${Json.string(request.rawText)}}
             ],
             "temperature": 0,
             "response_format": {"type": "json_object"}}
        """.trimIndent()
        val response = Http.postJson(
            url = "https://api.mistral.ai/v1/chat/completions",
            payload = body,
            headers = mapOf("Authorization" to "Bearer $apiKey"),
        )
        ReceiptJsonParser.parse(extractContent(response))
    }

    /** Extrae choices[0].message.content con el mismo parser tolerante a regex. */
    private fun extractContent(response: String): String {
        val regex = Regex("""\"content\"\s*:\s*\"((?:[^"\\]|\\.)*)\"""")
        return regex.find(response)?.groupValues?.get(1)
            ?.replace("\\n", "\n")
            ?.replace("\\\"", "\"")
            ?.replace("\\\\", "\\")
            ?: throw com.misgastos.ocr.api.OcrException(
                "Respuesta de Mistral sin contenido: ${response.take(200)}",
            )
    }
}
