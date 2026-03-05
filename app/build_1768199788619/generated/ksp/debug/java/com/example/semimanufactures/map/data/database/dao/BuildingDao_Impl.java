package com.example.semimanufactures.map.data.database.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.example.semimanufactures.map.data.database.Converters;
import com.example.semimanufactures.map.data.database.entities.BuildingEntity;
import com.example.semimanufactures.map.data.models.BuildingFloor;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BuildingDao_Impl implements BuildingDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<BuildingEntity> __insertionAdapterOfBuildingEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public BuildingDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBuildingEntity = new EntityInsertionAdapter<BuildingEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `buildings` (`id`,`name`,`coordinates`,`type`,`address`,`floors`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final BuildingEntity entity) {
        statement.bindString(1, entity.getId());
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        final String _tmp = __converters.toNestedDoubleList(entity.getCoordinates());
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, _tmp);
        }
        if (entity.getType() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getType());
        }
        if (entity.getAddress() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getAddress());
        }
        final String _tmp_1 = __converters.toBuildingFloorList(entity.getFloors());
        if (_tmp_1 == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, _tmp_1);
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM buildings";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<BuildingEntity> buildings,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBuildingEntity.insert(buildings);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object getAll(final Continuation<? super List<BuildingEntity>> $completion) {
    final String _sql = "SELECT * FROM buildings";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<BuildingEntity>>() {
      @Override
      @NonNull
      public List<BuildingEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCoordinates = CursorUtil.getColumnIndexOrThrow(_cursor, "coordinates");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfFloors = CursorUtil.getColumnIndexOrThrow(_cursor, "floors");
          final List<BuildingEntity> _result = new ArrayList<BuildingEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final BuildingEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final List<List<Double>> _tmpCoordinates;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfCoordinates)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfCoordinates);
            }
            _tmpCoordinates = __converters.fromNestedDoubleList(_tmp);
            final String _tmpType;
            if (_cursor.isNull(_cursorIndexOfType)) {
              _tmpType = null;
            } else {
              _tmpType = _cursor.getString(_cursorIndexOfType);
            }
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final List<BuildingFloor> _tmpFloors;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfFloors)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfFloors);
            }
            _tmpFloors = __converters.fromBuildingFloorList(_tmp_1);
            _item = new BuildingEntity(_tmpId,_tmpName,_tmpCoordinates,_tmpType,_tmpAddress,_tmpFloors);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final String id, final Continuation<? super BuildingEntity> $completion) {
    final String _sql = "SELECT * FROM buildings WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<BuildingEntity>() {
      @Override
      @Nullable
      public BuildingEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCoordinates = CursorUtil.getColumnIndexOrThrow(_cursor, "coordinates");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfAddress = CursorUtil.getColumnIndexOrThrow(_cursor, "address");
          final int _cursorIndexOfFloors = CursorUtil.getColumnIndexOrThrow(_cursor, "floors");
          final BuildingEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final List<List<Double>> _tmpCoordinates;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfCoordinates)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfCoordinates);
            }
            _tmpCoordinates = __converters.fromNestedDoubleList(_tmp);
            final String _tmpType;
            if (_cursor.isNull(_cursorIndexOfType)) {
              _tmpType = null;
            } else {
              _tmpType = _cursor.getString(_cursorIndexOfType);
            }
            final String _tmpAddress;
            if (_cursor.isNull(_cursorIndexOfAddress)) {
              _tmpAddress = null;
            } else {
              _tmpAddress = _cursor.getString(_cursorIndexOfAddress);
            }
            final List<BuildingFloor> _tmpFloors;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfFloors)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfFloors);
            }
            _tmpFloors = __converters.fromBuildingFloorList(_tmp_1);
            _result = new BuildingEntity(_tmpId,_tmpName,_tmpCoordinates,_tmpType,_tmpAddress,_tmpFloors);
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
