package com.example.semimanufactures

import android.annotation.SuppressLint
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

class SettingsActivity : ComponentActivity() {
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var data_user_info: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var id_user: TextView
    private lateinit var username_fio: TextView
    private lateinit var role_username: TextView
    private lateinit var device_username: TextView
    private lateinit var username_login: TextView
    private var userId: Int = 0
    private var username: String = ""
    private var roleCheck: String = ""
    private var mdmCode: String = ""
    private var fio: String = ""
    private var deviceInfo: String = ""
    private val rolesList: MutableList<String> = mutableListOf()
    private lateinit var button_exit: Button
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val intent = intent
        username = intent.getStringExtra("username") ?: ""
        roleCheck = intent.getStringExtra("roleCheck") ?: ""
        userId = intent.getIntExtra("userId", 0)
        mdmCode = intent.getStringExtra("mdmCode") ?: ""
        fio = intent.getStringExtra("fio") ?: ""
        deviceInfo = intent.getStringExtra("deviceInfo") ?: ""
        id_user = findViewById(R.id.id_user)
        id_user.text = userId.toString()
        username_fio = findViewById(R.id.username_fio)
        username_fio.text = fio
        device_username = findViewById(R.id.device_username)
        device_username.text = deviceInfo
        username_login = findViewById(R.id.username_login)
        username_login.text = username
        //
        role_username = findViewById(R.id.role_username)
        val rolesDictionary = mapOf(
            "2" to "Администратор",
            "64" to "Диспетчер ОВЛ",
            "42" to "Логистика производства",
            "43" to "Специалист по логистике",
            "65" to "Транспортировщик ОВЛ"
        )
        val rolesString = intent.getStringExtra("rolesString") ?: ""
        rolesList.addAll(rolesString.split(",").map { it.trim() })
        val rolesText = rolesList.map { role ->
            rolesDictionary[role] ?: "Неизвестная роль: $role"
        }.joinToString(separator = "\n")
        role_username.text = rolesText
        rolesList.forEach { role ->
            Log.d("Список ролей", "Роль: $role")
        }
        //
        button_exit = findViewById(R.id.button_exit)
        button_exit.setOnClickListener {
//            val intent = Intent(this@SettingsActivity, MainActivity::class.java)
//            intent.putExtra("CLEAR_FIELDS", true)
//            startActivity(intent)
//            finish()
            logout()
        }
        //
//        go_to_authorization = findViewById(R.id.go_to_authorization)
//        go_to_authorization.setOnClickListener {
//            startActivity(Intent(this, MainActivity::class.java))
//        }
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@SettingsActivity, LogisticActivity::class.java).apply {
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
            val intent = Intent(this@SettingsActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
                putExtra("rolesString", rolesString)
            }
            startActivity(intent)
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            val intent = Intent(this@SettingsActivity, AddActivity::class.java).apply {
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
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            Toast.makeText(this@SettingsActivity, "Вы находитесь в окне с настройками", Toast.LENGTH_LONG).show()
        }
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
    private fun logout() {
        val sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE).edit()

        // Удаление данных о логине
        sharedPreferences.remove("username")
        sharedPreferences.remove("password")

        // Сброс флага авторизации
        sharedPreferences.putBoolean("isAuthorized", false).apply()

        val intent = Intent(this@SettingsActivity, MainActivity::class.java)

        // Добавление extra-данных для очистки полей (если нужно)
        intent.putExtra("CLEAR_FIELDS", true)

        startActivity(intent)

        finish()
    }
}