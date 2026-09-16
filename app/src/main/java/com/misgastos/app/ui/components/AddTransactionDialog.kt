package com.misgastos.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.misgastos.app.data.entity.TransactionType

@Composable
fun AddTransactionDialog(
    type: TransactionType,
    categories: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (amount: Double, category: String, description: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(categories.firstOrNull() ?: "") }
    var description by remember { mutableStateOf("") }
    var amountError by remember { mutableStateOf(false) }

    val title = if (type == TransactionType.EXPENSE) "Nuevo gasto" else "Nuevo ingreso"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                        amountError = false
                    },
                    label = { Text("Importe") },
                    isError = amountError,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                if (categories.isNotEmpty()) {
                    CategoryDropdown(
                        selected = category,
                        options = categories,
                        onSelectedChange = { category = it },
                        label = "Categoría"
                    )
                } else {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Categoría") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val amount = amountText.replace(',', '.').toDoubleOrNull()
                if (amount == null || amount <= 0.0) {
                    amountError = true
                } else if (category.isBlank()) {
                    amountError = false
                } else {
                    onConfirm(amount, category.trim(), description.trim())
                    onDismiss()
                }
            }) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
