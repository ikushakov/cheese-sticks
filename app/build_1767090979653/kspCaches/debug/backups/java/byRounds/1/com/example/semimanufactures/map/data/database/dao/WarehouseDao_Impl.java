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
import com.example.semimanufactures.map.data.database.entities.WarehouseEntity;
import com.example.semimanufactures.map.data.models.Employee;
import java.lang.Class;
import java.lang.Double;
import java.lang.Exception;
import java.lang.Integer;
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
public final class WarehouseDao_Impl implements WarehouseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<WarehouseEntity> __insertionAdapterOfWarehouseEntity;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public WarehouseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfWarehouseEntity = new EntityInsertionAdapter<WarehouseEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `warehouses` (`id`,`name`,`coordinates`,`floorId`,`buildingId`,`responsible`,`capacity`,`status`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final WarehouseEntity entity) {
        statement.bindString(1, entity.getId());
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        final String _tmp = __converters.toDoubleList(entity.getCoordinates());
        if (_tmp == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, _tmp);
        }
        if (entity.getFloorId() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getFloorId());
        }
        if (entity.getBuildingId() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getBuildingId());
        }
        final String _tmp_1 = __converters.toEmployeeList(entity.getResponsible());
        if (_tmp_1 == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, _tmp_1);
        }
        if (entity.getCapacity() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getCapacity());
        }
        if (entity.getStatus() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getStatus());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM warehouses";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<WarehouseEntity> warehouses,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfWarehouseEntity.insert(warehouses);
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
  public Object getAll(final Continuation<? super List<WarehouseEntity>> $completion) {
    final String _sql = "SELECT * FROM warehouses";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<WarehouseEntity>>() {
      @Override
      @NonNull
      public List<WarehouseEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCoordinates = CursorUtil.getColumnIndexOrThrow(_cursor, "coordinates");
          final int _cursorIndexOfFloorId = CursorUtil.getColumnIndexOrThrow(_cursor, "floorId");
          final int _cursorIndexOfBuildingId = CursorUtil.getColumnIndexOrThrow(_cursor, "buildingId");
          final int _cursorIndexOfResponsible = CursorUtil.getColumnIndexOrThrow(_cursor, "responsible");
          final int _cursorIndexOfCapacity = CursorUtil.getColumnIndexOrThrow(_cursor, "capacity");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final List<WarehouseEntity> _result = new ArrayList<WarehouseEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final WarehouseEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final List<Double> _tmpCoordinates;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfCoordinates)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfCoordinates);
            }
            _tmpCoordinates = __converters.fromDoubleList(_tmp);
            final String _tmpFloorId;
            if (_cursor.isNull(_cursorIndexOfFloorId)) {
              _tmpFloorId = null;
            } else {
              _tmpFloorId = _cursor.getString(_cursorIndexOfFloorId);
            }
            final String _tmpBuildingId;
            if (_cursor.isNull(_cursorIndexOfBuildingId)) {
              _tmpBuildingId = null;
            } else {
              _tmpBuildingId = _cursor.getString(_cursorIndexOfBuildingId);
            }
            final List<Employee> _tmpResponsible;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfResponsible)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfResponsible);
            }
            _tmpResponsible = __converters.fromEmployeeList(_tmp_1);
            final Integer _tmpCapacity;
            if (_cursor.isNull(_cursorIndexOfCapacity)) {
              _tmpCapacity = null;
            } else {
              _tmpCapacity = _cursor.getInt(_cursorIndexOfCapacity);
            }
            final String _tmpStatus;
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _tmpStatus = null;
            } else {
              _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            }
            _item = new WarehouseEntity(_tmpId,_tmpName,_tmpCoordinates,_tmpFloorId,_tmpBuildingId,_tmpResponsible,_tmpCapacity,_tmpStatus);
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
  public Object getById(final String id, final Continuation<? super WarehouseEntity> $completion) {
    final String _sql = "SELECT * FROM warehouses WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<WarehouseEntity>() {
      @Override
      @Nullable
      public WarehouseEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfCoordinates = CursorUtil.getColumnIndexOrThrow(_cursor, "coordinates");
          final int _cursorIndexOfFloorId = CursorUtil.getColumnIndexOrThrow(_cursor, "floorId");
          final int _cursorIndexOfBuildingId = CursorUtil.getColumnIndexOrThrow(_cursor, "buildingId");
          final int _cursorIndexOfResponsible = CursorUtil.getColumnIndexOrThrow(_cursor, "responsible");
          final int _cursorIndexOfCapacity = CursorUtil.getColumnIndexOrThrow(_cursor, "capacity");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final WarehouseEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final List<Double> _tmpCoordinates;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfCoordinates)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfCoordinates);
            }
            _tmpCoordinates = __converters.fromDoubleList(_tmp);
            final String _tmpFloorId;
            if (_cursor.isNull(_cursorIndexOfFloorId)) {
              _tmpFloorId = null;
            } else {
              _tmpFloorId = _cursor.getString(_cursorIndexOfFloorId);
            }
            final String _tmpBuildingId;
            if (_cursor.isNull(_cursorIndexOfBuildingId)) {
              _tmpBuildingId = null;
            } else {
              _tmpBuildingId = _cursor.getString(_cursorIndexOfBuildingId);
            }
            final List<Employee> _tmpResponsible;
            final String _tmp_1;
            if (_cursor.isNull(_cursorIndexOfResponsible)) {
              _tmp_1 = null;
            } else {
              _tmp_1 = _cursor.getString(_cursorIndexOfResponsible);
            }
            _tmpResponsible = __converters.fromEmployeeList(_tmp_1);
            final Integer _tmpCapacity;
            if (_cursor.isNull(_cursorIndexOfCapacity)) {
              _tmpCapacity = null;
            } else {
              _tmpCapacity = _cursor.getInt(_cursorIndexOfCapacity);
            }
            final String _tmpStatus;
            if (_cursor.isNull(_cursorIndexOfStatus)) {
              _tmpStatus = null;
            } else {
              _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            }
            _result = new WarehouseEntity(_tmpId,_tmpName,_tmpCoordinates,_tmpFloorId,_tmpBuildingId,_tmpResponsible,_tmpCapacity,_tmpStatus);
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
