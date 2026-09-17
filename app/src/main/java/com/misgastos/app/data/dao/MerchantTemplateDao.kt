package com.misgastos.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.misgastos.app.data.entity.MerchantTemplate

@Dao
interface MerchantTemplateDao {
    @Query("SELECT * FROM merchant_templates WHERE merchant = :merchant LIMIT 1")
    suspend fun get(merchant: String): MerchantTemplate?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: MerchantTemplate)
}
