package com.misgastos.app.ui.screens.prices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.misgastos.app.R
import com.misgastos.app.ui.components.formatMoney
import com.misgastos.app.util.DateUtils
import com.misgastos.app.util.PriceAnalyzer
import com.misgastos.app.viewmodel.MisGastosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriceComparisonScreen(
    viewModel: MisGastosViewModel,
    onBack: () -> Unit = {},
) {
    val rows by viewModel.productPrices.collectAsState()
    val query by viewModel.priceQuery.collectAsState()
    val comparisons by viewModel.priceComparisons.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.prices_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_cancel))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setPriceQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.prices_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setPriceQuery("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.action_clear_filters))
                        }
                    }
                },
                singleLine = true,
            )

            when {
                rows.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.prices_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                query.isBlank() -> {
                    val suggestions = PriceAnalyzer.productNames(rows).take(10)
                    if (suggestions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.prices_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.prices_search_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(suggestions) { name ->
                                Card(onClick = { viewModel.setPriceQuery(name) }) {
                                    Text(
                                        text = name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                comparisons.isEmpty() -> {
                    Text(
                        text = stringResource(R.string.prices_no_matches, query),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(comparisons) { comparison ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = comparison.cheapest?.merchant ?: "",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = formatMoney(comparison.cheapest?.unitPrice ?: 0.0),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                    Text(
                                        text = stringResource(
                                            R.string.prices_latest_purchase,
                                            DateUtils.formatDate(comparison.cheapest?.date ?: 0L),
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.prices_count_prices,
                                            comparison.unitPrices.size,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (comparison.savingsPerUnit > 0.0) {
                                        Text(
                                            text = stringResource(
                                                R.string.prices_savings_unit,
                                                formatMoney(comparison.savingsPerUnit),
                                            ),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
