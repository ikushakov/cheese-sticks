package com.example.semimanufactures.map.data.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.semimanufactures.map.data.models.Employee
import com.example.semimanufactures.map.data.models.Warehouse

@Entity(tableName = "warehouses")
data class WarehouseEntity(
    @PrimaryKey
    val id: String, // Храним как String для совместимости с БД
    val name: String?,
    val coordinates: List<Double>?,
    val floorId: String?,
    val buildingId: String?,
    val responsible: List<Employee>?,
    val capacity: Int?,
    val status: String?
) {
    fun toDomain(): Warehouse {
        return Warehouse(
            id = id.toIntOrNull() ?: 0, // Конвертируем String в Int
            title = name,
            address = null,
            coordinate = coordinates,
            assignment = null,
            description = null,
            responsibleEmployeeMdmcode = null,
            secondResponsibleEmployeeMdmcode = null,
            plannerMDMcode = null,
            departmentMdmcode = null,
            isActive = null,
            mdmKey = null
        )
    }

    companion object {
        fun fromDomain(warehouse: Warehouse): WarehouseEntity {
            return WarehouseEntity(
                id = warehouse.id.toString(), // Конвертируем Int в String
                name = warehouse.name, // Используем вычисляемое свойство
                coordinates = warehouse.coordinates, // Используем вычисляемое свойство
                floorId = null, // Эти поля больше не используются в новой модели
                buildingId = null,
                responsible = null,
                capacity = null,
                status = null
            )
        }
    }
}
