package com.misgastos.app.ui.screens.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.misgastos.app.R
import com.misgastos.app.data.entity.Transaction
import com.misgastos.app.data.entity.TransactionType
import com.misgastos.app.ui.components.AddTransactionDialog
import com.misgastos.app.ui.components.TransactionRow
import com.misgastos.app.ui.components.formatMoney
import com.misgastos.app.ui.theme.Green
import com.misgastos.app.ui.theme.Red
import com.misgastos.app.util.Categories
import com.misgastos.app.viewmodel.MisGastosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: MisGastosViewModel,
    onScanClick: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onSettingsClick: () -> Unit = {},
) {
    val dashboard by viewModel.dashboard.collectAsState()
    val recent by viewModel.recentTransactions.collectAsState()
    var showAdd by remember { mutableStateOf(value = false) }
    var addType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var exportMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        if (uri != null) viewModel.exportToUri(uri, MisGastosViewModel.ExportFormat.CSV) { ok ->
            viewModel.showExportResult(context, ok, uri, MisGastosViewModel.ExportFormat.CSV)
        }
    }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null) viewModel.exportToUri(uri, MisGastosViewModel.ExportFormat.JSON) { ok ->
            viewModel.showExportResult(context, ok, uri, MisGastosViewModel.ExportFormat.JSON)
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dashboard_title)) },
                actions = {
                    IconButton(onClick = { exportMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.action_export))
                    }
                    DropdownMenu(
                        expanded = exportMenu,
                        onDismissRequest = { exportMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_export_csv)) },
                            onClick = {
                                exportMenu = false
                                csvLauncher.launch("misgastos.csv")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_export_json)) },
                            onClick = {
                                exportMenu = false
                                jsonLauncher.launch("misgastos.json")
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings_ocr_title)) },
                            onClick = {
                                exportMenu = false
                                onSettingsClick()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            var menuExpanded by remember { mutableStateOf(false) }
            Column(horizontalAlignment = Alignment.End) {
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_manual)) },
                        onClick = {
                            menuExpanded = false
                            addType = TransactionType.EXPENSE
                            showAdd = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_scan)) },
                        onClick = {
                            menuExpanded = false
                            onScanClick()
                        },
                    )
                }
                ExtendedFloatingActionButton(
                    onClick = { menuExpanded = true },
                    icon = { Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_add)) },
                    text = { Text(stringResource(R.string.action_add)) },
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = dashboard.monthLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            BalanceCard(dashboard.totalBalance)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryCard(
                    title = stringResource(R.string.dashboard_month_income),
                    value = dashboard.monthIncome,
                    color = Green,
                    icon = Icons.Filled.ArrowUpward,
                    modifier = Modifier.weight(1f),
                )
                SummaryCard(
                    title = stringResource(R.string.dashboard_month_expenses),
                    value = dashboard.monthExpenses,
                    color = Red,
                    icon = Icons.Filled.ArrowDownward,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                text = stringResource(R.string.dashboard_month_balance, formatMoney(dashboard.monthBalance)),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(R.string.dashboard_recent),
                style = MaterialTheme.typography.titleMedium,
            )
            RecentTransactionsList(
                transactions = recent,
                onDelete = viewModel::deleteTransaction,
                onTransactionClick = onTransactionClick,
            )
        }
    }
    if (showAdd) {
        AddTransactionDialog(
            type = addType,
            categories = if (addType == TransactionType.EXPENSE) {
                Categories.expenseCategories
            } else {
                Categories.incomeCategories
            },
            onDismiss = { showAdd = false },
        ) { amount, category, description ->
            viewModel.addTransaction(
                addType,
                amount,
                category,
                description,
                System.currentTimeMillis(),
            )
        }
    }
}

@Composable
private fun BalanceCard(balance: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.dashboard_total_balance),
                color = Color.White,
                fontSize = 14.sp,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatMoney(balance),
                color = Color.White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: Double,
    color: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .padding(0.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(text = title, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = formatMoney(value),
                fontWeight = FontWeight.Bold,
                color = color,
                fontSize = 16.sp,
            )
        }
    }
}

@Composable
private fun RecentTransactionsList(
    transactions: List<Transaction>,
    onDelete: (Transaction) -> Unit,
    onTransactionClick: (Long) -> Unit = {},
) {
    if (transactions.isEmpty()) {
        Text(
            text = stringResource(R.string.dashboard_empty),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(transactions.take(20)) { transaction ->
            TransactionRow(
                transaction = transaction,
                onDelete = onDelete,
                onClick = { onTransactionClick(transaction.id) },
            )
        }
    }
}
