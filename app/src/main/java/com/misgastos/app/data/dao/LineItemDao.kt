package com.misgastos.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.misgastos.app.data.entity.LineItem
import kotlinx.coroutines.flow.Flow

@Dao
interface LineItemDao {
    @Query("SELECT * FROM line_items WHERE transactionId = :transactionId ORDER BY id ASC")
    fun getForTransaction(transactionId: Long): Flow<List<LineItem>>

    @Query("SELECT * FROM line_items WHERE transactionId = :transactionId ORDER BY id ASC")
    suspend fun getForTransactionOnce(transactionId: Long): List<LineItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LineItem>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LineItem): Long

    @Delete
    suspend fun delete(item: LineItem)
}
