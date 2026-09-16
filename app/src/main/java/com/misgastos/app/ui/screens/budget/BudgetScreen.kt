package com.misgastos.app.ui.screens.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.misgastos.app.ui.components.CategoryDropdown
import com.misgastos.app.ui.components.formatMoney
import com.misgastos.app.ui.theme.Amber
import com.misgastos.app.ui.theme.Green
import com.misgastos.app.ui.theme.Red
import com.misgastos.app.util.Categories
import com.misgastos.app.viewmodel.BudgetStatus
import com.misgastos.app.viewmodel.MisGastosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(viewModel: MisGastosViewModel) {
    val statuses by viewModel.budgetStatus.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Presupuestos") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Filled.Add, contentDescription = "Nuevo presupuesto") },
                text = { Text("Nuevo") }
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
                text = "Límites mensuales por categoría",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (statuses.isEmpty()) {
                Text(
                    text = "No hay presupuestos definidos. Crea uno con el botón \"Nuevo\".",
                    modifier = Modifier.padding(top = 16.dp)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(statuses) { status ->
                        BudgetCard(
                            status = status,
                            onDelete = { viewModel.deleteBudget(status.budget) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddBudgetDialog(
            onDismiss = { showAdd = false },
            onConfirm = { category, limit ->
                viewModel.saveBudget(category, limit)
            }
        )
    }
}

@Composable
private fun BudgetCard(
    status: BudgetStatus,
    onDelete: () -> Unit
) {
    val color = when {
        status.overLimit -> Red
        status.progress >= 0.8f -> Amber
        else -> Green
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = status.budget.category,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onDelete) { Text("Eliminar") }
            }
            Text(
                text = "${formatMoney(status.spent)} de ${formatMoney(status.budget.monthlyLimit)}",
                color = color
            )
            LinearProgressIndicator(
                progress = { status.progress.coerceIn(0f, 1f) },
                color = color,
                modifier = Modifier.fillMaxWidth()
            )
            if (status.overLimit) {
                Text(
                    text = "Has superado el presupuesto",
                    color = Red,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "Restante: ${formatMoney(status.remaining)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddBudgetDialog(
    onDismiss: () -> Unit,
    onConfirm: (category: String, limit: Double) -> Unit
) {
    var category by remember { mutableStateOf(Categories.expenseCategories.first()) }
    var limitText by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nuevo presupuesto") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryDropdown(
                    selected = category,
                    options = Categories.expenseCategories,
                    onSelectedChange = { category = it },
                    label = "Categoría",
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = limitText,
                    onValueChange = {
                        limitText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                        error = false
                    },
                    label = { Text("Límite mensual") },
                    isError = error,
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val limit = limitText.replace(',', '.').toDoubleOrNull()
                if (limit == null || limit <= 0.0) {
                    error = true
                } else {
                    onConfirm(category, limit)
                    onDismiss()
                }
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
