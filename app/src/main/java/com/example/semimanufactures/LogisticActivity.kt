package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.SocketTimeoutException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class LogisticActivity : AppCompatActivity() {
    private lateinit var recycler_view_logistics: RecyclerView
    private lateinit var adapter: LogisticsAdapter
    private val TAG = "LogisticActivity"
    private var userId: Int = 0
    private var username: String = ""
    private var roleCheck: String = ""
    private var mdmCode: String = ""
    private var deviceInfo: String = ""
    private var fio: String = ""
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
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logistic)
        supportActionBar?.hide()
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout)
        recycler_view_logistics = findViewById(R.id.recycler_view_logistics)
        recycler_view_logistics.layoutManager = LinearLayoutManager(this)
        swipeRefreshLayout.setOnRefreshListener {
            fetchLogisticsData()
        }
        val intent = intent
        username = intent.getStringExtra("username") ?: ""
        roleCheck = intent.getStringExtra("roleCheck") ?: ""
        userId = intent.getIntExtra("userId", 0)
        mdmCode = intent.getStringExtra("mdmCode") ?: ""
        deviceInfo = intent.getStringExtra("deviceInfo") ?: ""
        fio = intent.getStringExtra("fio") ?: ""
        val rolesString = intent.getStringExtra("rolesString") ?: ""
        rolesList.addAll(rolesString.split(",").map { it.trim() })
        rolesList.forEach { role ->
            Log.d("Список ролей", "Роль: $role")
        }
        adapter = LogisticsAdapter(logisticsItems, mdmCode, userId, username, roleCheck, deviceInfo, fio)
        recycler_view_logistics.adapter = adapter
        Log.d(TAG, "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}, deviceInfo: ${deviceInfo}")
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            Toast.makeText(this, "Вы находитесь в окне Логистика", Toast.LENGTH_LONG).show()
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            Log.d(TAG, "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@LogisticActivity, AddActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            Log.d(TAG, "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@LogisticActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
        }
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            val intent = Intent(this@LogisticActivity, SettingsActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            showPopupMenuNotification(it)
        }
        create_logistic_button = findViewById(R.id.create_logistic_button)
        create_logistic_button.setOnClickListener {
            Log.d(TAG, "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@LogisticActivity, CreatingLogisticActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
        }
        create_logistic_doc_button = findViewById(R.id.create_logistic_doc_button)
        create_logistic_doc_button.setOnClickListener {
            Log.d(TAG, "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@LogisticActivity, CreatingLogisticDocActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
        }
        text_7days = findViewById(R.id.text_7days)
        text_closed = findViewById(R.id.text_closed)
        text_opened = findViewById(R.id.text_opened)
        text_7days.setOnClickListener {
            toggleFilter(text_7days)
        }
        text_closed.setOnClickListener {
            toggleFilter(text_closed)
            if (text_closed.background.constantState == resources.getDrawable(R.drawable.the_filter_is_pressed).constantState) {
                resetTextView(text_opened)
            }
        }
        text_opened.setOnClickListener {
            toggleFilter(text_opened)
            if (text_opened.background.constantState == resources.getDrawable(R.drawable.the_filter_is_pressed).constantState) {
                resetTextView(text_closed)
            }
        }
        main_layout = findViewById(R.id.main_layout)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("MainActivity", "Запуск получения версии...")
            val versionMobile = fetchMobileVersion(this@LogisticActivity)
            Log.d("MainActivity", "Версия получена: $versionMobile")
            if (versionMobile == null) {
                Log.e("MainActivity", "Не удалось получить версию")
                //disableUI()
            } else {
                Log.d("MainActivity", "Версия приложения: $versionMobile")
                if (versionMobile.toInt() != myGlobalVariable) {
                    Log.e("MainActivity", "Версии не совпадают. Доступ к функционалу отключен.")
                    disableUI()
                    main_layout.setOnClickListener {
                        Toast.makeText(this@LogisticActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        text_result_number = findViewById(R.id.text_result_number)
        text_result_number.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null) {
                    searchLogistics(s.toString())
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

    }
    private fun toggleFilter(selectedTextView: TextView) {
        val isPressed = selectedTextView.background.constantState == resources.getDrawable(R.drawable.the_filter_is_pressed).constantState
        if (isPressed) {
            selectedTextView.setBackgroundResource(R.drawable.the_filter_is_not_pressed)
            selectedTextView.setTextColor(Color.parseColor("#001852"))
        } else {
            selectedTextView.setBackgroundResource(R.drawable.the_filter_is_pressed)
            selectedTextView.setTextColor(Color.WHITE)
        }
        filterLogistics()
    }
    private fun resetTextView(otherTextView: TextView) {
        otherTextView.setBackgroundResource(R.drawable.the_filter_is_not_pressed)
        otherTextView.setTextColor(Color.parseColor("#001852"))
    }
    @SuppressLint("MissingInflatedId")
    private fun showPopupMenuNotification(view: View) {
        val popupView = layoutInflater.inflate(R.layout.custom_menu_notification, null)
        val popupWindow = PopupWindow(popupView, 500, 450)
        popupView.findViewById<LinearLayout>(R.id.item_write_sms).setOnClickListener {
            val rolesString = intent.getStringExtra("rolesString") ?: ""
            rolesList.addAll(rolesString.split(",").map { it.trim() })
            rolesList.forEach { role ->
                Log.d("Список ролей", "Роль: $role")
            }
            val intent = Intent(this, CreateNotificationActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.item_incoming_sms).setOnClickListener {
            Toast.makeText(this, "Входящие нажаты", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }
        popupView.findViewById<LinearLayout>(R.id.item_sent_sms).setOnClickListener {
            Toast.makeText(this, "Отправленные нажаты", Toast.LENGTH_SHORT).show()
            popupWindow.dismiss()
        }
        popupWindow.isFocusable = true
        popupWindow.showAsDropDown(view)
    }
    private fun fetchLogisticsData() {
        Log.d(TAG, "Начинаем загрузку данных с API")
        val trustAllCerts = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
        val sslContext = SSLContext.getInstance("SSL").apply {
            init(null, arrayOf<TrustManager>(trustAllCerts), SecureRandom())
        }
        val client = OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("http://192.168.200.250/api/get_logistics/")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Ошибка при загрузке данных: ${e.message}")
                runOnUiThread {
                    if (e is SocketTimeoutException) {
                        Toast.makeText(this@LogisticActivity, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@LogisticActivity, "Ошибка при загрузке данных: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    swipeRefreshLayout.isRefreshing = false
                }
                e.printStackTrace()
            }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e(TAG, "Ошибка ответа от сервера: ${response.code}")
                    runOnUiThread {
                        Toast.makeText(this@LogisticActivity, "Ошибка сервера: ${response.code}", Toast.LENGTH_SHORT).show()
                        swipeRefreshLayout.isRefreshing = false
                    }
                    return
                }
                response.body?.string()?.let { jsonResponse ->
                    Log.d(TAG, "Получен ответ от сервера")
                    val logisticsItemType = object : TypeToken<List<LogisticsItem>>() {}.type
                    logisticsItems = Gson().fromJson(jsonResponse, logisticsItemType)
                    runOnUiThread {
                        filterLogistics()
                        swipeRefreshLayout.isRefreshing=false
                        Log.d(TAG, "Данные успешно загружены и отображены")
                    }
                }
            }
        })
    }
    private fun searchLogistics(query: String) {
        val filteredItems = if (query.length >= 2) {
            logisticsItems.filter { item ->
                item.id.contains(query, ignoreCase = true)
            }.also { result ->
                Log.d(TAG, "Количество отфильтрованных по номеру заявки: ${result.size}")
            }
        } else {
            logisticsItems
        }
        adapter.updateData(filteredItems)
    }
    private fun filterLogistics() {
        val is7DaysChecked = text_7days.background.constantState == resources.getDrawable(R.drawable.the_filter_is_pressed).constantState
        val isClosedChecked = text_closed.background.constantState == resources.getDrawable(R.drawable.the_filter_is_pressed).constantState
        val isOpenedChecked = text_opened.background.constantState == resources.getDrawable(R.drawable.the_filter_is_pressed).constantState
        val filteredItems = when {
            is7DaysChecked && !isClosedChecked && !isOpenedChecked -> {
                logisticsItems.filter { item ->
                    val createdAtDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(item.created_at)
                    val sevenDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                    }.time
                    createdAtDate.before(sevenDaysAgo)
                }.also { result ->
                    Log.d(TAG, "Количество отфильтрованных по 7 дням: ${result.size}")
                }
            }
            isClosedChecked && !is7DaysChecked && !isOpenedChecked -> {
                logisticsItems.filter { item ->
                    item.status == "-1" || item.status == "4"
                }.also { result ->
                    Log.d(TAG, "Количество закрытых: ${result.size}")
                }
            }
            isOpenedChecked && !is7DaysChecked && !isClosedChecked -> {
                logisticsItems.filter { item ->
                    item.status in listOf("0", "1", "2", "3")
                }.also { result ->
                    Log.d(TAG, "Количество открытых: ${result.size}")
                }
            }
            is7DaysChecked && isClosedChecked && !isOpenedChecked -> {
                logisticsItems.filter { item ->
                    val createdAtDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(item.created_at)
                    val sevenDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                    }.time
                    createdAtDate.before(sevenDaysAgo) && (item.status == "-1" || item.status == "4")
                }.also { result ->
                    Log.d(TAG, "Количество закрытых за 7 дней: ${result.size}")
                }
            }
            is7DaysChecked && isOpenedChecked && !isClosedChecked -> {
                logisticsItems.filter { item ->
                    val createdAtDate = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(item.created_at)
                    val sevenDaysAgo = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, -7)
                    }.time
                    createdAtDate.before(sevenDaysAgo) && (item.status in listOf("0", "1", "2", "3"))
                }.also { result ->
                    Log.d(TAG, "Количество открытых за 7 дней: ${result.size}")
                }
            }
            else -> {
                Log.d(TAG, "Количество всех элементов: ${logisticsItems.size}")
                logisticsItems
            }
        }
        adapter.updateData(filteredItems)
    }
    private fun disableUI() {
        go_to_logistic.isEnabled = false
        recycler_view_logistics.isEnabled = false
        create_logistic_button.isEnabled = false
        go_to_issue.isEnabled = false
        create_logistic_doc_button.isEnabled = false
        go_to_add.isEnabled = false
        data_user_info.isEnabled = false
        go_to_send_notification.isEnabled = false
        Toast.makeText(this, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_DETAIL_LOGISTICS && resultCode == Activity.RESULT_OK) {
            data?.let {
                userId = it.getIntExtra("userId", 0)
                username = it.getStringExtra("username") ?: ""
                roleCheck = it.getStringExtra("roleCheck") ?: ""
                mdmCode = it.getStringExtra("mdmCode") ?: ""
                fio = it.getStringExtra("fio") ?: ""
                adapter.updateData(logisticsItems)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        fetchLogisticsData()
    }
    companion object {
        const val REQUEST_CODE_DETAIL_LOGISTICS = 1
    }
}