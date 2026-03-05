package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
import com.example.semimanufactures.service_mode.ServiceModeException
import com.google.gson.Gson
import io.ktor.utils.io.concurrent.shared
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Locale
import java.net.SocketTimeoutException
import java.security.cert.X509Certificate
import java.time.Instant
import java.time.LocalDate
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SettingsActivity : ComponentActivity() {
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var data_user_info: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var id_user: TextView
    private lateinit var username_fio: TextView
    private lateinit var role_username: RecyclerView
    private lateinit var device_username: TextView
    private lateinit var username_login: TextView
    private lateinit var scrollView: NestedScrollView
    //
    private var currentUsername: String? = null
    private var currentUserId: Int? = null
    private var currentRoleCheck: String? = null
    private var currentMdmCode: String? = null
    private var currentFio: String? = null
    private var currentDeviceInfo: String? = null
    private var currentRolesString: String? = null
    private var currentDeviceToken: String? = null
    private var currentIsAuthorized:  Boolean = false
    //
    private val rolesList: MutableList<String> = mutableListOf()
    private lateinit var button_exit: Button
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
        setContentView(R.layout.activity_settings)
        scrollView = findViewById(R.id.scrollView)
        id_user = findViewById(R.id.id_user)
        id_user.text = currentUserId.toString()
        username_fio = findViewById(R.id.username_fio)
        username_fio.text = currentFio
        device_username = findViewById(R.id.device_username)
        device_username.text = currentDeviceInfo
        username_login = findViewById(R.id.username_login)
        username_login.text = currentUsername
        role_username = findViewById(R.id.role_username)
        val rolesDictionary = mapOf(
            "2" to "Администратор",
            "64" to "Диспетчер ОВЛ",
            "42" to "Логистика производства",
            "43" to "Специалист по логистике",
            "65" to "Транспортировщик ОВЛ"
        )
        currentRolesString?.split(",")?.let { rolesList.addAll(it.map { it.trim() }) }
        val rolesText = rolesList.map { role ->
            rolesDictionary[role] ?: "Неизвестная роль: $role"
        }
        role_username.layoutManager = LinearLayoutManager(this)
        val adapter = RoleAdapter(rolesText)
        role_username.adapter = adapter
        if (currentRolesString?.isNotEmpty() == true) {
            rolesList.addAll(currentRolesString!!.split(",").map { it.trim() })
        }
        button_exit = findViewById(R.id.button_exit)
        button_exit.setOnClickListener {
            logout()
        }
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@SettingsActivity, LogisticActivity::class.java)
            startActivity(intent)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            val intent = Intent(this@SettingsActivity, FeaturesOfTheFunctionalityActivity::class.java)
            startActivity(intent)
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            val intent = Intent(this@SettingsActivity, AddActivity::class.java)
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            val intent = Intent(this@SettingsActivity, NotificationActivity::class.java)
            startActivity(intent)
        }
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            Toast.makeText(this@SettingsActivity, "Вы находитесь в окне с настройками", Toast.LENGTH_LONG).show()
        }
        Log.d("Токен", "токен устройства: $currentDeviceToken")
    }

    private fun getCurrentDateTime(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
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
    // В Activity/Fragment (есть applicationContext)
// Если это репозиторий без контекста — передайте OkHttpClient через конструктор.
    private suspend fun updateUserDevice(mdmCode: String, deviceToken: String): Boolean =
        withContext(Dispatchers.IO) {
            val client = (applicationContext as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val primaryUrl  = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/update_user_device"
            val fallbackUrl = "https://api.gkmmz.ru/api/update_user_device"

            val currentDateTime = getCurrentDateTime()
            Log.d("MainActivity", "updateUserDevice at $currentDateTime, token=$deviceToken")

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("mdm_code", mdmCode)
                .addFormDataPart("device_token", deviceToken)
                .addFormDataPart("data[is_active]", "0")           // как в вашем коде
                .addFormDataPart("data[created_at]", currentDateTime)
                .build()

            // Общие хедеры для обоих URL
            val baseReq = Request.Builder()
                .post(body)
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token", authToken)

            try {
                val primaryReq = baseReq.url(primaryUrl).build()
                Log.d("MainActivity", "→ $primaryUrl")
                var resp = client.newCall(primaryReq).execute()

                // Fallback на 429
                if (resp.code == 429) {
                    resp.close()
                    val fbReq = baseReq.url(fallbackUrl).build()
                    Log.w("MainActivity", "429 на primary, пробуем fallback → $fallbackUrl")
                    client.newCall(fbReq).execute().use { fbResp ->
                        val ok = fbResp.isSuccessful
                        if (ok) Log.d("MainActivity", "User device updated successfully (fallback)")
                        else Log.e("MainActivity", "Failed on fallback: ${fbResp.code}")
                        return@withContext ok
                    }
                }

                resp.use { r ->
                    val ok = r.isSuccessful
                    if (ok) Log.d("MainActivity", "User device updated successfully (primary)")
                    else Log.e("MainActivity", "Failed to update user device: ${r.code}")
                    return@withContext ok
                }
            } catch (e: ServiceModeException) {
                // Экран техработ уже показан интерсептором
                Log.w("MainActivity", "ServiceMode active, updateUserDevice skipped (until=${e.until})")
                false
            } catch (t: Throwable) {
                Log.e("MainActivity", "Error updating user device", t)
                false
            }
        }


    private fun getUnsafeSSLSocketFactory(): SSLSocketFactory {
        val trustAllCerts = arrayOf<TrustManager>(getUnsafeTrustManager())
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, trustAllCerts, java.security.SecureRandom())
        return sslContext.socketFactory
    }

    private fun getUnsafeTrustManager(): X509TrustManager {
        return object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }
    }
    private fun logout() {
        // Получаем mdmCode и deviceToken из Intent или SharedPreferences
//        val mdmCode = intent.getStringExtra("mdmCode") ?: ""
//        val deviceToken = intent.getStringExtra("device_token") ?: ""

        // Запускаем корутину для выполнения suspend-функции
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1. Отправляем запрос на обновление устройства (is_active = 0)
                val isUpdated = updateUserDevice(currentMdmCode ?: "", currentDeviceToken ?: "")
                if (isUpdated) {
                    Log.d("SettingsActivity", "User device status updated to inactive")
                } else {
                    Log.e("SettingsActivity", "Failed to update user device status")
                }

                // 2. Очищаем файл с пользовательскими данными
                clearUserDataFile()

                // 3. Очищаем SharedPreferences (если они еще используются)
                clearSharedPreferences()

                // 4. Переход на MainActivity с очисткой полей
                navigateToMainActivity()

            } catch (e: Exception) {
                Log.e("SettingsActivity", "Error during logout", e)
                // Даже если что-то пошло не так, продолжаем процесс выхода
                clearUserDataFile()
                navigateToMainActivity()
            }
        }
    }
    private fun clearUserDataFile() {
        try {
            // Открываем файл и записываем пустую строку
            openFileOutput("user_data", Context.MODE_PRIVATE).use {
                it.write("".toByteArray())
            }
            Log.d("SettingsActivity", "User data file cleared successfully")
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error clearing user data file", e)
        }
    }

    private fun clearSharedPreferences() {
        try {
            val sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE).edit()
            sharedPreferences.clear().apply()
            Log.d("SettingsActivity", "SharedPreferences cleared successfully")
        } catch (e: Exception) {
            Log.e("SettingsActivity", "Error clearing SharedPreferences", e)
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this@SettingsActivity, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("CLEAR_FIELDS", true)
        }
        startActivity(intent)
        finish()
    }
}