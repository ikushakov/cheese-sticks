package com.example.semimanufactures.map.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.semimanufactures.map.data.database.entities.WarehouseEntity

@Dao
interface WarehouseDao {
    @Query("SELECT * FROM warehouses")
    suspend fun getAll(): List<WarehouseEntity>

    @Query("SELECT * FROM warehouses WHERE id = :id")
    suspend fun getById(id: String): WarehouseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(warehouses: List<WarehouseEntity>)

    @Query("DELETE FROM warehouses")
    suspend fun deleteAll()
}
