package com.misgastos.ocr.providers

import com.misgastos.ocr.api.OcrProvider
import com.misgastos.ocr.api.OcrReceipt
import com.misgastos.ocr.api.ProviderField
import com.misgastos.ocr.api.OcrRequest
import com.misgastos.ocr.parser.ReceiptParser

/**
 * Proveedor local: extracción heurística sin red ni coste.
 * Es el proveedor por defecto y no requiere configuración.
 */
class LocalReceiptProvider : OcrProvider {

    override val id = "local"
    override val displayName = "Local (sin API)"
    override val configSchema: List<ProviderField> = emptyList()

    override suspend fun extract(
        request: OcrRequest,
        config: Map<String, String>,
    ): OcrReceipt = ReceiptParser.parse(request.rawText, request.template)
}
