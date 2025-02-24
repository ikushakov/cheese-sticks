package com.example.semimanufactures

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.semimanufactures.DatabaseManager.fetchDaysToAddMezhZavod
import com.example.semimanufactures.DatabaseManager.fetchDaysToAddOKR
import com.example.semimanufactures.DatabaseManager.fetchDaysToAddPosleProdazhnoeObsluzhivanie
import com.example.semimanufactures.DatabaseManager.fetchDaysToAddSeria
import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.example.semimanufactures.DatabaseManager.findDistributionDateByPrP
import com.example.semimanufactures.DatabaseManager.showToast
import java.time.LocalDate

class CheckingPrPSkladActivity : ComponentActivity() {
    private var tag: String = CheckingPrPSkladActivity::class.java.simpleName
    private val SCANNER_INIT = "unitech.scanservice.init"
    private val SCAN2KEY_SETTING = "unitech.scanservice.scan2key_setting"
    private val START_SCANSERVICE = "unitech.scanservice.start"
    private val CLOSE_SCANSERVICE = "unitech.scanservice.close"
    private val SOFTWARE_SCANKEY = "unitech.scanservice.software_scankey"
    private val ACTION_RECEIVE_DATA = "unitech.scanservice.data"
    private val ACTION_RECEIVE_DATABYTES = "unitech.scanservice.databyte"
    private val ACTION_RECEIVE_DATALENGTH = "unitech.scanservice.datalength"
    private val ACTION_RECEIVE_DATATYPE = "unitech.scanservice.datatype"
    private lateinit var button_for_add_barcode_scan: ImageButton
    private lateinit var button_for_add_qrcode_scan: ImageButton
    private lateinit var text_result_scan_sklad: TextView
    private lateinit var text_result_scan_prp: TextView
    private lateinit var button_for_inv_sklad: Button
    private var userId: Int = 0
    private var username: String = ""
    private var roleCheck: String = ""
    private var mdmCode: String = ""
    private var fio: String = ""
    private lateinit var progressBar: ProgressBar
//    private lateinit var go_to_authorization: ImageView
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var data_user_info: ImageView
    private var currentScanField: Int = 0
    private lateinit var go_to_send_notification: ImageView
    private lateinit var spinner_warehouse_names: Spinner
    private lateinit var text_info_sklad_scan: TextView
    private lateinit var go_to_logistic: ImageView
    private lateinit var text_result_add_barcode_scan_text: TextView
    private lateinit var text_result_add_qrcode_scan_text: TextView
    private var deviceInfo: String = ""
    private lateinit var main_layout: ConstraintLayout
    private val rolesList: MutableList<String> = mutableListOf()
    private val mScanReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            val bundle = intent.extras ?: return
            when (action) {
                ACTION_RECEIVE_DATA -> {
                    val barcodeStr = bundle.getString("text")?.replace("\\s".toRegex(), "")
                    Log.d("ScanReceiver", "Received barcode: $barcodeStr for field: $currentScanField")
                    progressBar.visibility = View.GONE
                    runOnUiThread {
                        when (currentScanField) {
                            1 -> {
                                text_result_scan_prp.setText(barcodeStr?.replace("\\s".toRegex(), ""))
                                Log.d("tag", "$text_result_scan_prp")
                                checkDistributionDate(barcodeStr?.trim())
                            }
                            2 -> {
                                text_result_scan_sklad.setText(barcodeStr?.replace("\\s".toRegex(), ""))
                                updateWarehouseInfo(barcodeStr)
                            }
                            3 -> {}
                        }
                    }
                }
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_checking_prp_sklad)
        initView()
        loadWarehouses()
        registerScannerReceiver()
        progressBar =  findViewById(R.id.progressBar)
        userId = intent.getIntExtra("userId", 0)
        username = intent.getStringExtra("username") ?: ""
        roleCheck = intent.getStringExtra("roleCheck") ?: ""
        mdmCode = intent.getStringExtra("mdmCode") ?: ""
        fio = intent.getStringExtra("fio") ?: ""
        deviceInfo = intent.getStringExtra("deviceInfo") ?: ""
        val rolesString = intent.getStringExtra("rolesString") ?: ""
        rolesList.addAll(rolesString.split(",").map { it.trim() })
        rolesList.forEach { role ->
            Log.d("Список ролей", "Роль: $role")
        }
        Log.d(tag, "Passing Username: $username, Role: ${roleCheck} User ID: ${userId}, fio: ${fio}, mdmCode: $mdmCode")
        button_for_inv_sklad.setOnClickListener {
            val barcodeValue = text_result_scan_prp.text.toString().replace("\\s".toRegex(), "")
            val qrcodeValue = text_result_scan_sklad.text.toString().replace("\\s".toRegex(), "")
            CoroutineScope(Dispatchers.Main).launch {
                DatabaseManager.addInventoryRecord(
                    this@CheckingPrPSkladActivity,
                    barcodeValue,
                    userId,
                    qrcodeValue,
                    fio,
                    text_result_scan_sklad
                )
            }
        }
        text_result_add_barcode_scan_text = findViewById(R.id.text_result_add_barcode_scan_text)
        text_result_add_qrcode_scan_text = findViewById(R.id.text_result_add_qrcode_scan_text)
        spinner_warehouse_names = findViewById(R.id.spinner_warehouse_names)
        text_info_sklad_scan = findViewById(R.id.text_info_sklad_scan)
//        go_to_authorization = findViewById(R.id.go_to_authorization)
//        go_to_authorization.setOnClickListener {
//            val intent = Intent(this@CheckingPrPSkladActivity, MainActivity::class.java)
//            startActivity(intent)
//        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            Log.d(tag, "Passing Username: $username, Role: ${roleCheck} User ID: ${userId}, fio: ${fio}, mdmCode: $mdmCode")
            val addIntent = Intent(this@CheckingPrPSkladActivity, AddActivity::class.java).apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            startActivity(addIntent)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            Log.d(tag, "Passing Username: $username, Role: ${roleCheck} User ID: ${userId}, fio: ${fio}, mdmCode: $mdmCode")
            val intent = Intent(this@CheckingPrPSkladActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
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
            val intent = Intent(this@CheckingPrPSkladActivity, SettingsActivity::class.java).apply {
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
        spinner_warehouse_names.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedWarehouse = parent.getItemAtPosition(position) as Warehouse
                text_result_scan_sklad.text = selectedWarehouse.id
                text_info_sklad_scan.text = selectedWarehouse.name
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        })
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            Log.d(tag, "Passing Username: $username, Role: ${roleCheck} User ID: ${userId}, fio: ${fio}, mdmCode: $mdmCode")
            val intent = Intent(this@CheckingPrPSkladActivity, LogisticActivity::class.java).apply {
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
        main_layout = findViewById(R.id.main_layout)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("CheckingPrPSkladSunmiActivity", "Запуск получения версии...")
            val versionMobile = fetchMobileVersion(this@CheckingPrPSkladActivity)
            Log.d("CheckingPrPSkladSunmiActivity", "Версия получена: $versionMobile")

            if (versionMobile == null) {
                Log.e("CheckingPrPSkladSunmiActivity", "Не удалось получить версию")
                //disableUI()
            } else {
                Log.d("CheckingPrPSkladSunmiActivity", "Версия приложения: $versionMobile")
                // Сравнение версий и отключение UI, если они не совпадают
                if (versionMobile.toInt() != myGlobalVariable) {
                    Log.e("CheckingPrPSkladSunmiActivity", "Версии не совпадают. Доступ к функционалу отключен.")
                    disableUI()
                    // Устанавливаем обработчик нажатия на Layout
                    main_layout.setOnClickListener {
                        // Код, который будет выполняться при нажатии на Layout
                        Toast.makeText(this@CheckingPrPSkladActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
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

    private fun loadWarehouses() {
        CoroutineScope(Dispatchers.Main).launch {
            val warehouses = DatabaseManager.getAllWarehouses(this@CheckingPrPSkladActivity)
            val adapter = ArrayAdapter(this@CheckingPrPSkladActivity, R.layout.new_simple_spinner_item, warehouses)
            adapter.setDropDownViewResource(R.layout.spinner_inv_item)
            spinner_warehouse_names.adapter = adapter
        }
    }
    private fun updateWarehouseInfo(barcodeValue: String?) {
        if (barcodeValue.isNullOrBlank()) return
        CoroutineScope(Dispatchers.Main).launch {
            val warehouseName = DatabaseManager.getWarehouseNameById(this@CheckingPrPSkladActivity, barcodeValue.trim())
            if (warehouseName != null) {
                text_info_sklad_scan.text = warehouseName
                showToast(this@CheckingPrPSkladActivity, "Склад найден: $warehouseName", 5000)
            } else {
                showToast(this@CheckingPrPSkladActivity, "Склад не найден по ID: $barcodeValue", 5000)
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkDistributionDate(barcodeValue: String?) {
        if (barcodeValue.isNullOrBlank() || barcodeValue.length < 4) return
        CoroutineScope(Dispatchers.Main).launch {
            val searchData = findDistributionDateByPrP(this@CheckingPrPSkladActivity, barcodeValue)
            if (searchData == null) {
                Log.e("DistributionCheck", "Нет данных для штрих-кода")
                return@launch
            }
            val distributionDate = searchData.dateDistribution
            val segment = searchData.segment
                if (distributionDate.isNullOrBlank()) {
                    Log.d("DistributionCheck", "Дата распределения не найдена")
                    text_result_scan_prp.setBackgroundResource(R.drawable.successful_prp_edit_text)
                    text_result_scan_prp.setTextColor(resources.getColor(R.color.black))
                } else {
                    try {
                        val currentDate = LocalDate.now()
                        val date = LocalDate.parse(distributionDate)

                        Log.d("Сегодняшняя дата: ", "$currentDate")
                        val daysPlusSeria = fetchDaysToAddSeria(this@CheckingPrPSkladActivity) ?: 3
                        val daysPlusMezhZavod = fetchDaysToAddMezhZavod(this@CheckingPrPSkladActivity) ?: 90
                        val daysPlusOKR = fetchDaysToAddOKR(this@CheckingPrPSkladActivity) ?: 45
                        val daysPlusPosleProd = fetchDaysToAddPosleProdazhnoeObsluzhivanie(this@CheckingPrPSkladActivity) ?: 45

                        if (segment == "Серия" && date.isAfter(currentDate.plusDays(daysPlusSeria))) {
                            text_result_scan_prp.setBackgroundResource(R.drawable.successful_prp_edit_text)
                            showToast(this@CheckingPrPSkladActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                            Log.d("DistributionCheck", "Зеленый")
                            text_result_scan_prp.setTextColor(resources.getColor(R.color.black))
                        }
                        else if (segment == "Межзавод" && date.isAfter(currentDate.plusDays(daysPlusMezhZavod))) {
                            text_result_scan_prp.setBackgroundResource(R.drawable.successful_prp_edit_text)
                            showToast(this@CheckingPrPSkladActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                            Log.d("DistributionCheck", "Зеленый")
                            text_result_scan_prp.setTextColor(resources.getColor(R.color.black))
                        }
                        else if (segment == "ОКР" && date.isAfter(currentDate.plusDays(daysPlusOKR))) {
                            text_result_scan_prp.setBackgroundResource(R.drawable.successful_prp_edit_text)
                            showToast(this@CheckingPrPSkladActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                            Log.d("DistributionCheck", "Зеленый")
                            text_result_scan_prp.setTextColor(resources.getColor(R.color.black))
                        }
                        else if (segment == "Послепродажное обслуживание" && date.isAfter(currentDate.plusDays(daysPlusPosleProd))) {
                            text_result_scan_prp.setBackgroundResource(R.drawable.successful_prp_edit_text)
                            showToast(this@CheckingPrPSkladActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                            Log.d("DistributionCheck", "Зеленый")
                            text_result_scan_prp.setTextColor(resources.getColor(R.color.black))
                        }
                        else {
                            text_result_scan_prp.setBackgroundResource(R.drawable.error_prp_edit_text)
                            showToast(this@CheckingPrPSkladActivity, "Дата распределения - $date и Сегмент - $segment", 10000)
                            Log.d("DistributionCheck", "Красный")
                            text_result_scan_prp.setTextColor(resources.getColor(R.color.black))
                        }
                    } catch (e: Exception) {
                        Log.e("DistributionCheck", "Ошибка при парсинге даты: ${e.message}")
                        text_result_scan_prp.setBackgroundResource(R.drawable.error_prp_edit_text)
                        Log.d("DistributionCheck", "Красный")
                        text_result_scan_prp.setTextColor(resources.getColor(R.color.black))
                    }
                }

        }
    }
    private fun initView() {
        button_for_add_barcode_scan = findViewById(R.id.button_for_add_barcode_scan)
        button_for_add_qrcode_scan = findViewById(R.id.button_for_add_qrcode_scan)
        text_result_scan_sklad = findViewById(R.id.text_result_scan_sklad)
        text_result_scan_prp = findViewById(R.id.text_result_scan_prp)
        button_for_inv_sklad = findViewById(R.id.button_for_inv_sklad)
        text_result_scan_prp.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            @RequiresApi(Build.VERSION_CODES.O)
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                checkDistributionDate(s.toString().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        button_for_add_barcode_scan.setOnClickListener {
            currentScanField = 2
            callScanner()
        }
        button_for_add_qrcode_scan.setOnClickListener {
            currentScanField = 1
            callScanner()
        }
        text_result_scan_sklad.setOnClickListener {
            currentScanField = 2
        }
        text_result_scan_prp.setOnClickListener {
            currentScanField = 1
        }
        text_result_scan_sklad.addTextChangedListener(textWatcher)
        text_result_scan_sklad.addTextChangedListener(textWatcher)
        spinner_warehouse_names = findViewById(R.id.spinner_warehouse_names)
    }
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            updateButtonStyle()
        }
        override fun afterTextChanged(s: Editable?) {}
    }
    private fun updateButtonStyle() {
        val barcodeValue = text_result_scan_sklad.text.toString().trim().replace("\\s".toRegex(), "")
        val qrCodeValue = text_result_scan_sklad.text.toString().trim().replace("\\s".toRegex(), "")
        if (barcodeValue.isNotEmpty() && qrCodeValue.isNotEmpty()) {
            button_for_inv_sklad.setBackgroundResource(R.drawable.button_background)
        } else {
            button_for_inv_sklad.setBackgroundResource(R.drawable.button_background_add)
        }
    }
    private fun registerScannerReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ACTION_RECEIVE_DATA)
        intentFilter.addAction(ACTION_RECEIVE_DATABYTES)
        intentFilter.addAction(ACTION_RECEIVE_DATALENGTH)
        intentFilter.addAction(ACTION_RECEIVE_DATATYPE)
        registerReceiver(mScanReceiver, intentFilter)
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.v(tag, "onDestroy()")
        closeScanService()
        unregisterReceiver(mScanReceiver)
    }
    private fun callScanner() {
        progressBar.visibility = View.VISIBLE
        startScanService()
        setScan2Key()
        setInit()
        val bundle = Bundle().apply {
            putBoolean("scan", true)
            putInt("field", currentScanField)
        }
        val mIntent = Intent().setAction(SOFTWARE_SCANKEY).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private fun setScan2Key() {
        val bundle = Bundle()
        bundle.putBoolean("scan2key", false)
        val mIntent = Intent().setAction(SCAN2KEY_SETTING).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private fun setInit() {
        val bundle = Bundle()
        bundle.putBoolean("enable", true)
        val mIntent1 = Intent().setAction(SCANNER_INIT).putExtras(bundle)
        sendBroadcast(mIntent1)
    }
    private fun startScanService() {
        val bundle = Bundle().apply {
            putBoolean("close", true)
        }
        val mIntent = Intent().setAction(START_SCANSERVICE).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private fun closeScanService() {
        val bundle = Bundle().apply {
            putBoolean("close", true)
        }
        val mIntent = Intent().setAction(CLOSE_SCANSERVICE).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private fun disableUI() {
        go_to_logistic.isEnabled = false
        text_result_scan_sklad.isEnabled = false
        button_for_add_barcode_scan.isEnabled = false
        spinner_warehouse_names.isEnabled = false
        text_info_sklad_scan.isEnabled = false
//        go_to_authorization.isEnabled = false
        text_result_scan_prp.isEnabled = false
        go_to_issue.isEnabled = false
        button_for_add_qrcode_scan.isEnabled = false
        button_for_inv_sklad.isEnabled = false
        go_to_add.isEnabled = false
        go_to_send_notification.isEnabled = false
        data_user_info.isEnabled = false
        Toast.makeText(this, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
    }
}
