package com.example.semimanufactures.map.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.semimanufactures.map.data.database.dao.BuildingDao
import com.example.semimanufactures.map.data.database.dao.WarehouseDao
import com.example.semimanufactures.map.data.database.entities.BuildingEntity
import com.example.semimanufactures.map.data.database.entities.WarehouseEntity

@Database(
    entities = [BuildingEntity::class, WarehouseEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class MapDatabase : RoomDatabase() {
    abstract fun buildingDao(): BuildingDao
    abstract fun warehouseDao(): WarehouseDao

    companion object {
        @Volatile
        private var INSTANCE: MapDatabase? = null

        fun getDatabase(context: Context): MapDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MapDatabase::class.java,
                    "map_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
