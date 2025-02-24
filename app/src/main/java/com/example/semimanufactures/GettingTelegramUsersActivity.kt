package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

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
    private suspend fun fetchUsers() {
        val url = "http://192.168.200.250/api/get_users_with_tg"
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    response.body?.string()?.let { responseData ->
                        val jsonResponse = JSONObject(responseData)
                        for (key in jsonResponse.keys()) {
                            val user = jsonResponse.getJSONObject(key)
                            val fio = user.getString("fio")
                            val username = user.getString("username")
                            userNames.add(fio)
                            userMap[fio] = username
                        }
                    }
                } else {
                    throw IOException("Ошибка загрузки пользователей")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    private fun sendAuthCode() {
        val selectedFio = autoCompleteTextViewSearch.text.toString()
        val username = userMap[selectedFio]
        if (username != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val url = "http://192.168.200.250/api/send_auth_code"
                val formBody = FormBody.Builder()
                    .add("login", username)
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build()
                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        response.body?.string()?.let { responseData ->
                            val jsonResponse = JSONObject(responseData)
                            if (jsonResponse.getString("result") == "Код отправлен вам в телеграм") {
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(this@GettingTelegramUsersActivity, "Код отправлен вам в телеграм", Toast.LENGTH_LONG).show()
                                    startActivity(Intent(this@GettingTelegramUsersActivity, TgAuthorizationActivity::class.java).apply {
                                        putExtra("username", username)
                                    })
                                }
                            }
                        }
                    } else {
                        throw IOException("Ошибка отправки кода")
                    }
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        } else {
            Toast.makeText(this, "Пользователь не найден", Toast.LENGTH_SHORT).show()
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