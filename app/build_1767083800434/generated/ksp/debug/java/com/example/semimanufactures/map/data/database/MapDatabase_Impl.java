package com.example.semimanufactures.map.data.database;

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
import com.example.semimanufactures.map.data.database.dao.BuildingDao;
import com.example.semimanufactures.map.data.database.dao.BuildingDao_Impl;
import com.example.semimanufactures.map.data.database.dao.WarehouseDao;
import com.example.semimanufactures.map.data.database.dao.WarehouseDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class MapDatabase_Impl extends MapDatabase {
  private volatile BuildingDao _buildingDao;

  private volatile WarehouseDao _warehouseDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `buildings` (`id` TEXT NOT NULL, `name` TEXT, `coordinates` TEXT, `type` TEXT, `address` TEXT, `floors` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `warehouses` (`id` TEXT NOT NULL, `name` TEXT, `coordinates` TEXT, `floorId` TEXT, `buildingId` TEXT, `responsible` TEXT, `capacity` INTEGER, `status` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f62f425110e64f752c1f63ec41bfd876')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `buildings`");
        db.execSQL("DROP TABLE IF EXISTS `warehouses`");
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
        final HashMap<String, TableInfo.Column> _columnsBuildings = new HashMap<String, TableInfo.Column>(6);
        _columnsBuildings.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBuildings.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBuildings.put("coordinates", new TableInfo.Column("coordinates", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBuildings.put("type", new TableInfo.Column("type", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBuildings.put("address", new TableInfo.Column("address", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBuildings.put("floors", new TableInfo.Column("floors", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBuildings = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBuildings = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBuildings = new TableInfo("buildings", _columnsBuildings, _foreignKeysBuildings, _indicesBuildings);
        final TableInfo _existingBuildings = TableInfo.read(db, "buildings");
        if (!_infoBuildings.equals(_existingBuildings)) {
          return new RoomOpenHelper.ValidationResult(false, "buildings(com.example.semimanufactures.map.data.database.entities.BuildingEntity).\n"
                  + " Expected:\n" + _infoBuildings + "\n"
                  + " Found:\n" + _existingBuildings);
        }
        final HashMap<String, TableInfo.Column> _columnsWarehouses = new HashMap<String, TableInfo.Column>(8);
        _columnsWarehouses.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("name", new TableInfo.Column("name", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("coordinates", new TableInfo.Column("coordinates", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("floorId", new TableInfo.Column("floorId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("buildingId", new TableInfo.Column("buildingId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("responsible", new TableInfo.Column("responsible", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("capacity", new TableInfo.Column("capacity", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsWarehouses.put("status", new TableInfo.Column("status", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysWarehouses = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesWarehouses = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoWarehouses = new TableInfo("warehouses", _columnsWarehouses, _foreignKeysWarehouses, _indicesWarehouses);
        final TableInfo _existingWarehouses = TableInfo.read(db, "warehouses");
        if (!_infoWarehouses.equals(_existingWarehouses)) {
          return new RoomOpenHelper.ValidationResult(false, "warehouses(com.example.semimanufactures.map.data.database.entities.WarehouseEntity).\n"
                  + " Expected:\n" + _infoWarehouses + "\n"
                  + " Found:\n" + _existingWarehouses);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "f62f425110e64f752c1f63ec41bfd876", "63be6f92d075e3883571bc81b7ceed3f");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "buildings","warehouses");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `buildings`");
      _db.execSQL("DELETE FROM `warehouses`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
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
    _typeConvertersMap.put(BuildingDao.class, BuildingDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(WarehouseDao.class, WarehouseDao_Impl.getRequiredConverters());
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
  public BuildingDao buildingDao() {
    if (_buildingDao != null) {
      return _buildingDao;
    } else {
      synchronized(this) {
        if(_buildingDao == null) {
          _buildingDao = new BuildingDao_Impl(this);
        }
        return _buildingDao;
      }
    }
  }

  @Override
  public WarehouseDao warehouseDao() {
    if (_warehouseDao != null) {
      return _warehouseDao;
    } else {
      synchronized(this) {
        if(_warehouseDao == null) {
          _warehouseDao = new WarehouseDao_Impl(this);
        }
        return _warehouseDao;
      }
    }
  }
}
