package com.example.semimanufactures.map.domain.repository

import com.example.semimanufactures.map.data.models.Building
import com.example.semimanufactures.map.data.models.Employee
import com.example.semimanufactures.map.data.models.Item
import com.example.semimanufactures.map.data.models.Warehouse

import com.example.semimanufactures.map.data.models.FloorDepartment

interface IMapRepository {
    // Получение данных
    suspend fun getBuildings(): Result<List<Building>>
    suspend fun getDeliveryPoints(): Result<List<Warehouse>>
    suspend fun getEmployees(): Result<List<Employee>>
    suspend fun getItems(): Result<List<Item>>
    suspend fun getDepartments(): Result<List<FloorDepartment>>
    
    // Здания (Buildings)
    suspend fun addBuilding(building: Map<String, Any>): Result<String>
    suspend fun updateBuilding(building: Map<String, Any>): Result<String>
    suspend fun deleteBuilding(id: String): Result<String>
    
    // Этажи (Floors)
    suspend fun addFloor(floor: Map<String, Any>): Result<String>
    suspend fun deleteFloor(id: String): Result<String>
    
    // Подразделения (Departments)
    suspend fun addDepartment(department: Map<String, Any>): Result<String>
    suspend fun deleteDepartment(id: String): Result<String>
    
    // Точки доставки (Warehouses)
    suspend fun addDeliveryPoint(warehouse: Map<String, Any>): Result<String>
    suspend fun updateDeliveryPoint(warehouse: Map<String, Any>): Result<String>
    suspend fun deleteDeliveryPoint(id: String): Result<String>
}
