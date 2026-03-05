package com.example.semimanufactures.map.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.semimanufactures.map.data.models.Building
import com.example.semimanufactures.map.data.models.BuildingFloor

@Entity(tableName = "buildings")
data class BuildingEntity(
    @PrimaryKey
    val id: String,
    val name: String?,
    val coordinates: List<List<Double>>?,
    val type: String?,
    val address: String?,
    val floors: List<BuildingFloor>?
) {
    fun toDomain(): Building {
        return Building(
            id = id,
            address = address,
            info = null,
            coordinate = coordinates,
            street = null,
            place = null,
            housing = null,
            floors = floors
        )
    }

    companion object {
        fun fromDomain(building: Building): BuildingEntity {
            return BuildingEntity(
                id = building.id,
                name = building.name, // Используем вычисляемое свойство
                coordinates = building.coordinates, // Используем вычисляемое свойство
                type = null, // type больше не используется в новой модели
                address = building.address,
                floors = building.floors
            )
        }
    }
}
