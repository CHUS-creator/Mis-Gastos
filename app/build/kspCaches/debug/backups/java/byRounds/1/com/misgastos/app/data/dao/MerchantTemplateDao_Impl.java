package com.misgastos.app.data.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.misgastos.app.data.entity.MerchantTemplate;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MerchantTemplateDao_Impl implements MerchantTemplateDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MerchantTemplate> __insertionAdapterOfMerchantTemplate;

  public MerchantTemplateDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMerchantTemplate = new EntityInsertionAdapter<MerchantTemplate>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `merchant_templates` (`merchant`,`totalKeyword`,`dateFormat`) VALUES (?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MerchantTemplate entity) {
        statement.bindString(1, entity.getMerchant());
        statement.bindString(2, entity.getTotalKeyword());
        if (entity.getDateFormat() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getDateFormat());
        }
      }
    };
  }

  @Override
  public Object upsert(final MerchantTemplate template,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMerchantTemplate.insert(template);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object get(final String merchant,
      final Continuation<? super MerchantTemplate> $completion) {
    final String _sql = "SELECT * FROM merchant_templates WHERE merchant = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, merchant);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MerchantTemplate>() {
      @Override
      @Nullable
      public MerchantTemplate call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfMerchant = CursorUtil.getColumnIndexOrThrow(_cursor, "merchant");
          final int _cursorIndexOfTotalKeyword = CursorUtil.getColumnIndexOrThrow(_cursor, "totalKeyword");
          final int _cursorIndexOfDateFormat = CursorUtil.getColumnIndexOrThrow(_cursor, "dateFormat");
          final MerchantTemplate _result;
          if (_cursor.moveToFirst()) {
            final String _tmpMerchant;
            _tmpMerchant = _cursor.getString(_cursorIndexOfMerchant);
            final String _tmpTotalKeyword;
            _tmpTotalKeyword = _cursor.getString(_cursorIndexOfTotalKeyword);
            final String _tmpDateFormat;
            if (_cursor.isNull(_cursorIndexOfDateFormat)) {
              _tmpDateFormat = null;
            } else {
              _tmpDateFormat = _cursor.getString(_cursorIndexOfDateFormat);
            }
            _result = new MerchantTemplate(_tmpMerchant,_tmpTotalKeyword,_tmpDateFormat);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
