package com.misgastos.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.misgastos.app.data.dao.CategoryTotal
import com.misgastos.app.data.entity.Budget
import com.misgastos.app.data.entity.Transaction
import com.misgastos.app.data.entity.TransactionType
import com.misgastos.app.data.repository.MisGastosRepository
import com.misgastos.app.util.DateUtils
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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

class MisGastosViewModel(private val repository: MisGastosRepository) : ViewModel() {

    private val now = System.currentTimeMillis()

    val dashboard: StateFlow<DashboardState> = combine(
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

    val monthExpensesByCategory: StateFlow<List<CategoryTotal>> =
        repository.monthExpensesByCategory(now).stateIn(
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

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch { repository.deleteTransaction(transaction) }
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

    fun consumeSnackbar() {
        _snackbar.value = null
    }

    private suspend fun checkBudgetAlerts(category: String) {
        val budget = repository.getBudgetForCategory(category) ?: return
        val totals = repository.monthExpensesByCategory(System.currentTimeMillis()).first()
        val spent = totals.firstOrNull { it.category == category }?.total ?: 0.0
        val spentFormatted = String.format(Locale.getDefault(), "%.2f", spent)
        val limitFormatted = String.format(Locale.getDefault(), "%.2f", budget.monthlyLimit)
        if (spent > budget.monthlyLimit) {
            _snackbar.value = "Has superado el presupuesto de $category ($spentFormatted / $limitFormatted)"
        } else if ((budget.monthlyLimit > 0) && (spent >= (budget.monthlyLimit * 0.8))) {
            _snackbar.value = "Estás cerca del límite en $category ($spentFormatted / $limitFormatted)"
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = misGastosApplication
                val db = com.misgastos.app.data.database.MisGastosDatabase.getInstance(app)
                val repo = MisGastosRepository(db.transactionDao(), db.budgetDao())
                return MisGastosViewModel(repo) as T
            }
        }
    }
}

internal lateinit var misGastosApplication: android.content.Context
