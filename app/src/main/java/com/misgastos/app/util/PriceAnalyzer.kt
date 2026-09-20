package com.misgastos.app.util

import com.misgastos.app.data.dao.ProductPriceRow

data class MerchantPrice(
    val merchant: String,
    val unitPrice: Double,
    val date: Long,
)

data class ProductComparison(
    val displayName: String,
    val unitPrices: List<MerchantPrice>,
    val cheapest: MerchantPrice?,
    val priciest: MerchantPrice?,
    val savingsPerUnit: Double,
)

object PriceAnalyzer {

    fun normalizeName(name: String): String {
        return name.lowercase()
            .replace(Regex("\\s+"), " ")
            .trim()
            .trimEnd('.', ';', ',')
            .replace(Regex("^\\d+\\s*[xX]?\\s*"), "")
            .trim()
    }

    fun unitPriceOf(row: ProductPriceRow): Double? {
        if (row.quantity <= 0.0) return null
        val unitPrice = row.price / row.quantity
        return unitPrice.takeIf { it > 0.0 }
    }

    fun compare(
        rows: List<ProductPriceRow>,
        query: String,
    ): List<ProductComparison> {
        if (query.isBlank()) return emptyList()
        val normalizedQuery = normalizeName(query)
        val matches = rows
            .mapNotNull { row ->
                unitPriceOf(row)?.let { unitPrice ->
                    val merchant = row.merchant.ifBlank { NO_MERCHANT }
                    row to MerchantPrice(merchant, unitPrice, row.date)
                }
            }
            .filter { (row, _) -> normalizeName(row.name).contains(normalizedQuery) }

        val byMerchant = matches
            .groupBy { (_, price) -> price.merchant }
            .map { (_, entries) ->
                ProductComparison(
                    displayName = query.trim(),
                    unitPrices = entries.map { it.second }.sortedByDescending { it.date },
                    cheapest = entries.minByOrNull { it.second.unitPrice }?.second,
                    priciest = entries.maxByOrNull { it.second.unitPrice }?.second,
                    savingsPerUnit = 0.0,
                )
            }
            .sortedBy { it.cheapest?.unitPrice ?: Double.MAX_VALUE }

        if (byMerchant.size < 2) return byMerchant

        val globalMin = byMerchant.minOf { it.cheapest?.unitPrice ?: Double.MAX_VALUE }
        return byMerchant.map { cmp ->
            val current = cmp.cheapest?.unitPrice ?: Double.MAX_VALUE
            cmp.copy(savingsPerUnit = current - globalMin)
        }
    }

    fun productNames(rows: List<ProductPriceRow>): List<String> =
        rows.map { it.name }.filter { it.isNotBlank() }.distinct()

    const val NO_MERCHANT = "Sin comercio"
}
