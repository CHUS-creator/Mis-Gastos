package com.misgastos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.misgastos.app.data.entity.MerchantHint

@Dao
interface MerchantHintDao {
    @Query("SELECT * FROM merchant_hints WHERE merchant = :merchant LIMIT 1")
    suspend fun get(merchant: String): MerchantHint?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(hint: MerchantHint)
}
