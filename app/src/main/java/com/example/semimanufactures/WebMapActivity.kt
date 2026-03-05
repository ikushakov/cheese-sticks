package com.example.semimanufactures

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.example.semimanufactures.map.data.models.Building as MapBuilding
import com.example.semimanufactures.map.data.models.Warehouse
import com.example.semimanufactures.map.data.repository.MapRepository
import kotlinx.coroutines.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.BoundingBox
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.*
import org.osmdroid.views.overlay.compass.CompassOverlay
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

import android.widget.TextView
import androidx.cardview.widget.CardView

// Вспомогательные модели для отображения на карте
data class BuildingDisplay(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val type: String,
    val address: String? = null,
    val coordinates: List<List<Double>>? = null // Полигон здания
)

data class DeliveryPointDisplay(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val capacity: Int? = null,
    val status: String? = null,
    val buildingId: String? = null // ID здания, к которому относится склад
)

data class RoutePoint(
    val id: String,
    val sequence: Int,
    val lat: Double,
    val lon: Double,
    val name: String? = null
)

data class Route(
    val points: List<GeoPoint>,
    val distance: Double, // Расстояние в метрах
    val duration: Int? = null // Продолжительность в секундах (опционально)
)

class WebMapActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnBuildings: Button
    private lateinit var btnDelivery: Button
    private lateinit var btnRoute: Button
    private lateinit var fabMyLocation: FloatingActionButton
    private lateinit var btnZoomIn: Button
    private lateinit var btnZoomOut: Button
    
    // Инфо-панель
    private lateinit var infoPanel: CardView
    private lateinit var infoTitle: TextView
    private lateinit var infoSubtitle: TextView
    private lateinit var btnCloseInfo: Button
    private lateinit var btnRouteTo: Button

    // Состояние маркеров
    private var lastSelectedMarker: Marker? = null
    private var lastSelectedMarkerIcon: Drawable? = null
    
    // Состояние маршрута
    private var routeFromMarker: Marker? = null
    private var routeToMarker: Marker? = null
    private var routeFromIcon: Drawable? = null
    private var routeToIcon: Drawable? = null
    private var currentRoute: Route? = null
    private var routePolyline: Polyline? = null

    // Overlays
    private lateinit var locationOverlay: MyLocationNewOverlay
    private lateinit var buildingsOverlay: FolderOverlay
    private lateinit var deliveryOverlay: FolderOverlay
    private lateinit var routeOverlay: FolderOverlay

    // Данные
    private val buildings = mutableListOf<BuildingDisplay>()
    private val deliveryPoints = mutableListOf<DeliveryPointDisplay>()
    private val routePoints = mutableListOf<RoutePoint>()

    // Состояние слоев
    private var buildingsVisible = true
    private var deliveryVisible = true
    private var routeVisible = true

    // Координаты центра (пример - Москва)
    private val defaultCenter = GeoPoint(55.7558, 37.6173)
    private val defaultZoom = 15.0

    // Репозиторий для работы с API
    private lateinit var mapRepository: MapRepository
    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    // Разрешения
    private val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val LOCATION_PERMISSION_REQUEST = 1001

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, WebMapActivity::class.java)
            context.startActivity(intent)
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webmap)

        // Инициализация osmdroid
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = packageName

        // Инициализация UI
        mapView = findViewById(R.id.mapView)
        progressBar = findViewById(R.id.progressBar)
        btnBuildings = findViewById(R.id.btn_buildings)
        btnDelivery = findViewById(R.id.btn_delivery)
        btnRoute = findViewById(R.id.btn_route)
        fabMyLocation = findViewById(R.id.fab_my_location)
        btnZoomIn = findViewById(R.id.btn_zoom_in)
        btnZoomOut = findViewById(R.id.btn_zoom_out)
        
        infoPanel = findViewById(R.id.info_panel)
        infoTitle = findViewById(R.id.info_title)
        infoSubtitle = findViewById(R.id.info_subtitle)
        btnCloseInfo = findViewById(R.id.btn_close_info)
        btnRouteTo = findViewById(R.id.btn_route_to)

        mapRepository = MapRepository(applicationContext)

        setupMap()
        setupButtons()
        setupOverlays()

        // Запрашиваем локацию сразу при старте
        if (checkLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }

        // Загружаем данные с API
        loadTestData()
    }

    private fun startLocationUpdates() {
        locationOverlay.enableMyLocation()
        // Не включаем автоматическое следование за местоположением,
        // так как карта будет центрироваться на объектах
        locationOverlay.disableFollowLocation()
    }

    private fun setupMap() {
        // Настройка карты - используем нейтральный стиль
        // Для нейтрального стиля используем стандартный тайл-сет, но настроим фон
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        
        // Устанавливаем нейтральный фон карты (темно-серый)
        mapView.setBackgroundColor(Color.rgb(45, 45, 45))

        // Начальная позиция (будет переопределена после загрузки данных)
        val mapController = mapView.controller
        mapController.setZoom(defaultZoom)
        mapController.setCenter(defaultCenter)

        // Компас
        val compassOverlay = CompassOverlay(this, InternalCompassOrientationProvider(this), mapView)
        compassOverlay.enableCompass()
        mapView.overlays.add(compassOverlay)

        // Вращение жестами
        val rotationGestureOverlay = RotationGestureOverlay(mapView)
        rotationGestureOverlay.isEnabled = true
        mapView.overlays.add(rotationGestureOverlay)

        // Масштабирование
        mapView.setBuiltInZoomControls(false)
        mapView.setMultiTouchControls(true)
    }

    private fun setupOverlays() {
        // Слой местоположения
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), mapView)
        locationOverlay.enableMyLocation()
        locationOverlay.enableFollowLocation()
        mapView.overlays.add(locationOverlay)

        // Слои для данных
        buildingsOverlay = FolderOverlay()
        deliveryOverlay = FolderOverlay()
        routeOverlay = FolderOverlay()

        mapView.overlays.add(buildingsOverlay)
        mapView.overlays.add(deliveryOverlay)
        mapView.overlays.add(routeOverlay)

        // Обновляем карту
        mapView.invalidate()
    }

    private fun setupButtons() {
        // Цвета кнопок
        btnBuildings.setBackgroundColor(Color.parseColor("#4CAF50"))
        btnDelivery.setBackgroundColor(Color.parseColor("#2196F3"))
        btnRoute.setBackgroundColor(Color.parseColor("#FF9800"))

        btnBuildings.setTextColor(Color.WHITE)
        btnDelivery.setTextColor(Color.WHITE)
        btnRoute.setTextColor(Color.WHITE)
        
        // Обработчики
        btnBuildings.setOnClickListener { toggleBuildingsLayer() }
        btnDelivery.setOnClickListener { toggleDeliveryLayer() }
        btnRoute.setOnClickListener { toggleRouteLayer() }
        fabMyLocation.setOnClickListener { centerOnMyLocation() }
        fabMyLocation.setOnLongClickListener {
            // Длинный клик - использовать текущее местоположение как точку отправления
            useMyLocationAsOrigin()
            true
        }
        
        btnZoomIn.setOnClickListener {
            mapView.controller.zoomIn()
        }
        
        btnZoomOut.setOnClickListener {
            mapView.controller.zoomOut()
        }
        
        btnCloseInfo.setOnClickListener {
            // Сбрасываем выделение при закрытии
            resetMarkerSelection()
            // Очищаем маршрут при закрытии панели
            clearRoute()
            infoPanel.visibility = android.view.View.GONE
        }
        
        btnRouteTo.setOnClickListener {
            // Устанавливаем точку назначения
            if (lastSelectedMarker != null) {
                setRouteDestination(lastSelectedMarker!!)
            }
        }
    }

    private fun loadTestData() {
        coroutineScope.launch {
            showLoading(true)

            try {
                // Загружаем данные с API
                loadBuildings()
                // Маршруты пока оставляем пустыми (будут реализованы позже)
                routePoints.clear()

                // Отображаем на карте
                displayDataOnMap()
                
                // Центрируем карту на загруженных объектах
                centerMapOnObjects()

                val buildingsCount = buildings.size
                val pointsCount = deliveryPoints.size

                Toast.makeText(
                    this@WebMapActivity,
                    "Загружено: $buildingsCount зданий, $pointsCount складов",
                    Toast.LENGTH_LONG
                ).show()

            } catch (e: Exception) {
                Log.e("WebMapActivity", "Error loading data", e)
                Toast.makeText(
                    this@WebMapActivity,
                    "Ошибка загрузки данных: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                showLoading(false)
            }
        }
    }

    private suspend fun loadBuildings() = withContext(Dispatchers.IO) {
        try {
            val result = mapRepository.getBuildings()
            result.onSuccess { buildingsList ->
                buildings.clear()
                deliveryPoints.clear() // Очищаем склады перед загрузкой
                Log.d("WebMapActivity", "Loaded ${buildingsList.size} buildings from repository")
                
                // Конвертируем данные из API в формат для отображения
                var buildingsWithCoordinates = 0
                var buildingsWithoutCoordinates = 0
                var warehousesCount = 0
                
                buildingsList.forEach { building ->
                    val polygon = building.coordinates
                    if (polygon != null && polygon.isNotEmpty()) {
                        try {
                            // Вычисляем центр полигона (простое среднее арифметическое)
                            val centerLat = polygon.map { it[0] }.average()
                            val centerLon = polygon.map { it[1] }.average()
                            
                            buildings.add(
                                BuildingDisplay(
                                    id = building.id,
                                    name = building.name ?: "Здание ${building.id}",
                                    lat = centerLat,
                                    lon = centerLon,
                                    type = "building", // Тип по умолчанию
                                    address = building.address,
                                    coordinates = polygon
                                )
                            )
                            buildingsWithCoordinates++
                        } catch (e: Exception) {
                            Log.e("WebMapActivity", "Error processing building ${building.id} coordinates", e)
                            buildingsWithoutCoordinates++
                        }
                    } else {
                        buildingsWithoutCoordinates++
                        Log.d("WebMapActivity", "Building ${building.id} (${building.name}) has no coordinates")
                    }
                    
                    // Извлекаем склады из этажей здания
                    // Склады должны отображаться только для зданий с полигонами
                    if (polygon != null && polygon.isNotEmpty()) {
                        building.floors?.forEach { floor ->
                            floor.warehouses?.forEach { warehouse ->
                                val coords = warehouse.coordinates
                                if (coords != null && coords.size >= 2) {
                                    // В JSON координаты складов в формате [lon, lat], а не [lat, lon]
                                    // Поэтому меняем местами
                                    val lat = coords[1]  // Вторая координата - это широта
                                    val lon = coords[0]  // Первая координата - это долгота
                                    
                                    // Проверяем, что координаты валидны
                                    if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                                        deliveryPoints.add(
                                            DeliveryPointDisplay(
                                                id = warehouse.id.toString(),
                                                name = warehouse.name ?: "Склад ${warehouse.id}",
                                                lat = lat,
                                                lon = lon,
                                                capacity = null,
                                                status = null,
                                                buildingId = building.id // Связываем склад с зданием
                                            )
                                        )
                                        warehousesCount++
                                        Log.d("WebMapActivity", "Added warehouse ${warehouse.id} at ($lat, $lon) for building ${building.id}")
                                    } else {
                                        Log.w("WebMapActivity", "Invalid warehouse coordinates: [${coords[0]}, ${coords[1]}] for warehouse ${warehouse.id}")
                                    }
                                } else {
                                    Log.d("WebMapActivity", "Warehouse ${warehouse.id} has no coordinates")
                                }
                            }
                        }
                    }
                }
                
                Log.d("WebMapActivity", "Processed buildings: $buildingsWithCoordinates with coordinates, $buildingsWithoutCoordinates without")
                Log.d("WebMapActivity", "Processed warehouses: $warehousesCount")
                
                if (buildingsList.isNotEmpty() && buildings.isEmpty()) {
                    Log.w("WebMapActivity", "All ${buildingsList.size} buildings have no coordinates!")
                }
            }.onFailure { error ->
                Log.e("WebMapActivity", "Error loading buildings", error)
                buildings.clear()
                deliveryPoints.clear()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@WebMapActivity, 
                        "Ошибка загрузки зданий: ${error.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } catch (e: Exception) {
            Log.e("WebMapActivity", "Exception loading buildings", e)
            buildings.clear()
            deliveryPoints.clear()
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    this@WebMapActivity,
                    "Исключение при загрузке зданий: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun loadDeliveryPoints() = withContext(Dispatchers.IO) {
        try {
            val result = mapRepository.getDeliveryPoints()
            result.onSuccess { warehousesList ->
                deliveryPoints.clear()
                warehousesList.forEach { warehouse ->
                    val coords = warehouse.coordinates
                    if (coords != null && coords.size >= 2) {
                        // В JSON координаты складов в формате [lon, lat], а не [lat, lon]
                        val lat = coords[1]  // Вторая координата - это широта
                        val lon = coords[0]  // Первая координата - это долгота
                        
                        // Проверяем, что координаты валидны
                        if (lat in -90.0..90.0 && lon in -180.0..180.0) {
                            deliveryPoints.add(
                                DeliveryPointDisplay(
                                    id = warehouse.id.toString(),
                                    name = warehouse.name ?: "Точка доставки ${warehouse.id}",
                                    lat = lat,
                                    lon = lon,
                                    capacity = null,
                                    status = null,
                                    buildingId = null // Неизвестно, к какому зданию относится
                                )
                            )
                        }
                    }
                }
            }.onFailure { error ->
                Log.e("WebMapActivity", "Error loading delivery points", error)
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@WebMapActivity, 
                        "Ошибка складов: ${error.message}", 
                        Toast.LENGTH_LONG
                    ).show()
                }
                throw error
            }
        } catch (e: Exception) {
            Log.e("WebMapActivity", "Exception loading delivery points", e)
            // В случае ошибки оставляем список пустым
            deliveryPoints.clear()
        }
    }

    private fun displayDataOnMap() {
        runOnUiThread {
            // Очищаем слои
            buildingsOverlay.items.clear()
            deliveryOverlay.items.clear()
            routeOverlay.items.clear()

            // Здания
            if (buildingsVisible) {
                buildings.forEach { building ->
                    // Добавляем маркер для здания
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(building.lat, building.lon)
                        title = building.name
                        snippet = building.address ?: "Здание"

                        // Иконка для здания - темно-синий, как на картинке
                        setIcon(ContextCompat.getDrawable(this@WebMapActivity, android.R.drawable.ic_menu_mylocation))
                        icon?.setTint(Color.rgb(0, 0, 139)) // Темно-синий

                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { marker, mapView ->
                            handleMarkerClick(marker as Marker)
                            true
                        }
                    }
                    buildingsOverlay.add(marker)
                    
                    // Добавляем полигон здания, если есть координаты
                    building.coordinates?.let { coords ->
                        if (coords.isNotEmpty()) {
                            val polygon = Polygon().apply {
                                val polygonPoints = coords.map { GeoPoint(it[0], it[1]) }
                                polygonPoints.forEach { addPoint(it) }
                                // Светло-синий заливка с темно-синим контуром, как на картинке
                                fillColor = Color.argb(120, 135, 206, 250) // Светло-синий (Sky Blue) с прозрачностью
                                strokeColor = Color.rgb(0, 0, 139) // Темно-синий контур (Dark Blue)
                                strokeWidth = 3.0f // Толщина контура
                            }
                            buildingsOverlay.add(polygon)
                        }
                    }
                }
            }

            // Точки доставки (склады)
            if (deliveryVisible) {
                deliveryPoints.forEach { point ->
                    val marker = Marker(mapView).apply {
                        position = GeoPoint(point.lat, point.lon)
                        title = point.name
                        snippet = if (point.buildingId != null) "Склад (здание: ${point.buildingId})" else "Склад"

                        // Иконка для точки доставки - яркий цвет для контраста
                        setIcon(ContextCompat.getDrawable(this@WebMapActivity, android.R.drawable.ic_menu_directions))
                        icon?.setTint(Color.rgb(255, 0, 255)) // Яркий фиолетовый/магента

                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        setOnMarkerClickListener { marker, mapView ->
                            handleMarkerClick(marker as Marker)
                            true
                        }
                    }
                    deliveryOverlay.add(marker)
                }
            }

            // Обновляем карту
            mapView.invalidate()
        }
    }

    private fun toggleBuildingsLayer() {
        buildingsVisible = !buildingsVisible
        btnBuildings.setBackgroundColor(
            if (buildingsVisible) Color.parseColor("#4CAF50") else Color.GRAY
        )
        displayDataOnMap()
        Toast.makeText(this, "Слой зданий: ${if (buildingsVisible) "вкл" else "выкл"}", Toast.LENGTH_SHORT).show()
    }

    private fun toggleDeliveryLayer() {
        deliveryVisible = !deliveryVisible
        btnDelivery.setBackgroundColor(
            if (deliveryVisible) Color.parseColor("#2196F3") else Color.GRAY
        )
        displayDataOnMap()
        Toast.makeText(this, "Слой складов: ${if (deliveryVisible) "вкл" else "выкл"}", Toast.LENGTH_SHORT).show()
    }

    private fun toggleRouteLayer() {
        routeVisible = !routeVisible
        btnRoute.setBackgroundColor(
            if (routeVisible) Color.parseColor("#FF9800") else Color.GRAY
        )
        
        // Показываем/скрываем маршрут
        routePolyline?.let { polyline ->
            if (routeVisible) {
                if (!routeOverlay.items.contains(polyline)) {
                    routeOverlay.add(polyline)
                }
            } else {
                routeOverlay.items.remove(polyline)
            }
            Unit // Явно возвращаем Unit для let
        }
        
        mapView.invalidate()
        Toast.makeText(this, "Слой маршрута: ${if (routeVisible) "вкл" else "выкл"}", Toast.LENGTH_SHORT).show()
    }

    private fun resetMarkerSelection() {
        // Не сбрасываем маркеры маршрута при обычном сбросе выделения
        if (lastSelectedMarker != routeFromMarker && lastSelectedMarker != routeToMarker) {
            lastSelectedMarker?.let { marker ->
                lastSelectedMarkerIcon?.let { icon ->
                    marker.icon = icon
                }
            }
        }
        lastSelectedMarker = null
        lastSelectedMarkerIcon = null
        mapView.invalidate()
    }

    private fun handleMarkerClick(marker: Marker) {
        if (marker == lastSelectedMarker) {
            // Двойной клик - устанавливаем точку отправления
            setRouteOrigin(marker)
            return
        }

        resetMarkerSelection()

        lastSelectedMarker = marker
        lastSelectedMarkerIcon = marker.icon

        marker.icon?.let { currentIcon ->
            val selectedIcon = currentIcon.constantState?.newDrawable()?.mutate()
            selectedIcon?.setTint(Color.RED)
            marker.icon = selectedIcon
        }

        mapView.invalidate()
        showMarkerInfo(marker.title, marker.snippet)
        mapView.controller.animateTo(marker.position)
    }
    
    /**
     * Установить точку отправления маршрута
     */
    private fun setRouteOrigin(marker: Marker) {
        // Сбрасываем предыдущую точку отправления
        routeFromMarker?.let { oldMarker ->
            routeFromIcon?.let { icon ->
                oldMarker.icon = icon
            }
        }
        
        routeFromMarker = marker
        routeFromIcon = marker.icon?.constantState?.newDrawable()?.mutate()
        
        // Выделяем точку отправления синим цветом
        marker.icon?.let { currentIcon ->
            val originIcon = currentIcon.constantState?.newDrawable()?.mutate()
            originIcon?.setTint(Color.BLUE)
            marker.icon = originIcon
        }
        
        mapView.invalidate()
        
        // Если есть точка назначения, строим маршрут
        if (routeToMarker != null) {
            buildRoute()
        } else {
            Toast.makeText(this, "Точка отправления установлена. Выберите точку назначения.", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Установить точку назначения маршрута
     */
    private fun setRouteDestination(marker: Marker) {
        // Сбрасываем предыдущую точку назначения
        routeToMarker?.let { oldMarker ->
            routeToIcon?.let { icon ->
                oldMarker.icon = icon
            }
        }
        
        routeToMarker = marker
        routeToIcon = marker.icon?.constantState?.newDrawable()?.mutate()
        
        // Выделяем точку назначения зеленым цветом
        marker.icon?.let { currentIcon ->
            val destIcon = currentIcon.constantState?.newDrawable()?.mutate()
            destIcon?.setTint(Color.GREEN)
            marker.icon = destIcon
        }
        
        mapView.invalidate()
        
        // Если есть точка отправления, строим маршрут
        if (routeFromMarker != null) {
            buildRoute()
        } else {
            Toast.makeText(this, "Точка назначения установлена. Выберите точку отправления (двойной клик).", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Построить маршрут между выбранными точками
     */
    private fun buildRoute() {
        val from = routeFromMarker?.position
        val to = routeToMarker?.position
        
        if (from == null || to == null) {
            Toast.makeText(this, "Выберите точки отправления и назначения", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Строим маршрут с учетом препятствий (зданий)
        val route = calculateRoute(from, to)
        currentRoute = route
        
        // Отображаем маршрут на карте
        displayRoute(route)
        
        // Показываем информацию о маршруте
        showRouteInfo(route)
    }
    
    /**
     * Вычислить маршрут между двумя точками с учетом препятствий
     */
    private fun calculateRoute(from: GeoPoint, to: GeoPoint): Route {
        // Упрощенный алгоритм: прямая линия с обходом зданий
        val routePoints = mutableListOf<GeoPoint>()
        
        // Добавляем точку отправления
        routePoints.add(from)
        
        // Проверяем, пересекает ли прямая линия какие-либо здания
        val directPath = listOf(from, to)
        val obstacles = findObstaclesOnPath(directPath)
        
        if (obstacles.isEmpty()) {
            // Прямой маршрут без препятствий
            routePoints.add(to)
        } else {
            // Обходим препятствия
            val detourPoints = calculateDetour(from, to, obstacles)
            routePoints.addAll(detourPoints)
            routePoints.add(to)
        }
        
        // Вычисляем расстояние маршрута
        val distance = calculateRouteDistance(routePoints)
        
        return Route(
            points = routePoints,
            distance = distance,
            duration = null
        )
    }
    
    /**
     * Найти препятствия (здания) на пути
     */
    private fun findObstaclesOnPath(path: List<GeoPoint>): List<BuildingDisplay> {
        val obstacles = mutableListOf<BuildingDisplay>()
        
        buildings.forEach { building ->
            building.coordinates?.let { polygon ->
                if (pathIntersectsPolygon(path, polygon)) {
                    obstacles.add(building)
                }
            }
        }
        
        return obstacles
    }
    
    /**
     * Проверить, пересекает ли путь полигон здания
     */
    private fun pathIntersectsPolygon(path: List<GeoPoint>, polygon: List<List<Double>>): Boolean {
        if (path.size < 2 || polygon.size < 3) return false
        
        val pathStart = path[0]
        val pathEnd = path[path.size - 1]
        
        // Проверяем, находится ли путь внутри полигона или пересекает его
        for (i in 0 until polygon.size - 1) {
            val p1 = GeoPoint(polygon[i][0], polygon[i][1])
            val p2 = GeoPoint(polygon[i + 1][0], polygon[i + 1][1])
            
            if (linesIntersect(pathStart, pathEnd, p1, p2)) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Проверить, пересекаются ли два отрезка
     */
    private fun linesIntersect(p1: GeoPoint, p2: GeoPoint, p3: GeoPoint, p4: GeoPoint): Boolean {
        val o1 = orientation(p1, p2, p3)
        val o2 = orientation(p1, p2, p4)
        val o3 = orientation(p3, p4, p1)
        val o4 = orientation(p3, p4, p2)
        
        return (o1 != o2 && o3 != o4) ||
               (o1 == 0 && onSegment(p1, p3, p2)) ||
               (o2 == 0 && onSegment(p1, p4, p2)) ||
               (o3 == 0 && onSegment(p3, p1, p4)) ||
               (o4 == 0 && onSegment(p3, p2, p4))
    }
    
    /**
     * Вычислить ориентацию трех точек
     */
    private fun orientation(p1: GeoPoint, p2: GeoPoint, p3: GeoPoint): Int {
        val val1 = (p2.longitude - p1.longitude) * (p3.latitude - p2.latitude)
        val val2 = (p2.latitude - p1.latitude) * (p3.longitude - p2.longitude)
        val result = val1 - val2
        
        return when {
            result > 0 -> 1  // По часовой стрелке
            result < 0 -> -1 // Против часовой стрелки
            else -> 0        // Коллинеарны
        }
    }
    
    /**
     * Проверить, находится ли точка на отрезке
     */
    private fun onSegment(p1: GeoPoint, p2: GeoPoint, p3: GeoPoint): Boolean {
        return p2.longitude <= maxOf(p1.longitude, p3.longitude) &&
               p2.longitude >= minOf(p1.longitude, p3.longitude) &&
               p2.latitude <= maxOf(p1.latitude, p3.latitude) &&
               p2.latitude >= minOf(p1.latitude, p3.latitude)
    }
    
    /**
     * Вычислить обход препятствий
     */
    private fun calculateDetour(from: GeoPoint, to: GeoPoint, obstacles: List<BuildingDisplay>): List<GeoPoint> {
        val detourPoints = mutableListOf<GeoPoint>()
        
        // Упрощенный алгоритм: обходим каждое препятствие по его границе
        obstacles.forEach { obstacle ->
            obstacle.coordinates?.let { polygon ->
                // Находим ближайшую точку на границе полигона к началу пути
                val nearestPoint = findNearestPointOnPolygon(from, polygon)
                if (nearestPoint != null) {
                    detourPoints.add(nearestPoint)
                }
            }
        }
        
        return detourPoints
    }
    
    /**
     * Найти ближайшую точку на полигоне к заданной точке
     */
    private fun findNearestPointOnPolygon(point: GeoPoint, polygon: List<List<Double>>): GeoPoint? {
        if (polygon.isEmpty()) return null
        
        var minDistance = Double.MAX_VALUE
        var nearestPoint: GeoPoint? = null
        
        for (i in 0 until polygon.size - 1) {
            val p1 = GeoPoint(polygon[i][0], polygon[i][1])
            val p2 = GeoPoint(polygon[i + 1][0], polygon[i + 1][1])
            
            val nearestOnSegment = findNearestPointOnSegment(point, p1, p2)
            val distance = calculateDistance(point, nearestOnSegment)
            
            if (distance < minDistance) {
                minDistance = distance
                nearestPoint = nearestOnSegment
            }
        }
        
        return nearestPoint
    }
    
    /**
     * Найти ближайшую точку на отрезке к заданной точке
     */
    private fun findNearestPointOnSegment(point: GeoPoint, segmentStart: GeoPoint, segmentEnd: GeoPoint): GeoPoint {
        val dx = segmentEnd.longitude - segmentStart.longitude
        val dy = segmentEnd.latitude - segmentStart.latitude
        val lengthSquared = dx * dx + dy * dy
        
        if (lengthSquared == 0.0) return segmentStart
        
        val t = maxOf(0.0, minOf(1.0, 
            ((point.longitude - segmentStart.longitude) * dx + 
             (point.latitude - segmentStart.latitude) * dy) / lengthSquared))
        
        return GeoPoint(
            segmentStart.latitude + t * dy,
            segmentStart.longitude + t * dx
        )
    }
    
    /**
     * Вычислить расстояние между двумя точками в метрах (формула гаверсинуса)
     */
    private fun calculateDistance(p1: GeoPoint, p2: GeoPoint): Double {
        val earthRadius = 6371000.0 // Радиус Земли в метрах
        
        val lat1Rad = Math.toRadians(p1.latitude)
        val lat2Rad = Math.toRadians(p2.latitude)
        val deltaLatRad = Math.toRadians(p2.latitude - p1.latitude)
        val deltaLonRad = Math.toRadians(p2.longitude - p1.longitude)
        
        val a = Math.sin(deltaLatRad / 2) * Math.sin(deltaLatRad / 2) +
                Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                Math.sin(deltaLonRad / 2) * Math.sin(deltaLonRad / 2)
        
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Вычислить расстояние маршрута в метрах
     */
    private fun calculateRouteDistance(points: List<GeoPoint>): Double {
        if (points.size < 2) return 0.0
        
        var totalDistance = 0.0
        for (i in 0 until points.size - 1) {
            totalDistance += calculateDistance(points[i], points[i + 1])
        }
        
        return totalDistance
    }
    
    /**
     * Отобразить маршрут на карте
     */
    private fun displayRoute(route: Route) {
        runOnUiThread {
            // Удаляем предыдущий маршрут
            routePolyline?.let { routeOverlay.items.remove(it) }
            
            // Создаем новую линию маршрута
            val polyline = Polyline().apply {
                route.points.forEach { point ->
                    addPoint(point)
                }
                color = Color.rgb(255, 165, 0) // Оранжевый цвет
                width = 8.0f
            }
            
            routePolyline = polyline
            routeOverlay.add(polyline)
            mapView.invalidate()
        }
    }
    
    /**
     * Показать информацию о маршруте
     */
    private fun showRouteInfo(route: Route) {
        runOnUiThread {
            val distanceKm = route.distance / 1000.0
            val distanceText = if (distanceKm < 1) {
                "${route.distance.toInt()} м"
            } else {
                String.format("%.2f км", distanceKm)
            }
            
            infoTitle.text = "Маршрут построен"
            infoSubtitle.text = "Расстояние: $distanceText\nТочек: ${route.points.size}"
            
            infoPanel.alpha = 0f
            infoPanel.visibility = android.view.View.VISIBLE
            infoPanel.animate().alpha(1f).setDuration(300).start()
            
            Toast.makeText(this, "Маршрут построен. Расстояние: $distanceText", Toast.LENGTH_LONG).show()
        }
    }
    
    /**
     * Использовать текущее местоположение как точку отправления
     */
    private fun useMyLocationAsOrigin() {
        if (!checkLocationPermission()) {
            requestLocationPermission()
            return
        }
        
        val myLocation = locationOverlay.myLocation
        if (myLocation != null) {
            val locationPoint = GeoPoint(myLocation.latitude, myLocation.longitude)
            
            // Создаем временный маркер для точки отправления
            // Или используем существующую логику
            Toast.makeText(this, "Текущее местоположение установлено как точка отправления", Toast.LENGTH_SHORT).show()
            
            // Если есть точка назначения, строим маршрут
            if (routeToMarker != null) {
                // Создаем временный маркер для точки отправления
                val tempMarker = Marker(mapView).apply {
                    position = locationPoint
                    setIcon(ContextCompat.getDrawable(this@WebMapActivity, android.R.drawable.ic_menu_mylocation))
                    icon?.setTint(Color.BLUE)
                }
                routeFromMarker = tempMarker
                routeFromIcon = tempMarker.icon?.constantState?.newDrawable()?.mutate()
                buildRoute()
            } else {
                // Сохраняем координаты для использования при построении маршрута
                // Можно создать невидимый маркер или хранить координаты отдельно
                Toast.makeText(this, "Выберите точку назначения для построения маршрута", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Местоположение не определено. Подождите...", Toast.LENGTH_SHORT).show()
            locationOverlay.runOnFirstFix {
                runOnUiThread {
                    val fixedLocation = locationOverlay.myLocation
                    if (fixedLocation != null) {
                        useMyLocationAsOrigin()
                    }
                }
            }
        }
    }
    
    /**
     * Очистить маршрут
     */
    private fun clearRoute() {
        routePolyline?.let { routeOverlay.items.remove(it) }
        routePolyline = null
        currentRoute = null
        
        // Восстанавливаем иконки маркеров
        routeFromMarker?.let { marker ->
            routeFromIcon?.let { icon ->
                marker.icon = icon
            }
        }
        routeToMarker?.let { marker ->
            routeToIcon?.let { icon ->
                marker.icon = icon
            }
        }
        
        routeFromMarker = null
        routeToMarker = null
        routeFromIcon = null
        routeToIcon = null
        
        mapView.invalidate()
    }

    private fun showMarkerInfo(title: String?, snippet: String?) {
        infoTitle.text = title ?: "Без названия"
        infoSubtitle.text = snippet ?: ""
        
        infoPanel.alpha = 0f
        infoPanel.visibility = android.view.View.VISIBLE
        infoPanel.animate().alpha(1f).setDuration(300).start()
    }

    private fun centerOnMyLocation() {
        if (checkLocationPermission()) {
            locationOverlay.enableFollowLocation()
            locationOverlay.enableMyLocation()

            val myLocation = locationOverlay.myLocation
            if (myLocation != null) {
                val locationPoint = GeoPoint(myLocation.latitude, myLocation.longitude)
                mapView.controller.animateTo(locationPoint)
                mapView.controller.setZoom(18.0)
                
                // Предлагаем использовать текущее местоположение как точку отправления
                if (routeFromMarker == null) {
                    Toast.makeText(this, "Двойной клик для установки точки отправления", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Поиск местоположения...", Toast.LENGTH_SHORT).show()
                // Попробуем получить локацию через listener
                locationOverlay.runOnFirstFix {
                    runOnUiThread {
                        val fixedLocation = locationOverlay.myLocation
                        if (fixedLocation != null) {
                            val locationPoint = GeoPoint(fixedLocation.latitude, fixedLocation.longitude)
                            mapView.controller.animateTo(locationPoint)
                            mapView.controller.setZoom(18.0)
                            Toast.makeText(this@WebMapActivity, "Местоположение найдено", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } else {
            requestLocationPermission()
        }
    }

    private fun checkLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            locationPermissions,
            LOCATION_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()
            } else {
                Toast.makeText(
                    this,
                    "Разрешение на геолокацию не предоставлено",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /**
     * Центрирует карту на всех загруженных объектах (зданиях и складах)
     */
    private fun centerMapOnObjects() {
        val allPoints = mutableListOf<GeoPoint>()
        
        // Собираем координаты из зданий (полигоны)
        buildings.forEach { building ->
            building.coordinates?.forEach { coord ->
                if (coord.size >= 2) {
                    allPoints.add(GeoPoint(coord[0], coord[1]))
                }
            }
            // Также добавляем центр здания
            allPoints.add(GeoPoint(building.lat, building.lon))
        }
        
        // Собираем координаты из складов
        deliveryPoints.forEach { point ->
            allPoints.add(GeoPoint(point.lat, point.lon))
        }
        
        if (allPoints.isEmpty()) {
            Log.d("WebMapActivity", "No points to center on, using default location")
            return
        }
        
        // Вычисляем bounding box
        var minLat = allPoints[0].latitude
        var maxLat = allPoints[0].latitude
        var minLon = allPoints[0].longitude
        var maxLon = allPoints[0].longitude
        
        allPoints.forEach { point ->
            minLat = minOf(minLat, point.latitude)
            maxLat = maxOf(maxLat, point.latitude)
            minLon = minOf(minLon, point.longitude)
            maxLon = maxOf(maxLon, point.longitude)
        }
        
        // Вычисляем центр
        val centerLat = (minLat + maxLat) / 2.0
        val centerLon = (minLon + maxLon) / 2.0
        val center = GeoPoint(centerLat, centerLon)
        
        // Создаем BoundingBox с небольшим отступом (10%)
        val latPadding = (maxLat - minLat) * 0.1
        val lonPadding = (maxLon - minLon) * 0.1
        
        val boundingBox = BoundingBox(
            maxLat + latPadding,
            maxLon + lonPadding,
            minLat - latPadding,
            minLon - lonPadding
        )
        
        // Применяем центрирование и масштабирование с использованием BoundingBox
        runOnUiThread {
            mapView.zoomToBoundingBox(boundingBox, true, 50) // 50 пикселей отступ от краев
            
            Log.d("WebMapActivity", "Centered map on objects: center=($centerLat, $centerLon), " +
                    "bbox=[$minLat-$maxLat, $minLon-$maxLon], points=${allPoints.size}")
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) ProgressBar.VISIBLE else ProgressBar.GONE
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
        Configuration.getInstance().save(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
        Configuration.getInstance().save(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDetach()
        coroutineScope.cancel()
    }
}
