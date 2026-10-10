package com.misgastos.ocr.api

/** Campo de configuración que un proveedor puede pedir al usuario. */
data class ProviderField(
    val id: String,               // clave en el mapa de config, p. ej. "apiKey"
    val label: String,            // texto visible en la UI, p. ej. "Clave de API"
    val secret: Boolean = false,  // si true, la UI debe enmascararlo y almacenarlo cifrado
    val required: Boolean = true,
    val placeholder: String = "",
    val defaultValue: String = "",
)

/** Plantilla opcional que afina al parser local para un comercio conocido. */
data class ReceiptTemplate(
    val totalKeyword: String? = null,
    val dateFormat: String? = null,
)

/** Entrada al pipeline: texto OCR ya reconocido (ML Kit o equivalente). */
data class OcrRequest(
    val rawText: String,
    val template: ReceiptTemplate? = null,
)

/** Resultado normalizado, independiente del proveedor. */
data class OcrReceipt(
    val merchant: String? = null,
    val address: String? = null,
    val date: String? = null,
    val total: Double? = null,
    val lineItems: List<ParsedLineItem> = emptyList(),
)

data class ParsedLineItem(
    val name: String,
    val price: Double,
    val quantity: Double = 1.0,
    val unitPrice: Double? = null,
)

class OcrException(message: String, cause: Throwable? = null) : Exception(message, cause)
