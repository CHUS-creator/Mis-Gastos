package com.misgastos.app.ocr

import com.misgastos.ocr.api.OcrProviderRegistry
import com.misgastos.ocr.api.OcrRequest
import com.misgastos.ocr.parser.ReceiptParser

/**
 * Orquestador del pipeline OCR de la app sobre la biblioteca :core-ocr.
 * Si el proveedor configurado falla, cae al parser local conservando
 * el error para mostrarlo en la revisión del ticket.
 */
object OcrService {

    private val registry: OcrProviderRegistry by lazy { OcrProviderRegistry.withBundledProviders() }

    fun providers() = registry.all()

    fun provider(id: String) = registry.get(id)

    suspend fun extract(
        settings: OcrSettings,
        rawText: String,
        template: ReceiptTemplate? = null,
        fallbackError: String? = null,
    ): ParsedReceipt {
        val provider = registry.get(settings.providerId)
        if (provider != null && settings.config.isNotEmpty()) {
            val outcome = runCatching {
                provider.extract(OcrRequest(rawText = rawText, template = template), settings.config)
            }
            val result = outcome.getOrNull()
            if (result != null) {
                return result.toParsedReceipt(rawText = rawText, source = ReceiptSource.API)
            }
            val local = ReceiptParser.parse(rawText, template)
            return local.toParsedReceipt(
                rawText = rawText,
                source = ReceiptSource.LOCAL,
                apiError = outcome.exceptionOrNull()?.message ?: fallbackError,
            )
        }
        return ReceiptParser.parse(rawText, template)
            .toParsedReceipt(rawText = rawText)
    }
}
