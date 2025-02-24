package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.example.semimanufactures.DatabaseManager.getAllSotrudnikiInfo
import com.example.semimanufactures.DatabaseManager.getFilteredSotrudnikiInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class NewLogisticDocActivity : ComponentActivity() {
    private var tag: String = NewLogisticActivity::class.java.simpleName
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var data_user_info: ImageView
    private var deviceInfo: String = ""
    private var userId: Int = 0
    private var username: String = ""
    private var roleCheck: String = ""
    private var mdmCode: String = ""
    private var fio: String = ""
    private var typeDoc: String = "doc"
    private lateinit var what_object: TextView
    private lateinit var when_go: TextView
    private lateinit var gruzchik: ImageView
    private lateinit var pogruzchik: ImageView
    private lateinit var sklad_otpravki: TextView
    private lateinit var otpravitel: AutoCompleteTextView
    private lateinit var phone_otpravitelya: EditText
    private lateinit var comment_otpravitelyu: EditText
    private lateinit var sklad_pribitiya: TextView
    private lateinit var priemshchik: AutoCompleteTextView
    private lateinit var phone_priemshchika: EditText
    private lateinit var comment_priemshchika: EditText
    private lateinit var primechanie: EditText
    private lateinit var button_save_new_logistic: Button
    private var responseBody: String = ""
    private var docValue: String = ""
    private lateinit var sotrudnikList: List<SotrudnikiInfo>
    private lateinit var assistant_status: String
    private var sotrudnikMdmcodeNew: String? = null
    private var sotrudnikFioNew: String? = null
    private var sotrudnikMdmcodeNewPriemshchik: String? = null
    private var sotrudnikFioNewPriemshchik: String? = null
    private lateinit var main_layout: ConstraintLayout
    private lateinit var card_view: CardView
    private lateinit var card_layout: LinearLayout
    private lateinit var scroll_view: ScrollView
    private val rolesList: MutableList<String> = mutableListOf()
    private lateinit var progressBar: ProgressBar
    private var senderName: String = ""
    private var receiverName: String = ""
    private var sendToTitle: String = ""
    private var sendFromTitle: String = ""
    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_logistic_doc)
        val intent = intent
        username = intent.getStringExtra("username") ?: ""
        val password = intent.getStringExtra("password") ?: ""
        roleCheck = intent.getStringExtra("roleCheck") ?: ""
        userId = intent.getIntExtra("userId", 0)
        mdmCode = intent.getStringExtra("mdmCode") ?: ""
        deviceInfo = intent.getStringExtra("deviceInfo") ?: ""
        fio = intent.getStringExtra("fio") ?: ""
        responseBody = intent.getStringExtra("responseBody") ?: ""
        docValue = intent.getStringExtra("box_id") ?: ""
        val rolesString = intent.getStringExtra("rolesString") ?: ""
        rolesList.addAll(rolesString.split(",").map { it.trim() })
        rolesList.forEach { role ->
            Log.d("Список ролей", "Роль: $role")
        }
        try {
            val jsonObject = JSONObject(responseBody)

            // Проверяем, есть ли ошибка в JSON-ответе
            if (jsonObject.has("error")) {
                val errorMessage = jsonObject.getString("error")
                Log.e(tag, "Ошибка с сервера: $errorMessage")
                Toast.makeText(this, "Для данного документа можно создать заявку только через сервис на ПК", Toast.LENGTH_LONG).show()
                finish()
                return
            }

            val plannedDate = jsonObject.getString("planned_date")
            sendToTitle = jsonObject.getString("send_to_title")
            sendFromTitle = jsonObject.getString("send_from_title")
            senderName = jsonObject.getString("sender_name")
            receiverName = jsonObject.getString("receiver_name")

            Log.d(tag, "Planned Date: $plannedDate")
            Log.d(tag, "Send To Title: $sendToTitle")
            Log.d(tag, "Send From Title: $sendFromTitle")
            Log.d(tag, "Sender Name: $senderName")
            Log.d(tag, "Receiver Name: $receiverName")

            what_object = findViewById(R.id.what_object)
            what_object.text = "Объект доставки: $docValue"
            when_go = findViewById(R.id.when_go)
            when_go.text = "Срок: $plannedDate"
            sklad_otpravki = findViewById(R.id.sklad_otpravki)
            sklad_otpravki.text = sendFromTitle
            sklad_pribitiya = findViewById(R.id.sklad_pribitiya)
            sklad_pribitiya.text = sendToTitle
            otpravitel = findViewById(R.id.otpravitel)
            priemshchik = findViewById(R.id.priemshchik)
            lifecycleScope.launch {
                try {
                    sotrudnikList = getFilteredSotrudnikiInfo(this@NewLogisticDocActivity, "")
                    setupSotrudnikOtpravitel()
                } catch (e: Exception) {
                    Log.e("Error", "Ошибка при получении информации о сотруднике: ${e.message}")
                }
            }
            lifecycleScope.launch {
                try {
                    sotrudnikList = getFilteredSotrudnikiInfo(this@NewLogisticDocActivity, "")
                    setupSotrudnikPriemshchik()
                } catch (e: Exception) {
                    Log.e("Error", "Ошибка при получении информации о сотруднике: ${e.message}")
                }
            }
        } catch (e: JSONException) {
            Log.e(tag, "Ошибка при парсинге JSON: ${e.message}", e)
            Toast.makeText(this, "Ошибка при получении данных с сервера. Попробуйте позже.", Toast.LENGTH_LONG).show()
            finish()
            return
        } catch (e: Exception) {
            Log.e(tag, "Неожиданная ошибка: ${e.message}", e)
            Toast.makeText(this, "Произошла ошибка. Попробуйте позже.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        phone_otpravitelya = findViewById(R.id.phone_otpravitelya)
        phone_otpravitelya.setText("+7")
        phone_otpravitelya.addTextChangedListener(PhoneTextWatcher(phone_otpravitelya))
        button_save_new_logistic = findViewById(R.id.button_save_new_logistic)
        sklad_pribitiya = findViewById(R.id.sklad_pribitiya)
        priemshchik = findViewById(R.id.priemshchik)
        phone_priemshchika = findViewById(R.id.phone_priemshchika)
        phone_priemshchika.setText("+7")
        phone_priemshchika.addTextChangedListener(PhoneTextWatcher(phone_priemshchika))
        main_layout = findViewById(R.id.main_layout)
        card_layout = findViewById(R.id.card_layout)
        scroll_view = findViewById(R.id.scroll_view)
        card_view = findViewById(R.id.card_view)
        otpravitel = findViewById(R.id.otpravitel)
        primechanie = findViewById(R.id.primechanie)
        progressBar = findViewById(R.id.progressBar)
        pogruzchik = findViewById(R.id.pogruzchik)
        gruzchik = findViewById(R.id.gruzchik)
        comment_otpravitelyu = findViewById(R.id.comment_otpravitelyu)
        comment_priemshchika = findViewById(R.id.comment_priemshchika)
        button_save_new_logistic = findViewById(R.id.button_save_new_logistic)
        progressBar = findViewById(R.id.progressBar)
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            Log.d("NewLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@NewLogisticDocActivity, LogisticActivity::class.java).apply {
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
            val intent = Intent(this@NewLogisticDocActivity, SettingsActivity::class.java).apply {
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
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            Log.d("NewLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@NewLogisticDocActivity, AddActivity::class.java).apply {
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
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            Log.d("NewLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@NewLogisticDocActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
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
        button_save_new_logistic.setOnClickListener {
            val jsonObject = JSONObject(responseBody)
            val id = jsonObject.getString("id")
            if (sklad_pribitiya.text.toString().isBlank()) {
                Toast.makeText(this@NewLogisticDocActivity, "Поле 'Склад прибытия' не может быть пустым", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else {
                // Показываем индикатор загрузки
                progressBar.visibility = View.VISIBLE
                button_save_new_logistic.isEnabled = false // Отключаем кнопку, чтобы избежать повторных нажатий

                updateLogistic(id)
            }
        }
        scroll_view = findViewById(R.id.scroll_view)
        card_layout = findViewById(R.id.card_layout)
        card_view = findViewById(R.id.card_view)
        main_layout = findViewById(R.id.main_layout)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("MainActivity", "Запуск получения версии...")
            val versionMobile = fetchMobileVersion(this@NewLogisticDocActivity)
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
                        Toast.makeText(this@NewLogisticDocActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
                    }
                }
            }
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
    private fun onGruzchikSelected() {
        if (assistant_status.contains("Грузчик")) {
            assistant_status = if (assistant_status.contains("и")) {
                assistant_status.replace("Грузчик и ", "").replace(", Грузчик", "").trim()
            } else {
                assistant_status.replace("Грузчик", "").trim()
            }
            gruzchik.setImageResource(R.drawable.circle)
            Log.d("AssistantStatus", "Грузчик сброшен. Текущий статус: $assistant_status")
        } else {
            if (assistant_status.isNotEmpty()) {
                if (assistant_status.contains("Погрузчик")) {
                    assistant_status = "Грузчик и Погрузчик"
                } else {
                    assistant_status += "Грузчик"
                }
            } else {
                assistant_status = "Грузчик"
            }
            gruzchik.setImageResource(R.drawable.remember)
            Log.d("AssistantStatus", "Грузчик выбран. Текущий статус: $assistant_status")
        }
    }
    private fun onPogruzchikSelected() {
        if (assistant_status.contains("Погрузчик")) {
            assistant_status = if (assistant_status.contains("и")) {
                assistant_status.replace("Погрузчик и ", "").replace(", Погрузчик", "").trim()
            } else {
                assistant_status.replace("Погрузчик", "").trim()
            }
            pogruzchik.setImageResource(R.drawable.circle)
            Log.d("AssistantStatus", "Погрузчик сброшен. Текущий статус: $assistant_status")
        } else {
            if (assistant_status.isNotEmpty()) {
                if (assistant_status.contains("Грузчик")) {
                    assistant_status = "Грузчик и Погрузчик"
                } else {
                    assistant_status += "Погрузчик"
                }
            } else {
                assistant_status = "Погрузчик"
            }
            pogruzchik.setImageResource(R.drawable.remember)
            Log.d("AssistantStatus", "Погрузчик выбран. Текущий статус: $assistant_status")
        }
    }
    private class PhoneTextWatcher(private val editText: EditText) : TextWatcher {
        private var isFormatting: Boolean = false
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (isFormatting) return
            isFormatting = true
            val cleaned = s.toString().replace(Regex("[^\\d]"), "")
            if (cleaned.isEmpty()) {
                editText.removeTextChangedListener(this)
                editText.setText("")
                editText.addTextChangedListener(this)
                isFormatting = false
                return
            }
            val formatted = StringBuilder()
            if (cleaned.isNotEmpty()) {
                formatted.append("+7")
            }
            when {
                cleaned.length in 1..3 -> {
                    formatted.append("(").append(cleaned.substring(1))
                    if (cleaned.length < 2) {
                        formatted.append(")")
                    }
                }
                cleaned.length in 4..6 -> {
                    formatted.append("(").append(cleaned.substring(1, 4)).append(")")
                    if (cleaned.length > 4) {
                        formatted.append(cleaned.substring(4))
                    }
                }
                cleaned.length in 7..9 -> {
                    formatted.append("(").append(cleaned.substring(1, 4)).append(")")
                    formatted.append(cleaned.substring(4, 7)).append("-")
                    if (cleaned.length > 7) {
                        formatted.append(cleaned.substring(7))
                    }
                }
                cleaned.length >= 10 -> {
                    formatted.append("(").append(cleaned.substring(1, 4)).append(")")
                    formatted.append(cleaned.substring(4, 7)).append("-")
                    if (cleaned.length > 9) {
                        formatted.append(cleaned.substring(7, 9)).append("-")
                    }
                    formatted.append(cleaned.substring(9).take(2))
                }
            }
            editText.removeTextChangedListener(this)
            editText.setText(formatted.toString())
            editText.setSelection(formatted.length)
            editText.addTextChangedListener(this)
            isFormatting = false
        }
    }
    private fun setupSotrudnikOtpravitel(){
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sotrudnikList.map { it.fio })
        otpravitel.setAdapter(adapter)
        otpravitel.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                otpravitel.showDropDown()
            }
        }
    }
    private fun setupSotrudnikPriemshchik(){
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, sotrudnikList.map { it.fio })
        priemshchik.setAdapter(adapter)
        priemshchik.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                priemshchik.showDropDown()
            }
        }
    }
    private fun updateSelectedSotrudnik(selectedSotrudnik: String) {
        val sotrudnikInfo = sotrudnikList.find { it.fio == selectedSotrudnik }
        if (sotrudnikInfo != null) {
            sotrudnikMdmcodeNew = sotrudnikInfo.mdmcode
            sotrudnikFioNew = sotrudnikInfo.fio
            Log.d("Сотрудник обновлен", """
                отправитель_mdmcode: $sotrudnikMdmcodeNew
                 отправитель_фио: $sotrudnikFioNew
            """.trimIndent())
        }
        else {
            Log.d("Сорудник не найден", "Выбранный сотрудник '$selectedSotrudnik' не найден.")
        }
    }
    private fun updateSelectedSotrudnikPriemshchik(selectedSotrudnik: String) {
        val sotrudnikInfo = sotrudnikList.find { it.fio == selectedSotrudnik }
        if (sotrudnikInfo != null) {
            sotrudnikMdmcodeNewPriemshchik = sotrudnikInfo.mdmcode
            sotrudnikFioNewPriemshchik = sotrudnikInfo.fio
            Log.d("Сотрудник обновлен", """
                получатель_mdmcode: $sotrudnikMdmcodeNewPriemshchik
                 получатель_фио: $sotrudnikFioNewPriemshchik
            """.trimIndent())
        }
        else {
            Log.d("Сорудник не найден", "Выбранный сотрудник '$selectedSotrudnik' не найден.")
        }
    }
    private fun fetchSotrudnikiInfo() {
        lifecycleScope.launch {
            try {
                val sotrudnikiList = getAllSotrudnikiInfo(this@NewLogisticDocActivity)
                for (sotrudnik in sotrudnikiList) {
                    Log.d("SotrudnikiInfo", "MdmCode: ${sotrudnik.mdmcode}, FIO: ${sotrudnik.fio}")
                }
            } catch (e: Exception) {
                Log.e("Error", "Ошибка при получении данных: ${e.message}")
            }
        }
    }
    private fun updateLogistic(id: String) {
        if (id.isEmpty()) return
        val url = "http://192.168.200.250/api/update_logistic/$id"
        Log.d(tag, "Request URL: $url")
        val currentDateTime = getCurrentDateTime()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("send_from_title", sklad_otpravki.text.toString())
            .addFormDataPart("send_comment", comment_otpravitelyu.text.toString())
            .addFormDataPart("sender_phone", phone_otpravitelya.text.toString())
            .addFormDataPart("receive_comment", comment_priemshchika.text.toString())
            .addFormDataPart("receiver_phone", phone_priemshchika.text.toString())
            .addFormDataPart("comment", primechanie.text.toString())
            .addFormDataPart("loader", assistant_status)
            .addFormDataPart("sender_mdm", sotrudnikMdmcodeNew ?: "")
            .addFormDataPart("is_accepted_by", mdmCode)
            .addFormDataPart("is_accepted_at", currentDateTime)
            .addFormDataPart("status", "1")
            .addFormDataPart("created_by", mdmCode)
            .addFormDataPart("receiver_mdm", sotrudnikMdmcodeNewPriemshchik ?: "")
        requestBody.addFormDataPart("sender_mdm", sotrudnikMdmcodeNew ?: "")
        requestBody.addFormDataPart("receiver_mdm", sotrudnikMdmcodeNewPriemshchik ?: "")
        val request = Request.Builder()
            .url(url)
            .post(requestBody.build())
            .build()
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS) // Установка таймаута на соединение
            .readTimeout(10, TimeUnit.SECONDS) // Установка таймаута на чтение
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(tag, "Ошибка во время обновления: ${e.message}")
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    button_save_new_logistic.isEnabled = true
                    if (e is SocketTimeoutException) {
                        Toast.makeText(this@NewLogisticDocActivity, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@NewLogisticDocActivity, "Ошибка во время обновления: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBodyString = response.body?.string() ?: return
                Log.d("ResponseBody: ", responseBodyString)
                progressBar.visibility = View.GONE
                button_save_new_logistic.isEnabled = true
                if (response.isSuccessful) {
                    Log.d(tag, "Обновление успешно: $responseBodyString")
                    runOnUiThread {
                        Toast.makeText(this@NewLogisticDocActivity, "Данные успешно сохранились", Toast.LENGTH_SHORT).show()
                        val context = this@NewLogisticDocActivity
                        val rolesString = intent.getStringExtra("rolesString") ?: ""
                        rolesList.addAll(rolesString.split(",").map { it.trim() })
                        rolesList.forEach { role ->
                            Log.d("Список ролей", "Роль: $role")
                        }
                        val intent = Intent(context, DetailLogisticsActivity::class.java).apply {
                            putExtra("logistics_id", id)
                            putExtra("mdmCode", mdmCode)
                            putExtra("userId", userId)
                            putExtra("username", username)
                            putExtra("roleCheck", roleCheck)
                            putExtra("fio", fio)
                            putExtra("deviceInfo", deviceInfo)
                            putExtra("type", typeDoc)
                            putExtra("rolesString", rolesString)
                        }
                        context.startActivity(intent)
                        finish()
                    }
                } else {
                    Log.e(tag, "Обновление не удалось: ${response.code} - ${response.message}")
                    Log.e(tag, "Тело ошибки: $responseBodyString")
                }
                response.headers.forEach { header ->
                    Log.d(tag, "${header.first}: ${header.second}")
                }
            }
        })
    }
    private fun getCurrentDateTime(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }
    private fun disableUI() {
        go_to_logistic.isEnabled = false
        what_object.isEnabled = false
        when_go.isEnabled = false
        gruzchik.isEnabled = false
        pogruzchik.isEnabled = false
//        go_to_authorization.isEnabled = false
        sklad_otpravki.isEnabled = false
        go_to_issue.isEnabled = false
        otpravitel.isEnabled = false
        phone_otpravitelya.isEnabled = false
        comment_otpravitelyu.isEnabled = false
        sklad_pribitiya.isEnabled = false
        priemshchik.isEnabled = false
        phone_priemshchika.isEnabled = false
        primechanie.isEnabled = false
        comment_priemshchika.isEnabled = false
        button_save_new_logistic.isEnabled = false
        go_to_add.isEnabled = false
        go_to_issue.isEnabled = false
        data_user_info.isEnabled = false
        go_to_send_notification.isEnabled = false
        card_view.isEnabled = false
        card_layout.isEnabled = false
        scroll_view.isEnabled = false
        Toast.makeText(this, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
    }

}
