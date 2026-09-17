package com.misgastos.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_hints")
data class MerchantHint(
    @PrimaryKey val merchant: String,
    val category: String,
)
