package com.misgastos.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.misgastos.app.data.dao.BudgetDao
import com.misgastos.app.data.dao.LineItemDao
import com.misgastos.app.data.dao.MerchantHintDao
import com.misgastos.app.data.dao.TransactionDao
import com.misgastos.app.data.entity.Budget
import com.misgastos.app.data.entity.EntrySource
import com.misgastos.app.data.entity.LineItem
import com.misgastos.app.data.entity.MerchantHint
import com.misgastos.app.data.entity.Transaction
import com.misgastos.app.data.entity.TransactionType

class Converters {
    @TypeConverter
    fun fromTransactionType(type: TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType = TransactionType.valueOf(value)

    @TypeConverter
    fun fromEntrySource(source: EntrySource): String = source.name

    @TypeConverter
    fun toEntrySource(value: String): EntrySource = EntrySource.valueOf(value)
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE transactions ADD COLUMN merchant TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE transactions ADD COLUMN source TEXT NOT NULL DEFAULT 'MANUAL'")
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS line_items (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                transactionId INTEGER NOT NULL,
                name TEXT NOT NULL,
                price REAL NOT NULL,
                quantity REAL NOT NULL,
                FOREIGN KEY(transactionId) REFERENCES transactions(id) ON DELETE CASCADE
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_line_items_transactionId ON line_items(transactionId)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS merchant_hints (
                merchant TEXT NOT NULL PRIMARY KEY,
                category TEXT NOT NULL
            )
            """.trimIndent(),
        )
    }
}

@Database(
    entities = [Transaction::class, Budget::class, LineItem::class, MerchantHint::class],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class MisGastosDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun lineItemDao(): LineItemDao
    abstract fun merchantHintDao(): MerchantHintDao

    companion object {
        @Volatile
        private var INSTANCE: MisGastosDatabase? = null

        fun getInstance(context: Context): MisGastosDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MisGastosDatabase::class.java,
                    "misgastos.db",
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build().also {
                        INSTANCE = it
                    }
            }
        }
    }
}
