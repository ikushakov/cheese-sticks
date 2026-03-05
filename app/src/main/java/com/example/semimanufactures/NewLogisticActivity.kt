package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.lifecycleScope
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
import com.example.semimanufactures.DatabaseManager.getAllSotrudnikiInfo
import com.example.semimanufactures.DatabaseManager.getFilteredSotrudnikiInfo
import com.example.semimanufactures.DatabaseManager.getFilteredWarehousesInfo
import com.example.semimanufactures.service_mode.ServiceModeException
import com.google.gson.Gson
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
import java.util.Calendar
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

class NewLogisticActivity : ComponentActivity() {
    private var tag: String = NewLogisticActivity::class.java.simpleName
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var data_user_info: ImageView
    private var typePrP: String = "prp"
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
    private var selectedOperation: String = ""
    private var selectedDemand: String = ""
    private var selectedUchastok: String = ""
    private var selectedNext_podrazd_mdm_code: String = ""
    private var selectedOperation2: String = ""
    private var selectedPodrazd_mdm_code: String = ""
    private var scannedValue: String = ""
    private lateinit var assistant_status: String
    private var skladName: String = ""
    private var skladShelf: String = ""
    private var skladUnit: String = ""
    private var responseBody: String = ""
    private var mdmcodeOtpravitelya: String = ""
    private var mdmcodePoluchatelya: String = ""
    private var selectedZahodNomer: String = ""
    private lateinit var warehouseList: List<WarehouseInfo>
    private var selectedSladRuchkamiID: String = ""
    private var selectedSladRuchkamiNaimenovanie: String = ""
    private var sendToNewSklad: String? = null
    private var sendToTitleNewSklad: String? = null
    private var skladToNameNewSklad: String? = null
    private var skladToAddressNewSklad: String? = null
    private var skladToRespNameNewSklad: String? = null
    private var skladToRespDeptNameNewSklad: String? = null
    private var skladToPurposeNewSklad: String? = null
    private lateinit var sotrudnikList: List<SotrudnikiInfo>
    private var sotrudnikMdmcodeNew: String? = null
    private var sotrudnikFioNew: String? = null
    private var sotrudnikMdmcodeNewPriemshchik: String? = null
    private var sotrudnikFioNewPriemshchik: String? = null
    private var selectedSotrudnikRuchkamiMdmcode: String = ""
    private var selectedSotrudnikRuchkamiFio: String = ""
    private var selectedSotrudnikRuchkamiMdmcodePriemshchik: String = ""
    private var selectedSotrudnikRuchkamiFioPriemshchik: String = ""
    private lateinit var main_layout: ConstraintLayout
    private lateinit var card_view: LinearLayout
    private lateinit var card_layout: LinearLayout
    private lateinit var scroll_view: ScrollView
    private var currentUsername: String? = null
    private var currentUserId: Int? = null
    private var currentRoleCheck: String? = null
    private var currentMdmCode: String? = null
    private var currentFio: String? = null
    private var currentDeviceInfo: String? = null
    private var currentRolesString: String? = null
    private var currentDeviceToken: String? = null
    private var currentIsAuthorized: Boolean = false
    private val rolesList: MutableList<String> = mutableListOf()
    private lateinit var progressBar: ProgressBar
    private var lastSelectedOtpravitel: String? = null
    private var lastSelectedPriemshchik: String? = null
    private var isPhoneOtpravitelyaEdited = false
    private var isPhonePriemshchikaEdited = false

//    private val apiUrl = "https://api.gkmmz.ru/api/get_all_skladi"
//    private val httpClient: OkHttpClient by lazy {
//        OkHttpClient.Builder()
//            .connectTimeout(30, TimeUnit.SECONDS)
//            .readTimeout(30, TimeUnit.SECONDS)
//            .writeTimeout(30, TimeUnit.SECONDS)
//            .sslSocketFactory(getUnsafeSSLSocketFactory(), getUnsafeTrustManager())
//            .hostnameVerifier { _, _ -> true }
//            .build()
//    }

    private var senderFio: String? = null
    private var receiverFio: String? = null
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
        setContentView(R.layout.activity_new_logistic)
        val intent = intent
        selectedOperation = intent.getStringExtra("selectedOperation") ?: ""
        selectedDemand = intent.getStringExtra("selectedDemand") ?: ""
        selectedUchastok = intent.getStringExtra("selectedUchastok") ?: ""
        selectedPodrazd_mdm_code = intent.getStringExtra("selectedPodrazd_mdm_code") ?: ""
        selectedOperation2 = intent.getStringExtra("selectedOperation2") ?: ""
        if (currentRolesString?.isNotEmpty() == true) {
            rolesList.addAll(currentRolesString!!.split(",").map { it.trim() })
        }
        scannedValue = intent.getStringExtra("scannedValue") ?: ""
        selectedNext_podrazd_mdm_code = intent.getStringExtra("selectedNext_podrazd_mdm_code") ?: ""
        skladName = intent.getStringExtra("skladName") ?: ""
        skladShelf = intent.getStringExtra("skladShelf") ?: ""
        skladUnit = intent.getStringExtra("skladUnit") ?: ""
        responseBody = intent.getStringExtra("responseBody") ?: ""
        selectedZahodNomer = intent.getStringExtra("selectedZahodNomer") ?: ""
        val sendFrom = intent.getStringExtra("send_from") ?: ""
        val sendFromTitle = intent.getStringExtra("send_from_title") ?: ""
        Log.d("Склад отправки", "$scannedValue")
        Log.d(
            "Наши данные: ",
            "Роль колчана - $currentRoleCheck," +
                    " Username колчана - $currentUsername," +
                    " Mdmcode колчана - $currentMdmCode," +
                    " Device колчана - $currentDeviceInfo," +
                    " ФИО колчана - $currentFio," +
                    " Выбранная операция для колчана - $selectedOperation," +
                    " Полное название ПРП - $selectedDemand," +
                    " Участок, на котором лежит ПРП - $selectedUchastok," +
                    " mdmcode подразделения - $selectedPodrazd_mdm_code," +
                    " Много циферек для операции - $selectedOperation2," +
                    " Id склада: $scannedValue," +
                    " Наименование склада - $skladName," +
                    " Полка - $skladShelf," +
                    " Стеллаж - $skladUnit," +
                    " mdmcode следующего подразделения - $selectedNext_podrazd_mdm_code," +
                    " Ответ(созданная заявка): $responseBody," +
                    " Номер захода: $selectedZahodNomer"
        )

        val jsonObject = JSONObject(responseBody)
        if (jsonObject.has("id")) {
            val id = jsonObject.getString("id")
            val sendTo = jsonObject.getString("send_to")
            val sendToTitle = jsonObject.getString("send_to_title")
            val plannedDate = jsonObject.getString("planned_date")
            val createdBy = jsonObject.getString("created_by")
            val senderMdm = jsonObject.getString("sender_mdm")
            val senderName = jsonObject.optString("sender_name", null).takeIf { it.isNotEmpty() && it != "null" }
            val receiverMdm = jsonObject.getString("receiver_mdm")
            val receiverName = jsonObject.optString("receiver_name", null).takeIf { it.isNotEmpty() && it != "null" }
            val sender_phone = jsonObject.getString("sender_phone")
            val receiver_phone = jsonObject.getString("receiver_phone")
            mdmcodeOtpravitelya = senderMdm
            mdmcodePoluchatelya = receiverMdm
            selectedSladRuchkamiID = sendTo
            selectedSladRuchkamiNaimenovanie = sendToTitle
            selectedSotrudnikRuchkamiMdmcode = senderMdm
            selectedSotrudnikRuchkamiFio = senderName ?: "Неизвестно"
            selectedSotrudnikRuchkamiMdmcodePriemshchik = receiverMdm
            selectedSotrudnikRuchkamiFioPriemshchik = receiverName ?: "Неизвестно"

            Log.d(tag, "ID: $id")
            Log.d(tag, "Send To: $sendTo")
            Log.d(tag, "Send To Title: $sendToTitle")
            Log.d(tag, "Planned Date: $plannedDate")
            Log.d(tag, "Created By: $createdBy")
            Log.d(tag, "Sender MDM: $senderMdm")
            Log.d(tag, "Sender Name: $senderName")
            Log.d(tag, "Receiver MDM: $receiverMdm")
            Log.d(tag, "Receiver Name: $receiverName")
            what_object = findViewById(R.id.what_object)
            what_object.text = selectedDemand
            when_go = findViewById(R.id.when_go)
            when_go.text = plannedDate
            when_go.setOnClickListener {
                showDatePickerDialog()
            }
            gruzchik = findViewById(R.id.gruzchik)
            pogruzchik = findViewById(R.id.pogruzchik)
            sklad_otpravki = findViewById(R.id.sklad_otpravki)
            
            // Если переданы send_from и send_from_title, используем их, иначе используем данные склада
            if (sendFromTitle.isNotEmpty()) {
                sklad_otpravki.text = sendFromTitle
            } else if (skladName.isNotEmpty() && skladUnit.isNotEmpty() && skladShelf.isNotEmpty()) {
                sklad_otpravki.text = "$skladName / $skladUnit / $skladShelf"
            } else {
                sklad_otpravki.text = sendFrom.ifEmpty { scannedValue }
            }
            otpravitel = findViewById(R.id.otpravitel)
            otpravitel.setText(senderName ?: "Неизвестно")

//            if (senderName.isNotEmpty() && senderName != "null") {
//                // если в ответе уже есть имя — делаем поле readonly
//                otpravitel.isEnabled = false
//                otpravitel.isFocusable = false
//                otpravitel.isClickable = false
//                otpravitel.setBackgroundColor(Color.TRANSPARENT)
//            } else {
//                // иначе подгружаем только ответственного за склад sendFrom = scannedValue
//                lifecycleScope.launch {
//                    try {
//                        val warehouseList = getFilteredWarehousesInfo(this@NewLogisticActivity)
//                        val warehouse = warehouseList.firstOrNull { it.id.toString() == scannedValue }
//                        val responsibleFio = warehouse?.responsibleName.orEmpty()
//                        Log.d(tag, "Склад sendFrom=$scannedValue → ОтветственныйФИО='$responsibleFio'")
//
//                        if (responsibleFio.isNotBlank()) {
//                            sotrudnikList = getFilteredSotrudnikiInfo(this@NewLogisticActivity, responsibleFio)
//                            setupSotrudnikOtpravitel()
//                            otpravitel.setText(responsibleFio)
//                            otpravitel.isEnabled = false
//                            otpravitel.isFocusable = false
//                            otpravitel.isClickable = false
//                        } else {
//                            // если не нашли — список остаётся пустым
//                            sotrudnikList = emptyList()
//                            setupSotrudnikOtpravitel()
//                        }
//                        Log.d(tag, "Отфильтрованные отправители: ${sotrudnikList.map { it.fio }}")
//                    } catch (e: Exception) {
//                        Log.e(tag, "Ошибка загрузки отправителя: ${e.message}")
//                    }
//                }
//            }
            otpravitel.setOnItemClickListener { parent, view, position, id ->
                updateSelectedSotrudnik(parent.getItemAtPosition(position) as String)
            }
            phone_otpravitelya = findViewById(R.id.phone_otpravitelya)

            phone_otpravitelya.setText(sender_phone)
            phone_otpravitelya.addTextChangedListener(PhoneTextWatcher(phone_otpravitelya))
            comment_otpravitelyu = findViewById(R.id.comment_otpravitelyu)
            sklad_pribitiya = findViewById(R.id.sklad_pribitiya)
            sklad_pribitiya.setText(sendToTitle)
            lifecycleScope.launch {
                try {
                    warehouseList = getFilteredWarehousesInfo(this@NewLogisticActivity)
                    //setupSkladPribitiya()
                } catch (e: Exception) {
                    Log.e("Error", "Ошибка при получении информации о складах: ${e.message}")
                }
            }
//            sklad_pribitiya.setOnItemClickListener { parent, view, position, id ->
//                val selectedWarehouseName = parent.getItemAtPosition(position) as String
//                updateSelectedWarehouse(selectedWarehouseName)
//            }
            priemshchik = findViewById(R.id.priemshchik)
            priemshchik.setText(receiverName ?: "Неизвестно")

            lifecycleScope.launch {
                try {
                    // 1. Подтягиваем весь JSON один раз
                    val allWarehousesJson = fetchAllWarehouses()

                    // 2. Для отправителя — если send_from = "Погрузка/Разгрузка", не ищем ответственного
                    if (sendFrom == "Погрузка/Разгрузка" || sendFromTitle == "Погрузка/Разгрузка") {
                        // Для Погрузка/Разгрузка не заполняем отправителя автоматически
                        Log.d(tag, "Режим Погрузка/Разгрузка - отправитель не заполняется автоматически")
                    } else {
                        // Для отправителя — берем scannedValue или sendFrom
                        val warehouseId = if (scannedValue.isNotEmpty()) scannedValue else sendFrom
                        allWarehousesJson.findResponsibleInfoById(warehouseId)?.let { info ->
                            // заполняем AutoCompleteTextView и локальную переменную MDM-кода
                            otpravitel.setText(info.fio)
                            otpravitel.apply {
                                isEnabled = false; isFocusable = false; isClickable = false
                            }
                            // Запоминаем на уровне Activity
                            senderFio        = info.fio
                            sotrudnikMdmcodeNew = info.mdmCode
                            Log.d(tag, "Отправитель: fio='${info.fio}', mdm='${info.mdmCode}'")
                        } ?: Log.w(tag, "Не нашли ответственного за sendFrom=$warehouseId")
                        if (senderFio != null) {
                            otpravitel.setText(senderFio)
                            otpravitel.isEnabled = false
                            otpravitel.isFocusable = false
                            otpravitel.isClickable = false
                            Log.d(tag, "Отправитель: $senderFio")
                        } else {
                            Log.w(tag, "Не нашли ответственного за sendFrom=$warehouseId")
                            // Если отправитель не найден, устанавливаем "Неизвестно"
                            if (otpravitel.text.isNullOrEmpty() || otpravitel.text.toString() == "null") {
                                otpravitel.setText("Неизвестно")
                            }
                        }
                    }

                    // 3. Для приёмщика — берем sendTo (selectedSladRuchkamiID)
                    allWarehousesJson.findResponsibleInfoById(sendTo)?.let { info ->
                        priemshchik.setText(info.fio)
                        priemshchik.apply {
                            isEnabled = false; isFocusable = false; isClickable = false
                        }
                        receiverFio            = info.fio
                        sotrudnikMdmcodeNewPriemshchik = info.mdmCode
                        Log.d(tag, "Приёмщик: fio='${info.fio}', mdm='${info.mdmCode}'")
                    } ?: Log.w(tag, "Не нашли ответственного за sendTo=$sendTo")
                    if (receiverFio != null) {
                        priemshchik.setText(receiverFio)
                        priemshchik.isEnabled = false
                        priemshchik.isFocusable = false
                        priemshchik.isClickable = false
                        Log.d(tag, "Приёмщик: $receiverFio")
                    } else {
                        Log.w(tag, "Не нашли ответственного за sendTo=$selectedSladRuchkamiID")
                        // Если получатель не найден, устанавливаем "Неизвестно"
                        if (priemshchik.text.isNullOrEmpty() || priemshchik.text.toString() == "null") {
                            priemshchik.setText("Неизвестно")
                        }
                    }

                    // Если нужно — можно сохранить mdm-коды или телефоны, если они в JSON,
                    // но в вашем примере там только ФИО по ключу "ОтветственныйФИО".

                } catch (e: Exception) {
                    Log.e(tag, "Ошибка при загрузке списка складов: ${e.message}")
                }
            }
            priemshchik.setOnItemClickListener { parent, view, position, id ->
                updateSelectedSotrudnikPriemshchik(parent.getItemAtPosition(position) as String)
            }
            phone_priemshchika = findViewById(R.id.phone_priemshchika)

            phone_priemshchika.setText(receiver_phone)
            phone_priemshchika.addTextChangedListener(PhoneTextWatcher(phone_priemshchika))
            comment_priemshchika = findViewById(R.id.comment_priemshchika)
            primechanie = findViewById(R.id.primechanie)
            button_save_new_logistic = findViewById(R.id.button_save_new_logistic)

            gruzchik.setOnClickListener {
                onGruzchikSelected()
            }
            pogruzchik.setOnClickListener {
                onPogruzchikSelected()
            }
            assistant_status = ""
            fetchSotrudnikiInfo()
            setupPhoneListeners()
            scroll_view = findViewById(R.id.scroll_view)
            card_layout = findViewById(R.id.card_layout)
            card_view = findViewById(R.id.card_view)
            button_save_new_logistic.setOnClickListener {
                val jsonObject = JSONObject(responseBody)
                val id = jsonObject.getString("id")
                if (sklad_pribitiya.text.toString().isBlank()) {
                    Toast.makeText(
                        this@NewLogisticActivity,
                        "Поле 'Склад прибытия' не может быть пустым",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                } else {
                    progressBar.visibility = View.VISIBLE
                    button_save_new_logistic.isEnabled = false
                    updateLogistic(id)
                }
            }
        } else {
            finish()
            Toast.makeText(
                this,
                "Для данной ПрП можно создать заявку только через веб-сервис!",
                Toast.LENGTH_LONG
            ).show()
        }
        // конец
        progressBar = findViewById(R.id.progressBar)
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@NewLogisticActivity, LogisticActivity::class.java)
            startActivity(intent)
        }
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            val intent = Intent(this@NewLogisticActivity, SettingsActivity::class.java)
            startActivity(intent)
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            val intent = Intent(this@NewLogisticActivity, AddActivity::class.java)
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            val intent = Intent(this@NewLogisticActivity, NotificationActivity::class.java)
            startActivity(intent)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            val intent =
                Intent(this@NewLogisticActivity, FeaturesOfTheFunctionalityActivity::class.java)
            startActivity(intent)
        }
        main_layout = findViewById(R.id.main_layout)
    }

    private fun onGruzchikSelected() {
        if (assistant_status.contains("Грузчик")) {
            assistant_status = if (assistant_status.contains("и")) {
                assistant_status.replace("Грузчик и ", "").replace(", Грузчик", "").trim()
            } else {
                assistant_status.replace("Грузчик", "").trim()
            }
            gruzchik.setImageResource(R.drawable.no_remember_me_svg)
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
            gruzchik.setImageResource(R.drawable.remember_me_svg)
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
            pogruzchik.setImageResource(R.drawable.no_remember_me_svg)
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
            pogruzchik.setImageResource(R.drawable.remember_me_svg)
            Log.d("AssistantStatus", "Погрузчик выбран. Текущий статус: $assistant_status")
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

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog =
            DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                val formattedDate = formatDate(selectedDay, selectedMonth + 1, selectedYear)
                when_go.setText(formattedDate)
            }, year, month, day)
        datePickerDialog.show()
    }

    private fun formatDate(day: Int, month: Int, year: Int): String {
        return "$year-$month-$day"
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

    private fun updateLogistic(id: String) {
        if (id.isEmpty()) {
            progressBar.visibility = View.GONE
            button_save_new_logistic.isEnabled = true
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val client = (application as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            val primaryUrl  = "https://api.gkmmz.ru/api/update_logistic_objects/$id"
            val fallbackUrl = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/update_logistic_objects/$id"

            val currentDateTime = getCurrentDateTime()

            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                //.addFormDataPart("send_from", scannedValue)
                //.addFormDataPart("send_from_title", sklad_otpravki.text.toString())
                //.addFormDataPart("planned_date", when_go.text.toString())
                .addFormDataPart("send_comment", comment_otpravitelyu.text.toString())
//                .addFormDataPart("sender_phone",
//                    if (phone_otpravitelya.text.toString() == "+7" || phone_otpravitelya.text.isEmpty()) "" else phone_otpravitelya.text.toString()
//                )
//                .addFormDataPart("receiver_phone",
//                    if (phone_priemshchika.text.toString() == "+7" || phone_priemshchika.text.isEmpty()) "" else phone_priemshchika.text.toString()
//                )
                .addFormDataPart("receive_comment", comment_priemshchika.text.toString())
                .addFormDataPart("comment", primechanie.text.toString())
                .addFormDataPart("loader", assistant_status)
                .addFormDataPart("is_accepted_by", currentMdmCode ?: "")
                .addFormDataPart("is_accepted_at", currentDateTime)
                .addFormDataPart("status", "1")
                .addFormDataPart("created_by_name", currentFio ?: "")
//                .addFormDataPart("sender_mdm",   sotrudnikMdmcodeNew ?: "")
//                .addFormDataPart("receiver_mdm", sotrudnikMdmcodeNewPriemshchik ?: "")
                .addFormDataPart("executor", currentMdmCode ?: "")
                .addFormDataPart("mdm_code", currentMdmCode ?: "")
                .addFormDataPart("version_name", version_name)
                .build()

            val base = Request.Builder()
                .post(body)
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token",  authToken)

            fun exec(url: String): Response? = try {
                val req = base.url(url).build()
                client.newCall(req).execute()
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
                        progressBar.visibility = View.GONE
                        button_save_new_logistic.isEnabled = true
                        Toast.makeText(this@NewLogisticActivity, "Все серверы недоступны", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                resp.use { r ->
                    val bodyStr = r.body?.string().orEmpty()
                    Log.d("updateLogistic", "ResponseBody: $bodyStr")

                    withContext(Dispatchers.Main) {
                        progressBar.visibility = View.GONE
                        button_save_new_logistic.isEnabled = true
                    }

                    if (r.isSuccessful) {
                        Log.d("updateLogistic", "Обновление успешно")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@NewLogisticActivity, "Данные успешно сохранились", Toast.LENGTH_SHORT).show()

                            val context = this@NewLogisticActivity
                            val rolesString = intent.getStringExtra("rolesString") ?: ""
                            rolesList.addAll(rolesString.split(",").map { it.trim() })

                            val intent = Intent(context, DetailLogisticsActivity::class.java).apply {
                                putExtra("logistics_id", id)
                                putExtra("type", typePrP)
                            }
                            context.startActivity(intent)
                            finish()
                        }
                    } else {
                        Log.e("updateLogistic", "Обновление не удалось: ${r.code} - ${r.message}")
                        Log.e("updateLogistic", "Тело ошибки: $bodyStr")
                        for (i in 0 until r.headers.size) {
                            val name  = r.headers.name(i)
                            val value = r.headers.value(i)
                            Log.d("updateLogistic", "$name: $value")
                        }

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@NewLogisticActivity, "Ошибка сервера: ${r.code}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: ServiceModeException) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    button_save_new_logistic.isEnabled = true
                }
                // Экран техработ уже открыт интерсептором
            } catch (t: Throwable) {
                Log.e("updateLogistic", "Ошибка во время обновления", t)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    button_save_new_logistic.isEnabled = true
                    Toast.makeText(this@NewLogisticActivity, "Ошибка: ${t.message}", Toast.LENGTH_LONG).show()
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

//    private fun setupSkladPribitiya() {
//        val adapter = ArrayAdapter(
//            this,
//            android.R.layout.simple_dropdown_item_1line,
//            warehouseList.map { it.name })
//        sklad_pribitiya.setAdapter(adapter)
//        sklad_pribitiya.setOnFocusChangeListener { _, hasFocus ->
//            if (hasFocus) {
//                sklad_pribitiya.showDropDown()
//            }
//        }
//    }

    private fun setupSotrudnikOtpravitel() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            sotrudnikList.map { it.fio })
        otpravitel.setAdapter(adapter)
        otpravitel.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                otpravitel.showDropDown()
            }
        }
    }

    private fun setupSotrudnikPriemshchik() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line,
            sotrudnikList.map { it.fio })
        priemshchik.setAdapter(adapter)
        priemshchik.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                priemshchik.showDropDown()
            }
        }
    }

    private fun updateSelectedWarehouse(selectedWarehouse: String) {
        val warehouseInfo = warehouseList.find { it.name == selectedWarehouse }
        if (warehouseInfo != null) {
            sendToNewSklad = warehouseInfo.id.toString()
            sendToTitleNewSklad = warehouseInfo.name
            skladToNameNewSklad = warehouseInfo.name
            skladToAddressNewSklad = warehouseInfo.address
            skladToRespNameNewSklad = warehouseInfo.responsibleName
            skladToRespDeptNameNewSklad = warehouseInfo.plannerName
            skladToPurposeNewSklad = warehouseInfo.purpose
            Log.d(
                "Склад обновлен", """
            send_to: $sendToNewSklad
            send_to_title: $sendToTitleNewSklad
            sklad_to_name: $skladToNameNewSklad
            sklad_to_address: $skladToAddressNewSklad
            sklad_to_resp_name: $skladToRespNameNewSklad
            sklad_to_resp_dept_name: $skladToRespDeptNameNewSklad
            sklad_to_purpose: $skladToPurposeNewSklad
        """.trimIndent()
            )
        } else {
            Log.d("Склад не найден", "Выбранный склад '$selectedWarehouse' не найден.")
        }
    }

    private fun updateSelectedSotrudnik(selectedSotrudnik: String) {
        val sotrudnikInfo = sotrudnikList.find { it.fio == selectedSotrudnik }
        if (sotrudnikInfo != null) {

            val isSameOtpravitel = lastSelectedOtpravitel == selectedSotrudnik
            lastSelectedOtpravitel = selectedSotrudnik

            sotrudnikMdmcodeNew = sotrudnikInfo.mdmcode
            sotrudnikFioNew = sotrudnikInfo.fio

            if (!isSameOtpravitel && !isPhoneOtpravitelyaEdited && sotrudnikInfo.phone.isNotBlank()) {
                phone_otpravitelya.setText(formatPhoneNumber(sotrudnikInfo.phone))
            }
        }
    }

    private fun updateSelectedSotrudnikPriemshchik(selectedSotrudnik: String) {
        val sotrudnikInfo = sotrudnikList.find { it.fio == selectedSotrudnik }
        if (sotrudnikInfo != null) {

            val isSamePriemshchik = lastSelectedPriemshchik == selectedSotrudnik
            lastSelectedPriemshchik = selectedSotrudnik

            sotrudnikMdmcodeNewPriemshchik = sotrudnikInfo.mdmcode
            sotrudnikFioNewPriemshchik = sotrudnikInfo.fio

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
            digits.length == 11 && digits.startsWith("7") -> "+7 (${
                digits.substring(
                    1,
                    4
                )
            }) ${digits.substring(4, 7)}-${digits.substring(7, 9)}-${digits.substring(9)}"

            digits.length == 11 && digits.startsWith("8") -> "+7 (${
                digits.substring(
                    1,
                    4
                )
            }) ${digits.substring(4, 7)}-${digits.substring(7, 9)}-${digits.substring(9)}"

            digits.length == 10 -> "+7 (${digits.substring(0, 3)}) ${
                digits.substring(
                    3,
                    6
                )
            }-${digits.substring(6, 8)}-${digits.substring(8)}"

            else -> phone
        }
    }

    private fun fetchSotrudnikiInfo() {
        lifecycleScope.launch {
            try {
                val sotrudnikiList = getAllSotrudnikiInfo(this@NewLogisticActivity)
                for (sotrudnik in sotrudnikiList) {
                    //Log.d("SotrudnikiInfo", "MdmCode: ${sotrudnik.mdmcode}, FIO: ${sotrudnik.fio}")
                }
            } catch (e: Exception) {
                Log.e("Error", "Ошибка при получении данных: ${e.message}")
            }
        }
    }

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


    private val primaryWarehousesUrl  = "https://api.gkmmz.ru/api/get_all_skladi"
    private val fallbackWarehousesUrl = "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_all_skladi"

    private suspend fun fetchAllWarehouses(): JSONObject = withContext(Dispatchers.IO) {
        val client = (application as App).okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val base = Request.Builder()
            .get()
            .addHeader("X-Apig-AppCode", authTokenAPI)
            .addHeader("X-Auth-Token",  authToken)

        fun exec(url: String): Response? = try {
            client.newCall(base.url(url).build()).execute()
        } catch (e: IOException) {
            Log.w("fetchAllWarehouses", "IO error for $url: ${e.message}")
            null
        }

        try {
            var resp = exec(primaryWarehousesUrl)
            if (resp == null || resp.code == 429) {
                resp?.close()
                Log.w("fetchAllWarehouses", "fallback → $fallbackWarehousesUrl (reason: ${if (resp == null) "IO error" else "429"})")
                resp = exec(fallbackWarehousesUrl)
            }
            if (resp == null) throw IOException("No response from both URLs")

            resp.use { r ->
                if (!r.isSuccessful) throw IOException("HTTP ${r.code}")
                val body = r.body?.string().orEmpty()
                if (body.isBlank()) throw IOException("Empty body")
                JSONObject(body)
            }
        } catch (e: ServiceModeException) {
            // экран техработ покажет интерсептор
            throw IOException("Service mode active", e)
        }
    }

    data class ResponsibleInfo(val fio: String, val mdmCode: String)
    /** Ищет в JSON объекте склад с нужным id и возвращает его "ОтветственныйФИО" или null */
    private fun JSONObject.findResponsibleInfoById(id: String): ResponsibleInfo? {
        for (key in keys()) {
            val node = getJSONObject(key)
            if (node.optString("id") == id) {
                val fio     = node.optString("ОтветственныйФИО")
                val mdmCode = node.optString("ОтветственныйMDMкод")
                if (fio.isNotBlank() && mdmCode.isNotBlank()) {
                    return ResponsibleInfo(fio, mdmCode)
                }
                // даже если одно из полей пустое — можно вернуть неполный результат:
                return ResponsibleInfo(fio, mdmCode)
            }
        }
        return null
    }
}