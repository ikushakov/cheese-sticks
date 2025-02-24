package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.DatePickerDialog
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
import com.example.semimanufactures.DatabaseManager.getFilteredWarehousesInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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

class NewLogisticActivity : ComponentActivity() {
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
    private var typePrP: String = "prp"
    private lateinit var what_object: TextView
    private lateinit var when_go: TextView
    private lateinit var gruzchik: ImageView
    private lateinit var pogruzchik: ImageView
    private lateinit var sklad_otpravki: TextView
    private lateinit var otpravitel: AutoCompleteTextView
    private lateinit var phone_otpravitelya: EditText
    private lateinit var comment_otpravitelyu: EditText
    private lateinit var sklad_pribitiya: AutoCompleteTextView
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
    private val rolesList: MutableList<String> = mutableListOf()
    private lateinit var progressBar: ProgressBar
    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_logistic)
        val intent = intent
        selectedOperation = intent.getStringExtra("selectedOperation") ?: ""
        selectedDemand = intent.getStringExtra("selectedDemand") ?: ""
        selectedUchastok = intent.getStringExtra("selectedUchastok") ?: ""
        selectedPodrazd_mdm_code = intent.getStringExtra("selectedPodrazd_mdm_code") ?: ""
        selectedOperation2 = intent.getStringExtra("selectedOperation2") ?: ""
        username = intent.getStringExtra("username") ?: ""
        roleCheck = intent.getStringExtra("roleCheck") ?: ""
        userId = intent.getIntExtra("userId", 0)
        mdmCode = intent.getStringExtra("mdmCode") ?: ""
        deviceInfo = intent.getStringExtra("deviceInfo") ?: ""
        fio = intent.getStringExtra("fio") ?: ""
        val rolesString = intent.getStringExtra("rolesString") ?: ""
        rolesList.addAll(rolesString.split(",").map { it.trim() })
        rolesList.forEach { role ->
            Log.d("Список ролей", "Роль: $role")
        }
        Log.d("NewLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
        scannedValue = intent.getStringExtra("scannedValue") ?: ""
        selectedNext_podrazd_mdm_code = intent.getStringExtra("selectedNext_podrazd_mdm_code") ?: ""
        skladName = intent.getStringExtra("skladName") ?: ""
        skladShelf = intent.getStringExtra("skladShelf") ?: ""
        skladUnit = intent.getStringExtra("skladUnit") ?: ""
        responseBody = intent.getStringExtra("responseBody") ?: ""
        selectedZahodNomer = intent.getStringExtra("selectedZahodNomer") ?: ""
        Log.d("Наши данные: ",
            "Роль колчана - $roleCheck," +
                    " Username колчана - $username," +
                    " Mdmcode колчана - $mdmCode," +
                    " Device колчана - $deviceInfo," +
                    " ФИО колчана - $fio," +
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
                    " Номер захода: $selectedZahodNomer")
        val jsonObject = JSONObject(responseBody)
        val id = jsonObject.getString("id")
        val sendTo = jsonObject.getString("send_to")
        val sendToTitle = jsonObject.getString("send_to_title")
        val plannedDate = jsonObject.getString("planned_date")
        val createdBy = jsonObject.getString("created_by")
        val senderMdm = jsonObject.getString("sender_mdm")
        val senderName = jsonObject.getString("sender_name")
        val receiverMdm = jsonObject.getString("receiver_mdm")
        val receiverName = jsonObject.getString("receiver_name")
        mdmcodeOtpravitelya = senderMdm
        mdmcodePoluchatelya = receiverMdm
        selectedSladRuchkamiID = sendTo
        selectedSladRuchkamiNaimenovanie = sendToTitle
        selectedSotrudnikRuchkamiMdmcode = senderMdm
        selectedSotrudnikRuchkamiFio = senderName
        selectedSotrudnikRuchkamiMdmcodePriemshchik = receiverMdm
        selectedSotrudnikRuchkamiFioPriemshchik = receiverName
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
        sklad_otpravki.text = "$skladName / $skladUnit / $skladShelf"
        otpravitel = findViewById(R.id.otpravitel)
        otpravitel.setText(senderName)
        lifecycleScope.launch {
            try {
                sotrudnikList = getFilteredSotrudnikiInfo(this@NewLogisticActivity, "")
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
        phone_otpravitelya.setText("+7")
        phone_otpravitelya.addTextChangedListener(PhoneTextWatcher(phone_otpravitelya))
        comment_otpravitelyu = findViewById(R.id.comment_otpravitelyu)
        sklad_pribitiya = findViewById(R.id.sklad_pribitiya)
        sklad_pribitiya.setText(sendToTitle)
        lifecycleScope.launch {
            try {
                warehouseList = getFilteredWarehousesInfo(this@NewLogisticActivity)
                setupSkladPribitiya()
            } catch (e: Exception) {
                Log.e("Error", "Ошибка при получении информации о складах: ${e.message}")
            }
        }
        sklad_pribitiya.setOnItemClickListener { parent, view, position, id ->
            val selectedWarehouseName = parent.getItemAtPosition(position) as String
            updateSelectedWarehouse(selectedWarehouseName)
        }
        priemshchik = findViewById(R.id.priemshchik)
        priemshchik.setText(receiverName)
        lifecycleScope.launch {
            try {
                sotrudnikList = getFilteredSotrudnikiInfo(this@NewLogisticActivity, "")
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
        phone_priemshchika.setText("+7")
        phone_priemshchika.addTextChangedListener(PhoneTextWatcher(phone_priemshchika))
        comment_priemshchika = findViewById(R.id.comment_priemshchika)
        primechanie = findViewById(R.id.primechanie)
        button_save_new_logistic = findViewById(R.id.button_save_new_logistic)
        progressBar = findViewById(R.id.progressBar)
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            Log.d("NewLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@NewLogisticActivity, LogisticActivity::class.java).apply {
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
            val intent = Intent(this@NewLogisticActivity, SettingsActivity::class.java).apply {
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
            val intent = Intent(this@NewLogisticActivity, AddActivity::class.java).apply {
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
            val intent = Intent(this@NewLogisticActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
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
        gruzchik.setOnClickListener {
            onGruzchikSelected()
        }
        pogruzchik.setOnClickListener {
            onPogruzchikSelected()
        }
        assistant_status = ""
        fetchSotrudnikiInfo()
        scroll_view = findViewById(R.id.scroll_view)
        card_layout = findViewById(R.id.card_layout)
        card_view = findViewById(R.id.card_view)
        button_save_new_logistic.setOnClickListener {
            val jsonObject = JSONObject(responseBody)
            val id = jsonObject.getString("id")
            if (sklad_pribitiya.text.toString().isBlank()) {
                Toast.makeText(this@NewLogisticActivity, "Поле 'Склад прибытия' не может быть пустым", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            else {
                updateLogistic(id)
            }
        }
        main_layout = findViewById(R.id.main_layout)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("MainActivity", "Запуск получения версии...")
            val versionMobile = fetchMobileVersion(this@NewLogisticActivity)
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
                        Toast.makeText(this@NewLogisticActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
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
    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
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
        if (id.isEmpty()) return
        val url = "http://192.168.200.250/api/update_logistic/$id"
        Log.d(tag, "Request URL: $url")
        val currentDateTime = getCurrentDateTime()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("send_from", scannedValue)
            .addFormDataPart("send_from_title", sklad_otpravki.text.toString())
            .addFormDataPart("planned_date", when_go.text.toString())
            .addFormDataPart("send_comment", comment_otpravitelyu.text.toString())
            .addFormDataPart("sender_phone", phone_otpravitelya.text.toString())
            .addFormDataPart("receive_comment", comment_priemshchika.text.toString())
            .addFormDataPart("receiver_phone", phone_priemshchika.text.toString())
            .addFormDataPart("comment", primechanie.text.toString())
            .addFormDataPart("loader", assistant_status)
            .addFormDataPart("sender_mdm", mdmcodeOtpravitelya)
            .addFormDataPart("is_accepted_by", mdmCode)
            .addFormDataPart("is_accepted_at", currentDateTime)
            .addFormDataPart("status", "1")
            .addFormDataPart("created_by_name", fio)
            .addFormDataPart("receiver_mdm", mdmcodePoluchatelya)
        requestBody.addFormDataPart("send_to", sendToNewSklad ?: selectedSladRuchkamiID)
        requestBody.addFormDataPart("send_to_title", sendToTitleNewSklad ?: selectedSladRuchkamiNaimenovanie)
        requestBody.addFormDataPart("sender_mdm", sotrudnikMdmcodeNew ?: selectedSotrudnikRuchkamiMdmcode)
        requestBody.addFormDataPart("receiver_mdm", sotrudnikMdmcodeNewPriemshchik ?: selectedSotrudnikRuchkamiMdmcodePriemshchik)
        val request = Request.Builder()
            .url(url)
            .post(requestBody.build())
            .build()
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(tag, "Ошибка во время обновления: ${e.message}")
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    if (e is SocketTimeoutException) {
                        Toast.makeText(this@NewLogisticActivity, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@NewLogisticActivity, "Ошибка во время обновления: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            override fun onResponse(call: Call, response: Response) {
                val responseBodyString = response.body?.string() ?: return
                Log.d("ResponseBody: ", responseBodyString)
                progressBar.visibility = View.GONE
                if (response.isSuccessful) {
                    Log.d(tag, "Обновление успешно: $responseBodyString")
                    runOnUiThread {
                        Toast.makeText(this@NewLogisticActivity, "Данные успешно сохранились", Toast.LENGTH_SHORT).show()
                        val context = this@NewLogisticActivity
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
                            putExtra("type", typePrP)
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
    private fun setupSkladPribitiya() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, warehouseList.map { it.name })
        sklad_pribitiya.setAdapter(adapter)
        sklad_pribitiya.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                sklad_pribitiya.showDropDown()
            }
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
            Log.d("Склад обновлен", """
            send_to: $sendToNewSklad
            send_to_title: $sendToTitleNewSklad
            sklad_to_name: $skladToNameNewSklad
            sklad_to_address: $skladToAddressNewSklad
            sklad_to_resp_name: $skladToRespNameNewSklad
            sklad_to_resp_dept_name: $skladToRespDeptNameNewSklad
            sklad_to_purpose: $skladToPurposeNewSklad
        """.trimIndent())
        } else {
            Log.d("Склад не найден", "Выбранный склад '$selectedWarehouse' не найден.")
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
                val sotrudnikiList = getAllSotrudnikiInfo(this@NewLogisticActivity)
                for (sotrudnik in sotrudnikiList) {
                    //Log.d("SotrudnikiInfo", "MdmCode: ${sotrudnik.mdmcode}, FIO: ${sotrudnik.fio}")
                }
            } catch (e: Exception) {
                Log.e("Error", "Ошибка при получении данных: ${e.message}")
            }
        }
    }
    private fun disableUI() {
        go_to_logistic.isEnabled = false
        what_object.isEnabled = false
        when_go.isEnabled = false
        gruzchik.isEnabled = false
        pogruzchik.isEnabled = false
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