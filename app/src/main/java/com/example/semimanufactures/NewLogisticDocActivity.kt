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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.cardview.widget.CardView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
//import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.example.semimanufactures.DatabaseManager.getAllSotrudnikiInfo
import com.example.semimanufactures.DatabaseManager.getFilteredSotrudnikiInfo
import com.example.semimanufactures.service_mode.ServiceModeException
import com.google.gson.Gson
import com.squareup.picasso.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
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

class NewLogisticDocActivity : ComponentActivity() {
    private var tag: String = NewLogisticActivity::class.java.simpleName
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var data_user_info: ImageView
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
    //
    private var lastSelectedOtpravitel: String? = null
    private var lastSelectedPriemshchik: String? = null
    private var isPhoneOtpravitelyaEdited = false
    private var isPhonePriemshchikaEdited = false
    //
    private val rolesList: MutableList<String> = mutableListOf()
    @SuppressLint("MissingInflatedId", "SetTextI18n")
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
        setContentView(R.layout.activity_new_logistic_doc)
        val intent = intent
        responseBody = intent.getStringExtra("responseBody") ?: ""
        docValue = intent.getStringExtra("box_id") ?: ""
        if (currentRolesString?.isNotEmpty() == true) {
            rolesList.addAll(currentRolesString!!.split(",").map { it.trim() })
        }
        // начало
        val jsonObject = JSONObject(responseBody)
        if (jsonObject.has("id")) {
            Log.d("Документ", "$jsonObject")
            val id = jsonObject.getString("id")
            val plannedDate = jsonObject.getString("planned_date")
            val sendToTitle = jsonObject.getString("send_to_title")
            val sendFromTitle = jsonObject.getString("send_from_title")
            Log.d("NewLogisticActivity", "ID доставки: $id")
            what_object = findViewById(R.id.what_object)
            what_object.text = "Объект доставки: $docValue"
            when_go = findViewById(R.id.when_go)
            when_go.text = "Срок: $plannedDate"
            gruzchik = findViewById(R.id.gruzchik)
            pogruzchik = findViewById(R.id.pogruzchik)
            gruzchik.setOnClickListener {
                onGruzchikSelected()
            }
            pogruzchik.setOnClickListener {
                onPogruzchikSelected()
            }
            assistant_status = ""
            fetchSotrudnikiInfo()
            sklad_otpravki = findViewById(R.id.sklad_otpravki)
            sklad_otpravki.text = sendFromTitle
            otpravitel = findViewById(R.id.otpravitel)
            lifecycleScope.launch {
                try {
                    sotrudnikList = getFilteredSotrudnikiInfo(this@NewLogisticDocActivity, "")
                    setupSotrudnikOtpravitel()
                } catch (e: Exception) {
                    Log.e("Error", "Ошибка при получении информации о сотруднике: ${e.message}")
                }
            }
            otpravitel.setOnItemClickListener { parent, view, position, id ->
                val selectedSotrudnik = parent.getItemAtPosition(position) as String
                updateSelectedSotrudnik(selectedSotrudnik)
            }
            phone_otpravitelya = findViewById(R.id.phone_otpravitelya)
            //phone_otpravitelya.setText("+7")
            phone_otpravitelya.addTextChangedListener(PhoneTextWatcher(phone_otpravitelya))
            comment_otpravitelyu = findViewById(R.id.comment_otpravitelyu)
            sklad_pribitiya = findViewById(R.id.sklad_pribitiya)
            sklad_pribitiya.text = sendToTitle
            priemshchik = findViewById(R.id.priemshchik)
            lifecycleScope.launch {
                try {
                    sotrudnikList = getFilteredSotrudnikiInfo(this@NewLogisticDocActivity, "")
                    setupSotrudnikPriemshchik()
                } catch (e: Exception) {
                    Log.e("Error", "Ошибка при получении информации о сотруднике: ${e.message}")
                }
            }
            priemshchik.setOnItemClickListener { parent, view, position, id ->
                val selectedSotrudnik = parent.getItemAtPosition(position) as String
                updateSelectedSotrudnikPriemshchik(selectedSotrudnik)
            }
            phone_priemshchika = findViewById(R.id.phone_priemshchika)
            //phone_priemshchika.setText("+7")
            phone_priemshchika.addTextChangedListener(PhoneTextWatcher(phone_priemshchika))
            setupPhoneListeners()
            comment_priemshchika = findViewById(R.id.comment_priemshchika)
            primechanie = findViewById(R.id.primechanie)
            button_save_new_logistic = findViewById(R.id.button_save_new_logistic)
            button_save_new_logistic.setOnClickListener {
                val jsonObject = JSONObject(responseBody)
                val id = jsonObject.getString("id")
                if (sklad_pribitiya.text.toString().isBlank()) {
                    Toast.makeText(this@NewLogisticDocActivity, "Поле 'Склад прибытия' не может быть пустым", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else {
                    updateLogistic(id)
                }
            }
        }
        else {
            finish()
            Toast.makeText(this, "Для данного документа можно создать заявку только через веб-сервис!", Toast.LENGTH_LONG).show()
        }
        // конец

        scroll_view = findViewById(R.id.scroll_view)
        card_layout = findViewById(R.id.card_layout)
        card_view = findViewById(R.id.card_view)
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@NewLogisticDocActivity, LogisticActivity::class.java)
            startActivity(intent)
        }
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            val intent = Intent(this@NewLogisticDocActivity, SettingsActivity::class.java)
            startActivity(intent)
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            val intent = Intent(this@NewLogisticDocActivity, AddActivity::class.java)
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            val intent = Intent(this@NewLogisticDocActivity, NotificationActivity::class.java)
            startActivity(intent)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            val intent = Intent(this@NewLogisticDocActivity, FeaturesOfTheFunctionalityActivity::class.java)
            startActivity(intent)
        }
        main_layout = findViewById(R.id.main_layout)
//        CoroutineScope(Dispatchers.Main).launch {
//            Log.d("MainActivity", "Запуск получения версии...")
//            val versionMobile = fetchMobileVersion(this@NewLogisticDocActivity)
//            Log.d("MainActivity", "Версия получена: $versionMobile")
//            if (versionMobile == null) {
//                Log.e("MainActivity", "Не удалось получить версию")
//                //disableUI()
//            } else {
//                Log.d("MainActivity", "Версия приложения: $versionMobile")
//                if (versionMobile.toInt() != myGlobalVariable) {
//                    Log.e("MainActivity", "Версии не совпадают. Доступ к функционалу отключен.")
//                    disableUI()
//                    main_layout.setOnClickListener {
//                        Toast.makeText(this@NewLogisticDocActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
//                    }
//                }
//            }
//        }
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
//    @SuppressLint("MissingInflatedId")
//    private fun showPopupMenuNotification(view: View) {
//        val popupView = layoutInflater.inflate(R.layout.custom_menu_notification, null)
//        val popupWindow = PopupWindow(popupView, 550, 450)
//        popupView.findViewById<LinearLayout>(R.id.item_write_sms).setOnClickListener {
//            val intent = Intent(this, CreateNotificationActivity::class.java)
//            startActivity(intent)
//            popupWindow.dismiss()
//        }
//        popupView.findViewById<LinearLayout>(R.id.item_incoming_sms).setOnClickListener {
//            Toast.makeText(this, "Входящие нажаты", Toast.LENGTH_SHORT).show()
//            popupWindow.dismiss()
//        }
//        popupView.findViewById<LinearLayout>(R.id.item_sent_sms).setOnClickListener {
//            Toast.makeText(this, "Отправленные нажаты", Toast.LENGTH_SHORT).show()
//            popupWindow.dismiss()
//        }
//        popupWindow.isFocusable = true
//        popupWindow.showAsDropDown(view)
//    }
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
    private fun setupSotrudnikOtpravitel() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            sotrudnikList.map { it.fio }) // Показываем только ФИО
        otpravitel.setAdapter(adapter)
        otpravitel.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                otpravitel.showDropDown()
            }
        }
    }

    private fun setupSotrudnikPriemshchik() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            sotrudnikList.map { it.fio }) // Показываем только ФИО
        priemshchik.setAdapter(adapter)
        priemshchik.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                priemshchik.showDropDown()
            }
        }
    }
    // Обновляем функцию выбора отправителя
    private fun updateSelectedSotrudnik(selectedSotrudnik: String) {
        val sotrudnikInfo = sotrudnikList.find { it.fio == selectedSotrudnik }
        if (sotrudnikInfo != null) {
            // Проверяем, изменился ли сотрудник
            val isSameOtpravitel = lastSelectedOtpravitel == selectedSotrudnik
            lastSelectedOtpravitel = selectedSotrudnik

            sotrudnikMdmcodeNew = sotrudnikInfo.mdmcode
            sotrudnikFioNew = sotrudnikInfo.fio

            // Обновляем телефон только если:
            // 1. Сотрудник изменился
            // 2. Пользователь не редактировал телефон вручную
            // 3. У сотрудника есть телефон
            if (!isSameOtpravitel && !isPhoneOtpravitelyaEdited && sotrudnikInfo.phone.isNotBlank()) {
                phone_otpravitelya.setText(formatPhoneNumber(sotrudnikInfo.phone))
            }
        }
    }
    // Обновляем функцию выбора получателя
    private fun updateSelectedSotrudnikPriemshchik(selectedSotrudnik: String) {
        val sotrudnikInfo = sotrudnikList.find { it.fio == selectedSotrudnik }
        if (sotrudnikInfo != null) {
            // Проверяем, изменился ли сотрудник
            val isSamePriemshchik = lastSelectedPriemshchik == selectedSotrudnik
            lastSelectedPriemshchik = selectedSotrudnik

            sotrudnikMdmcodeNewPriemshchik = sotrudnikInfo.mdmcode
            sotrudnikFioNewPriemshchik = sotrudnikInfo.fio

            // Обновляем телефон только если:
            // 1. Сотрудник изменился
            // 2. Пользователь не редактировал телефон вручную
            // 3. У сотрудника есть телефон
            if (!isSamePriemshchik && !isPhonePriemshchikaEdited && sotrudnikInfo.phone.isNotBlank()) {
                phone_priemshchika.setText(formatPhoneNumber(sotrudnikInfo.phone))
            }
        }
    }
    private fun formatPhoneNumber(phone: String): String {
        if (phone.isBlank()) return "+7"

        // Удаляем все нецифровые символы
        val digits = phone.replace(Regex("[^0-9]"), "")

        return when {
            digits.length == 11 && digits.startsWith("7") -> "+7 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7, 9)}-${digits.substring(9)}"
            digits.length == 11 && digits.startsWith("8") -> "+7 (${digits.substring(1, 4)}) ${digits.substring(4, 7)}-${digits.substring(7, 9)}-${digits.substring(9)}"
            digits.length == 10 -> "+7 (${digits.substring(0, 3)}) ${digits.substring(3, 6)}-${digits.substring(6, 8)}-${digits.substring(8)}"
            else -> phone // Возвращаем как есть, если формат не распознан
        }
    }
    private fun fetchSotrudnikiInfo() {
        lifecycleScope.launch {
            try {
                val sotrudnikiList = getAllSotrudnikiInfo(this@NewLogisticDocActivity)
                for (sotrudnik in sotrudnikiList) {
                    //Log.d("SotrudnikiInfo", "MdmCode: ${sotrudnik.mdmcode}, FIO: ${sotrudnik.fio}")
                }
            } catch (e: Exception) {
                Log.e("Error", "Ошибка при получении данных: ${e.message}")
            }
        }
    }
    private fun updateLogistic(id: String) {
        if (id.isEmpty()) return

        // Нормализация телефонов: "+7" или пусто -> ""
        fun normPhone(s: String): String =
            if (s == "+7" || s.isBlank()) "" else s

        CoroutineScope(Dispatchers.IO).launch {
            val client = (application as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            // primary → fallback (как в предыдущих примерах)
            val primaryUrl  = "https://api.gkmmz.ru/api/update_logistic_objects/$id"
            val fallbackUrl = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/update_logistic_objects/$id"

            val currentDateTime = getCurrentDateTime()

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("send_from_title", sklad_otpravki.text.toString())
                .addFormDataPart("send_comment",    comment_otpravitelyu.text.toString())
                .addFormDataPart("sender_phone",    normPhone(phone_otpravitelya.text.toString()))
                .addFormDataPart("receive_comment", comment_priemshchika.text.toString())
                .addFormDataPart("receiver_phone",  normPhone(phone_priemshchika.text.toString()))
                .addFormDataPart("comment",         primechanie.text.toString())
                .addFormDataPart("loader",          assistant_status)
                .addFormDataPart("sender_mdm",      sotrudnikMdmcodeNew ?: "")
                .addFormDataPart("receiver_mdm",    sotrudnikMdmcodeNewPriemshchik ?: "")
                .addFormDataPart("is_accepted_by",  currentMdmCode ?: "")
                .addFormDataPart("is_accepted_at",  currentDateTime)
                .addFormDataPart("status",          "1")
                .addFormDataPart("created_by",      currentMdmCode ?: "")
                .addFormDataPart("executor",        currentMdmCode ?: "")
                .addFormDataPart("mdm_code",        currentMdmCode ?: "")
                .addFormDataPart("version_name",    version_name)
                .build()

            val baseReq = Request.Builder()
                .post(body)
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token",  authToken)

            fun exec(url: String): Response? = try {
                client.newCall(baseReq.url(url).build()).execute()
            } catch (e: IOException) {
                Log.e("updateLogistic", "IO error for $url: ${e.message}")
                null
            }

            try {
                var resp = exec(primaryUrl)
                if (resp == null || resp.code == 429) {
                    resp?.close()
                    Log.w("updateLogistic", "fallback → $fallbackUrl (reason: ${if (resp == null) "IO error" else "429"})")
                    resp = exec(fallbackUrl)
                }
                if (resp == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@NewLogisticDocActivity, "Все серверы недоступны", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                resp.use { r ->
                    val bodyStr = r.body?.string().orEmpty()
                    Log.d("updateLogistic", "ResponseBody: $bodyStr")

                    // Корректное логирование заголовков без ошибки типов
                    for (i in 0 until r.headers.size) {
                        Log.d("updateLogistic", "${r.headers.name(i)}: ${r.headers.value(i)}")
                    }

                    if (r.isSuccessful) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@NewLogisticDocActivity, "Данные успешно сохранились", Toast.LENGTH_SHORT).show()

                            val context = this@NewLogisticDocActivity
                            val rolesString = intent.getStringExtra("rolesString") ?: ""
                            rolesList.addAll(rolesString.split(",").map { it.trim() })

                            val intent = Intent(context, DetailLogisticsActivity::class.java).apply {
                                putExtra("logistics_id", id)
                                putExtra("type", typeDoc)
                            }
                            context.startActivity(intent)
                            finish()
                        }
                    } else {
                        Log.e("updateLogistic", "Обновление не удалось: ${r.code} - ${r.message}")
                        Log.e("updateLogistic", "Тело ошибки: $bodyStr")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@NewLogisticDocActivity, "Ошибка сервера: ${r.code}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: ServiceModeException) {
                // Интерсептор уже показал экран техработ; здесь просто выходим
            } catch (t: Throwable) {
                Log.e("updateLogistic", "Unexpected error", t)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@NewLogisticDocActivity, "Ошибка: ${t.message}", Toast.LENGTH_LONG).show()
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

    private fun getCurrentDateTime(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }
    // Обновляем слушатели для полей телефонов
    private fun setupPhoneListeners() {
        phone_otpravitelya.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.toString() != "+7") {
                    isPhoneOtpravitelyaEdited = true
                }
            }
        })

        phone_priemshchika.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (s?.toString() != "+7") {
                    isPhonePriemshchikaEdited = true
                }
            }
        })
    }
}
