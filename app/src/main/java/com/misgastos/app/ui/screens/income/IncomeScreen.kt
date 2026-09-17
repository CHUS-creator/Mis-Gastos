package com.misgastos.app.ui.screens.income

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.misgastos.app.R
import com.misgastos.app.data.entity.TransactionType
import com.misgastos.app.ui.components.AddTransactionDialog
import com.misgastos.app.ui.components.SearchFilterBar
import com.misgastos.app.ui.components.TransactionRow
import com.misgastos.app.ui.components.formatMoney
import com.misgastos.app.ui.theme.Green
import com.misgastos.app.util.Categories
import com.misgastos.app.util.CategoryKey
import com.misgastos.app.util.categoryLabel
import com.misgastos.app.viewmodel.MisGastosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeScreen(
    viewModel: MisGastosViewModel,
    onTransactionClick: (Long) -> Unit = {},
) {
    val incomes by viewModel.incomes.collectAsState()
    val dashboard by viewModel.dashboard.collectAsState()
    var showAdd by remember { mutableStateOf(value = false) }
    var query by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<CategoryKey?>(null) }
    val filtered = remember(incomes, query, categoryFilter) {
        incomes.filter { tx ->
            val matchesQuery = query.isBlank() ||
                tx.merchant.contains(query, ignoreCase = true) ||
                tx.description.contains(query, ignoreCase = true) ||
                categoryLabel(tx.category).contains(query, ignoreCase = true)
            val matchesCategory = categoryFilter == null || tx.category == categoryFilter?.stableValue
            matchesQuery && matchesCategory
        }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.income_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add_income)) },
                text = { Text(stringResource(R.string.nav_income)) },
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
            Text(
                text = stringResource(R.string.income_total_month),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatMoney(dashboard.monthIncome),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Green,
            )
            SearchFilterBar(
                query = query,
                onQueryChange = { query = it },
                selectedCategory = categoryFilter,
                onCategoryChange = { categoryFilter = it },
                categories = Categories.incomeCategories,
            )
            if (filtered.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    modifier = Modifier.padding(top = 16.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(filtered) { income ->
                        TransactionRow(
                            transaction = income,
                            onDelete = viewModel::deleteTransaction,
                            onClick = { onTransactionClick(income.id) },
                        )
                    }
                }
            }
        }
    }
    if (showAdd) {
        AddTransactionDialog(
            type = TransactionType.INCOME,
            categories = Categories.incomeCategories,
            onDismiss = { showAdd = false },
        ) { amount, category, description ->
            viewModel.addTransaction(
                TransactionType.INCOME,
                amount,
                category,
                description,
                System.currentTimeMillis(),
            )
        }
    }
}
