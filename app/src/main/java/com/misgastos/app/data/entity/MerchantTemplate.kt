package com.misgastos.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "merchant_templates")
data class MerchantTemplate(
    @PrimaryKey val merchant: String,
    val totalKeyword: String,
    val dateFormat: String? = null,
)
