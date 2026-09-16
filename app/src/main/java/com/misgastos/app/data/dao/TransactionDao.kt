package com.misgastos.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.misgastos.app.data.entity.Transaction
import com.misgastos.app.data.entity.TransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY date DESC")
    fun getByType(type: TransactionType): Flow<List<Transaction>>

    @Query(
        "SELECT * FROM transactions WHERE date >= :start AND date <= :end ORDER BY date DESC"
    )
    fun getByDateRange(start: Long, end: Long): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getById(id: Long): Transaction?

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = :type"
    )
    fun sumByType(type: TransactionType): Flow<Double>

    @Query(
        "SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = :type " +
            "AND date >= :start AND date <= :end"
    )
    fun sumByTypeInRange(type: TransactionType, start: Long, end: Long): Flow<Double>

    @Query(
        "SELECT category, COALESCE(SUM(amount), 0) AS total FROM transactions " +
            "WHERE type = :type AND date >= :start AND date <= :end " +
            "GROUP BY category ORDER BY total DESC"
    )
    fun sumByCategory(type: TransactionType, start: Long, end: Long): Flow<List<CategoryTotal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: Transaction): Long

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)
}

data class CategoryTotal(
    val category: String,
    val total: Double
)
