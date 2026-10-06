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

/**
 * Proveedor genérico para que el usuario final apunte a CUALQUIER endpoint
 * compatible: su propio servidor, un proxy local, un LLM autoalojado, etc.
 *
 * El endpoint debe aceptar POST con {"text": "..."} y devolver el JSON de
 * ticket estándar de esta biblioteca (el mismo esquema del prompt).
 * El campo del texto y las cabeceras extra son configurables.
 */
class CustomHttpProvider : OcrProvider {

    override val id = "custom"
    override val displayName = "Personalizado (HTTP)"

    override val configSchema = listOf(
        ProviderField(
            id = "baseUrl",
            label = "URL del endpoint",
            required = true,
            placeholder = "https://mi-servidor.example.com/receipt",
        ),
        ProviderField(
            id = "textField",
            label = "Nombre del campo de texto",
            required = false,
            defaultValue = "text",
        ),
        ProviderField(
            id = "apiKey",
            label = "Clave de API / Bearer (opcional)",
            secret = true,
            required = false,
        ),
        ProviderField(
            id = "extraHeader",
            label = "Cabecera extra 'Nombre: Valor' (opcional)",
            required = false,
            placeholder = "X-Api-Version: 1",
        ),
    )

    override suspend fun extract(
        request: OcrRequest,
        config: Map<String, String>,
    ): OcrReceipt = withContext(Dispatchers.IO) {
        val baseUrl = config.required("baseUrl", id).trim()
        val textField = config["textField"]?.takeIf { it.isNotBlank() } ?: "text"
        val payload = "{${Json.string(textField)}: ${Json.string(request.rawText)}}"

        val headers = buildMap {
            config["apiKey"]?.takeIf { it.isNotBlank() }?.let {
                put("Authorization", "Bearer ${it.trim()}")
            }
            config["extraHeader"]?.takeIf { it.isNotBlank() }?.let { raw ->
                val idx = raw.indexOf(':')
                if (idx > 0) {
                    put(raw.substring(0, idx).trim(), raw.substring(idx + 1).trim())
                }
            }
        }
        val response = Http.postJson(baseUrl, payload, headers)
        ReceiptJsonParser.parse(response)
    }
}
