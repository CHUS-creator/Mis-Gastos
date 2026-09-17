package com.misgastos.app.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "line_items",
    indices = [Index("transactionId")],
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class LineItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long = 0,
    val name: String,
    val price: Double,
    val quantity: Double = 1.0,
)
