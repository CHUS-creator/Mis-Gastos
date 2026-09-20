package com.misgastos.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.misgastos.app.R
import com.misgastos.app.data.dao.CategoryTotal
import com.misgastos.app.data.entity.Budget
import com.misgastos.app.data.entity.EntrySource
import com.misgastos.app.data.entity.Transaction
import com.misgastos.app.data.entity.TransactionType
import com.misgastos.app.data.entity.LineItem
import com.misgastos.app.data.repository.MisGastosRepository
import com.misgastos.app.ocr.OcrRecognizer
import com.misgastos.app.ocr.ReceiptTemplate
import com.misgastos.app.util.CategoryKey
import com.misgastos.app.util.DataExporter
import com.misgastos.app.util.DataFormat
import com.misgastos.app.util.DataParser
import com.misgastos.app.ocr.ParsedLineItem
import com.misgastos.app.ocr.ParsedReceipt
import com.misgastos.app.ocr.ReceiptParser
import com.misgastos.app.util.DateUtils
import com.misgastos.app.util.categoryLabel
import android.net.Uri
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardState(
    val totalBalance: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpenses: Double = 0.0,
    val monthBalance: Double = 0.0,
    val monthLabel: String = DateUtils.formatMonth(System.currentTimeMillis()),
)

data class BudgetStatus(
    val budget: Budget,
    val spent: Double,
    val remaining: Double,
    val progress: Float,
    val overLimit: Boolean,
)

class MisGastosViewModel(
    private val context: Context,
    private val repository: MisGastosRepository,
) : ViewModel() {

    private val nowFlow = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(REFRESH_INTERVAL_MS)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val dashboard: StateFlow<DashboardState> = nowFlow.flatMapLatest { now ->
        combine(
            repository.totalBalance(),
            repository.monthIncomes(now),
            repository.monthExpenses(now),
            repository.monthBalance(now),
        ) { total, inc, exp, balance ->
            DashboardState(
                totalBalance = total,
                monthIncome = inc,
                monthExpenses = exp,
                monthBalance = balance,
                monthLabel = DateUtils.formatMonth(now),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardState())

    val recentTransactions: StateFlow<List<Transaction>> =
        repository.getAllTransactions().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList(),
        )

    val expenses: StateFlow<List<Transaction>> =
        repository.getExpenses().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList(),
        )

    val incomes: StateFlow<List<Transaction>> =
        repository.getIncomes().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList(),
        )

    val budgets: StateFlow<List<Budget>> =
        repository.getAllBudgets().stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList(),
        )

    @OptIn(ExperimentalCoroutinesApi::class)
    val monthExpensesByCategory: StateFlow<List<CategoryTotal>> =
        nowFlow.flatMapLatest { now ->
            repository.monthExpensesByCategory(now)
        }.stateIn(
            viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList(),
        )

    val budgetStatus: StateFlow<List<BudgetStatus>> =
        combine(budgets, monthExpensesByCategory) { allBudgets, categoryTotals ->
            allBudgets.map { budget ->
                val spent = categoryTotals.firstOrNull { it.category == budget.category }?.total ?: 0.0
                val remaining = budget.monthlyLimit - spent
                val progress = if (budget.monthlyLimit > 0) {
                    (spent / budget.monthlyLimit).toFloat()
                } else 0f
                BudgetStatus(
                    budget = budget,
                    spent = spent,
                    remaining = remaining,
                    progress = progress,
                    overLimit = spent > budget.monthlyLimit,
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _snackbar = MutableStateFlow<String?>(null)
    val snackbar: StateFlow<String?> = _snackbar.asStateFlow()

    private val ocrRecognizer = OcrRecognizer()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Idle)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()

    private val _pendingReceipt = MutableStateFlow<EditableReceipt?>(null)
    val pendingReceipt: StateFlow<EditableReceipt?> = _pendingReceipt.asStateFlow()

    private val _detailState = MutableStateFlow<TransactionDetail?>(null)
    val detailState: StateFlow<TransactionDetail?> = _detailState.asStateFlow()

    fun addTransaction(
        type: TransactionType,
        amount: Double,
        category: String,
        description: String,
        date: Long,
    ) {
        viewModelScope.launch {
            repository.addTransaction(
                Transaction(
                    type = type,
                    amount = amount,
                    category = category,
                    description = description,
                    date = date,
                ),
            )
            checkBudgetAlerts(category)
        }
    }

    fun processReceipt(uri: Uri) {
        _scanState.value = ScanState.Processing
        viewModelScope.launch {
            runCatching { ocrRecognizer.recognize(context, uri) }
                .onSuccess { text ->
                    val template = lookupTemplate(text)
                    val parsed = ReceiptParser.parse(text, template)
                    val receipt = EditableReceipt.fromParsed(uri, parsed)
                    val hinted = applyMerchantHint(receipt)
                    _pendingReceipt.value = hinted
                    _scanState.value = ScanState.Done(uri)
                }
                .onFailure { _scanState.value = ScanState.Error }
        }
    }

    private suspend fun lookupTemplate(rawText: String): ReceiptTemplate? {
        val parsed = ReceiptParser.parse(rawText)
        val merchant = normalizeMerchant(parsed.merchant.orEmpty()) ?: return null
        val stored = repository.getMerchantTemplate(merchant) ?: return null
        return ReceiptTemplate(totalKeyword = stored.totalKeyword, dateFormat = stored.dateFormat)
    }

    private suspend fun applyMerchantHint(receipt: EditableReceipt): EditableReceipt {
        val merchant = normalizeMerchant(receipt.merchant) ?: return receipt
        val hint = repository.getMerchantHint(merchant) ?: return receipt
        val key = CategoryKey.fromValue(hint.category) ?: return receipt
        return receipt.copy(suggestedCategory = key)
    }

    private fun normalizeMerchant(merchant: String): String? =
        merchant.trim().lowercase(Locale.getDefault())
            .replace(Regex("\\s+"), " ")
            .takeIf { it.isNotBlank() }

    fun consumeScanState() {
        _scanState.value = ScanState.Idle
    }

    fun updatePendingReceipt(receipt: EditableReceipt) {
        _pendingReceipt.value = receipt
    }

    fun savePendingReceipt(
        type: TransactionType,
        category: String,
        receipt: EditableReceipt,
    ) {
        viewModelScope.launch {
            val amount = receipt.total
            val transaction = Transaction(
                type = type,
                amount = amount,
                category = category,
                description = receipt.description,
                date = receipt.dateTimestamp,
                merchant = receipt.merchant,
                source = EntrySource.SCAN,
            )
            val items = receipt.lineItems
                .filter { it.name.isNotBlank() && it.price > 0.0 }
                .map { LineItem(name = it.name, price = it.price, quantity = it.quantity) }
            repository.addTransactionWithItems(transaction, items)
            val merchant = normalizeMerchant(receipt.merchant)
            if (merchant != null) {
                repository.saveMerchantHint(merchant, category)
                val totalKeyword = ReceiptParser.detectTotalKeyword(receipt.rawText)
                val dateFormat = ReceiptParser.detectDateFormat(receipt.dateText)
                if (totalKeyword != null) {
                    repository.saveMerchantTemplate(merchant, totalKeyword, dateFormat)
                }
            }
            _pendingReceipt.value = null
            checkBudgetAlerts(category)
        }
    }

    fun cancelPendingReceipt() {
        _pendingReceipt.value = null
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { repository.deleteTransaction(transaction) }
    }

    fun updateTransaction(transaction: Transaction, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            _detailState.value = TransactionDetail(
                transaction = transaction,
                lineItems = _detailState.value?.lineItems.orEmpty(),
            )
            checkBudgetAlerts(transaction.category)
            onDone()
        }
    }

    fun loadDetail(transactionId: Long) {
        viewModelScope.launch {
            val tx = repository.getTransaction(transactionId)
            _detailState.value = tx?.let {
                val items = repository.getLineItemsForTransaction(it.id)
                TransactionDetail(transaction = it, lineItems = items)
            }
        }
    }

    fun clearDetail() {
        _detailState.value = null
    }

    fun saveBudget(category: String, monthlyLimit: Double) {
        viewModelScope.launch {
            val existing = repository.getBudgetForCategory(category)
            repository.saveBudget(
                Budget(
                    id = existing?.id ?: 0,
                    category = category,
                    monthlyLimit = monthlyLimit,
                ),
            )
        }
    }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch { repository.deleteBudget(budget) }
    }

    fun exportToUri(uri: Uri, format: ExportFormat, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val transactions = repository.getAllTransactionsOnce()
            val ok = when (format) {
                ExportFormat.CSV -> DataExporter.exportCsv(context, uri, transactions)
                ExportFormat.JSON -> DataExporter.exportJson(context, uri, transactions)
            }
            onResult(ok)
        }
    }

    enum class ExportFormat { CSV, JSON }

    fun importFromUri(uri: Uri, format: ExportFormat, onResult: (Int, Int) -> Unit) {
        viewModelScope.launch {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader().readText()
                }
            }.getOrNull()
            if (text == null) {
                _snackbar.value = context.getString(R.string.import_error)
                onResult(0, 0)
                return@launch
            }
            val bundle = when (format) {
                ExportFormat.CSV -> DataParser.parseCsv(text)
                ExportFormat.JSON -> DataParser.parseJson(text)
            }
            if (bundle.transactions.isEmpty()) {
                _snackbar.value = context.getString(
                    R.string.import_nothing,
                    bundle.warnings.firstOrNull() ?: "",
                )
                onResult(0, 0)
                return@launch
            }
            bundle.transactions.forEach { tx ->
                val date = DataFormat.parseDate(tx.dateText) ?: System.currentTimeMillis()
                val source = if (tx.source == EntrySource.SCAN.name) EntrySource.SCAN else EntrySource.MANUAL
                val defaultCategory = if (tx.type == TransactionType.EXPENSE) {
                    CategoryKey.OTHER_EXPENSE.stableValue
                } else {
                    CategoryKey.OTHER_INCOME.stableValue
                }
                val transaction = Transaction(
                    type = tx.type,
                    amount = tx.amount,
                    category = tx.category.ifBlank { defaultCategory },
                    description = tx.description,
                    date = date,
                    merchant = tx.merchant,
                    source = source,
                )
                val items = tx.lineItems
                    .filter { it.name.isNotBlank() && it.price > 0.0 }
                    .map { LineItem(name = it.name, price = it.price, quantity = it.quantity) }
                repository.addTransactionWithItems(transaction, items)
            }
            val skipped = bundle.warnings.size
            _snackbar.value = context.getString(R.string.import_success, bundle.transactions.size, skipped)
            onResult(bundle.transactions.size, skipped)
        }
    }

    fun consumeSnackbar() {
        _snackbar.value = null
    }

    fun showExportResult(
        context: Context,
        ok: Boolean,
        uri: android.net.Uri,
        format: ExportFormat,
    ) {
        if (!ok) {
            _snackbar.value = context.getString(R.string.export_error)
            return
        }
        val name = uri.lastPathSegment ?: uri.toString()
        _snackbar.value = when (format) {
            ExportFormat.CSV -> context.getString(R.string.export_snackbar_csv, name)
            ExportFormat.JSON -> context.getString(R.string.export_snackbar_json, name)
        }
    }

    private suspend fun checkBudgetAlerts(category: String) {
        val budget = repository.getBudgetForCategory(category) ?: return
        val totals = repository.monthExpensesByCategory(System.currentTimeMillis()).first()
        val spent = totals.firstOrNull { it.category == category }?.total ?: 0.0
        val spentFormatted = String.format(Locale.getDefault(), "%.2f", spent)
        val limitFormatted = String.format(Locale.getDefault(), "%.2f", budget.monthlyLimit)
        val categoryLabelStr = categoryLabel(context, category)
        if (spent > budget.monthlyLimit) {
            _snackbar.value = context.getString(
                R.string.snackbar_budget_exceeded,
                categoryLabelStr,
                spentFormatted,
                limitFormatted,
            )
        } else if ((budget.monthlyLimit > 0) && (spent >= (budget.monthlyLimit * 0.8))) {
            _snackbar.value = context.getString(
                R.string.snackbar_budget_near_limit,
                categoryLabelStr,
                spentFormatted,
                limitFormatted,
            )
        }
    }

    companion object {
        private const val REFRESH_INTERVAL_MS = 15 * 60 * 1000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = misGastosApplication
                val db = com.misgastos.app.data.database.MisGastosDatabase.getInstance(app)
                val repo = MisGastosRepository(
                    db.transactionDao(),
                    db.budgetDao(),
                    db.lineItemDao(),
                    db.merchantHintDao(),
                    db.merchantTemplateDao(),
                )
                return MisGastosViewModel(app, repo) as T
            }
        }
    }
}

sealed class ScanState {
    data object Idle : ScanState()
    data object Processing : ScanState()
    data object Error : ScanState()
    data class Done(val uri: Uri) : ScanState()
}

data class EditableLineItem(
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Double = 1.0,
)

data class TransactionDetail(
    val transaction: Transaction,
    val lineItems: List<LineItem> = emptyList(),
)

data class EditableReceipt(
    val imageUri: Uri,
    val merchant: String = "",
    val dateText: String = "",
    val dateTimestamp: Long = System.currentTimeMillis(),
    val total: Double = 0.0,
    val description: String = "",
    val lineItems: List<EditableLineItem> = emptyList(),
    val rawText: String = "",
    val suggestedCategory: CategoryKey? = null,
) {
    companion object {
        fun fromParsed(uri: Uri, parsed: ParsedReceipt): EditableReceipt =
            EditableReceipt(
                imageUri = uri,
                merchant = parsed.merchant.orEmpty(),
                dateText = parsed.date.orEmpty(),
                dateTimestamp = System.currentTimeMillis(),
                total = parsed.total ?: 0.0,
                description = parsed.merchant.orEmpty(),
                lineItems = parsed.lineItems.map {
                    EditableLineItem(
                        name = it.name,
                        price = it.price,
                        quantity = it.quantity,
                    )
                },
                rawText = parsed.rawText,
                suggestedCategory = null,
            )
    }
}

internal lateinit var misGastosApplication: android.content.Context
