package com.misgastos.app.ocr

data class ParsedReceipt(
    val total: Double? = null,
    val date: String? = null,
    val merchant: String? = null,
    val lineItems: List<ParsedLineItem> = emptyList(),
    val rawText: String = "",
)

data class ParsedLineItem(
    val name: String,
    val price: Double,
    val quantity: Double = 1.0,
)
