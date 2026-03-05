package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.Dialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
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
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
//import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.example.semimanufactures.DatabaseManager.getOperationsForPrp
import com.example.semimanufactures.DatabaseManager.getWarehouseNameById
import com.google.gson.Gson
import com.squareup.picasso.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

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
    private lateinit var go_to_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var go_to_send_notification: ImageView
    private lateinit var go_to_logistic: ImageView
    private lateinit var data_user_info: ImageView
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
    private lateinit var checkbox_loading_unloading: ImageView
    private lateinit var layout_loading_unloading: LinearLayout
    private var isLoadingUnloadingChecked: Boolean = false
    private var skladName: String? = null
    private var skladShelf: String? = null
    private var skladUnit: String? = null
    @SuppressLint("MissingInflatedId")
    private var operations: List<OperationWithDemand> = emptyList()
    private var lastFetchTime = 0L
    private val fetchDelay = 300L
    private var typePrP: String = "prp"
    private lateinit var main_layout: ConstraintLayout
    private val REQUEST_CAMERA_PERMISSION = 1001
    private val REQUEST_NFC_PERMISSION = 1002
    private lateinit var supporterManager: SupporterManager
    private var pm84ScannerManager: PM84ScannerManager? = null
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
    private lateinit var button_show_operations_dialog: ImageButton
    private lateinit var layout_spinner: LinearLayout
    //
    private val rolesList: MutableList<String> = mutableListOf()


    ///
    private var warehouseCheckJob: Job? = null
    ////

    private var testNameSkldOtkuda: String? = null
    @SuppressLint("MissingInflatedId", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.O)
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
        setContentView(R.layout.activity_creating_ligistic)
        scan_button = findViewById(R.id.scan_button)
        progressBar = findViewById(R.id.progressBar)
        prp_for_creating = findViewById(R.id.prp_for_creating)
        operation_for_creating = findViewById(R.id.operation_for_creating)
        create_new_logistic_button = findViewById(R.id.create_new_logistic_button)
        spinner_operations = findViewById(R.id.spinner_operations)
        scaning_sklad = findViewById(R.id.scaning_sklad)
        checkbox_loading_unloading = findViewById(R.id.checkbox_loading_unloading)
        layout_loading_unloading = findViewById(R.id.layout_loading_unloading)
        button_show_operations_dialog = findViewById(R.id.button_show_operations_dialog)
        button_show_operations_dialog.isEnabled = false
        layout_spinner = findViewById(R.id.layout_spinner)
        initView()
        registerScannerReceiver()
        if (currentRolesString?.isNotEmpty() == true) {
            rolesList.addAll(currentRolesString!!.split(",").map { it.trim() })
        }
        Log.d(tag, "User id: $currentUserId, Username: $currentUsername, Role: $currentRoleCheck, MDM Code: $currentMdmCode")
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            val intent = Intent(this@CreatingLogisticActivity, LogisticActivity::class.java)
            startActivity(intent)
        }
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_send_notification.setOnClickListener {
            val intent = Intent(this@CreatingLogisticActivity, NotificationActivity::class.java)
            startActivity(intent)
        }
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_issue.setOnClickListener {
            val intent = Intent(this@CreatingLogisticActivity, FeaturesOfTheFunctionalityActivity::class.java)
            startActivity(intent)
        }
        data_user_info = findViewById(R.id.data_user_info)
        data_user_info.setOnClickListener {
            val intent = Intent(this@CreatingLogisticActivity, SettingsActivity::class.java)
            startActivity(intent)
        }
        go_to_add = findViewById(R.id.go_to_add)
        go_to_add.setOnClickListener {
            val intent = Intent(this@CreatingLogisticActivity, AddActivity::class.java)
            startActivity(intent)
        }
        prp_for_creating.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val prpValue = s.toString()
                // Как только длина ровно 8 символов (== ввели 8-й) или больше 8,
                // сразу запускаем fetchOperations
                if (prpValue.length >= 8) {
                    fetchOperations(prpValue)
                } else {
                    // Если длина < 8 — сбрасываем спиннер, скрываем прогресс
                    spinner_operations.adapter = null
                    spinner_operations.isEnabled = false
                    progressBar.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        spinner_operations.isClickable = false
        spinner_operations.isFocusable = false
        spinner_operations.isEnabled = false/////////////////////////


        button_show_operations_dialog.setOnClickListener {
            showOperationsFullScreenDialog()
        }
        
        // Обработчик чекпоинта Погрузка/Разгрузка
        layout_loading_unloading.setOnClickListener {
            isLoadingUnloadingChecked = !isLoadingUnloadingChecked
            if (isLoadingUnloadingChecked) {
                checkbox_loading_unloading.setImageResource(R.drawable.remember_me_svg)
                scaning_sklad.visibility = View.GONE
            } else {
                checkbox_loading_unloading.setImageResource(R.drawable.no_remember_me_svg)
                scaning_sklad.visibility = View.VISIBLE
            }
        }
        
        checkbox_loading_unloading.setOnClickListener {
            layout_loading_unloading.performClick()
        }
        
        // внутри CreatingLogisticActivity

        create_new_logistic_button.setOnClickListener {
            if (currentUsername == "T.Test") {
                Toast.makeText(
                    this,
                    "У вас недостаточно прав для совершения данной операции",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            val prpValue = prp_for_creating.text.toString()
            val scannedValue = scaning_sklad.text.toString()
            Log.d(tag, "Button clicked. PRP value retrieved: $prpValue")

            // Проверка на чекпоинт Погрузка/Разгрузка
            if (!isLoadingUnloadingChecked && scannedValue.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, введите или отсканируйте ID склада", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            if (prpValue.isEmpty()) {
                Toast.makeText(this, "Пожалуйста, введите ПрП", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }

            CoroutineScope(Dispatchers.Main).launch {
                // Проверяем склад только если чекпоинт не выбран
                if (!isLoadingUnloadingChecked) {
                    if (!checkSkladExists(this@CreatingLogisticActivity, scannedValue)) {
                        Toast.makeText(this@CreatingLogisticActivity, "Склад с таким ID не найден", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        return@launch
                    }
                    // подгружаем задачи и операции
                    fetchSkladData(scannedValue)
                }

                try {
                    Log.d(tag, "Fetching operations for PRP: $prpValue")
                    val operations: List<OperationWithDemand> =
                        withContext(Dispatchers.IO) { getOperationsForPrp(this@CreatingLogisticActivity, prpValue) }

                    if (operations.isEmpty()) {
                        Log.d(tag, "No operations found for PRP: $prpValue")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CreatingLogisticActivity, "Операций для данной ПрП не найдено", Toast.LENGTH_SHORT).show()
                            progressBar.visibility = View.GONE
                        }
                        return@launch
                    }

                    val pos = spinner_operations.selectedItemPosition
                    if (pos < 0 || pos !in operations.indices) {
                        Log.e(tag, "Selected position is out of bounds: $pos")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@CreatingLogisticActivity, "Пожалуйста, выберите операцию", Toast.LENGTH_SHORT).show()
                            progressBar.visibility = View.GONE
                        }
                        return@launch
                    }

                    val op = operations[pos]
                    Log.d(tag, "Selected operation: ${op.operation2} on ${op.demand}")

                    val logistics: List<*> = withContext(Dispatchers.IO) {
                        DatabaseManager.getDeliveryLogisticsByDemand(op.demand, op.operation2, typePrP, this@CreatingLogisticActivity)
                    }
                    if (logistics.isNotEmpty()) {
                        val id = extractIdFromLog(logistics.toString())
                        if (id != null) {
                            startActivity(
                                Intent(this@CreatingLogisticActivity, DetailLogisticsActivity::class.java)
                                    .putExtra("logistics_id", id.toString())
                            )
                        }
                        progressBar.visibility = View.GONE
                        return@launch
                    }

                    Log.d("Тут", "$testNameSkldOtkuda")

                    val urls = listOf(
                        "https://api.gkmmz.ru/api/create_logistic_prp",
                        "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/create_logistic_prp"
                    )

                    val client = (application as App).okHttpClient.newBuilder()
                        .connectTimeout(30, TimeUnit.SECONDS)
                        .readTimeout(60, TimeUnit.SECONDS)
                        .writeTimeout(60, TimeUnit.SECONDS)
                        .build()

                    var created = false
                    var lastError: String? = null

                    withContext(Dispatchers.IO) {
                        for (url in urls) {
                            val bodyBuilder = MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("created_by", currentMdmCode ?: "")
                                .addFormDataPart("type", "prp")
                                .addFormDataPart("planned_date", "1")
                                .addFormDataPart("box_id", op.operation2)
                            
                            // Если чекпоинт выбран, передаем "Погрузка/Разгрузка"
                            if (isLoadingUnloadingChecked) {
                                bodyBuilder.addFormDataPart("send_from", "Погрузка/Разгрузка")
                                bodyBuilder.addFormDataPart("send_from_title", "Погрузка/Разгрузка")
                            } else {
                                bodyBuilder.addFormDataPart("send_from", scannedValue)
                                bodyBuilder.addFormDataPart("send_from_title", testNameSkldOtkuda ?: "")
                            }
                            
                            bodyBuilder.addFormDataPart("created_by_name", currentFio ?: "")
                                .addFormDataPart("mdm_code", currentMdmCode ?: "")
                                .addFormDataPart("version_name", version_name)

                            if (op.needProsk) {
                                bodyBuilder.addFormDataPart("send_to", "812")
                                bodyBuilder.addFormDataPart("send_to_title", "ПРОСК полуфабрикатов")
                            }

                            val req = Request.Builder()
                                .url(url)
                                .post(bodyBuilder.build())
                                .addHeader("X-Auth-Token", authToken)
                                .addHeader("X-Apig-AppCode", authTokenAPI)
                                .build()

                            val resp = try { client.newCall(req).execute() } catch (e: Exception) {
                                Log.e(tag, "create_logistic_prp call failed for $url", e)
                                lastError = e.message
                                null
                            } ?: continue

                            try {
                                if (resp.code == 429) {
                                    Log.w(tag, "429 on $url, trying fallback…")
                                    continue
                                }
                                if (resp.isSuccessful) {
                                    val body = resp.body?.string().orEmpty()
                                    withContext(Dispatchers.Main) {
                                        progressBar.visibility = View.GONE
                                        
                                        // Определяем send_from и send_from_title для передачи
                                        val sendFrom = if (isLoadingUnloadingChecked) "Погрузка/Разгрузка" else scannedValue
                                        val sendFromTitle = if (isLoadingUnloadingChecked) "Погрузка/Разгрузка" else (testNameSkldOtkuda ?: "")
                                        
                                        startActivity(
                                            Intent(this@CreatingLogisticActivity, NewLogisticActivity::class.java).apply {
                                                putExtra("responseBody", body)
                                                putExtra("selectedOperation", op.operation)
                                                putExtra("selectedDemand", op.demand)
                                                putExtra("selectedUchastok", op.uchastok)
                                                putExtra("selectedNext_podrazd_mdm_code", op.next_podrazd_mdm_code)
                                                putExtra("selectedZahodNomer", op.zahodNomer)
                                                putExtra("skladName", skladName)
                                                putExtra("skladShelf", skladShelf)
                                                putExtra("skladUnit", skladUnit)
                                                putExtra("type", typePrP)
                                                putExtra("scannedValue", scannedValue)
                                                putExtra("send_from", sendFrom)
                                                putExtra("send_from_title", sendFromTitle)
                                            }
                                        )
                                    }
                                    created = true
                                    break
                                } else {
                                    lastError = "HTTP ${resp.code} - ${resp.message}"
                                }
                            } finally {
                                resp.close()
                            }
                        }
                    }

                    if (!created) {
                        withContext(Dispatchers.Main) {
                            progressBar.visibility = View.GONE
                            val errorMessage = if (lastError != null) {
                                "Не удалось создать заявку: $lastError"
                            } else {
                                "Не удалось создать заявку. Пожалуйста, попробуйте еще раз."
                            }
                            Toast.makeText(
                                this@CreatingLogisticActivity,
                                errorMessage,
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error creating logistics: ${e.message}", e)
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this@CreatingLogisticActivity,
                        "Произошла ошибка: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }


        scaning_sklad.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                warehouseCheckJob?.cancel()

                val enteredText = s.toString().trim()
                create_new_logistic_button.isEnabled = false

                if (enteredText.isEmpty() || enteredText.length <= 2) {
                    progressBar.visibility = View.GONE
                    create_new_logistic_button.isEnabled = false
                    return
                }

                warehouseCheckJob = CoroutineScope(Dispatchers.Main).launch {
                    delay(500)

                    progressBar.visibility = View.VISIBLE
                    try {
                        val warehouseStatus = withContext(Dispatchers.IO) {
                            getWarehouseNameById(this@CreatingLogisticActivity, enteredText)
                        }

                        if (warehouseStatus == null) {
                            Toast.makeText(
                                this@CreatingLogisticActivity,
                                "Склад с таким ID не найден",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (!warehouseStatus.second) {
                            Toast.makeText(
                                this@CreatingLogisticActivity,
                                "Склад с ID $enteredText не активен",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            create_new_logistic_button.isEnabled = true
                            val warehouseName = warehouseStatus.first
                            testNameSkldOtkuda = warehouseName
                            Toast.makeText(this@CreatingLogisticActivity, "Склад: $warehouseName", Toast.LENGTH_SHORT).show()
                            prp_for_creating.requestFocus()
                            prp_for_creating.setSelection(prp_for_creating.text.length)
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Ошибка при проверке склада", e)
                        Toast.makeText(this@CreatingLogisticActivity, "Ошибка при проверке склада", Toast.LENGTH_SHORT).show()
                    } finally {
                        progressBar.visibility = View.GONE
                    }
                }
            }
        })

        setupNfcAdapter()
        main_layout = findViewById(R.id.main_layout)

        if (currentDeviceInfo == "PM84") {
            pm84ScannerManager = PM84ScannerManager.getInstance(applicationContext)
            pm84ScannerManager?.registerScannerReceiver()
            pm84ScannerManager?.setOnScanResultListener(object : PM84ScannerManager.OnScanResultListener {
                override fun onScanResultReceived(result: String) {
                    handleScanResult(result.trim())
                }
            })
            scan_button.setOnClickListener {
                if (checkCameraPermission()) {
                    pm84ScannerManager?.startScanning(prp_for_creating)
                }
            }
        } else if (currentDeviceInfo == "L2H-N") {
            supporterManager = SupporterManager(this, object : SupporterManager.IScanListener {
                override fun onScannerResultChange(result: String?) {
                    handleScanResult(result?.trim())
                }

                override fun onScannerServiceConnected() {
                    Log.d(tag, "Scanner service connected")
                }

                override fun onScannerServiceDisconnected() {
                    Log.d(tag, "Scanner service disconnected")
                }

                override fun onScannerInitFail() {
                    Log.e(tag, "Scanner initialization failed")
                }
            })
            scan_button.setOnClickListener {
                supporterManager?.singleScan(true)
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
    private fun handleScanResult(result: String?) {
        result?.let {
            prp_for_creating.setText(it)
            fetchOperations(it)
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
        if (checkNfcPermission()) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
            Toast.makeText(this, "Приблизьте тег NFC", Toast.LENGTH_LONG).show()
        } else {
            requestNfcPermission()
        }
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
        if (checkNfcPermission()) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        } else {
            requestNfcPermission()
        }
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

    private fun initView() {
        fetchOperations(prp_for_creating.text.toString())
        scan_button.setOnClickListener {
            if (checkCameraPermission()) {
                callScanner()
            }
        }
    }
    private fun checkCameraPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
            false
        }
    }
    private fun registerScannerReceiver() {
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_RECEIVE_DATA)
            addAction(ACTION_RECEIVE_DATABYTES)
            addAction(ACTION_RECEIVE_DATALENGTH)
            addAction(ACTION_RECEIVE_DATATYPE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Для Android 14 и выше
            registerReceiver(mScanReceiver, intentFilter, RECEIVER_EXPORTED)
        } else {
            // Для версий ниже Android 14
            registerReceiver(mScanReceiver, intentFilter)
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        Log.v(tag, "onDestroy()")
        if (currentDeviceInfo == "EA630") {
            closeScanService()
            unregisterReceiver(mScanReceiver)
        } else if (currentDeviceInfo == "PM84") {
            pm84ScannerManager?.unregisterScannerReceiver()
        } else if (currentDeviceInfo == "L2H-N") {
            supporterManager?.recycle()
        }
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
        if (!prpValue.isNullOrEmpty() && prpValue.length > 7) {
            Log.d(tag, "Fetching operations for PRP: $prpValue")
            progressBar.visibility = View.VISIBLE
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val operationsFromApi = getOperationsForPrp(this@CreatingLogisticActivity, prpValue) ?: emptyList()
                    if (operationsFromApi.isEmpty()) {
                        Log.d(tag, "No operations found for the given PRP.")
                        Toast.makeText(
                            this@CreatingLogisticActivity,
                            "Операций для данной ПрП не найдено",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Log.d(tag, "Fetched operations: $operationsFromApi")
                        updateOperationSpinner(operationsFromApi)
                    }
                } catch (e: Exception) {
                    Log.e(tag, "Error fetching operations: ${e.message}", e)
                    Toast.makeText(
                        this@CreatingLogisticActivity,
                        "Не удалось загрузить операции: ${e.localizedMessage}",
                        Toast.LENGTH_LONG
                    ).show()
                }
                finally {
                    // Скрываем прогресс-бар в любом случае
                    progressBar.visibility = View.GONE
                }
            }
        } else {
            Log.d(tag, "PRP value is empty, null or too short, skipping fetch.")
            spinner_operations.adapter = null
        }
    }

    private fun updateOperationSpinner(newOperations: List<OperationWithDemand>) {
        operations = newOperations

        fun cleanDest(s: String?): String =
            s?.trim()?.takeUnless { it.isEmpty() || it.equals("null", true) } ?: ""

        val items = operations.mapIndexed { i, op ->
            val nextOpName = operations.getOrNull(i + 1)?.operation ?: "конечную"
            val rawDest = if (op.needProsk) "ПРОСК Полуфабрикатов" else op.nextUchastok
            val dest = cleanDest(rawDest)

            val mainText = op.operation
            val subText = if (dest.isNotEmpty()) {
                "На $nextOpName в $dest"
            } else {
                ""
            }
            mainText to subText
        }

        if (items.isEmpty()) {
            spinner_operations.adapter = null
            return
        }

        val adapter = object : ArrayAdapter<Pair<String, String>>(
            this,
            R.layout.spinner_inv_item,
            items
        ) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.spinner_inv_item, parent, false)
                bindRow(view, getItem(position)!!)
                return view
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(R.layout.spinner_inv_item, parent, false)
                bindRow(view, getItem(position)!!)
                return view
            }

            private fun bindRow(row: View, item: Pair<String, String>) {
                val (mainText, subText) = item
                val tvMain = row.findViewById<TextView>(R.id.tv_main)
                val tvSub = row.findViewById<TextView>(R.id.tv_sub)
                tvMain.text = mainText
                if (subText.isEmpty()) {
                    tvSub.visibility = View.GONE
                } else {
                    tvSub.visibility = View.VISIBLE
                    tvSub.text = subText
                }
            }
        }

        spinner_operations.adapter = adapter
        button_show_operations_dialog.isEnabled = operations.isNotEmpty()
    }


    private fun showOperationsFullScreenDialog() {
        if (operations.isEmpty()) return

        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_operations_fullscreen)

        val recyclerView = dialog.findViewById<RecyclerView>(R.id.recycler_operations)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = OperationAdapter(operations) { position ->
            val selectedOp = operations[position]
            operation_for_creating.setText(selectedOp.operation)
            spinner_operations.setSelection(position)
            dialog.dismiss()

            // Показываем предупреждение, если статус не 68 и не 69
            if (selectedOp.status != "68" && selectedOp.status != "69") {
                showStatusWarningDialog()
            }
        }
        recyclerView.adapter = adapter

        val closeButton = dialog.findViewById<Button>(R.id.btn_close_dialog)
        closeButton.setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialog.show()
    }

    // тот же экран
    private fun fetchSkladData(scannedValue: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val urls = listOf(
                "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_all_skladi",
                "https://api.gkmmz.ru/api/get_all_skladi"
            )

            try {
                val client = (application as App).okHttpClient.newBuilder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build()

                val result: String? = withContext(Dispatchers.IO) {
                    var text: String? = null
                    for (url in urls) {
                        val req = Request.Builder()
                            .url(url)
                            .addHeader("X-Auth-Token", authToken)
                            .addHeader("X-Apig-AppCode", authTokenAPI)
                            .build()

                        val resp = try { client.newCall(req).execute() } catch (e: Exception) {
                            Log.e("FetchSkladData", "call failed for $url", e)
                            null
                        } ?: continue

                        try {
                            if (resp.code == 429) {
                                Log.w("FetchSkladData", "429 on $url, trying fallback…")
                                continue
                            }
                            if (resp.isSuccessful) {
                                text = resp.body?.string()
                                break
                            } else {
                                Log.e("FetchSkladData", "HTTP ${resp.code} - ${resp.message} on $url")
                            }
                        } finally {
                            resp.close()
                        }
                    }
                    text
                }

                if (result != null) {
                    parseSkladData(result, scannedValue)
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
    private fun checkNfcPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.NFC) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            false
        }
    }

    private fun requestNfcPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.NFC)) {
            // Объясните, зачем нужно разрешение (опционально)
            Toast.makeText(this, "Для работы с NFC необходимо разрешение", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.NFC), REQUEST_NFC_PERMISSION)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_NFC_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Разрешение было дано
                    Log.d(tag, "NFC permission granted")
                    // Повторно инициализируйте NFC, чтобы изменения вступили в силу
                    setupNfcAdapter()
                } else {
                    // Разрешение было отклонено
                    Log.d(tag, "NFC permission denied")
                    Toast.makeText(this, "Разрешение на использование NFC отклонено", Toast.LENGTH_SHORT).show()
                }
                return
            }
            REQUEST_CAMERA_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Разрешение на камеру предоставлено
                    pm84ScannerManager?.startScanning(prp_for_creating)
                } else {
                    Toast.makeText(this, "Разрешение на использование камеры отклонено", Toast.LENGTH_SHORT).show()
                }
                return
            }
        }
    }
}