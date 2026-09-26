package com.misgastos.app.ocr

enum class ReceiptSource { LOCAL, API }

data class ParsedReceipt(
    val total: Double? = null,
    val date: String? = null,
    val merchant: String? = null,
    val address: String? = null,
    val lineItems: List<ParsedLineItem> = emptyList(),
    val rawText: String = "",
    val source: ReceiptSource = ReceiptSource.LOCAL,
)

data class ParsedLineItem(
    val name: String,
    val price: Double,
    val quantity: Double = 1.0,
    val unitPrice: Double? = null,
)
