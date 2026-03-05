package com.example.semimanufactures.map.data.repository

import com.example.semimanufactures.map.data.api.MapApiClient
import com.example.semimanufactures.map.data.models.Building
import com.example.semimanufactures.map.data.models.Employee
import com.example.semimanufactures.map.data.models.Item
import com.example.semimanufactures.map.data.models.Warehouse
import com.example.semimanufactures.map.domain.repository.IMapRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import com.example.semimanufactures.map.data.models.FloorDepartment

import android.content.Context
import com.example.semimanufactures.map.data.database.MapDatabase
import com.example.semimanufactures.map.data.database.entities.BuildingEntity
import com.example.semimanufactures.map.data.database.entities.WarehouseEntity

/**
 * Репозиторий для работы с данными карты
 */
class MapRepository(context: Context) : IMapRepository {
    
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val database = MapDatabase.getDatabase(context)
    
    /**
     * Получить все здания
     */
    override suspend fun getBuildings(): Result<List<Building>> = withContext(Dispatchers.IO) {
        try {
            // Получаем данные с API
            val result = MapApiClient.get("api/map/buildings", String::class.java)
            
            val jsonString = result.getOrElse { error ->
                // Ошибка API, пробуем БД
                android.util.Log.e("MapRepository", "Error fetching buildings from API", error)
                return@withContext try {
                    val localEntities = database.buildingDao().getAll()
                    if (localEntities.isNotEmpty()) {
                        android.util.Log.w("MapRepository", "Using ${localEntities.size} buildings from database")
                        Result.success(localEntities.map { it.toDomain() })
                    } else {
                        Result.failure(error)
                    }
                } catch (dbError: Exception) {
                    Result.failure(error)
                }
            }
            
            // Логируем полный JSON для отладки
            android.util.Log.w("MapRepository", "=== FULL JSON RESPONSE ===")
            android.util.Log.w("MapRepository", "JSON length: ${jsonString.length}")
            // Логируем первые 5000 символов для анализа структуры
            android.util.Log.w("MapRepository", "JSON (first 5000 chars): ${jsonString.take(5000)}")
            if (jsonString.length > 5000) {
                android.util.Log.w("MapRepository", "... (truncated, total ${jsonString.length} chars)")
            }
            
            // Парсим JSON
            try {
                val buildingsFromApi = parseBuildingsFromJson(jsonString)
                android.util.Log.w("MapRepository", "=== PARSED ${buildingsFromApi.size} BUILDINGS ===")
                
                // Детальное логирование структуры первых зданий
                if (buildingsFromApi.isNotEmpty()) {
                    buildingsFromApi.take(3).forEachIndexed { index, building ->
                        android.util.Log.w("MapRepository", "Building[$index]: id=${building.id}, name=${building.name}")
                        val coords = building.coordinates
                        android.util.Log.w("MapRepository", "  - coordinates: ${if (coords != null) "YES (${coords.size} points)" else "NO"}")
                        android.util.Log.w("MapRepository", "  - address: ${building.address}")
                        android.util.Log.w("MapRepository", "  - floors: ${building.floors?.size ?: 0}")
                        
                        // Логируем координаты первого здания, если есть
                        if (index == 0 && coords != null && coords.isNotEmpty()) {
                            val firstCoords = coords.take(3)
                            android.util.Log.w("MapRepository", "  - first coordinates: $firstCoords")
                        }
                        
                        // Логируем структуру этажей и отделов
                        building.floors?.forEachIndexed { floorIndex, floor ->
                            android.util.Log.w("MapRepository", "    Floor[$floorIndex]: id=${floor.id}, number=${floor.number}, name=${floor.name}")
                            android.util.Log.w("MapRepository", "      - departments: ${floor.departments?.size ?: 0}")
                            floor.departments?.take(2)?.forEachIndexed { deptIndex, dept ->
                                android.util.Log.w("MapRepository", "        Dept[$deptIndex]: id=${dept.id}, name=${dept.name}, coords=${if (dept.coordinates != null) "YES" else "NO"}")
                            }
                        }
                    }
                } else {
                    android.util.Log.e("MapRepository", "=== NO BUILDINGS PARSED! ===")
                }
                
                // Сохраняем в БД
                if (buildingsFromApi.isNotEmpty()) {
                    val entities = buildingsFromApi.map { BuildingEntity.fromDomain(it) }
                    database.buildingDao().deleteAll()
                    database.buildingDao().insertAll(entities)
                    android.util.Log.w("MapRepository", "Saved ${entities.size} buildings to database")
                }
                
                Result.success(buildingsFromApi)
            } catch (parseError: Exception) {
                android.util.Log.e("MapRepository", "=== ERROR PARSING JSON ===", parseError)
                android.util.Log.e("MapRepository", "Error: ${parseError.message}")
                android.util.Log.e("MapRepository", "Stack trace: ${parseError.stackTraceToString()}")
                
                // Пробуем БД
                val localEntities = database.buildingDao().getAll()
                if (localEntities.isNotEmpty()) {
                    android.util.Log.w("MapRepository", "Using ${localEntities.size} buildings from database as fallback")
                    Result.success(localEntities.map { it.toDomain() })
                } else {
                    Result.failure(parseError)
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MapRepository", "Exception in getBuildings", e)
            try {
                val localEntities = database.buildingDao().getAll()
                if (localEntities.isNotEmpty()) {
                    Result.success(localEntities.map { it.toDomain() })
                } else {
                    Result.failure(e)
                }
            } catch (dbError: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Парсинг зданий из JSON с поддержкой различных структур
     */
    private fun parseBuildingsFromJson(jsonString: String): List<Building> {
        val listType = Types.newParameterizedType(List::class.java, Building::class.java)
        val adapter = moshi.adapter<List<Building>>(listType)
        
        // Попытка 1: Прямой массив зданий
        try {
            val jsonArray = org.json.JSONArray(jsonString)
            android.util.Log.w("MapRepository", "JSON is a direct array with ${jsonArray.length()} items")
            val buildings = adapter.fromJson(jsonString)
            if (buildings != null && buildings.isNotEmpty()) {
                android.util.Log.w("MapRepository", "Successfully parsed ${buildings.size} buildings as direct array")
                return buildings
            }
        } catch (e: Exception) {
            android.util.Log.d("MapRepository", "Not a direct array, trying object parsing", e)
        }
        
        // Попытка 2: Объект с различными ключами
        try {
            val jsonObject = org.json.JSONObject(jsonString)
            val keys = jsonObject.keys()
            val keyList = keys.asSequence().toList()
            android.util.Log.w("MapRepository", "JSON is an object with keys: ${keyList.joinToString(", ")}")
            
            // Анализируем каждый ключ
            for (key in keyList) {
                val value = jsonObject.get(key)
                android.util.Log.w("MapRepository", "Key '$key': type=${value.javaClass.simpleName}")
                
                if (value is org.json.JSONArray) {
                    android.util.Log.w("MapRepository", "  Array length: ${value.length()}")
                    if (value.length() > 0) {
                        val firstItem = value.get(0)
                        if (firstItem is org.json.JSONObject) {
                            val itemKeys = firstItem.keys().asSequence().toList()
                            android.util.Log.w("MapRepository", "  First item keys: ${itemKeys.joinToString(", ")}")
                        }
                    }
                }
            }
            
            // Пробуем найти массив зданий по различным ключам
            val possibleKeys = listOf("buildings", "data", "items", "results", "content")
            for (key in possibleKeys) {
                if (jsonObject.has(key)) {
                    android.util.Log.w("MapRepository", "Trying key: '$key'")
                    try {
                        val value = jsonObject.get(key)
                        when {
                            value is org.json.JSONArray -> {
                                val buildingsJson = value.toString()
                                val buildings = adapter.fromJson(buildingsJson)
                                if (buildings != null && buildings.isNotEmpty()) {
                                    android.util.Log.w("MapRepository", "Successfully parsed ${buildings.size} buildings from key '$key'")
                                    return buildings
                                }
                            }
                            value is org.json.JSONObject -> {
                                // Вложенный объект, ищем внутри него
                                val nestedKeys = value.keys().asSequence().toList()
                                android.util.Log.w("MapRepository", "  Nested object keys: ${nestedKeys.joinToString(", ")}")
                                for (nestedKey in nestedKeys) {
                                    if (nestedKey.lowercase().contains("building") || nestedKey.lowercase().contains("item")) {
                                        val nestedValue = value.get(nestedKey)
                                        if (nestedValue is org.json.JSONArray) {
                                            val buildingsJson = nestedValue.toString()
                                            val buildings = adapter.fromJson(buildingsJson)
                                            if (buildings != null && buildings.isNotEmpty()) {
                                                android.util.Log.w("MapRepository", "Successfully parsed ${buildings.size} buildings from key '$key.$nestedKey'")
                                                return buildings
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.d("MapRepository", "Failed to parse from key '$key'", e)
                    }
                }
            }
            
            // Пробуем найти любой массив, который может содержать здания
            for (key in keyList) {
                val value = jsonObject.get(key)
                if (value is org.json.JSONArray && value.length() > 0) {
                    val firstItem = value.get(0)
                    if (firstItem is org.json.JSONObject) {
                        val itemKeys = firstItem.keys().asSequence().toList()
                        // Проверяем, похоже ли это на здание (есть id, name, coordinates и т.д.)
                        if (itemKeys.any { it.lowercase() == "id" } && 
                            (itemKeys.any { it.lowercase().contains("coord") } || 
                             itemKeys.any { it.lowercase() == "floors" } ||
                             itemKeys.any { it.lowercase() == "name" })) {
                            android.util.Log.w("MapRepository", "Trying key '$key' as potential buildings array")
                            try {
                                val buildingsJson = value.toString()
                                val buildings = adapter.fromJson(buildingsJson)
                                if (buildings != null && buildings.isNotEmpty()) {
                                    android.util.Log.w("MapRepository", "Successfully parsed ${buildings.size} buildings from key '$key'")
                                    return buildings
                                }
                            } catch (e: Exception) {
                                android.util.Log.d("MapRepository", "Failed to parse from key '$key'", e)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("MapRepository", "Error parsing JSON object", e)
        }
        
        android.util.Log.e("MapRepository", "Could not parse buildings from JSON")
        return emptyList()
    }
    
    /**
     * Получить все точки доставки (Временно отключено)
     */
    override suspend fun getDeliveryPoints(): Result<List<Warehouse>> = Result.success(emptyList())
    /*
    override suspend fun getDeliveryPoints(): Result<List<Warehouse>> = withContext(Dispatchers.IO) {
        try {
            val result = MapApiClient.get("api/map/deliveries/post", String::class.java)
            
            val warehousesFromApi = result.mapCatching { jsonString ->
                val listType = Types.newParameterizedType(List::class.java, Warehouse::class.java)
                val adapter = moshi.adapter<List<Warehouse>>(listType)
                adapter.fromJson(jsonString) ?: emptyList()
            }.getOrNull()

            if (warehousesFromApi != null) {
                // Сохраняем в БД
                val entities = warehousesFromApi.map { WarehouseEntity.fromDomain(it) }
                database.warehouseDao().deleteAll()
                database.warehouseDao().insertAll(entities)
                
                Result.success(warehousesFromApi)
            } else {
                // Из БД
                val localEntities = database.warehouseDao().getAll()
                if (localEntities.isNotEmpty()) {
                    Result.success(localEntities.map { it.toDomain() })
                } else {
                    result.map { emptyList() }
                }
            }
        } catch (e: Exception) {
            try {
                val localEntities = database.warehouseDao().getAll()
                if (localEntities.isNotEmpty()) {
                    Result.success(localEntities.map { it.toDomain() })
                } else {
                    Result.failure(e)
                }
            } catch (dbError: Exception) {
                Result.failure(e)
            }
        }
    }
    */
    
    /**
     * Получить всех сотрудников (Временно отключено)
     */
    override suspend fun getEmployees(): Result<List<Employee>> = Result.success(emptyList())
    /*
    override suspend fun getEmployees(): Result<List<Employee>> = withContext(Dispatchers.IO) {
        try {
            val result = MapApiClient.get("api/map/employee", String::class.java)
            result.mapCatching { jsonString ->
                val listType = Types.newParameterizedType(List::class.java, Employee::class.java)
                val adapter = moshi.adapter<List<Employee>>(listType)
                adapter.fromJson(jsonString) ?: emptyList()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    */
    
    /**
     * Получить все предметы (Временно отключено)
     */
    override suspend fun getItems(): Result<List<Item>> = Result.success(emptyList())
    /*
    override suspend fun getItems(): Result<List<Item>> = withContext(Dispatchers.IO) {
        try {
            val result = MapApiClient.get("api/map/building/item", String::class.java)
            result.mapCatching { jsonString ->
                val listType = Types.newParameterizedType(List::class.java, Item::class.java)
                val adapter = moshi.adapter<List<Item>>(listType)
                adapter.fromJson(jsonString) ?: emptyList()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    */
    
    /**
     * Получить все подразделения (Временно отключено)
     */
    override suspend fun getDepartments(): Result<List<FloorDepartment>> = Result.success(emptyList())
    /*
    override suspend fun getDepartments(): Result<List<FloorDepartment>> = withContext(Dispatchers.IO) {
        try {
            val result = MapApiClient.get("api/map/departments", String::class.java)
            result.mapCatching { jsonString ->
                val listType = Types.newParameterizedType(List::class.java, FloorDepartment::class.java)
                val adapter = moshi.adapter<List<FloorDepartment>>(listType)
                adapter.fromJson(jsonString) ?: emptyList()
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    */

    // --- Buildings ---
    
    override suspend fun addBuilding(building: Map<String, Any>): Result<String> {
        return MapApiClient.post("api/map/buildings", building, String::class.java)
    }

    override suspend fun updateBuilding(building: Map<String, Any>): Result<String> {
         // Предполагаем, что endpoint такой же, как при создании, но метод PUT, или есть ID.
         // В roadmap не указан конкретный endpoint для обновления здания, обычно это PUT /map/buildings
         // Но нужно быть осторожным. Предположим стандартный REST.
         // В roadmap написано: `/map/buildings - CRUD operations`
        return MapApiClient.put("api/map/buildings", building, String::class.java)
    }

    override suspend fun deleteBuilding(id: String): Result<String> {
        return MapApiClient.delete("api/map/buildings?id=$id", String::class.java)
    }

    // --- Floors ---

    override suspend fun addFloor(floor: Map<String, Any>): Result<String> = Result.success("Temporarily disabled")
    /*
    override suspend fun addFloor(floor: Map<String, Any>): Result<String> {
        return MapApiClient.post("api/map/building/floor", floor, String::class.java)
    }
    */

    override suspend fun deleteFloor(id: String): Result<String> = Result.success("Temporarily disabled")
    /*
    override suspend fun deleteFloor(id: String): Result<String> {
        return MapApiClient.delete("api/map/building/floor?id=$id", String::class.java)
    }
    */

    // --- Departments ---

    override suspend fun addDepartment(department: Map<String, Any>): Result<String> = Result.success("Temporarily disabled")
    /*
    override suspend fun addDepartment(department: Map<String, Any>): Result<String> {
        return MapApiClient.post("api/map/building/floor/department", department, String::class.java)
    }
    */

    override suspend fun deleteDepartment(id: String): Result<String> = Result.success("Temporarily disabled")
    /*
    override suspend fun deleteDepartment(id: String): Result<String> {
        return MapApiClient.delete("api/map/building/floor/department?id=$id", String::class.java)
    }
    */
    
    /**
     * Добавить точку доставки
     */
    override suspend fun addDeliveryPoint(warehouse: Map<String, Any>): Result<String> = Result.success("Temporarily disabled")
    /*
    override suspend fun addDeliveryPoint(warehouse: Map<String, Any>): Result<String> {
        return MapApiClient.post("api/map/deliveries/post", warehouse, String::class.java)
    }
    */
    
    /**
     * Обновить точку доставки
     */
    override suspend fun updateDeliveryPoint(warehouse: Map<String, Any>): Result<String> = Result.success("Temporarily disabled")
    /*
    override suspend fun updateDeliveryPoint(warehouse: Map<String, Any>): Result<String> {
        return MapApiClient.put("api/map/deliveries/post", warehouse, String::class.java)
    }
    */
    
    /**
     * Удалить точку доставки
     */
    override suspend fun deleteDeliveryPoint(id: String): Result<String> = Result.success("Temporarily disabled")
    /*
    override suspend fun deleteDeliveryPoint(id: String): Result<String> {
        return MapApiClient.delete("api/map/deliveries/post?id=$id", String::class.java)
    }
    */
}

