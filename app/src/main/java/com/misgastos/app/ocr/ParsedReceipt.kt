package com.misgastos.app.ocr

import com.misgastos.ocr.api.OcrReceipt
import com.misgastos.ocr.api.ParsedLineItem as LibParsedLineItem

enum class ReceiptSource { LOCAL, API }

/** Plantilla de comercio: alias del tipo de la biblioteca :core-ocr. */
typealias ReceiptTemplate = com.misgastos.ocr.api.ReceiptTemplate

/**
 * Modelo de la app: resultado del pipeline OCR con metadatos de UI
 * (texto crudo, origen y error de API para el fallback local).
 */
data class ParsedReceipt(
    val total: Double? = null,
    val date: String? = null,
    val merchant: String? = null,
    val address: String? = null,
    val lineItems: List<ParsedLineItem> = emptyList(),
    val rawText: String = "",
    val source: ReceiptSource = ReceiptSource.LOCAL,
    val apiError: String? = null,
)

data class ParsedLineItem(
    val name: String,
    val price: Double,
    val quantity: Double = 1.0,
    val unitPrice: Double? = null,
)

fun OcrReceipt.toParsedReceipt(
    rawText: String,
    source: ReceiptSource = ReceiptSource.LOCAL,
    apiError: String? = null,
): ParsedReceipt = ParsedReceipt(
    total = total,
    date = date,
    merchant = merchant,
    address = address,
    lineItems = lineItems.map { it.toAppLineItem() },
    rawText = rawText,
    source = source,
    apiError = apiError,
)

fun LibParsedLineItem.toAppLineItem(): ParsedLineItem = ParsedLineItem(
    name = name,
    price = price,
    quantity = quantity,
    unitPrice = unitPrice,
)
