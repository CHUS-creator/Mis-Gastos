package com.misgastos.app.data.repository

import com.misgastos.app.data.dao.BudgetDao
import com.misgastos.app.data.dao.CategoryTotal
import com.misgastos.app.data.dao.LineItemDao
import com.misgastos.app.data.dao.MerchantHintDao
import com.misgastos.app.data.dao.TransactionDao
import com.misgastos.app.data.entity.Budget
import com.misgastos.app.data.entity.LineItem
import com.misgastos.app.data.entity.MerchantHint
import com.misgastos.app.data.entity.Transaction
import com.misgastos.app.data.entity.TransactionType
import com.misgastos.app.util.DateUtils
import kotlinx.coroutines.flow.Flow

class MisGastosRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val lineItemDao: LineItemDao,
    private val merchantHintDao: MerchantHintDao,
) {
    fun getAllTransactions(): Flow<List<Transaction>> = transactionDao.getAll()

    fun getExpenses(): Flow<List<Transaction>> =
        transactionDao.getByType(TransactionType.EXPENSE)

    fun getIncomes(): Flow<List<Transaction>> =
        transactionDao.getByType(TransactionType.INCOME)

    @Suppress("unused")
    fun getTransactionsByMonth(timestamp: Long): Flow<List<Transaction>> {
        val range = DateUtils.monthRange(timestamp)
        return transactionDao.getByDateRange(range.first, range.last)
    }

    fun totalBalance(): Flow<Double> {
        val incomes = transactionDao.sumByType(TransactionType.INCOME)
        val expenses = transactionDao.sumByType(TransactionType.EXPENSE)
        return kotlinx.coroutines.flow.combine(incomes, expenses) { i, e -> i - e }
    }

    fun monthExpenses(timestamp: Long): Flow<Double> {
        val range = DateUtils.monthRange(timestamp)
        return transactionDao.sumByTypeInRange(TransactionType.EXPENSE, range.first, range.last)
    }

    fun monthIncomes(timestamp: Long): Flow<Double> {
        val range = DateUtils.monthRange(timestamp)
        return transactionDao.sumByTypeInRange(TransactionType.INCOME, range.first, range.last)
    }

    fun monthBalance(timestamp: Long): Flow<Double> {
        val range = DateUtils.monthRange(timestamp)
        val e = transactionDao.sumByTypeInRange(TransactionType.EXPENSE, range.first, range.last)
        val i = transactionDao.sumByTypeInRange(TransactionType.INCOME, range.first, range.last)
        return kotlinx.coroutines.flow.combine(i, e) { inc, exp -> inc - exp }
    }

    fun monthExpensesByCategory(timestamp: Long): Flow<List<CategoryTotal>> {
        val range = DateUtils.monthRange(timestamp)
        return transactionDao.sumByCategory(TransactionType.EXPENSE, range.first, range.last)
    }

    suspend fun addTransaction(transaction: Transaction): Long =
        transactionDao.insert(transaction)

    suspend fun addTransactionWithItems(transaction: Transaction, items: List<LineItem>): Long {
        val txId = transactionDao.insert(transaction)
        if (items.isNotEmpty()) {
            lineItemDao.insertAll(items.map { it.copy(transactionId = txId) })
        }
        return txId
    }

    suspend fun getLineItemsForTransaction(transactionId: Long): List<LineItem> =
        lineItemDao.getForTransactionOnce(transactionId)

    suspend fun getMerchantHint(merchant: String): MerchantHint? =
        merchantHintDao.get(merchant)

    suspend fun saveMerchantHint(merchant: String, category: String) {
        merchantHintDao.upsert(MerchantHint(merchant = merchant, category = category))
    }

    @Suppress("unused")
    suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.update(transaction)

    suspend fun deleteTransaction(transaction: Transaction) =
        transactionDao.delete(transaction)

    fun getAllBudgets(): Flow<List<Budget>> = budgetDao.getAll()

    suspend fun getBudgetForCategory(category: String): Budget? =
        budgetDao.getByCategory(category)

    suspend fun saveBudget(budget: Budget): Long = budgetDao.insert(budget)

    suspend fun deleteBudget(budget: Budget) = budgetDao.delete(budget)
}
