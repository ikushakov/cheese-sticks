package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.gson.Gson

class CreateNotificationSunmiActivity : ComponentActivity() {
    private lateinit var recipient: EditText
    private lateinit var description: EditText
    private lateinit var button_submit: Button
    private lateinit var go_to_authorization: ImageView
    private lateinit var go_to_search_and_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var go_to_users_settings: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
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
        setContentView(R.layout.activity_create_notification_sunmi)
        recipient = findViewById(R.id.recipient)
        description = findViewById(R.id.description)
        button_submit = findViewById(R.id.button_submit)
        go_to_authorization = findViewById(R.id.go_to_authorization)
        go_to_search_and_add = findViewById(R.id.go_to_search_and_add)
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_users_settings = findViewById(R.id.go_to_users_settings)
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_authorization.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        go_to_search_and_add.setOnClickListener {
            val intent = Intent(this, AddActivity::class.java)
            startActivity(intent)
        }
        go_to_issue.setOnClickListener {
            val intent = Intent(this, FeaturesOfTheFunctionalityActivity::class.java)
            startActivity(intent)
        }
        go_to_users_settings.setOnClickListener {
            Toast.makeText(this, "Настройки пользователя можно посмотреть в любом другом окне кроме этого", Toast.LENGTH_LONG).show()
        }
        go_to_send_notification.setOnClickListener {
            showPopupMenuNotification(it)
        }
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this, LogisticActivity::class.java)
            startActivity(intent)
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
    @SuppressLint("MissingInflatedId")
    private fun showPopupMenuNotification(view: View) {
        val popupView = layoutInflater.inflate(R.layout.custom_menu_notification, null)
        val popupWindow = PopupWindow(popupView, 550, 500)
        popupView.findViewById<LinearLayout>(R.id.item_write_sms).setOnClickListener {
            Toast.makeText(this, "Написать нажаты", Toast.LENGTH_SHORT).show()
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
}
