package com.misgastos.ocr.providers

/** Prompt de extracción compartido por los proveedores basados en LLM. */
object ExtractionPrompt {
    val text = """
        Eres un extractor de datos de tickets de compra (recibos) españoles.
        Devuelve SOLO un objeto JSON válido, sin markdown ni explicaciones:
        {"merchant": string|null, "address": string|null, "date": string|null, "total": number|null,
         "lineItems": [{"name": string, "quantity": number, "unitPrice": number, "price": number}]}
        El texto proviene de un OCR: puede tener mayúsculas/minúsculas inconsistentes,
        abreviaturas (C/, AV., CTRA., NIF, TFNO.), comas o puntos como separador decimal
        y ruido. Sé tolerante y normaliza.
        - merchant: nombre del comercio; corrige errores evidentes del OCR
          (p. ej. "nercadona" -> "MERCADONA, S.A."). Null si no aparece.
        - address: dirección del establecimiento tal como aparece en el ticket. Null si no aparece.
        - date: fecha del ticket en formato dd/MM/yyyy (null si no aparece).
        - total: importe final pagado con IVA/IGIC incluido, número con punto decimal.
        - lineItems: líneas de producto. Para cada una:
          - name: descripción del producto, corrigiendo errores evidentes del OCR.
          - quantity: cantidad comprada (número; por defecto 1 si el ticket no la indica).
          - unitPrice: precio por unidad con punto decimal.
          - price: importe total de la línea (quantity x unitPrice) con punto decimal.
          Lista vacía si el ticket no detalla productos.
        No inventes valores que no aparezcan en el texto.
    """.trimIndent()
}
