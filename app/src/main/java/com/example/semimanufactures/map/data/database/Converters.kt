package com.example.semimanufactures.map.data.database

import androidx.room.TypeConverter
import com.example.semimanufactures.map.data.models.BuildingFloor
import com.example.semimanufactures.map.data.models.Employee
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

class Converters {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, String::class.java)
        val adapter = moshi.adapter<List<String>>(type)
        return adapter.toJson(list)
    }

    @TypeConverter
    fun fromDoubleList(value: String?): List<Double>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, Double::class.javaObjectType)
        val adapter = moshi.adapter<List<Double>>(type)
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun toDoubleList(list: List<Double>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, Double::class.javaObjectType)
        val adapter = moshi.adapter<List<Double>>(type)
        return adapter.toJson(list)
    }

    @TypeConverter
    fun fromNestedDoubleList(value: String?): List<List<Double>>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, Types.newParameterizedType(List::class.java, Double::class.javaObjectType))
        val adapter = moshi.adapter<List<List<Double>>>(type)
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun toNestedDoubleList(list: List<List<Double>>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, Types.newParameterizedType(List::class.java, Double::class.javaObjectType))
        val adapter = moshi.adapter<List<List<Double>>>(type)
        return adapter.toJson(list)
    }

    @TypeConverter
    fun fromBuildingFloorList(value: String?): List<BuildingFloor>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, BuildingFloor::class.java)
        val adapter = moshi.adapter<List<BuildingFloor>>(type)
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun toBuildingFloorList(list: List<BuildingFloor>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, BuildingFloor::class.java)
        val adapter = moshi.adapter<List<BuildingFloor>>(type)
        return adapter.toJson(list)
    }

    @TypeConverter
    fun fromEmployeeList(value: String?): List<Employee>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, Employee::class.java)
        val adapter = moshi.adapter<List<Employee>>(type)
        return adapter.fromJson(value)
    }

    @TypeConverter
    fun toEmployeeList(list: List<Employee>?): String? {
        if (list == null) return null
        val type = Types.newParameterizedType(List::class.java, Employee::class.java)
        val adapter = moshi.adapter<List<Employee>>(type)
        return adapter.toJson(list)
    }
}
