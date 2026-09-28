package com.misgastos.app.ui.screens.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.misgastos.app.R
import com.misgastos.app.data.entity.TransactionType
import com.misgastos.app.ocr.ReceiptSource
import com.misgastos.app.ui.components.CategoryDropdown
import com.misgastos.app.ui.components.formatMoney
import com.misgastos.app.util.Categories
import com.misgastos.app.viewmodel.EditableLineItem
import com.misgastos.app.viewmodel.EditableReceipt
import com.misgastos.app.viewmodel.MisGastosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewReceiptScreen(
    viewModel: MisGastosViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
) {
    val receiptState by viewModel.pendingReceipt.collectAsState()
    val receipt = receiptState ?: run { onBack(); return }

    BackHandler(onBack = onBack)

    var category by androidx.compose.runtime.remember(receipt.suggestedCategory) {
        androidx.compose.runtime.mutableStateOf(receipt.suggestedCategory ?: Categories.expenseCategories.first())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.review_title))
                        Text(
                            text = stringResource(
                                if (receipt.source == ReceiptSource.API) R.string.review_source_api
                                else R.string.review_source_local,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                    .data(receipt.imageUri)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )

            if (receipt.apiError != null) {
                Text(
                    text = stringResource(R.string.review_api_error, receipt.apiError ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            OutlinedTextField(
                value = receipt.merchant,
                onValueChange = { viewModel.updatePendingReceipt(receipt.copy(merchant = it)) },
                label = { Text(stringResource(R.string.review_merchant)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = receipt.description,
                onValueChange = { viewModel.updatePendingReceipt(receipt.copy(description = it)) },
                label = { Text(stringResource(R.string.review_address)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = receipt.dateText,
                onValueChange = { viewModel.updatePendingReceipt(receipt.copy(dateText = it)) },
                label = { Text(stringResource(R.string.review_date)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = if (receipt.total > 0.0) formatMoney(receipt.total) else "",
                onValueChange = { value ->
                    val parsed = value.filter { c -> c.isDigit() || (c == '.') || (c == ',') }
                        .replace(',', '.').toDoubleOrNull() ?: 0.0
                    viewModel.updatePendingReceipt(receipt.copy(total = parsed))
                },
                label = { Text(stringResource(R.string.field_amount)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            CategoryDropdown(
                selected = category,
                options = Categories.expenseCategories,
                onSelectedChange = { category = it },
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.review_line_items),
                style = MaterialTheme.typography.titleSmall,
            )

            if (receipt.lineItems.isEmpty()) {
                Text(
                    text = stringResource(R.string.review_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            receipt.lineItems.forEachIndexed { index, item ->
                LineItemRow(
                    item = item,
                    onChange = { newItem ->
                        val updated = receipt.lineItems.toMutableList().also { it[index] = newItem }
                        viewModel.updatePendingReceipt(receipt.copy(lineItems = updated))
                    },
                    onDelete = {
                        val updated = receipt.lineItems.toMutableList().also { it.removeAt(index) }
                        viewModel.updatePendingReceipt(receipt.copy(lineItems = updated))
                    },
                )
            }

            OutlinedButton(
                onClick = {
                    val updated = receipt.lineItems + EditableLineItem()
                    viewModel.updatePendingReceipt(receipt.copy(lineItems = updated))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text(stringResource(R.string.review_add_line_item))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = {
                        viewModel.savePendingReceipt(
                            type = TransactionType.EXPENSE,
                            category = category.stableValue,
                            receipt = receipt,
                        )
                        onSaved()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.action_save)) }
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LineItemRow(
    item: EditableLineItem,
    onChange: (EditableLineItem) -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = item.name,
            onValueChange = { onChange(item.copy(name = it)) },
            label = { Text(stringResource(R.string.field_line_name)) },
            singleLine = true,
            modifier = Modifier.weight(1f),
        )
        OutlinedTextField(
            value = if (item.price > 0.0) item.price.toString() else "",
            onValueChange = { value ->
                val parsed = value.filter { c -> c.isDigit() || (c == '.') || (c == ',') }
                    .replace(',', '.').toDoubleOrNull() ?: 0.0
                onChange(item.copy(price = parsed))
            },
            label = { Text(stringResource(R.string.field_line_price)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onDelete) {
            Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
        }
    }
}
