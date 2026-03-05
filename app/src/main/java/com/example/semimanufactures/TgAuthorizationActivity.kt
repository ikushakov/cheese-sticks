package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import ru.rustore.sdk.pushclient.RuStorePushClient
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

class TgAuthorizationActivity : AppCompatActivity() {
    private lateinit var codeOne: EditText
    private lateinit var codeTwo: EditText
    private lateinit var codeThree: EditText
    private lateinit var codeFour: EditText
    private lateinit var codeFive: EditText
    private lateinit var the_button_for_check_code: Button
    private lateinit var button_back: Button
    private lateinit var go_to_logistic: ImageView
    private var currentUsername: String = ""
        //private val client = OkHttpClient()
    private lateinit var go_to_search_and_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var go_to_users_settings: ImageView
    private lateinit var go_to_send_notification: ImageView
    private val rolesList: MutableList<String> = mutableListOf()
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tg_authorization)
        supportActionBar?.hide()
        button_back = findViewById(R.id.button_back)
        button_back.setOnClickListener {
            finish()
        }
        codeOne = findViewById(R.id.codeOne)
        codeTwo = findViewById(R.id.codeTwo)
        codeThree = findViewById(R.id.codeThree)
        codeFour = findViewById(R.id.codeFour)
        codeFive = findViewById(R.id.codeFive)
        the_button_for_check_code = findViewById(R.id.the_button_for_check_code)
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            showPopupMenu(it)
        }
        currentUsername = intent.getStringExtra("username").toString()
        Log.d("TgAuthorizationActivity", "login: $currentUsername")
        setupCodeInputListeners()
        the_button_for_check_code.setOnClickListener {
            sendAuthCode()
        }
        codeFive.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                sendAuthCode()
                true
            } else {
                false
            }
        }
        go_to_search_and_add = findViewById(R.id.go_to_search_and_add)
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_users_settings = findViewById(R.id.go_to_users_settings)
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_search_and_add.setOnClickListener {
            Toast.makeText(this, "Для начала работы авторизируйтесь", Toast.LENGTH_LONG).show()
        }
        go_to_users_settings.setOnClickListener {
            Toast.makeText(this, "Для начала работы авторизируйтесь", Toast.LENGTH_LONG).show()
        }
        go_to_issue.setOnClickListener {
            Toast.makeText(this, "Для начала работы авторизируйтесь", Toast.LENGTH_LONG).show()
        }
        go_to_send_notification.setOnClickListener {
            Toast.makeText(this, "Появится позже\uD83D\uDE01)))", Toast.LENGTH_LONG).show()
        }
    }
    private fun setupCodeInputListeners() {
        val editTexts = arrayOf(codeOne, codeTwo, codeThree, codeFour, codeFive)
        for (i in editTexts.indices) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s.toString().length == 1) {
                        if (i < editTexts.size - 1) {
                            editTexts[i + 1].requestFocus()
                        }
                    } else if (s.toString().isEmpty() && i > 0) {
                        editTexts[i - 1].requestFocus()
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
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
    private fun sendAuthCode() {
        val code = "${codeOne.text}${codeTwo.text}${codeThree.text}${codeFour.text}${codeFive.text}"
        val username = currentUsername ?: run {
            Toast.makeText(this, "Пользователь не выбран", Toast.LENGTH_SHORT).show()
            return
        }
        if (code.length != 5) {
            Toast.makeText(this, "Введите код полностью (5 символов)", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val client = (application as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val primaryUrl  = "https://api.gkmmz.ru/api/check_auth_code"
            val fallbackUrl = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/check_auth_code"

            val formBody = FormBody.Builder()
                .add("login", username)
                .add("code", code)
                .build()

            val baseReq = Request.Builder()
                .post(formBody)
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token",  authToken)

            fun exec(url: String): Response? = try {
                client.newCall(baseReq.url(url).build()).execute()
            } catch (e: IOException) {
                Log.e("AuthCode", "IO error for $url: ${e.message}")
                null
            }

            try {
                var resp = exec(primaryUrl)
                if (resp == null || resp.code == 429) {
                    resp?.close()
                    Log.w("AuthCode", "fallback → $fallbackUrl (reason: ${if (resp == null) "IO error" else "429"})")
                    resp = exec(fallbackUrl)
                }
                if (resp == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TgAuthorizationActivity, "Все серверы недоступны", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                resp.use { r ->
                    val body = r.body?.string().orEmpty()
                    if (!r.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@TgAuthorizationActivity, "Ошибка авторизации: ${r.code}", Toast.LENGTH_SHORT).show()
                        }
                        return@launch
                    }

                    // --- Парсим ответ ---
                    val json = org.json.JSONObject(body)
                    val userId  = json.optInt("id")
                    val roleId  = json.optString("role_id")
                    val mdmCode = json.optString("sotrudnik_mdm_code")
                    val fio     = json.optString("fio")

                    val rolesArr = json.optJSONArray("role")
                    val roles = mutableListOf<String>().apply {
                        if (rolesArr != null) for (i in 0 until rolesArr.length()) add(rolesArr.optString(i))
                    }
                    val rolesString = roles.joinToString(",")

                    val user = UserInfoRoles(
                        id = userId,
                        username = username,
                        password = "",
                        sotrudnikMdmCode = mdmCode,
                        fio = fio,
                        roleCheck = roleId,
                        isDis = "",
                        roles = roles
                    )

                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@TgAuthorizationActivity, "Вы успешно авторизовались", Toast.LENGTH_SHORT).show()

                        // Получаем push-токен и регистрируем устройство
                        RuStorePushClient.getToken()
                            .addOnSuccessListener { token ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    try {
                                        // сначала пытаемся обновить, если не вышло — добавить
                                        val okUpdate = updateUserDevice(mdmCode, token)
                                        if (!okUpdate) addUserDevice(mdmCode, token)

                                        val userData = UserData(
                                            username = user.username,
                                            userId = user.id,
                                            roleCheck = user.roleCheck,
                                            mdmCode = user.sotrudnikMdmCode,
                                            fio = user.fio,
                                            deviceInfo = Build.MODEL,
                                            rolesString = rolesString,
                                            device_token = token,
                                            isAuthorized = true
                                        )
                                        saveUserDataToFile(userData)

                                        withContext(Dispatchers.Main) {
                                            startActivity(Intent(this@TgAuthorizationActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
                                                putExtra("username", username)
                                                putExtra("userId", userId)
                                                putExtra("roleCheck", roleId.toIntOrNull() ?: 0)
                                                putExtra("mdmCode", mdmCode)
                                                putExtra("fio", fio)
                                                putExtra("deviceInfo", Build.MODEL)
                                                putExtra("rolesString", rolesString)
                                                putExtra("device_token", token)
                                            })
                                        }
                                    } catch (t: Throwable) {
                                        Log.e("AuthCode", "post-auth error", t)
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("PushToken", "Не удалось получить токен устройства", e)
                                // Продолжаем без токена
                                val userData = UserData(
                                    username = user.username,
                                    userId = user.id,
                                    roleCheck = user.roleCheck,
                                    mdmCode = user.sotrudnikMdmCode,
                                    fio = user.fio,
                                    deviceInfo = Build.MODEL,
                                    rolesString = rolesString,
                                    device_token = "",
                                    isAuthorized = true
                                )
                                saveUserDataToFile(userData)
                                startActivity(Intent(this@TgAuthorizationActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
                                    putExtra("username", username)
                                    putExtra("userId", userId)
                                    putExtra("roleCheck", roleId.toIntOrNull() ?: 0)
                                    putExtra("mdmCode", mdmCode)
                                    putExtra("fio", fio)
                                    putExtra("deviceInfo", Build.MODEL)
                                    putExtra("rolesString", rolesString)
                                    putExtra("device_token", "")
                                })
                            }
                    }
                }
            } catch (e: ServiceModeException) {
                // Перехватчик уже показал экран техработ
            } catch (t: Throwable) {
                Log.e("AuthCode", "Unexpected", t)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@TgAuthorizationActivity, "Ошибка авторизации", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun saveUserDataToFile(userData: UserData) {
        try {
            val json = Gson().toJson(userData)
            openFileOutput("user_data", Context.MODE_PRIVATE).use {
                it.write(json.toByteArray())
            }
        } catch (e: Exception) {
            Log.e("TgAuthorizationActivity", "Error saving user data", e)
        }
    }
//    private fun saveUserData(user: UserInfoRoles, deviceInfo: String, deviceToken: String?) {
//        val sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE).edit()
//        sharedPreferences.putString("username", user.username)
//        sharedPreferences.putString("password", user.password)
//        sharedPreferences.putInt("userId", user.id)
//        sharedPreferences.putString("mdmCode", user.sotrudnikMdmCode)
//        sharedPreferences.putString("deviceInfo", deviceInfo)
//        sharedPreferences.putString("fio", user.fio)
//        sharedPreferences.putString("roleCheck", user.roleCheck)
//        sharedPreferences.putString("rolesString", user.roles.joinToString(separator = ","))
//        sharedPreferences.putBoolean("isAuthorized", true)
//        if (deviceToken != null) {
//            sharedPreferences.putString("device_token", deviceToken)
//        }
//        sharedPreferences.apply()
//    }
private suspend fun updateUserDevice(mdmCode: String, deviceToken: String): Boolean = withContext(Dispatchers.IO) {
    val client = (application as App).okHttpClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val primaryUrl  = "https://api.gkmmz.ru/api/update_user_device"
    val fallbackUrl = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/update_user_device"

    val currentDateTime = getCurrentDateTime()
    val body = MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("mdm_code", mdmCode)
        .addFormDataPart("device_token", deviceToken)
        .addFormDataPart("data[is_active]", "1")
        .addFormDataPart("data[created_at]", currentDateTime)
        .build()

    val baseReq = Request.Builder()
        .post(body)
        .addHeader("X-Apig-AppCode", authTokenAPI)
        .addHeader("X-Auth-Token",  authToken)

    fun exec(url: String): Response? = try {
        client.newCall(baseReq.url(url).build()).execute()
    } catch (e: IOException) {
        Log.e("updateUserDevice", "IO error $url: ${e.message}")
        null
    }

    try {
        var resp = exec(primaryUrl)
        if (resp == null || resp.code == 429) {
            resp?.close()
            Log.w("updateUserDevice", "fallback → $fallbackUrl")
            resp = exec(fallbackUrl)
        }
        resp?.use { r -> r.isSuccessful } ?: false
    } catch (e: ServiceModeException) {
        false
    }
}

    private suspend fun addUserDevice(mdmCode: String, deviceToken: String): Boolean = withContext(Dispatchers.IO) {
        val client = (application as App).okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val primaryUrl  = "https://api.gkmmz.ru/api/add_user_device"
        val fallbackUrl = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/add_user_device"

        val currentDateTime = getCurrentDateTime()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("mdm_code", mdmCode)
            .addFormDataPart("device_token", deviceToken)
            .addFormDataPart("is_active", "1")
            .addFormDataPart("created_at", currentDateTime)
            .build()

        val baseReq = Request.Builder()
            .post(body)
            .addHeader("X-Apig-AppCode", authTokenAPI)
            .addHeader("X-Auth-Token",  authToken)

        fun exec(url: String): Response? = try {
            client.newCall(baseReq.url(url).build()).execute()
        } catch (e: IOException) {
            Log.e("addUserDevice", "IO error $url: ${e.message}")
            null
        }

        try {
            var resp = exec(primaryUrl)
            if (resp == null || resp.code == 429) {
                resp?.close()
                Log.w("addUserDevice", "fallback → $fallbackUrl")
                resp = exec(fallbackUrl)
            }
            resp?.use { r -> r.isSuccessful } ?: false
        } catch (e: ServiceModeException) {
            false
        }
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
}