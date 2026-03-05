package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
import com.example.semimanufactures.service_mode.ServiceModeException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
class GettingTelegramUsersActivity : AppCompatActivity() {
    private lateinit var autoCompleteTextViewSearch: AutoCompleteTextView
    private lateinit var buttonSendAuthCode: Button
    private lateinit var button_back: Button
    private lateinit var go_to_logistic: ImageView
    private val userNames = mutableListOf<String>()
    private val userMap = mutableMapOf<String, String>()
    private val client = OkHttpClient()
//    private lateinit var go_to_authorization: ImageView
    private lateinit var go_to_search_and_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var go_to_users_settings: ImageView
    private lateinit var go_to_send_notification: ImageView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_getting_telegram_users)
        supportActionBar?.hide()
        autoCompleteTextViewSearch = findViewById(R.id.auto_complete_text_search)
        buttonSendAuthCode = findViewById(R.id.button_send_auth_code)
        button_back = findViewById(R.id.button_back)
        button_back.setOnClickListener {
            finish()
        }
        CoroutineScope(Dispatchers.Main).launch {
            fetchUsers()
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, userNames)
        autoCompleteTextViewSearch.setAdapter(adapter)
        autoCompleteTextViewSearch.setOnItemClickListener { _, _, position, _ ->
            val selectedFio = adapter.getItem(position).toString()
            autoCompleteTextViewSearch.setText(selectedFio)
        }
        buttonSendAuthCode.setOnClickListener {
            sendAuthCode()
        }
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            showPopupMenu(it)
        }
//        go_to_authorization = findViewById(R.id.go_to_authorization)
//        go_to_authorization.setOnClickListener {
//            Toast.makeText(this, "Вы находитесь в окне авторизации", Toast.LENGTH_LONG).show()
//        }
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
    // Загрузка списка пользователей (Telegram)
    private suspend fun fetchUsers() = withContext(Dispatchers.IO) {
        val client = (application as App).okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val primaryUrl  = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_users_with_tg"
        val fallbackUrl = "https://api.gkmmz.ru/api/get_users_with_tg"

        val baseReq = Request.Builder()
            .addHeader("X-Apig-AppCode", authTokenAPI)
            .addHeader("X-Auth-Token", authToken)

        fun exec(url: String): Response? = try {
            val req = baseReq.url(url).build()
            client.newCall(req).execute()
        } catch (e: IOException) {
            Log.w("FetchUsers", "IO error: ${e.message}")
            null
        }

        try {
            var resp = exec(primaryUrl)
            if (resp == null || resp.code == 429) {
                resp?.close()
                Log.w("FetchUsers", "fallback → $fallbackUrl (reason: ${if (resp == null) "IO error" else "429"})")
                resp = exec(fallbackUrl)
            }
            if (resp == null) {
                Log.e("FetchUsers", "Не удалось загрузить данные пользователей (оба URL)")
                return@withContext
            }

            resp.use { r ->
                if (!r.isSuccessful) {
                    Log.e("FetchUsers", "Ошибка ответа: ${r.code}")
                    return@withContext
                }
                val body = r.body?.string().orEmpty()
                val json = JSONObject(body)

                val newNames = mutableListOf<String>()
                val newMap = mutableMapOf<String, String>()
                val it = json.keys()
                while (it.hasNext()) {
                    val key = it.next()
                    val user = json.getJSONObject(key)
                    val fio = user.getString("fio")
                    val username = user.getString("username")
                    newNames.add(fio)
                    newMap[fio] = username
                }

                withContext(Dispatchers.Main) {
                    // Поведение как в исходнике — добавляем к существующим
                    userNames.addAll(newNames)
                    userMap.putAll(newMap)
                }
            }
        } catch (e: ServiceModeException) {
            // экран техработ показан интерсептором
            Log.w("FetchUsers", "ServiceMode active; fetchUsers skipped (until=${e.until})")
        } catch (t: Throwable) {
            Log.e("FetchUsers", "Unexpected error", t)
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

    // Отправка кода авторизации
    private fun sendAuthCode() {
        val selectedFio = autoCompleteTextViewSearch.text.toString()
        val currentUsername = userMap[selectedFio]
        if (currentUsername == null) {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val client = (application as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val primaryUrl  = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/send_auth_code"
            val fallbackUrl = "https://api.gkmmz.ru/api/send_auth_code"

            val formBody = FormBody.Builder()
                .add("login", currentUsername)
                .build()

            val baseReq = Request.Builder()
                .post(formBody)
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token", authToken)

            fun exec(url: String): Response? = try {
                val req = baseReq.url(url).build()
                client.newCall(req).execute()
            } catch (e: IOException) {
                Log.w("SendAuthCode", "IO error: ${e.message}")
                null
            }

            try {
                var resp = exec(primaryUrl)
                if (resp == null || resp.code == 429) {
                    resp?.close()
                    Log.w("SendAuthCode", "fallback → $fallbackUrl (reason: ${if (resp == null) "IO error" else "429"})")
                    resp = exec(fallbackUrl)
                }
                if (resp == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@GettingTelegramUsersActivity, "Не удалось отправить код авторизации", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                resp.use { r ->
                    if (!r.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@GettingTelegramUsersActivity, "Ошибка сервера: ${r.code}", Toast.LENGTH_LONG).show()
                        }
                        return@launch
                    }

                    val body = r.body?.string().orEmpty()
                    val json = JSONObject(body)
                    if (json.optString("result") == "Код отправлен вам в телеграм") {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@GettingTelegramUsersActivity, "Код отправлен вам в телеграм", Toast.LENGTH_LONG).show()
                            startActivity(Intent(this@GettingTelegramUsersActivity, TgAuthorizationActivity::class.java).apply {
                                putExtra("username", currentUsername)
                            })
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@GettingTelegramUsersActivity, "Не удалось отправить код", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: ServiceModeException) {
                // экран техработ показан интерсептором
            } catch (t: Throwable) {
                Log.e("SendAuthCode", "Unexpected error", t)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@GettingTelegramUsersActivity, "Ошибка: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
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