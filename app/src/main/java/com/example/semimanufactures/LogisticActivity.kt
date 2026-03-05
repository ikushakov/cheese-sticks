package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
import com.example.semimanufactures.service_mode.ServiceModeException
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class LogisticActivity : AppCompatActivity() {

    private lateinit var recycler_view_logistics: RecyclerView
    private lateinit var adapter: LogisticsAdapter
    private val TAG = "LogisticActivity"
    private var currentUsername: String? = null
    private var currentUserId: Int? = null
    private var currentRoleCheck: String? = null
    private var currentMdmCode: String? = null
    private var currentFio: String? = null
    private var currentDeviceInfo: String? = null
    private var currentRolesString: String? = null
    private var currentDeviceToken: String? = null
    private var currentIsAuthorized:  Boolean = false
    private lateinit var go_to_logistic: ImageView
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var data_user_info: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var create_logistic_button: Button
    private var logisticsItems: List<LogisticsItem> = listOf()
    private lateinit var text_7days: TextView
    private lateinit var text_closed: TextView
    private lateinit var text_opened: TextView
    private lateinit var create_logistic_doc_button: Button
    private lateinit var main_layout: ConstraintLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    val rolesList: MutableList<String> = mutableListOf()
    private lateinit var text_result_number: EditText
    private lateinit var text_filter_createdBy: TextView
    private lateinit var text_filter_executor: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var create_logistic_other_button: ImageView
    private val PAGE_SIZE = 500
    private var isLoading = false
    private var isLastPage = false
    private lateinit var text_filter_inwork: TextView
    private var isFiltering = false
    private var filterJob: Job? = null
    private var savedSearchText: String = ""
    private var savedFilterStates = mutableMapOf<String, Boolean>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userData = readUserData()
        userData?.let {
            currentUsername = it.username
            currentUserId = it.userId
            currentRoleCheck = it.roleCheck
            currentMdmCode = it.mdmCode
            currentFio = it.fio
            currentDeviceInfo = it.deviceInfo
            currentRolesString = it.rolesString
            currentDeviceToken = it.device_token
            currentIsAuthorized = it.isAuthorized
            Log.d("UserData", "Логин: ${it.username}")
            Log.d("UserData", "User ID: ${it.userId}")
            Log.d("UserData", "Роль: ${it.roleCheck}")
            Log.d("UserData", "MdmdCode: ${it.mdmCode}")
            Log.d("UserData", "ФИО: ${it.fio}")
            Log.d("UserData", "Название устройства: ${it.deviceInfo}")
            Log.d("UserData", "Список ролей: ${it.rolesString}")
            Log.d("UserData", "Токен устройства: ${it.device_token}")
            Log.d("isAuthorized", "Авторизован? ${it.isAuthorized}")
        } ?: run {
            Toast.makeText(this, "Ошибка загрузки данных", Toast.LENGTH_SHORT).show()
        }
        if (!currentIsAuthorized) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContentView(R.layout.activity_logistic)
        supportActionBar?.hide()
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        recycler_view_logistics = findViewById(R.id.recycler_view_logistics)
        recycler_view_logistics.layoutManager = LinearLayoutManager(this)
        text_7days = findViewById(R.id.text_7days)
        text_closed = findViewById(R.id.text_closed)
        text_opened = findViewById(R.id.text_opened)
        text_filter_createdBy = findViewById(R.id.text_filter_createdBy)
        text_filter_executor = findViewById(R.id.text_filter_executor)
        text_filter_inwork = findViewById(R.id.text_filter_inwork)
        main_layout = findViewById(R.id.main_layout)
        text_result_number = findViewById(R.id.text_result_number)
        progressBar = findViewById(R.id.progressBar)
        swipeRefreshLayout.setOnRefreshListener {
            fetchLogisticsData()
        }
        if (currentRolesString?.isNotEmpty() == true) {
            rolesList.addAll(currentRolesString!!.split(",").map { it.trim() })
        }
        adapter = LogisticsAdapter(logisticsItems, currentUsername ?: "", currentUserId ?: 0, currentRoleCheck ?: "", currentMdmCode ?: "", currentFio ?: "", currentDeviceInfo ?: "", currentRolesString ?: "", currentDeviceToken ?: "", currentIsAuthorized ?: false)
        recycler_view_logistics.adapter = adapter
        Log.d(TAG, "User id: $currentUserId, Username: $currentUsername, Role: $currentRoleCheck, MDM Code: $currentMdmCode")
        recycler_view_logistics.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!isLoading && !isLastPage) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                        && totalItemCount >= PAGE_SIZE) {

                        if (!text_7days.isFilterActive &&
                            !text_closed.isFilterActive &&
                            !text_opened.isFilterActive &&
                            !text_filter_createdBy.isFilterActive &&
                            !text_filter_executor.isFilterActive &&
                            !text_filter_inwork.isFilterActive &&
                            text_result_number.text.isEmpty()) {
                            fetchLogisticsData(loadMore = true)
                        }
                    }
                }
            }
        })
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            Toast.makeText(this, "Вы находитесь в окне Логистика", Toast.LENGTH_LONG).show()
        }
        lifecycleScope.launch {
            progressBar.visibility = View.VISIBLE
            fetchLogisticsData()
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            val intent = Intent(this@LogisticActivity, AddActivity::class.java)
            startActivity(intent)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            val intent = Intent(this@LogisticActivity, FeaturesOfTheFunctionalityActivity::class.java)
            startActivity(intent)
        }
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            val intent = Intent(this@LogisticActivity, SettingsActivity::class.java)
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            val intent = Intent(this@LogisticActivity, NotificationActivity::class.java)
            startActivity(intent)
        }
        create_logistic_button = findViewById(R.id.create_logistic_button)
        create_logistic_button.setOnClickListener {
            val intent = Intent(this@LogisticActivity, CreatingLogisticActivity::class.java)
            startActivity(intent)
        }
        create_logistic_doc_button = findViewById(R.id.create_logistic_doc_button)
        create_logistic_doc_button.setOnClickListener {
            val intent = Intent(this@LogisticActivity, CreatingLogisticDocActivity::class.java)
            startActivity(intent)
        }
        create_logistic_other_button = findViewById(R.id.create_logistic_other_button)
        create_logistic_other_button.setOnClickListener {
            startActivity(Intent(this, CreatingLogisticOtherActivity::class.java))
        }
        text_7days.setOnClickListener {
            toggleFilter(text_7days)
        }
        text_closed.setOnClickListener {
            toggleFilter(text_closed)
            if (text_closed.isFilterActive) {
                resetTextView(text_opened)
                resetTextView(text_filter_inwork)
                applyCombinedFilters()
            }
        }
        text_opened.setOnClickListener {
            toggleFilter(text_opened)
            if (text_opened.isFilterActive) {
                resetTextView(text_closed)
                resetTextView(text_filter_inwork)  // Добавляем сброс "В работе"
                applyCombinedFilters()
            }
        }

        text_filter_createdBy.setOnClickListener { toggleFilter(text_filter_createdBy) }

        text_filter_executor.setOnClickListener { toggleFilter(text_filter_executor) }

        text_filter_inwork.setOnClickListener {
            toggleFilter(text_filter_inwork)
            if (text_filter_inwork.isFilterActive) {
                resetTextView(text_closed)
                resetTextView(text_opened)  // Добавляем сброс "Незакрытые"
                applyCombinedFilters()
            }
        }

        text_result_number.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Отменяем предыдущий запрос фильтрации
                filterJob?.cancel()

                // Показываем индикатор загрузки
                isFiltering = true
                progressBar.visibility = View.VISIBLE

                // Запускаем новую фильтрацию с задержкой для дебаунса
                filterJob = lifecycleScope.launch {
                    delay(300) // Задержка для оптимизации
                    applyCombinedFilters()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        if (savedInstanceState != null) {
            restoreState(savedInstanceState)
        } else {
            // Или восстанавливаем из SharedPreferences
            restoreStateFromPrefs()
        }
    }
    private fun readUserData(): UserData? {
        return try {
            openFileInput("user_data").use {
                val json = it.bufferedReader().use { reader -> reader.readText() }
                Gson().fromJson(json, UserData::class.java)
            }
        } catch (e: Exception) {
            Log.e("FeaturesActivity", "Error reading user data", e)
            null
        }
    }
    // Обновляем метод toggleFilter
    private fun toggleFilter(selectedTextView: TextView) {
        val isPressed = selectedTextView.background.constantState == resources.getDrawable(R.drawable.the_filter_is_pressed).constantState
        if (isPressed) {
            selectedTextView.setBackgroundResource(R.drawable.the_filter_is_not_pressed)
            selectedTextView.setTextColor(Color.parseColor("#001852"))
        } else {
            selectedTextView.setBackgroundResource(R.drawable.the_filter_is_pressed)
            selectedTextView.setTextColor(Color.WHITE)
        }

        // Показываем индикатор при изменении фильтров
        isFiltering = true
        progressBar.visibility = View.VISIBLE

        // Запускаем фильтрацию
        lifecycleScope.launch {
            applyCombinedFilters()
        }
    }
    private fun resetTextView(otherTextView: TextView) {
        otherTextView.setBackgroundResource(R.drawable.the_filter_is_not_pressed)
        otherTextView.setTextColor(Color.parseColor("#001852"))
    }
    private var currentPage = 0
    private var allLogisticsItems: List<LogisticsItem> = listOf()
    private var filteredLogisticsItems: List<LogisticsItem> = listOf()
    private fun fetchLogisticsData(loadMore: Boolean = false) {
        if (isLoading) return
        isLoading = true

        if (!loadMore) {
            currentPage = 0
            isLastPage = false
            // Показываем индикатор только если не идет фильтрация
            if (!isFiltering) {
                progressBar.visibility = View.VISIBLE
            }
            allLogisticsItems = emptyList()
        } else {
            currentPage++
        }

        val primaryUrl  = "https://api.gkmmz.ru/api/get_logistics"
        val fallbackUrl = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_logistics"

        val client = (application as App).okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        lifecycleScope.launch(Dispatchers.IO) {
            val start = currentPage * PAGE_SIZE
            val formBody = FormBody.Builder()
                .add("start", start.toString())
                .add("length", PAGE_SIZE.toString())
                .add("draw", "1")
                .build()

            val base = Request.Builder()
                .post(formBody)
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token", authToken)

            suspend fun call(url: String): Response {
                val req = base.url(url).build()
                Log.d(TAG, "→ POST $url start=$start length=$PAGE_SIZE")
                return client.newCall(req).execute()
            }

            try {
                var resp: Response? = null
                var networkError: IOException? = null

                try { resp = call(primaryUrl) } catch (e: IOException) {
                    networkError = e
                    Log.w(TAG, "primary request failed: ${e.message}")
                }

                if (resp == null || resp.code == 429) {
                    resp?.close()
                    try {
                        Log.w(TAG, "fallback → $fallbackUrl (reason: ${if (resp?.code == 429) "429" else "IO error"})")
                        resp = call(fallbackUrl)
                    } catch (e: IOException) {
                        networkError = e
                        resp = null
                    }
                }

                if (resp == null) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        swipeRefreshLayout.isRefreshing = false
                        isLoading = false
                        Toast.makeText(
                            this@LogisticActivity,
                            networkError?.message ?: "Все серверы недоступны",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    return@launch
                }

                resp.use { r ->
                    if (!r.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            swipeRefreshLayout.isRefreshing = false
                            isLoading = false
                            Toast.makeText(
                                this@LogisticActivity,
                                "Ошибка сервера: ${r.code}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@launch
                    }

                    val bodyStr = r.body?.string().orEmpty()
                    try {
                        val apiResponse = Gson().fromJson(bodyStr, ApiResponse::class.java)
                        @Suppress("UNCHECKED_CAST")
                        val newItems = (apiResponse.data ?: emptyList<Any>()) as List<LogisticsItem>

                        withContext(Dispatchers.Main) {
                            allLogisticsItems = if (loadMore) allLogisticsItems + newItems else newItems
                            applyLocalFilters()
                            isLastPage = newItems.size < PAGE_SIZE
                            swipeRefreshLayout.isRefreshing = false
                            // Скрываем индикатор только если не идет фильтрация
                            if (!isFiltering) {
                                progressBar.visibility = View.GONE
                            }
                            isLoading = false
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "JSON parse error", e)
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            swipeRefreshLayout.isRefreshing = false
                            isLoading = false
                            Toast.makeText(this@LogisticActivity, "Ошибка обработки данных", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: ServiceModeException) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    isLoading = false
                }
            } catch (e: SocketTimeoutException) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    isLoading = false
                    Toast.makeText(this@LogisticActivity, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
            } catch (t: Throwable) {
                Log.e(TAG, "fetchLogisticsData error", t)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    swipeRefreshLayout.isRefreshing = false
                    isLoading = false
                    Toast.makeText(this@LogisticActivity, "Ошибка: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DETAIL_LOGISTICS && resultCode == Activity.RESULT_OK) {
            data?.let {
                currentUserId = it.getIntExtra("userId", 0)
                currentUsername = it.getStringExtra("username") ?: ""
                currentRoleCheck = it.getStringExtra("roleCheck") ?: ""
                currentMdmCode = it.getStringExtra("mdmCode") ?: ""
                currentFio = it.getStringExtra("fio") ?: ""
                adapter.updateData(logisticsItems)
                val shouldRefresh = data?.getBooleanExtra("shouldRefresh", false) ?: false
                if (shouldRefresh) {
                    // Обновляем данные
                    lifecycleScope.launch {
                        fetchLogisticsData()
                    }
                }
                // Восстанавливаем состояние фильтров после возврата
                restoreStateFromPrefs()
            }
        }
    }
    // Обновляем метод applyCombinedFilters
    private fun applyCombinedFilters() {
        // Если уже идет загрузка данных с сервера, не показываем индикатор фильтрации
        if (isLoading) {
            isFiltering = false
            return
        }

        val layoutManager = recycler_view_logistics.layoutManager as LinearLayoutManager
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val topView = layoutManager.findViewByPosition(firstVisiblePosition)
        val offset = topView?.top ?: 0

        // Фильтруем локальные данные
        filteredLogisticsItems = allLogisticsItems.filter { item ->
            val matchesSearch = if (text_result_number.text.length >= 2) {
                item.id.contains(text_result_number.text.toString(), ignoreCase = true) ||
                        item.created_by_name?.contains(text_result_number.text.toString(), ignoreCase = true) ?: false ||
                        item.object_name?.contains(text_result_number.text.toString(), ignoreCase = true) ?: false ||
                        item.spros?.contains(text_result_number.text.toString(), ignoreCase = true) ?: false ||
                        item.object_id?.contains(text_result_number.text.toString(), ignoreCase = true) ?: false
            } else true

            if (!matchesSearch) return@filter false

            if (text_7days.isFilterActive) {
                try {
                    val createdAtDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(item.created_at)
                    val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.time
                    createdAtDate.after(sevenDaysAgo)
                } catch (e: Exception) {
                    Log.e(TAG, "Ошибка парсинга даты", e)
                    false
                }
            } else true
        }.filter { item ->
            // ФИЛЬТРАЦИЯ ПО СТАТУСАМ
            when {
                // "Закрытые" - статусы -1 и 4
                text_closed.isFilterActive -> item.status == "-1" || item.status == "4"

                // "Незакрытые" - ТОЛЬКО статус 0
                text_opened.isFilterActive -> item.status == "0"

                // Если нет активных статусных фильтров
                else -> true
            }
        }.filter { item ->
            // "В работе" - статусы 1, 2, 3
            if (text_filter_inwork.isFilterActive) {
                item.status in listOf("1", "2", "3")
            } else true
        }.filter { item ->
            if (text_filter_createdBy.isFilterActive) item.created_by == currentMdmCode else true
        }.filter { item ->
            if (text_filter_executor.isFilterActive) item.executor == currentMdmCode else true
        }

        // Сортировка если выбрано "Незакрытые" и "Исполнитель"
        if (text_opened.isFilterActive && text_filter_executor.isFilterActive) {
            filteredLogisticsItems = maybeSortOpenedByExecutor(filteredLogisticsItems)
        }

        runOnUiThread {
            adapter.updateData(filteredLogisticsItems)

            // Скрываем индикатор только после обновления данных
            if (isFiltering) {
                progressBar.visibility = View.GONE
                isFiltering = false
            }

            if (text_result_number.text.isEmpty()) {
                if (firstVisiblePosition >= 0 && firstVisiblePosition < adapter.itemCount) {
                    layoutManager.scrollToPositionWithOffset(firstVisiblePosition, offset)
                }
            }
        }
    }

    private val createdAtFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    private fun parseCreatedAtMillis(s: String?): Long? = try {
        if (s.isNullOrBlank()) null else createdAtFormat.parse(s)?.time
    } catch (_: Exception) { null }

    private fun List<LogisticsItem>.sortedByCreatedAtAsc(): List<LogisticsItem> =
        this.sortedBy { parseCreatedAtMillis(it.created_at) ?: Long.MAX_VALUE }

    private fun maybeSortOpenedByExecutor(list: List<LogisticsItem>): List<LogisticsItem> =
        if (text_opened.isFilterActive && text_filter_executor.isFilterActive) {
            list.sortedByCreatedAtAsc()
        } else {
            list
        }

    private fun applyLocalFilters() {
        // Показываем индикатор при применении фильтров к новым данным
        if (!isFiltering) {
            isFiltering = true
            progressBar.visibility = View.VISIBLE
        }

        val layoutManager = recycler_view_logistics.layoutManager as LinearLayoutManager
        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
        val topView = layoutManager.findViewByPosition(firstVisiblePosition)
        val offset = topView?.top ?: 0
        val query = text_result_number.text.toString()

        var result = allLogisticsItems
            .filter { item ->
                val matchesSearch = if (query.length >= 2) {
                    item.id.contains(query, true) ||
                            (item.created_by_name?.contains(query, true) ?: false) ||
                            (item.object_name?.contains(query, true) ?: false) ||
                            (item.spros?.contains(query, true) ?: false) ||
                            (item.object_id?.contains(query, true) ?: false)
                } else true
                if (!matchesSearch) return@filter false

                if (text_7days.isFilterActive) {
                    val createdAt = parseCreatedAtMillis(item.created_at)
                    val sevenDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }.timeInMillis
                    createdAt != null && createdAt > sevenDaysAgo
                } else true
            }
            .filter { item ->
                // ФИЛЬТРАЦИЯ ПО СТАТУСАМ
                when {
                    // "Закрытые" - статусы -1 и 4
                    text_closed.isFilterActive -> item.status == "-1" || item.status == "4"

                    // "Незакрытые" - ТОЛЬКО статус 0
                    text_opened.isFilterActive -> item.status == "0"

                    // Если нет активных статусных фильтров
                    else -> true
                }
            }
            .filter { item ->
                // "В работе" - статусы 1, 2, 3
                if (text_filter_inwork.isFilterActive) {
                    item.status in listOf("1", "2", "3")
                } else true
            }
            .filter { item ->
                if (text_filter_createdBy.isFilterActive) item.created_by == currentMdmCode else true
            }
            .filter { item ->
                if (text_filter_executor.isFilterActive) item.executor == currentMdmCode else true
            }

        result = maybeSortOpenedByExecutor(result)
        filteredLogisticsItems = result
        adapter.updateData(filteredLogisticsItems)

        // Скрываем индикатор после обновления данных
        progressBar.visibility = View.GONE
        isFiltering = false

        if (firstVisiblePosition >= 0 && firstVisiblePosition < adapter.itemCount) {
            layoutManager.scrollToPositionWithOffset(firstVisiblePosition, offset)
        }
    }

    val TextView.isFilterActive: Boolean
        get() = background.constantState == resources.getDrawable(R.drawable.the_filter_is_pressed).constantState

    private fun saveState(outState: Bundle) {
        outState.putString("search_text", text_result_number.text.toString())
        outState.putBoolean("filter_7days", text_7days.isFilterActive)
        outState.putBoolean("filter_closed", text_closed.isFilterActive)
        outState.putBoolean("filter_opened", text_opened.isFilterActive)
        outState.putBoolean("filter_createdBy", text_filter_createdBy.isFilterActive)
        outState.putBoolean("filter_executor", text_filter_executor.isFilterActive)
        outState.putBoolean("filter_inwork", text_filter_inwork.isFilterActive)
    }

    private fun saveStateToPrefs() {
        val prefs = getSharedPreferences("logistic_filters", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("search_text", text_result_number.text.toString())
            putBoolean("filter_7days", text_7days.isFilterActive)
            putBoolean("filter_closed", text_closed.isFilterActive)
            putBoolean("filter_opened", text_opened.isFilterActive)
            putBoolean("filter_createdBy", text_filter_createdBy.isFilterActive)
            putBoolean("filter_executor", text_filter_executor.isFilterActive)
            putBoolean("filter_inwork", text_filter_inwork.isFilterActive)
            apply()
        }
    }

    private fun restoreState(savedInstanceState: Bundle) {
        savedSearchText = savedInstanceState.getString("search_text", "")
        savedFilterStates = mutableMapOf(
            "7days" to savedInstanceState.getBoolean("filter_7days", false),
            "closed" to savedInstanceState.getBoolean("filter_closed", false),
            "opened" to savedInstanceState.getBoolean("filter_opened", false),
            "createdBy" to savedInstanceState.getBoolean("filter_createdBy", false),
            "executor" to savedInstanceState.getBoolean("filter_executor", false),
            "inwork" to savedInstanceState.getBoolean("filter_inwork", false)
        )
        applySavedState()
    }

    private fun restoreStateFromPrefs() {
        val prefs = getSharedPreferences("logistic_filters", Context.MODE_PRIVATE)
        savedSearchText = prefs.getString("search_text", "") ?: ""
        savedFilterStates = mutableMapOf(
            "7days" to prefs.getBoolean("filter_7days", false),
            "closed" to prefs.getBoolean("filter_closed", false),
            "opened" to prefs.getBoolean("filter_opened", false),
            "createdBy" to prefs.getBoolean("filter_createdBy", false),
            "executor" to prefs.getBoolean("filter_executor", false),
            "inwork" to prefs.getBoolean("filter_inwork", false)
        )
        applySavedState()
    }

    private fun applySavedState() {
        // Восстанавливаем текстовое поле
        text_result_number.setText(savedSearchText)

        // Восстанавливаем состояние фильтров
        if (savedFilterStates["7days"] == true && !text_7days.isFilterActive) {
            text_7days.performClick()
        }
        if (savedFilterStates["closed"] == true && !text_closed.isFilterActive) {
            text_closed.performClick()
        }
        if (savedFilterStates["opened"] == true && !text_opened.isFilterActive) {
            text_opened.performClick()
        }
        if (savedFilterStates["createdBy"] == true && !text_filter_createdBy.isFilterActive) {
            text_filter_createdBy.performClick()
        }
        if (savedFilterStates["executor"] == true && !text_filter_executor.isFilterActive) {
            text_filter_executor.performClick()
        }
        if (savedFilterStates["inwork"] == true && !text_filter_inwork.isFilterActive) {
            text_filter_inwork.performClick()
        }

        // Применяем фильтры если есть сохраненное состояние
        if (savedSearchText.isNotEmpty() || savedFilterStates.values.any { it }) {
            lifecycleScope.launch {
                progressBar.visibility = View.VISIBLE
                applyCombinedFilters()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        restoreStateFromPrefs()
        // Обновляем данные если нужно
        if (allLogisticsItems.isEmpty()) {
            fetchLogisticsData()
        }
    }

    companion object {
        const val REQUEST_CODE_DETAIL_LOGISTICS = 1
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        saveState(outState)
        saveStateToPrefs()
    }

    override fun onPause() {
        super.onPause()
        saveStateToPrefs()
    }
}