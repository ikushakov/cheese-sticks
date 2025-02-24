package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.example.semimanufactures.DatabaseManager.getOperationsForPrp
import com.example.semimanufactures.DatabaseManager.getWarehouseNameById
import com.squareup.picasso.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit

class CreatingLogisticActivity : ComponentActivity() {
    private var tag: String = CreatingLogisticActivity::class.java.simpleName
    private val SCANNER_INIT = "unitech.scanservice.init"
    private val SCAN2KEY_SETTING = "unitech.scanservice.scan2key_setting"
    private val START_SCANSERVICE = "unitech.scanservice.start"
    private val CLOSE_SCANSERVICE = "unitech.scanservice.close"
    private val SOFTWARE_SCANKEY = "unitech.scanservice.software_scankey"
    private val ACTION_RECEIVE_DATA = "unitech.scanservice.data"
    private val ACTION_RECEIVE_DATABYTES = "unitech.scanservice.databyte"
    private val ACTION_RECEIVE_DATALENGTH = "unitech.scanservice.datalength"
    private val ACTION_RECEIVE_DATATYPE = "unitech.scanservice.datatype"
//    private lateinit var go_to_authorization: ImageView
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private var deviceInfo: String = ""
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var data_user_info: ImageView
    private var userId: Int = 0
    private var username: String = ""
    private var roleCheck: String = ""
    private var mdmCode: String = ""
    private var fio: String = ""
    private lateinit var progressBar: ProgressBar
    private lateinit var prp_for_creating: EditText
    private lateinit var operation_for_creating: EditText
    private lateinit var create_new_logistic_button: Button
    private lateinit var scan_button: ImageButton
    private lateinit var spinner_operations: Spinner

    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>

    private lateinit var scaning_sklad: EditText
    private val apiUrl = "http://192.168.200.250/api/get_all_skladi"
    private var skladName: String? = null
    private var skladShelf: String? = null
    private var skladUnit: String? = null
    @SuppressLint("MissingInflatedId")
    private lateinit var operations: List<OperationWithDemand>
    private var lastFetchTime = 0L
    private val fetchDelay = 300L
    private var typePrP: String = "prp"
    private lateinit var main_layout: ConstraintLayout
    private val rolesList: MutableList<String> = mutableListOf()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_creating_ligistic)
        scan_button = findViewById(R.id.scan_button)
        progressBar = findViewById(R.id.progressBar)
        prp_for_creating = findViewById(R.id.prp_for_creating)
        operation_for_creating = findViewById(R.id.operation_for_creating)
        create_new_logistic_button = findViewById(R.id.create_new_logistic_button)
        spinner_operations = findViewById(R.id.spinner_operations)
        scaning_sklad = findViewById(R.id.scaning_sklad)
        initView()
        registerScannerReceiver()
        val intent = intent
        username = intent.getStringExtra("username") ?: ""
        val password = intent.getStringExtra("password") ?: ""
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
        Log.d("CreatingLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            Log.d("CreatingLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@CreatingLogisticActivity, LogisticActivity::class.java).apply {
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
            Log.d("CreatingLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@CreatingLogisticActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
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
            val intent = Intent(this@CreatingLogisticActivity, SettingsActivity::class.java).apply {
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
            Log.d("CreatingLogisticActivity", "User id: ${userId}, Username: $username, Role: $roleCheck, mdmCode: ${mdmCode}, fio: ${fio}")
            val intent = Intent(this@CreatingLogisticActivity, AddActivity::class.java).apply {
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
        prp_for_creating.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val prpValue = s.toString()
                if (prpValue.isNotEmpty()) {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastFetchTime > fetchDelay) {
                        lastFetchTime = currentTime
                        Log.d(tag, "PRP value changed: $prpValue")
                        fetchOperations(prpValue)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        setUpSpinner()
        create_new_logistic_button.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            val prpValue = prp_for_creating.text.toString()
            val scannedValue = scaning_sklad.text.toString()
            Log.d(tag, "Button clicked. PRP value retrieved: $prpValue")

            if (scannedValue.isEmpty()) {
                Toast.makeText(this@CreatingLogisticActivity, "Пожалуйста, введите или отсканируйте ID склада", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (prpValue.isNotEmpty()) {
                Log.d(tag, "PRP value is not empty. Proceeding to fetch operations.")
                CoroutineScope(Dispatchers.Main).launch {
                    if (checkSkladExists(this@CreatingLogisticActivity, scannedValue)) {
                        Log.d(tag, "Button clicked. PRP value retrieved: $prpValue")
                        if (prpValue.isNotEmpty()) {
                            Log.d(tag, "PRP value is not empty. Proceeding to fetch operations.")
                            fetchSkladData(scannedValue)
                            try {
                                Log.d(tag, "Fetching operations for PRP: $prpValue")
                                val operations: List<OperationWithDemand> =
                                    withContext(Dispatchers.IO) { getOperationsForPrp(this@CreatingLogisticActivity, prpValue) }
                                Log.d("Операции для спинера: ", "$operations")

                                if (operations.isNotEmpty()) {
                                    Log.d(tag, "Operations found for PRP: $prpValue. Number of operations: ${operations.size}")
                                    val selectedPosition = spinner_operations.selectedItemPosition
                                    if (selectedPosition >= 0 && selectedPosition < operations.size) {
                                        val selectedOperation = operations[selectedPosition]
                                        val selectedDemand = selectedOperation.demand
                                        val selectedUchastok = selectedOperation.uchastok
                                        val selectedOperation2 = selectedOperation.operation2
                                        val selectedPodrazd_mdm_code = selectedOperation.podrazd_mdm_code
                                        val selectedNext_podrazd_mdm_code = selectedOperation.next_podrazd_mdm_code
                                        val selectedZahodNomer = selectedOperation.zahodNomer
                                        val selectedStatus = selectedOperation.status

                                        Log.d(tag, "Необходимая операция: ${selectedOperation.operation}")
                                        Log.d(tag, "Название ПрП: $selectedDemand")
                                        Log.d(tag, "Участок, на котором находится: $selectedUchastok")
                                        Log.d(tag, "mdmcode подразделения: $selectedPodrazd_mdm_code")
                                        Log.d(tag, "Operation - много циферек: $selectedOperation2")
                                        Log.d(tag, "mdmcode следующего подразделения: $selectedNext_podrazd_mdm_code")
                                        Log.d(tag, "Номер захода: $selectedZahodNomer")
                                        Log.d(tag, "Fetching delivery logistics for demand: $selectedDemand and Operation: $selectedOperation2")
                                        Log.d(tag, "Статус для проверки, будет ли выполнена операция при создании заявки и ее дальнейшем завершении: $selectedStatus")

                                        DatabaseManager.connect2(this@CreatingLogisticActivity)
                                        if (!DatabaseManager.isConnected2()) {
                                            Log.e(tag, "Database connection is not initialized")
                                            progressBar.visibility = View.GONE
                                            return@launch
                                        }

                                        val logistics: List<*> =
                                            withContext(Dispatchers.IO) {
                                                DatabaseManager.getDeliveryLogisticsByDemand(
                                                    selectedOperation.demand,
                                                    selectedOperation.operation2,
                                                    typePrP,
                                                    this@CreatingLogisticActivity
                                                )
                                            }

                                        if (logistics.isNotEmpty()) {
                                            Log.d(tag, "Delivery logistics found: $logistics")
                                            val id = extractIdFromLog(logistics.toString())
                                            id?.let {
                                                Log.d(tag, "Extracted ID: $it")
                                                val context = this@CreatingLogisticActivity
                                                Log.d("CreatingLogisticActivity", "User id: ${userId} и mdmCode: ${mdmCode}, deviceInfo: ${deviceInfo}, fio: ${fio}, Type: ${typePrP}")
                                                val intent = Intent(context, DetailLogisticsActivity::class.java)
                                                intent.putExtra("logistics_id", it.toString())
                                                intent.putExtra("mdmCode", mdmCode)
                                                intent.putExtra("userId", userId)
                                                intent.putExtra("username", username)
                                                intent.putExtra("roleCheck", roleCheck)
                                                intent.putExtra("fio", fio)
                                                intent.putExtra("deviceInfo", deviceInfo)
                                                intent.putExtra("type", typePrP)
                                                intent.putExtra("rolesString", rolesString)
                                                context.startActivity(intent)
                                                progressBar.visibility = View.GONE
                                            } ?: run {
                                                Log.d(tag, "No ID found in the logistics log.")
                                                progressBar.visibility = View.GONE
                                            }
                                        } else {
                                            Log.d(tag, "No delivery logistics found for demand: $selectedDemand and Operation: $selectedOperation2")
                                            Toast.makeText(this@CreatingLogisticActivity, "Не найдено активной заявки для данной ПрП", Toast.LENGTH_LONG).show()
                                            Log.d("Передача", "$skladName, $skladShelf, $skladUnit")
                                            val requestBody = MultipartBody.Builder()
                                                .setType(MultipartBody.FORM)
                                                .addFormDataPart("created_by", mdmCode)
                                                .addFormDataPart("type", "prp")
                                                .addFormDataPart("planned_date", "1")
                                                .addFormDataPart("box_id", selectedOperation2)
                                                .addFormDataPart("send_from", scannedValue)
                                                .build()

                                            Log.d(tag, "Отправляемый запрос с form-data.")
                                            withContext(Dispatchers.IO) {
                                                val client = OkHttpClient.Builder()
                                                    .connectTimeout(10, TimeUnit.SECONDS)
                                                    .readTimeout(10, TimeUnit.SECONDS)
                                                    .build()
                                                val request = Request.Builder()
                                                    .url("http://192.168.200.250/api/create_logistic/")
                                                    .post(requestBody)
                                                    .build()
                                                try {
                                                    Log.d(tag, "Запрос: ${request.method} ${request.url} \nТело запроса: ${requestBody.toString()}")
                                                    val response: Response = client.newCall(request).execute()
                                                    val responseBody = response.body?.string()
                                                    if (response.isSuccessful && responseBody != null) {
                                                        Log.d(tag, "Логистика успешно создана: $responseBody")
                                                        Log.d("CreatingLogisticActivity", "User id: ${userId} и mdmCode: ${mdmCode}, deviceInfo: ${deviceInfo}, fio: ${fio}, Type: ${typePrP}")
                                                        withContext(Dispatchers.Main) {
                                                            val intent2 = Intent(this@CreatingLogisticActivity, NewLogisticActivity::class.java).apply {
                                                                progressBar.visibility = View.GONE
                                                                putExtra("selectedOperation", selectedOperation.operation)
                                                                putExtra("selectedDemand", selectedDemand)
                                                                putExtra("selectedUchastok", selectedUchastok)
                                                                putExtra("selectedPodrazd_mdm_code", selectedPodrazd_mdm_code)
                                                                putExtra("selectedOperation2", selectedOperation2)
                                                                putExtra("mdmCode", mdmCode)
                                                                putExtra("userId", userId)
                                                                putExtra("username", username)
                                                                putExtra("roleCheck", roleCheck)
                                                                putExtra("fio", fio)
                                                                putExtra("scannedValue", scannedValue)
                                                                putExtra("selectedNext_podrazd_mdm_code", selectedNext_podrazd_mdm_code)
                                                                putExtra("selectedZahodNomer", selectedZahodNomer)
                                                                putExtra("deviceInfo", deviceInfo)
                                                                putExtra("skladName", skladName)
                                                                putExtra("skladShelf", skladShelf)
                                                                putExtra("skladUnit", skladUnit)
                                                                putExtra("responseBody", responseBody)
                                                                putExtra("type", typePrP)
                                                                putExtra("deviceInfo", deviceInfo)
                                                                putExtra("rolesString", rolesString)
                                                            }
                                                            startActivity(intent2)
                                                        }
                                                    } else {
                                                        Log.e(tag, "Ошибка при создании логистики: ${response.message}")
                                                        withContext(Dispatchers.Main) {
                                                            progressBar.visibility = View.GONE
                                                        }
                                                    }
                                                } catch (e: SocketTimeoutException) {
                                                    withContext(Dispatchers.Main) {
                                                        Toast.makeText(this@CreatingLogisticActivity, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                                                        progressBar.visibility = View.GONE
                                                    }
                                                } catch (e: Exception) {
                                                    Log.e(tag, "Ошибка при выполнении запроса: ${e.message}")
                                                    withContext(Dispatchers.Main) {
                                                        progressBar.visibility = View.GONE
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Log.e(tag, "Selected position is out of bounds.")
                                    }
                                } else {
                                    Log.d(tag, "No operations found for PRP: $prpValue")
                                }
                            } catch (e: Exception) {
                                Log.e(tag, "Error fetching delivery logistics: ${e.message}")
                            }
                        }
                    } else {
                        Log.d(tag, "PRP value is empty.")
                        create_new_logistic_button.isEnabled = false
                        Toast.makeText(this@CreatingLogisticActivity, "Склад с таким ID не найден", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        scaning_sklad.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                create_new_logistic_button.isEnabled = true
                val enteredText = s.toString().trim()
                if (!enteredText.isEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        if (!checkSkladExists(this@CreatingLogisticActivity, enteredText)) {
                            create_new_logistic_button.isEnabled = false
                            Toast.makeText(this@CreatingLogisticActivity,"Склад с таким ID не найден",Toast.LENGTH_SHORT).show()
                        } else {
                            create_new_logistic_button.isEnabled = true
                            getWarehouseNameById(this@CreatingLogisticActivity , enteredText)?.let{ name ->
                                Toast.makeText(this@CreatingLogisticActivity,"Вы на складе с наименованием: $name",Toast.LENGTH_SHORT).show()
                                runOnUiThread {
                                    prp_for_creating.requestFocus()
                                    val textLength = prp_for_creating.text.length
                                    prp_for_creating.setSelection(textLength)
                                }
                            }
                        }
                    }
                }
            }
        })
        setupNfcAdapter()
        main_layout = findViewById(R.id.main_layout)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("MainActivity", "Запуск получения версии...")
            val versionMobile = fetchMobileVersion(this@CreatingLogisticActivity)
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
                        Toast.makeText(this@CreatingLogisticActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
    private suspend fun checkSkladExists(context: Context, skladiDataId: String): Boolean {
        val warehouseName = getWarehouseNameById(context, skladiDataId)
        return warehouseName != null
    }
    private fun showStatusWarningDialog() {
        val popupView = layoutInflater.inflate(R.layout.status_warning_popup, null)
        val popupWindow = PopupWindow(popupView, 750, 700)
        popupView.findViewById<TextView>(R.id.warning_text).text = "После выполнения заявки операция изменит статус на \"Выполнена\""
        popupView.findViewById<ImageView>(R.id.warning_icon).setImageResource(R.drawable.warning)
        popupWindow.showAtLocation(window.decorView, Gravity.CENTER, 0, 0)
        popupView.findViewById<Button>(R.id.ok_button).setOnClickListener {
            popupWindow.dismiss()
        }
        popupWindow.isFocusable = true
    }
    private fun enableNfcReaderMode(isStartWork: Boolean) {
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        Toast.makeText(this, "Приблизьте тег NFC", Toast.LENGTH_LONG).show()
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (NfcAdapter.ACTION_NDEF_DISCOVERED == intent.action) {
            val tag: Tag? = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
            tag?.let { readFromTag(it) }
        }
    }
    private fun readFromTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        ndef?.connect()
        val ndefMessage = ndef?.ndefMessage
        val message = ndefMessage?.records?.joinToString("\n") { record ->
            String(record.payload).replace(Regex("[^0-9]"), "")
        } ?: "Данные не найдены"
        scaning_sklad.setText(message)
        Toast.makeText(this, "Вы на складе с id: $message", Toast.LENGTH_LONG).show()
        Log.d("NFC_TAG", "Успешно начали работу на складе с ID: $message")
        ndef.close()
    }
    private fun setupNfcAdapter() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this) ?: let {
            Toast.makeText(this, "NFC не поддерживается на этом устройстве", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_IMMUTABLE
        } else {
            0
        }
        val versionName = BuildConfig.VERSION_NAME
        val versionCode = BuildConfig.VERSION_CODE
        val sdkInt = Build.VERSION.SDK_INT
        Log.d("Версия android", "versionName - $versionName \n versionCode - $versionCode \n sdkInt - $sdkInt")
        if (sdkInt > 30) {
            val intent = Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_MUTABLE)
            } else {
                PendingIntent.getActivity(
                    this,
                    0,
                    intent,
                    PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        } else {
            pendingIntent = PendingIntent.getActivity(
                this, 0,
                Intent(this, javaClass).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), flags
            )
        }
        val ndef = IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED).apply {
            try {
                addDataType("*/*")
            } catch (e: IntentFilter.MalformedMimeTypeException) {
                throw RuntimeException("Не удалось добавить тип MIME", e)
            }
        }
        intentFilters = arrayOf(ndef)
    }

    override fun onResume() {
        super.onResume()
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
    }

    override fun onPause() {
        super.onPause()
        nfcAdapter.disableForegroundDispatch(this)
    }
    private fun extractIdFromLog(log: String): Int? {
        val regex = Regex("id=(\\d+)")
        val matchResult = regex.find(log)
        return matchResult?.groups?.get(1)?.value?.toInt()
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
    private fun initView() {
        fetchOperations(prp_for_creating.text.toString())
        scan_button.setOnClickListener {
            callScanner()
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
        Log.v(tag, "callScanner()")
        progressBar.visibility = View.GONE
        startScanService()
        setScan2Key()
        setInit()
        val bundle = Bundle()
        bundle.putBoolean("scan", true)
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
        val bundle1 = Bundle()
        bundle1.putBoolean("enable", true)
        val mIntent1 = Intent().setAction(SCANNER_INIT).putExtras(bundle1)
        sendBroadcast(mIntent1)
    }
    private fun startScanService() {
        val bundle = Bundle()
        bundle.putBoolean("close", true)
        val mIntent = Intent().setAction(START_SCANSERVICE).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private fun closeScanService() {
        val bundle = Bundle()
        bundle.putBoolean("close", true)
        val mIntent = Intent().setAction(CLOSE_SCANSERVICE).putExtras(bundle)
        sendBroadcast(mIntent)
    }
    private val mScanReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.v(tag, "onReceive()")
            val action = intent.action
            val bundle = intent.extras ?: return
            when (action) {
                ACTION_RECEIVE_DATA -> {
                    Log.v(tag, "ACTION_RECEIVE_DATA")
                    val barcodeStr = bundle.getString("text")?.trim()
                    Log.v(tag, "barcode data: $barcodeStr")
                    val cleanedBarcodeStr = barcodeStr?.replace("\\s+".toRegex(), "")
                    prp_for_creating.setText(cleanedBarcodeStr)
                    fetchOperations(cleanedBarcodeStr)
                    progressBar.visibility = View.GONE
                }
            }
        }
    }
    private fun fetchOperations(prpValue: String?) {
        if (!prpValue.isNullOrEmpty() && prpValue.length > 5) {
            Log.d(tag, "Fetching operations for PRP: $prpValue")
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val operationsFromApi = getOperationsForPrp(this@CreatingLogisticActivity, prpValue) ?: emptyList()
                    if (operationsFromApi.isEmpty()) {
                        Log.d(tag, "No operations found for the given PRP.")
                    } else {
                        Log.d(tag, "Fetched operations: $operationsFromApi")
                        updateOperationSpinner(operationsFromApi)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error fetching operations: ${e.message}", e)
                }
            }
        } else {
            Log.d(tag, "PRP value is empty, null or too short, skipping fetch.")
            spinner_operations.adapter = null
        }
    }
    private fun updateOperationSpinner(newOperations: List<OperationWithDemand>) {
        operations = newOperations
        val operationNames = operations.map { it.operation }
        if (operationNames.isNotEmpty()) {
            ArrayAdapter(this, R.layout.spinner_inv_item, operationNames).also { adapter ->
            adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
            spinner_operations.adapter = adapter
        }
        } else {
            Log.d(tag, "No operations found for the given PRP.")
            spinner_operations.adapter = null
        }
    }
    private fun setUpSpinner() {
        spinner_operations.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (operations.isNotEmpty()) {
                    val selectedOperation = operations[position]
                    operation_for_creating.setText(selectedOperation.operation)
                    val selectedStatus = selectedOperation.status
                    if (selectedStatus != "68" && selectedStatus != "69") {
                        showStatusWarningDialog()
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }
    private fun fetchSkladData(scannedValue: String) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url(apiUrl)
                        .build()
                    val response: Response = client.newCall(request).execute()
                    response.body?.string()
                }
                if (result != null) {
                    parseSkladData(result!!, scannedValue)
                } else {
                    Log.d(tag, "Данные не получены.")

                    Toast.makeText(this@CreatingLogisticActivity, "Не удалось получить данные", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e(tag, e.message.toString())

                Toast.makeText(this@CreatingLogisticActivity, "Ошибка при получении данных", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private suspend fun parseSkladData(jsonData: String, scannedValue: String) {
        withContext(Dispatchers.Main) {
            try {
                val jsonObject = JSONObject(jsonData)
                val skladObject = jsonObject.optJSONObject(scannedValue)
                if (skladObject != null) {
                    skladName = skladObject.getString("Наименование")
                    skladShelf = skladObject.getString("Стеллаж")
                    skladUnit = skladObject.getString("Полка")
                    Log.d(tag, "Склад найден: Наименование: $skladName / Стеллаж: $skladShelf / Полка: $skladUnit")
                } else {
                    Log.d(tag, "Склад с ID $scannedValue не найден.")
                }
            } catch (e: Exception) {
                Log.e(tag, "Error parsing JSON data: ${e.message}")
                Toast.makeText(this@CreatingLogisticActivity, "Ошибка при получении данных", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun disableUI() {
        go_to_logistic.isEnabled = false
        scaning_sklad.isEnabled = false
        prp_for_creating.isEnabled = false
        scan_button.isEnabled = false
        spinner_operations.isEnabled = false
        operation_for_creating.isEnabled = false
        go_to_issue.isEnabled = false
        create_new_logistic_button.isEnabled = false
        go_to_add.isEnabled = false
        data_user_info.isEnabled = false
        go_to_send_notification.isEnabled = false
        Toast.makeText(this, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
    }
}