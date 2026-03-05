package com.example.semimanufactures.map.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Модель здания
 */
@JsonClass(generateAdapter = true)
data class Building(
    @Json(name = "id")
    val id: String,
    @Json(name = "address")
    val address: String? = null,
    @Json(name = "info")
    val info: String? = null,
    @Json(name = "coordinate")
    val coordinate: List<List<Double>>? = null, // Полигон здания [[lat, lon], [lat, lon], ...]
    @Json(name = "street")
    val street: String? = null,
    @Json(name = "place")
    val place: String? = null,
    @Json(name = "housing")
    val housing: String? = null,
    @Json(name = "floors")
    val floors: List<BuildingFloor>? = null
) {
    // Вычисляемое свойство для обратной совместимости
    val coordinates: List<List<Double>>? get() = coordinate
    
    // Вычисляемое свойство для имени (из address или других полей)
    val name: String? get() = address ?: info
}

/**
 * Модель этажа здания
 */
@JsonClass(generateAdapter = true)
data class BuildingFloor(
    @Json(name = "id")
    val id: String,
    @Json(name = "info")
    val info: String? = null,
    @Json(name = "floorNumber")
    val floorNumber: Int? = null,
    @Json(name = "departments")
    val departments: List<FloorDepartment>? = null,
    @Json(name = "warehouses")
    val warehouses: List<Warehouse>? = null,
    @Json(name = "items")
    val items: List<Item>? = null
) {
    // Вычисляемое свойство для обратной совместимости
    val number: Int? get() = floorNumber
    val name: String? get() = info
}

/**
 * Модель подразделения на этаже
 */
@JsonClass(generateAdapter = true)
data class FloorDepartment(
    @Json(name = "id")
    val id: String,
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "coordinates")
    val coordinates: List<Double>? = null, // [lat, lon]
    @Json(name = "type")
    val type: String? = null
)

