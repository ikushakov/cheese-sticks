package com.example.semimanufactures.map.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.semimanufactures.map.data.database.entities.BuildingEntity

@Dao
interface BuildingDao {
    @Query("SELECT * FROM buildings")
    suspend fun getAll(): List<BuildingEntity>

    @Query("SELECT * FROM buildings WHERE id = :id")
    suspend fun getById(id: String): BuildingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(buildings: List<BuildingEntity>)

    @Query("DELETE FROM buildings")
    suspend fun deleteAll()
}
