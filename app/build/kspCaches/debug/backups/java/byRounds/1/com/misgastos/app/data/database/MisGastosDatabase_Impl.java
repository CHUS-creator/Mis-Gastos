package com.misgastos.app.data.database;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.misgastos.app.data.dao.BudgetDao;
import com.misgastos.app.data.dao.BudgetDao_Impl;
import com.misgastos.app.data.dao.LineItemDao;
import com.misgastos.app.data.dao.LineItemDao_Impl;
import com.misgastos.app.data.dao.MerchantHintDao;
import com.misgastos.app.data.dao.MerchantHintDao_Impl;
import com.misgastos.app.data.dao.MerchantTemplateDao;
import com.misgastos.app.data.dao.MerchantTemplateDao_Impl;
import com.misgastos.app.data.dao.TransactionDao;
import com.misgastos.app.data.dao.TransactionDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MisGastosDatabase_Impl extends MisGastosDatabase {
  private volatile TransactionDao _transactionDao;

  private volatile BudgetDao _budgetDao;

  private volatile LineItemDao _lineItemDao;

  private volatile MerchantHintDao _merchantHintDao;

  private volatile MerchantTemplateDao _merchantTemplateDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(4) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `transactions` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `type` TEXT NOT NULL, `amount` REAL NOT NULL, `category` TEXT NOT NULL, `description` TEXT NOT NULL, `date` INTEGER NOT NULL, `merchant` TEXT NOT NULL, `source` TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `budgets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `category` TEXT NOT NULL, `monthlyLimit` REAL NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `line_items` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `transactionId` INTEGER NOT NULL, `name` TEXT NOT NULL, `price` REAL NOT NULL, `quantity` REAL NOT NULL, FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_line_items_transactionId` ON `line_items` (`transactionId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `merchant_hints` (`merchant` TEXT NOT NULL, `category` TEXT NOT NULL, PRIMARY KEY(`merchant`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `merchant_templates` (`merchant` TEXT NOT NULL, `totalKeyword` TEXT NOT NULL, `dateFormat` TEXT, PRIMARY KEY(`merchant`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e4512b0f93f56b097375c6528218996f')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `transactions`");
        db.execSQL("DROP TABLE IF EXISTS `budgets`");
        db.execSQL("DROP TABLE IF EXISTS `line_items`");
        db.execSQL("DROP TABLE IF EXISTS `merchant_hints`");
        db.execSQL("DROP TABLE IF EXISTS `merchant_templates`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsTransactions = new HashMap<String, TableInfo.Column>(8);
        _columnsTransactions.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("amount", new TableInfo.Column("amount", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("description", new TableInfo.Column("description", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("date", new TableInfo.Column("date", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("merchant", new TableInfo.Column("merchant", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsTransactions.put("source", new TableInfo.Column("source", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysTransactions = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesTransactions = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoTransactions = new TableInfo("transactions", _columnsTransactions, _foreignKeysTransactions, _indicesTransactions);
        final TableInfo _existingTransactions = TableInfo.read(db, "transactions");
        if (!_infoTransactions.equals(_existingTransactions)) {
          return new RoomOpenHelper.ValidationResult(false, "transactions(com.misgastos.app.data.entity.Transaction).\n"
                  + " Expected:\n" + _infoTransactions + "\n"
                  + " Found:\n" + _existingTransactions);
        }
        final HashMap<String, TableInfo.Column> _columnsBudgets = new HashMap<String, TableInfo.Column>(3);
        _columnsBudgets.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBudgets.put("monthlyLimit", new TableInfo.Column("monthlyLimit", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBudgets = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBudgets = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBudgets = new TableInfo("budgets", _columnsBudgets, _foreignKeysBudgets, _indicesBudgets);
        final TableInfo _existingBudgets = TableInfo.read(db, "budgets");
        if (!_infoBudgets.equals(_existingBudgets)) {
          return new RoomOpenHelper.ValidationResult(false, "budgets(com.misgastos.app.data.entity.Budget).\n"
                  + " Expected:\n" + _infoBudgets + "\n"
                  + " Found:\n" + _existingBudgets);
        }
        final HashMap<String, TableInfo.Column> _columnsLineItems = new HashMap<String, TableInfo.Column>(5);
        _columnsLineItems.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLineItems.put("transactionId", new TableInfo.Column("transactionId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLineItems.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLineItems.put("price", new TableInfo.Column("price", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsLineItems.put("quantity", new TableInfo.Column("quantity", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysLineItems = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysLineItems.add(new TableInfo.ForeignKey("transactions", "CASCADE", "NO ACTION", Arrays.asList("transactionId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesLineItems = new HashSet<TableInfo.Index>(1);
        _indicesLineItems.add(new TableInfo.Index("index_line_items_transactionId", false, Arrays.asList("transactionId"), Arrays.asList("ASC")));
        final TableInfo _infoLineItems = new TableInfo("line_items", _columnsLineItems, _foreignKeysLineItems, _indicesLineItems);
        final TableInfo _existingLineItems = TableInfo.read(db, "line_items");
        if (!_infoLineItems.equals(_existingLineItems)) {
          return new RoomOpenHelper.ValidationResult(false, "line_items(com.misgastos.app.data.entity.LineItem).\n"
                  + " Expected:\n" + _infoLineItems + "\n"
                  + " Found:\n" + _existingLineItems);
        }
        final HashMap<String, TableInfo.Column> _columnsMerchantHints = new HashMap<String, TableInfo.Column>(2);
        _columnsMerchantHints.put("merchant", new TableInfo.Column("merchant", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMerchantHints.put("category", new TableInfo.Column("category", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMerchantHints = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMerchantHints = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMerchantHints = new TableInfo("merchant_hints", _columnsMerchantHints, _foreignKeysMerchantHints, _indicesMerchantHints);
        final TableInfo _existingMerchantHints = TableInfo.read(db, "merchant_hints");
        if (!_infoMerchantHints.equals(_existingMerchantHints)) {
          return new RoomOpenHelper.ValidationResult(false, "merchant_hints(com.misgastos.app.data.entity.MerchantHint).\n"
                  + " Expected:\n" + _infoMerchantHints + "\n"
                  + " Found:\n" + _existingMerchantHints);
        }
        final HashMap<String, TableInfo.Column> _columnsMerchantTemplates = new HashMap<String, TableInfo.Column>(3);
        _columnsMerchantTemplates.put("merchant", new TableInfo.Column("merchant", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMerchantTemplates.put("totalKeyword", new TableInfo.Column("totalKeyword", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMerchantTemplates.put("dateFormat", new TableInfo.Column("dateFormat", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMerchantTemplates = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMerchantTemplates = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMerchantTemplates = new TableInfo("merchant_templates", _columnsMerchantTemplates, _foreignKeysMerchantTemplates, _indicesMerchantTemplates);
        final TableInfo _existingMerchantTemplates = TableInfo.read(db, "merchant_templates");
        if (!_infoMerchantTemplates.equals(_existingMerchantTemplates)) {
          return new RoomOpenHelper.ValidationResult(false, "merchant_templates(com.misgastos.app.data.entity.MerchantTemplate).\n"
                  + " Expected:\n" + _infoMerchantTemplates + "\n"
                  + " Found:\n" + _existingMerchantTemplates);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e4512b0f93f56b097375c6528218996f", "0c66b53798b0b4ea9bb00013ca76ef55");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "transactions","budgets","line_items","merchant_hints","merchant_templates");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `transactions`");
      _db.execSQL("DELETE FROM `budgets`");
      _db.execSQL("DELETE FROM `line_items`");
      _db.execSQL("DELETE FROM `merchant_hints`");
      _db.execSQL("DELETE FROM `merchant_templates`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(TransactionDao.class, TransactionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BudgetDao.class, BudgetDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(LineItemDao.class, LineItemDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MerchantHintDao.class, MerchantHintDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MerchantTemplateDao.class, MerchantTemplateDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public TransactionDao transactionDao() {
    if (_transactionDao != null) {
      return _transactionDao;
    } else {
      synchronized(this) {
        if(_transactionDao == null) {
          _transactionDao = new TransactionDao_Impl(this);
        }
        return _transactionDao;
      }
    }
  }

  @Override
  public BudgetDao budgetDao() {
    if (_budgetDao != null) {
      return _budgetDao;
    } else {
      synchronized(this) {
        if(_budgetDao == null) {
          _budgetDao = new BudgetDao_Impl(this);
        }
        return _budgetDao;
      }
    }
  }

  @Override
  public LineItemDao lineItemDao() {
    if (_lineItemDao != null) {
      return _lineItemDao;
    } else {
      synchronized(this) {
        if(_lineItemDao == null) {
          _lineItemDao = new LineItemDao_Impl(this);
        }
        return _lineItemDao;
      }
    }
  }

  @Override
  public MerchantHintDao merchantHintDao() {
    if (_merchantHintDao != null) {
      return _merchantHintDao;
    } else {
      synchronized(this) {
        if(_merchantHintDao == null) {
          _merchantHintDao = new MerchantHintDao_Impl(this);
        }
        return _merchantHintDao;
      }
    }
  }

  @Override
  public MerchantTemplateDao merchantTemplateDao() {
    if (_merchantTemplateDao != null) {
      return _merchantTemplateDao;
    } else {
      synchronized(this) {
        if(_merchantTemplateDao == null) {
          _merchantTemplateDao = new MerchantTemplateDao_Impl(this);
        }
        return _merchantTemplateDao;
      }
    }
  }
}
