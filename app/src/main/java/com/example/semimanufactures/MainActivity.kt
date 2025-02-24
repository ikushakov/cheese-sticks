package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
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
import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

var myGlobalVariable: Int = 1

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
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("MainActivity", "Запуск получения версии...")
            val versionMobile = fetchMobileVersion(this@MainActivity)
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
                        Toast.makeText(this@MainActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        go_to_send_notification.setOnClickListener {
            Toast.makeText(this, "Появится позже\uD83D\uDE01)))", Toast.LENGTH_LONG).show()
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
        // Установите слушатель кликов на кнопку видимости
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
        //
        val clearFields = intent.getBooleanExtra("CLEAR_FIELDS", false)
        if (clearFields) {
            clearAllFields() // Вызов метода для очистки всех полей
        }
    }
    fun getScreenDensity(activity: Activity) {
        val metrics = activity.resources.displayMetrics

        val screenDensityDpi = metrics.densityDpi // Общая плотность экрана в dpi
        val logicalDensity = metrics.density // Логическая плотность для масштабирования элементов UI

        Log.d("ScreenDensity", "Плотность экрана: $screenDensityDpi dpi")
        Log.d("LogicalDensity", "Логическая плотность: $logicalDensity")
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
    // Метод переключения видимости пароля
    private fun togglePasswordVisibility() {
        if (!isPasswordVisible) {
            password_for_authorization.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            password_visibility_button.setImageResource(R.drawable.eae /* Иконка открытого глаза */)
        } else {
            password_for_authorization.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            password_visibility_button.setImageResource(R.drawable.hide_eye_svg /* Иконка закрытого глаза */)
        }
        isPasswordVisible = !isPasswordVisible

        // Обновите позицию курсора после изменения типа ввода.
        // Это может быть не нужно в вашем случае.
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
    private fun checkUserExists(username: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val url = "http://192.168.200.250/api/get_user"
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val requestBody = FormBody.Builder()
                .add("username", username)
                .build()
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()
            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw IOException("Unexpected code $response")
                }
                val responseData = response.body?.string()
                val userResponse = parseUserResponse(responseData)
                withContext(Dispatchers.Main) {
                    if (userResponse != null) {
                        login_for_authorization.setBackgroundResource(R.drawable.successful_login)
                            password_for_authorization.requestFocus()
                    } else {
                        login_for_authorization.setBackgroundResource(R.drawable.no_founded_login)
                    }
                }
            } catch (e: SocketTimeoutException) {
                Log.e("MainActivity", "Timeout error checking user existence", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                    login_for_authorization.setBackgroundResource(R.drawable.no_founded_login)
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error checking user existence", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Ошибка при проверке пользователя.", Toast.LENGTH_LONG).show()
                    login_for_authorization.setBackgroundResource(R.drawable.no_founded_login)
                }
            }
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
    private fun loginUser(username: String, password: String) {
        val deviceInfo = Build.MODEL
        Log.d("DeviceInfo", deviceInfo)
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("login", username)
                .addFormDataPart("password", password)
                .addFormDataPart("version", myGlobalVariable.toString())
                .build()
            val request = Request.Builder()
                .url("http://192.168.200.250/api/login")
                .post(requestBody)
                .build()
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(responseBody)
                    if (jsonResponse.has("error")) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            Toast.makeText(this@MainActivity, "Версия приложения устарела. Необходимо обновить:)\uD83D\uDE22", Toast.LENGTH_LONG).show()
                            Log.d("MainActivity", "Версия приложения устарела. Необходимо обновить:)\uD83D\uDE22")
                        }
                        return@launch
                    }
                    val user = parseUserInfo(responseBody)
                    if (user != null) {
                        if (user.isDis.isEmpty()) {
                            withContext(Dispatchers.Main) {
                                progressBar.visibility = View.GONE
                                Toast.makeText(this@MainActivity, "Вы уволены!:)\uD83D\uDE22", Toast.LENGTH_LONG).show()
                            }
                            return@launch
                        }
                        val mdmCode = user.sotrudnikMdmCode
                        val fio = user.fio
                        Log.d("MainActivity", "Сотрудник MDM Код: $mdmCode")
                        Log.d("MainActivity", "User ID: ${user.id}")
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            handleLoginSuccess(user, deviceInfo)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "Такого пользователя не существует!", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@MainActivity, "Ошибка при авторизации: ${response.code}", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: SocketTimeoutException) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                }
                Log.e("MainActivity", "Timeout error during login", e)
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@MainActivity, "Ошибка при авторизации: ${e.message}", Toast.LENGTH_LONG).show()
                }
                Log.e("MainActivity", "Error during login", e)
            }
        }
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
        Toast.makeText(this@MainActivity,
            "Вы успешно авторизованы!",
            Toast.LENGTH_LONG).show()
        val rolesString = user.roles.joinToString(separator = ",")
        val sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE).edit()
        sharedPreferences.putString("username", user.username)
        sharedPreferences.putString("password", user.password)
        sharedPreferences.putInt("userId", user.id)
        sharedPreferences.putString("mdmCode", user.sotrudnikMdmCode)
        sharedPreferences.putString("deviceInfo", deviceInfo)
        sharedPreferences.putString("fio", user.fio)
        sharedPreferences.putString("roleCheck", user.roleCheck)
        sharedPreferences.putString("rolesString", rolesString)
        sharedPreferences.putBoolean("isAuthorized", true).apply()
        Log.d("LoginActivity", "Passing Username: ${user.username}, User ID: ${user.id}, fio: ${user.fio}")
        val intent = Intent(this@MainActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
            putExtra("username", user.username)
            putExtra("password", user.password)
            putExtra("userId", user.id)
            putExtra("mdmCode", user.sotrudnikMdmCode)
            putExtra("deviceInfo", deviceInfo)
            putExtra("fio", user.fio)
            putExtra("roleCheck", user.roleCheck)
            putExtra("rolesString", rolesString)
        }
        startActivity(intent)
    }
    private fun disableUI() {
        login_for_authorization.isEnabled = false
        password_for_authorization.isEnabled = false
        the_button_for_authorization.isEnabled = false
        back_to_registration.isEnabled = false
        forget_your_password.isEnabled = false
        go_to_search_and_add.isEnabled = false
        go_to_issue.isEnabled = false
        go_to_users_settings.isEnabled = false
        remember_your_password.isEnabled = false
        remember_me.isEnabled = false
        Toast.makeText(this, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
    }
    override fun onDestroy() {
        super.onDestroy()
    }
    private fun clearAllFields() {
        login_for_authorization.text.clear()
        password_for_authorization.text.clear()
    }
}