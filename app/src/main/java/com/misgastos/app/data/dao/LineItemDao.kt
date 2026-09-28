package com.misgastos.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.misgastos.app.data.entity.LineItem
import kotlinx.coroutines.flow.Flow

data class ProductPriceRow(
    val name: String,
    val price: Double,
    val quantity: Double,
    val merchant: String,
    val date: Long,
)

@Dao
interface LineItemDao {

    @Query("SELECT * FROM line_items WHERE transactionId = :transactionId ORDER BY id ASC")
    fun getForTransaction(transactionId: Long): Flow<List<LineItem>>

    @Query("SELECT * FROM line_items WHERE transactionId = :transactionId ORDER BY id ASC")
    suspend fun getForTransactionOnce(transactionId: Long): List<LineItem>

    @Query(
        """
        SELECT line_items.name AS name,
               line_items.price AS price,
               line_items.quantity AS quantity,
               transactions.merchant AS merchant,
               transactions.date AS date
        FROM line_items
        INNER JOIN transactions ON line_items.transactionId = transactions.id
        WHERE transactions.type = 'EXPENSE'
        ORDER BY transactions.date DESC
        """
    )
    fun getAllProductPrices(): Flow<List<ProductPriceRow>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<LineItem>): List<Long>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: LineItem): Long

    @Delete
    suspend fun delete(item: LineItem)
}
