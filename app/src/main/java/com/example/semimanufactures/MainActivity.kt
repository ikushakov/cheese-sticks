package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
import com.example.semimanufactures.service_mode.ServiceModeException
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import ru.rustore.sdk.pushclient.RuStorePushClient
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

class MainActivity : AppCompatActivity() {
    private lateinit var login_for_authorization: EditText
    private lateinit var password_for_authorization: EditText
    private lateinit var the_button_for_authorization: Button
    private lateinit var back_to_registration: LinearLayout
    private lateinit var forget_your_password: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var go_to_search_and_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var go_to_users_settings: ImageView
    private lateinit var remember_your_password: TextView
    private lateinit var remember_me: ImageView
    private var isRememberMeChecked = false
    private var isPasswordVisible = false
    private lateinit var login_layout: LinearLayout
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var main_layout: ConstraintLayout
    private lateinit var tg_text: Button
    private lateinit var password_visibility_button: ImageView
    @SuppressLint("MissingInflatedId", "ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_login_window)
        supportActionBar?.hide()
        checkAndRequestPermissions()
        checkPushPermissions()
        Log.d("MainActivity", "Activity started")
        progressBar = findViewById(R.id.progressBar)
        login_for_authorization = findViewById(R.id.login_for_authorization)
        password_for_authorization = findViewById(R.id.password_for_authorization)
        the_button_for_authorization = findViewById(R.id.the_button_for_authorization)
        back_to_registration = findViewById(R.id.back_to_registration)
        forget_your_password = findViewById(R.id.forget_your_password)
        go_to_search_and_add = findViewById(R.id.go_to_search_and_add)
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_users_settings = findViewById(R.id.go_to_users_settings)
        login_layout = findViewById(R.id.white_layout)
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        password_visibility_button = findViewById(R.id.password_visibility_button)
        tg_text = findViewById(R.id.tg_text)
        tg_text.setOnClickListener {
            val intent = Intent(this@MainActivity, GettingTelegramUsersActivity :: class.java)
            startActivity(intent)
        }
        main_layout = findViewById(R.id.main_layout)
        go_to_send_notification.setOnClickListener {
            Toast.makeText(this, "Нажмите на кнопку Войти", Toast.LENGTH_LONG).show()
        }
        remember_your_password = findViewById(R.id.remember_your_password)
        remember_me = findViewById(R.id.remember_me)
        go_to_search_and_add.setOnClickListener {
            Toast.makeText(this, "Нажмите на кнопку Войти", Toast.LENGTH_LONG).show()
        }
        go_to_users_settings.setOnClickListener {
            Toast.makeText(this, "Нажмите на кнопку Войти", Toast.LENGTH_LONG).show()
        }
        go_to_issue.setOnClickListener {
            Toast.makeText(this, "Нажмите на кнопку Войти", Toast.LENGTH_LONG).show()
        }
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            showPopupMenu(it)
        }
        loadRememberMeState()
        remember_me.setOnClickListener {
            isRememberMeChecked = !isRememberMeChecked
            if (isRememberMeChecked) {
                remember_me.setImageResource(R.drawable.remember_me_svg)
                saveCredentials()
            } else {
                remember_me.setImageResource(R.drawable.no_remember_me_svg)
                clearCredentials()
            }
        }
        login_for_authorization.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val username = s.toString().trim()
                if (username.isNotEmpty()) {
                    checkUserExists(username)
                } else {
                    //login_for_authorization.setBackgroundColor(R.drawable.layout_background_login_and_password)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        the_button_for_authorization.setOnClickListener {
            val username = login_for_authorization.text.toString().trim()
            val password = password_for_authorization.text.toString().trim()
            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, введите имя пользователя и пароль!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            loginUser(username, password)
            if (isRememberMeChecked) {
                saveCredentials()
            }
        }
        back_to_registration.setOnClickListener {
            Toast.makeText(this@MainActivity, "Обратитесь к администратору для создания аккаунта!", Toast.LENGTH_LONG).show()
        }
        forget_your_password.setOnClickListener {
            Toast.makeText(this, "Обратитесь к администратору для восстановления пароля!", Toast.LENGTH_LONG).show()
        }
        password_visibility_button.setOnClickListener {
            togglePasswordVisibility()
        }
        password_for_authorization.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        password_for_authorization.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                authorizeUser()
                return@setOnEditorActionListener true
            }
            false
        }
        getScreenDensity(this)
        val clearFields = intent.getBooleanExtra("CLEAR_FIELDS", false)
        if (clearFields) {
            clearAllFields()
        }
        val currentDateTime = getCurrentDateTime()
        Log.d("Время сейчас", "${currentDateTime}")
    }
    fun getScreenDensity(activity: Activity) {
        val metrics = activity.resources.displayMetrics
        val screenDensityDpi = metrics.densityDpi
        val logicalDensity = metrics.density
        Log.d("ScreenDensity", "Плотность экрана: $screenDensityDpi dpi")
        Log.d("LogicalDensity", "Логическая плотность: $logicalDensity")
    }
    private fun getCurrentDateTime(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }
    @SuppressLint("ResourceType")
    private fun showPopupMenu(view: View) {
        val popupView = layoutInflater.inflate(R.layout.info_popup_menu, null)
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow.elevation = 10f
        val popupTextView: TextView = popupView.findViewById(R.id.popup_item_text)
        popupTextView.text = getString(R.string.info_about_app)
        popupTextView.setOnClickListener {
            popupWindow.dismiss()
        }
        popupWindow.isFocusable = true
        popupWindow.showAsDropDown(view)
    }
    private fun togglePasswordVisibility() {
        if (!isPasswordVisible) {
            password_for_authorization.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            password_visibility_button.setImageResource(R.drawable.eae /* Иконка открытого глаза */)
        } else {
            password_for_authorization.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            password_visibility_button.setImageResource(R.drawable.hide_eye_svg /* Иконка закрытого глаза */)
        }
        isPasswordVisible = !isPasswordVisible
        password_for_authorization.setSelection(password_for_authorization.text.length)
    }
    private fun authorizeUser() {
        val username = login_for_authorization.text.toString().trim()
        val password = password_for_authorization.text.toString().trim()
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Пожалуйста, введите имя пользователя и пароль!", Toast.LENGTH_LONG).show()
            return
        }
        loginUser(username, password)
        if (isRememberMeChecked) {
            saveCredentials()
        }
    }
    // === 1) Проверка существования пользователя ===
    private fun checkUserExists(username: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val app = application as App
            val client = app.okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val primaryUrl  = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_user"
            val fallbackUrl = "https://api.gkmmz.ru/api/get_user"

            fun buildBody(): RequestBody = FormBody.Builder()
                .add("username", username)
                .build()

            fun exec(url: String): Response? = try {
                val req = Request.Builder()
                    .url(url)
                    .post(buildBody())
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .addHeader("X-Auth-Token",  authToken)
                    .build()
                client.newCall(req).execute()
            } catch (e: Exception) {
                Log.e("CheckUserExists", "exec error for $url", e)
                null
            }

            try {
                var resp = exec(primaryUrl)
                if (resp == null || resp.code == 429) {
                    resp?.close()
                    Log.w("CheckUserExists", "429/ошибка на primary, fallback…")
                    resp = exec(fallbackUrl)
                }

                if (resp == null) {
                    withContext(Dispatchers.Main) {
                        login_for_authorization.setBackgroundResource(R.drawable.no_founded_login)
                    }
                    return@launch
                }

                resp.use { r ->
                    if (!r.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            login_for_authorization.setBackgroundResource(R.drawable.no_founded_login)
                        }
                        return@launch
                    }
                    val responseData = r.body?.string()
                    val userResponse = parseUserResponse(responseData)
                    withContext(Dispatchers.Main) {
                        if (userResponse != null) {
                            login_for_authorization.setBackgroundResource(R.drawable.successful_login)
                            password_for_authorization.requestFocus()
                        } else {
                            login_for_authorization.setBackgroundResource(R.drawable.no_founded_login)
                        }
                    }
                }
            } catch (_: ServiceModeException) {
                // экран техработ покажет перехватчик
            } catch (t: Throwable) {
                Log.e("CheckUserExists", "unexpected", t)
                withContext(Dispatchers.Main) {
                    login_for_authorization.setBackgroundResource(R.drawable.no_founded_login)
                }
            }
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

    private fun parseUserResponse(responseData: String?): UserInfo? {
        return try {
            val jsonObject = JSONObject(responseData)
            UserInfo(
                id = jsonObject.optInt("id", 0),
                username = jsonObject.optString("username", ""),
                password = jsonObject.optString("password", ""),
                sotrudnikMdmCode = jsonObject.optString("sotrudnik_mdm_code", ""),
                fio = jsonObject.optString("fio", ""),
                roleCheck = jsonObject.optString("role_id", ""),
                isDis = jsonObject.optString("is_dis", "")
            )
        } catch (e: JSONException) {
            Log.e("MainActivity", "Error parsing JSON response", e)
            null
        }
    }
    private fun loadRememberMeState() {
        val sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE)
        isRememberMeChecked = sharedPreferences.getBoolean("remember_me", false)
        if (isRememberMeChecked) {
            remember_me.setImageResource(R.drawable.remember_me_svg)
            login_for_authorization.setText(sharedPreferences.getString("username", ""))
            password_for_authorization.setText(sharedPreferences.getString("password", ""))
        } else {
            remember_me.setImageResource(R.drawable.no_remember_me_svg)
        }
    }
    private fun saveCredentials() {
        val sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("remember_me", true)
        editor.putString("username", login_for_authorization.text.toString())
        editor.putString("password", password_for_authorization.text.toString())
        editor.apply()
    }
    private fun clearCredentials() {
        val sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.remove("username")
        editor.remove("password")
        editor.putBoolean("remember_me", false)
        editor.apply()
    }
    // === 2) Логин ===
    private fun loginUser(username: String, password: String) {
        val deviceInfo = Build.MODEL
        Log.d("DeviceInfo", deviceInfo)
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            val app = application as App
            val client = app.okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val primaryUrl  = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/login"
            val fallbackUrl = "https://api.gkmmz.ru/api/login"

            fun buildBody(): RequestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("login", username)
                .addFormDataPart("password", password)
                .build()

            fun exec(url: String): Response? = try {
                val req = Request.Builder()
                    .url(url)
                    .post(buildBody())
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .addHeader("X-Auth-Token",  authToken)
                    .build()
                client.newCall(req).execute()
            } catch (e: Exception) {
                Log.e("LoginUser", "exec error for $url", e)
                null
            }

            try {
                var resp = exec(primaryUrl)
                if (resp == null || resp.code == 429) {
                    resp?.close()
                    Log.w("LoginUser", "429/ошибка на primary, fallback…")
                    resp = exec(fallbackUrl)
                }
                if (resp == null) {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "Не удалось выполнить авторизацию", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                resp.use { r ->
                    val responseBody = r.body?.string()
                    if (!r.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "Ошибка при авторизации: ${r.code}", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    val json = JSONObject(responseBody ?: "")
                    if (json.has("error")) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "Ошибка при авторизации :) \uD83D\uDE22", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    val user = parseUserInfo(responseBody)
                    if (user == null) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "Такого пользователя не существует!", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    if (user.isDis.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "Вы уволены! :)", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        handleLoginSuccess(user, deviceInfo)
                    }
                }
            } catch (_: ServiceModeException) {
                // перехвачено экраном техработ
            } catch (t: Throwable) {
                Log.e("LoginUser", "unexpected", t)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Ошибка авторизации", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private suspend fun updateUserDevice(mdmCode: String, deviceToken: String): Boolean =
        withContext(Dispatchers.IO) {
            val client = (application as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val urls = listOf(
                "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/update_user_device",
                "https://api.gkmmz.ru/api/update_user_device"
            )

            for (url in urls) {
                // тело лучше собирать каждый раз, чтобы не думать о повторном чтении
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("mdm_code", mdmCode)
                    .addFormDataPart("device_token", deviceToken)
                    .addFormDataPart("data[is_active]", "1")
                    .addFormDataPart("data[created_at]", getCurrentDateTime())
                    .build()

                val req = Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .addHeader("X-Auth-Token",  authToken)
                    .build()

                val resp = try { client.newCall(req).execute() } catch (e: Exception) {
                    Log.e("TgAuthorizationActivity", "updateUserDevice call failed for $url", e)
                    null
                } ?: continue

                try {
                    if (resp.code == 429) {
                        Log.w("TgAuthorizationActivity", "429 on $url, trying fallback…")
                        continue
                    }
                    if (resp.isSuccessful) return@withContext true
                } finally {
                    resp.close()
                }
            }
            false
        }


    private suspend fun addUserDevice(mdmCode: String, deviceToken: String): Boolean =
        withContext(Dispatchers.IO) {
            val client = (application as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()

            val urls = listOf(
                "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/add_user_device",
                "https://api.gkmmz.ru/api/add_user_device"
            )

            for (url in urls) {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("mdm_code", mdmCode)
                    .addFormDataPart("device_token", deviceToken)
                    .addFormDataPart("is_active", "1")
                    .addFormDataPart("created_at", getCurrentDateTime())
                    .build()

                val req = Request.Builder()
                    .url(url)
                    .post(body)
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .addHeader("X-Auth-Token",  authToken)
                    .build()

                val resp = try { client.newCall(req).execute() } catch (e: Exception) {
                    Log.e("TgAuthorizationActivity", "addUserDevice call failed for $url", e)
                    null
                } ?: continue

                try {
                    if (resp.code == 429) {
                        Log.w("TgAuthorizationActivity", "429 on $url, trying fallback…")
                        continue
                    }
                    if (resp.isSuccessful) return@withContext true
                } finally {
                    resp.close()
                }
            }
            false
        }


    private fun parseUserInfo(responseBody: String?): UserInfoRoles? {
        return try {
            val jsonObject = JSONObject(responseBody)
            val rolesArray = jsonObject.getJSONArray("role")
            val rolesList = mutableListOf<String>()
            for (i in 0 until rolesArray.length()) {
                rolesList.add(rolesArray.getString(i))
            }
            UserInfoRoles(
                id = jsonObject.getInt("id"),
                username = jsonObject.getString("username"),
                password = jsonObject.getString("password"),
                sotrudnikMdmCode = jsonObject.getString("sotrudnik_mdm_code"),
                fio = jsonObject.getString("fio"),
                roleCheck = jsonObject.getString("role_id"),
                isDis = jsonObject.optString("is_dis", ""),
                roles = rolesList
            )
        } catch (e: JSONException) {
            Log.e("MainActivity", "Ошибка парсинга JSON: ${e.message}", e)
            null
        }
    }
    private fun handleLoginSuccess(user: UserInfoRoles, deviceInfo: String) {
        Toast.makeText(
            this@MainActivity,
            "Вы успешно авторизованы!",
            Toast.LENGTH_LONG
        ).show()
//        RuStorePushClient.getToken()
//            .addOnSuccessListener { result ->
                //Log.d("MainActivity", "Device Token: $result")
                val userData = UserData(
                    username = user.username,
                    userId = user.id,
                    roleCheck = user.roleCheck,
                    mdmCode = user.sotrudnikMdmCode,
                    fio = user.fio,
                    deviceInfo = deviceInfo,
                    rolesString = user.roles.joinToString(","),
                    device_token = "",
                    isAuthorized = true
                )

                saveUserDataToFile(userData)
//                CoroutineScope(Dispatchers.Main).launch {
//                    val isUpdated = updateUserDevice(user.sotrudnikMdmCode, result)
//                    if (!isUpdated) {
//                        val isAdded = addUserDevice(user.sotrudnikMdmCode, result)
//                        if (!isAdded) {
//                            Log.e("MainActivity", "Failed to add user device")
//                        }
//                    }
//                }
                startActivity(Intent(this@MainActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
                    putExtra("username", user.username)
                    putExtra("userId", user.id)
                    putExtra("roleCheck", user.roleCheck)
                    putExtra("mdmCode", user.sotrudnikMdmCode)
                    putExtra("fio", user.fio)
                    putExtra("deviceInfo", deviceInfo)
                    putExtra("rolesString", user.roles.joinToString(","))
                    putExtra("device_token", "")
                })
            //}
//            .addOnFailureListener { throwable ->
//                Log.e("MainActivity", "Ошибка получения токена", throwable)
//                Toast.makeText(
//                    this@MainActivity,
//                    "Не удалось получить токен устройства. Проверьте соединение с RuStore.",
//                    Toast.LENGTH_LONG
//                ).show()
//            }
    }
    private fun saveUserDataToFile(userData: UserData) {
        try {
            val json = Gson().toJson(userData)
            openFileOutput("user_data", Context.MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error saving user data", e)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
    }
    private fun clearAllFields() {
        login_for_authorization.text.clear()
        password_for_authorization.text.clear()
    }
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            for (i in permissions.indices) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("MainActivity", "Разрешение предоставлено: ${permissions[i]}")
                } else {
                    Log.d("MainActivity", "Разрешение отклонено: ${permissions[i]}")
                }
            }
        }
    }
    private fun checkPushPermissions() {
        val app = application as App
        app.checkBackgroundPermission(this)
        val prefs = getSharedPreferences("myPrefs", MODE_PRIVATE)
        val error = prefs.getString("last_push_error", null)
        if (error != null && error.contains("HostAppBackgroundWorkPermissionNotGranted")) {
            if (app.shouldShowBackgroundPermissionDialog()) {
                app.showBackgroundPermissionDialog(this)
            }
        }
    }
    override fun onResume() {
        super.onResume()
        (application as App).checkBackgroundPermission(this)
    }
    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
    }
}