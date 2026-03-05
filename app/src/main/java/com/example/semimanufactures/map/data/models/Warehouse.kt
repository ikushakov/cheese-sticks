package com.example.semimanufactures.map.data.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Модель точки доставки (warehouse/delivery point)
 */
@JsonClass(generateAdapter = true)
data class Warehouse(
    @Json(name = "id")
    val id: Int, // В JSON это число, не строка
    @Json(name = "title")
    val title: String? = null,
    @Json(name = "address")
    val address: String? = null,
    @Json(name = "coordinate")
    val coordinate: List<Double>? = null, // [lat, lon] или null
    @Json(name = "assignment")
    val assignment: String? = null,
    @Json(name = "description")
    val description: String? = null,
    @Json(name = "ResponsibleEmployeeMdmcode")
    val responsibleEmployeeMdmcode: String? = null,
    @Json(name = "secondResponsibleEmployeeMdmcode")
    val secondResponsibleEmployeeMdmcode: String? = null,
    @Json(name = "PlannerMDMcode")
    val plannerMDMcode: String? = null,
    @Json(name = "departmentMdmcode")
    val departmentMdmcode: String? = null,
    @Json(name = "isActive")
    val isActive: Boolean? = null,
    @Json(name = "mdmKey")
    val mdmKey: String? = null
) {
    // Вычисляемые свойства для обратной совместимости
    val name: String? get() = title
    val coordinates: List<Double>? get() = coordinate
}

