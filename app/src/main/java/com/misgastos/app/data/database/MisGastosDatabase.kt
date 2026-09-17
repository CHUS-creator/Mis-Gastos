package com.misgastos.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.misgastos.app.data.dao.BudgetDao
import com.misgastos.app.data.dao.TransactionDao
import com.misgastos.app.data.entity.Budget
import com.misgastos.app.data.entity.Transaction
import com.misgastos.app.data.entity.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)
}

@Database(
    entities = [Transaction::class, Budget::class],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MisGastosDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao

    companion object {
        @Volatile
        private var INSTANCE: MisGastosDatabase? = null

        fun getInstance(context: Context): MisGastosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MisGastosDatabase::class.java,
                    "misgastos.db",
                ).fallbackToDestructiveMigration().build().also {
                    INSTANCE = it
                }
            }
        }
    }
}
