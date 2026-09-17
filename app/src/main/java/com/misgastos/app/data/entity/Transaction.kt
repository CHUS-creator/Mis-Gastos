package com.misgastos.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: TransactionType,
    val amount: Double,
    val category: String,
    val description: String,
    val date: Long,
    val merchant: String = "",
    val source: EntrySource = EntrySource.MANUAL,
)

enum class TransactionType { INCOME, EXPENSE }

enum class EntrySource { MANUAL, SCAN }
