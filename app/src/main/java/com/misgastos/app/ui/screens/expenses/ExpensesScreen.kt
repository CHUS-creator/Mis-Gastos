package com.misgastos.app.ui.screens.expenses

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.misgastos.app.data.entity.TransactionType
import com.misgastos.app.ui.components.AddTransactionDialog
import com.misgastos.app.ui.components.TransactionRow
import com.misgastos.app.ui.components.formatMoney
import com.misgastos.app.util.Categories
import com.misgastos.app.viewmodel.MisGastosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(viewModel: MisGastosViewModel) {
    val expenses by viewModel.expenses.collectAsState()
    val dashboard by viewModel.dashboard.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gastos") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Añadir gasto") },
                text = { Text("Gasto") }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Total gastado este mes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatMoney(dashboard.monthExpenses),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            if (expenses.isEmpty()) {
                Text(
                    text = "No hay gastos registrados todavía.",
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(expenses) { expense ->
                        TransactionRow(
                            transaction = expense,
                            onDelete = viewModel::deleteTransaction
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddTransactionDialog(
            type = TransactionType.EXPENSE,
            categories = Categories.expenseCategories,
            onDismiss = { showAdd = false },
            onConfirm = { amount, category, description ->
                viewModel.addTransaction(
                    TransactionType.EXPENSE,
                    amount,
                    category,
                    description,
                    System.currentTimeMillis()
                )
            }
        )
    }
}
