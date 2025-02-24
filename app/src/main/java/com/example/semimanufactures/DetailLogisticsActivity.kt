package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.semimanufactures.DatabaseManager.client
import com.example.semimanufactures.DatabaseManager.fetchMobileVersion
import com.example.semimanufactures.DetailLogisticsAdapter.Companion.REQUEST_CAMERA
import com.example.semimanufactures.DetailLogisticsAdapter.Companion.REQUEST_GALLERY
import com.example.semimanufactures.DetailLogisticsAdapter.Companion.currentPhotoFile
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import com.squareup.picasso.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.SocketTimeoutException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class DetailLogisticsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DetailLogisticsAdapter
    private val logisticsItems = mutableListOf<LogisticsItem>()
    private val tag = "DetailLogisticsActivity"
    private var sendFromId: String? = null
    private var sendTo: String? = null
    private var currentLogisticsId: String? = null
    private var userId: Int = 0
    private var username: String = ""
    private var roleCheck: String = ""
    private var mdmCode: String = ""
    private var deviceInfo: String = ""
    private var fio: String = ""
    private var type: String = ""
    private lateinit var nfcAdapter: NfcAdapter
    private lateinit var pendingIntent: PendingIntent
    private lateinit var intentFilters: Array<IntentFilter>
    private var isStartWork: Boolean = false
    private var isNfcScanned = false
    private lateinit var main_layout: LinearLayout
    private lateinit var btnClose: ImageButton
    private lateinit var go_to_logistic: ImageView
    val rolesList: MutableList<String> = mutableListOf()
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_logistics)
        supportActionBar?.hide()
        recyclerView = findViewById(R.id.detail_recycler_view)
        adapter = DetailLogisticsAdapter(logisticsItems, deviceInfo, fio, username,
            userId.toString(), mdmCode, roleCheck) { item -> showStatusUpdateDialog(this, item) }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        val intent = intent
        username = intent.getStringExtra("username") ?: ""
        roleCheck = intent.getStringExtra("roleCheck") ?: ""
        userId = intent.getIntExtra("userId", 0)
        mdmCode = intent.getStringExtra("mdmCode") ?: ""
        deviceInfo = intent.getStringExtra("deviceInfo") ?: ""
        fio = intent.getStringExtra("fio") ?: ""
        type = intent.getStringExtra("type") ?: ""
        val rolesString = intent.getStringExtra("rolesString") ?: ""
        rolesList.addAll(rolesString.split(",").map { it.trim() })
        rolesList.forEach { role ->
            Log.d("Список ролей", "Роль: $role")
        }
        val logisticsId = intent.getStringExtra("logistics_id") ?: return
        Log.d(tag, "User id: $userId, Username: $username, Role: $roleCheck, mdmCode: $mdmCode, fio: $fio, Type: $type")
        fetchLogisticsDetail(logisticsId)
        go_to_logistic = findViewById(R.id.go_to_logistic)
        btnClose = findViewById(R.id.btnClose)
        btnClose.setOnClickListener {
            val resultIntent = Intent().apply {
                putExtra("userId", userId)
                putExtra("username", username)
                putExtra("roleCheck", roleCheck)
                putExtra("mdmCode", mdmCode)
                putExtra("fio", fio)
                putExtra("deviceInfo", deviceInfo)
                putExtra("rolesString", rolesString)
            }
            setResult(Activity.RESULT_OK, resultIntent)
            finish()
        }
        if (type == "prp" || type == "doc" || type == "other" || type == "stanok") {
            setupNfcAdapter()
        }
        main_layout = findViewById(R.id.main_layout)
        CoroutineScope(Dispatchers.Main).launch {
            Log.d("MainActivity", "Запуск получения версии...")
            val versionMobile = fetchMobileVersion(this@DetailLogisticsActivity)
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
                        Toast.makeText(this@DetailLogisticsActivity, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
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
    private fun showNfcDialog(message: String, isStart: Boolean) {
        isStartWork = isStart
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.nfc_new_dialog, null)
        val messageTextView: TextView = dialogView.findViewById(R.id.messageTextView)
        messageTextView.text = message
        dialogBuilder.setView(dialogView)
        val dialog = dialogBuilder.create()
        val buttonYes: Button = dialogView.findViewById(R.id.buttonYes)
        buttonYes.setOnClickListener {
            enableNfcReaderMode(isStart)
            dialog.dismiss()
        }
        val buttonNo: Button = dialogView.findViewById(R.id.buttonNo)
        buttonNo.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }
    private fun showResultDialog(resultMessage: String) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.nfc_new_result_dialog, null)
        val messageTextView: TextView = dialogView.findViewById(R.id.messageTextView)
        messageTextView.text = resultMessage
        dialogBuilder.setView(dialogView)
        val dialog = dialogBuilder.create()
        val buttonClose: Button = dialogView.findViewById(R.id.buttonClose)
        buttonClose.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
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
        val resultMessage: String
        val mdmCode = intent.getStringExtra("mdmCode") ?: ""
        val currentDateTime = getCurrentDateTime()
        if (isStartWork) {
            resultMessage = if (message == sendFromId) {
                Toast.makeText(this, "Можете приступать к работе", Toast.LENGTH_SHORT).show()
                Log.d("tag", "Успешно начали работу на складе с ID: $message")
                updateLogistics(currentLogisticsId, status = "1", fieldName = "is_accepted_by", newValue = mdmCode, isAcceptedAt = currentDateTime)
                "Успешно начали работу на складе с ID: $message"
            } else {
                Toast.makeText(this, "Неверный склад", Toast.LENGTH_SHORT).show()
                Log.e("tag", "Ошибка: Неверный склад для начала работы. Ожидался: $sendFromId, получено: $message")
                "Ошибка: Неверный склад для начала работы. Ожидался: $sendFromId, получено: $message"
            }
        } else {
            resultMessage = if (message == sendTo) {
                Toast.makeText(this, "Заявка на перемещение выполнена", Toast.LENGTH_SHORT).show()
                Log.d("tag", "Заявка на перемещение выполнена. Место хранения с ID: $message")
                updateLogistics(currentLogisticsId, status = "4", fieldName = "is_ready_by", newValue = mdmCode, isReadyAt = currentDateTime)
                "Заявка на перемещение выполнена на склад с ID: $message"
            } else {
                Toast.makeText(this, "Неверный склад", Toast.LENGTH_SHORT).show()
                Log.e("tag", "Ошибка: Неверный склад для завершения работы. Ожидался: $sendTo, получено: $message")
                "Ошибка: Неверный склад для завершения работы. Ожидался: $sendTo, получено: $message"
            }
        }
        ndef.close()
        showResultDialog(resultMessage)
        isNfcScanned = true
    }
    override fun onResume() {
        super.onResume()
        if (type == "prp") {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
        }
    }
    override fun onPause() {
        super.onPause()
        if (type == "prp") {
            nfcAdapter.disableForegroundDispatch(this)
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("sendFromId", sendFromId)
        outState.putString("sendTo", sendTo)
        outState.putString("currentLogisticsId", currentLogisticsId)
    }
    private fun fetchLogisticsDetail(id: String) {
        Log.d(tag, "Fetching details for logistics ID: $id")
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder()
            .url("http://192.168.200.250/api/get_logistics/$id")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(tag, "Error fetching details: ${e.message}")
                runOnUiThread {
                    if (e is SocketTimeoutException) {
                        Toast.makeText(this@DetailLogisticsActivity, "Попробуйте позже. Сервер не отвечает.", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@DetailLogisticsActivity, "Ошибка при получении данных: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e(tag, "Error response from server: ${response.code}")
                    runOnUiThread {
                        Toast.makeText(this@DetailLogisticsActivity, "Ошибка сервера: ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                    return
                }
                response.body?.string()?.let { jsonResponse ->
                    Log.d(tag, "Received response for details: $jsonResponse")
                    try {
                        val logisticsDetail = Gson().fromJson(jsonResponse, LogisticsItem::class.java)
                        runOnUiThread {
                            currentLogisticsId = logisticsDetail.id
                            sendFromId = logisticsDetail.send_from
                            sendTo = logisticsDetail.send_to
                            updateUI(logisticsDetail)
                            Log.d(tag, "Откуда: $sendFromId и Куда: $sendTo")
                            if (type == "doc") {
                                fetchFioByMdmCode(logisticsDetail.created_by_name) { fio ->
                                    logisticsDetail.created_by_name = fio
                                    updateUI(logisticsDetail)
                                }
                            } else {
                                updateUI(logisticsDetail)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Error parsing JSON response: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(this@DetailLogisticsActivity, "Ошибка при обработке данных.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        })
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun updateUI(item: LogisticsItem) {
        logisticsItems.clear()
        logisticsItems.add(item)
        adapter.notifyDataSetChanged()
    }
    private fun showStatusUpdateDialog(context: Context, item: LogisticsItem) {
        val statuses: Array<String>
        val statusValues: Array<String>
        val icons: Array<Int>

        if (rolesList.contains("2")) { // Check for admin role
            statuses = arrayOf("Принята", "Аннулирована", "Выполняется", "На подтверждении", "Выполнена")
            statusValues = arrayOf("1", "-1", "2", "3", "4")
            icons = arrayOf(
                R.drawable.icon_accepted,
                R.drawable.icon_cancelled,
                R.drawable.icon_in_progress,
                R.drawable.icon_pending,
                R.drawable.icon_completed
            )
        } else {
            when (item.status) {
                "0" -> { // Зарегистрирована
                    statuses = arrayOf("Принята", "Аннулирована")
                    statusValues = arrayOf("1", "-1")
                    icons = arrayOf(R.drawable.icon_accepted, R.drawable.icon_cancelled)
                }
                "1" -> { // Принята
                    statuses = arrayOf("Выполняется")
                    statusValues = arrayOf("2")
                    icons = arrayOf(R.drawable.icon_in_progress)
                }
                "-1" -> { // Аннулирована
                    statuses = arrayOf()
                    statusValues = arrayOf()
                    icons = arrayOf()
                }
                "2" -> { // Выполняется
                    statuses = arrayOf("На подтверждении")
                    statusValues = arrayOf("3")
                    icons = arrayOf(R.drawable.icon_pending)
                }
                "3" -> { // На подтверждении
                    statuses = arrayOf("Выполнена")
                    statusValues = arrayOf("4")
                    icons = arrayOf(R.drawable.icon_completed)
                }
                "4" -> { // Выполнена
                    statuses = arrayOf()
                    statusValues = arrayOf()
                    icons = arrayOf()
                }
                else -> { // Default case (e.g., unknown status)
                    statuses = arrayOf()
                    statusValues = arrayOf()
                    icons = arrayOf()
                }
            }
        }

        val adapter = StatusAdapter(context, statuses, icons)
        val bottomSheetDialog = BottomSheetDialog(context)
        val bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_layout, null)
        val listView = bottomSheetView.findViewById<ListView>(R.id.status_list_view)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedStatus = statusValues[position]
            val currentDateTime = getCurrentDateTime()
            when (selectedStatus) {
                "1" -> handleStatus1(item, currentDateTime)
                "-1" -> handleStatusMinus1(item, currentDateTime)
                "2" -> handleStatus2(item, currentDateTime)
                "3" -> handleStatus3(item, currentDateTime)
                "4" -> handleStatus4(item, currentDateTime)
            }
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.setContentView(bottomSheetView)
        bottomSheetDialog.window?.setBackgroundDrawableResource(R.drawable.transparent_background)
        bottomSheetDialog.window?.setBackgroundDrawable(
            ContextCompat.getDrawable(
                context,
                R.drawable.rounded_bottom_sheet_background
            )
        )
        bottomSheetDialog.show()
    }
    // ПРИНЯТА
    private fun handleStatus1(item: LogisticsItem, currentDateTime: String) {
        if (roleCheck == "2" || isNfcScanned) {
            updateLogistics(item.id, status = "1", fieldName = "is_accepted_by", newValue = mdmCode, isAcceptedAt = currentDateTime)
        } else {
            showNfcDialog("Сканируйте NFC метку для начала работы", true)
        }
    }
    // АННУЛИРОВАНА
    private fun handleStatusMinus1(item: LogisticsItem, currentDateTime: String) {
        if (roleCheck == "2" || roleCheck == "64" || rolesList.contains("64")) {
            updateLogistics(item.id, status = "-1", fieldName = "is_decline_by", newValue = mdmCode, isDeclineAt = currentDateTime)
        } else {
            Toast.makeText(this, "Недостаточно прав для изменения статуса", Toast.LENGTH_LONG).show()
        }
    }
    // ВЫПОЛНЯЕТСЯ
    private fun handleStatus2(item: LogisticsItem, currentDateTime: String) {
        if (roleCheck == "2" || roleCheck == "64" || roleCheck == "65" || rolesList.contains("64") || rolesList.contains("65")) {
            updateLogistics(item.id, status = "2", fieldName = "is_doing_by", newValue = mdmCode, isDoingAt = currentDateTime)
        } else {
            Toast.makeText(this, "Недостаточно прав для изменения статуса", Toast.LENGTH_LONG).show()
        }
    }
    // НА ПОДТВЕРЖДЕНИИ
    private fun handleStatus3(item: LogisticsItem, currentDateTime: String) {
        if (roleCheck == "2" || roleCheck == "64" || roleCheck == "65" || rolesList.contains("64") || rolesList.contains("65")) {
            updateLogistics(item.id, status = "3", fieldName = "is_check_by", newValue = mdmCode, isCheckAt = currentDateTime)
        } else {
            Toast.makeText(this, "Недостаточно прав для изменения статуса", Toast.LENGTH_LONG).show()
        }
    }
    // ВЫПОЛНЕНА
    private fun handleStatus4(item: LogisticsItem, currentDateTime: String) {
        if (type == "prp") {
            if (roleCheck == "2" || isNfcScanned || item.receiver_mdm == mdmCode) {
                updateLogistics(item.id, status = "4", fieldName = "is_ready_by", newValue = mdmCode, isReadyAt = currentDateTime)
            } else {
                showNfcDialog("Сканируйте NFC метку для завершения выполнения работы", false)
            }
        } else {
            if (roleCheck == "2" || roleCheck == "64" || isNfcScanned || item.receiver_mdm == mdmCode || rolesList.contains("64")) {
                updateLogistics(item.id, status = "4", fieldName = "is_ready_by", newValue = mdmCode, isReadyAt = currentDateTime)
            } else {
                showNfcDialog("Сканируйте NFC метку для завершения выполнения работы", false)
            }
        }
    }
    private fun isNfcScanned(): Boolean {
        return isNfcScanned
    }
    private fun updateLogistics(
        logisticsId: String?,
        status: String? = null,
        fieldName: String? = null,
        newValue: String? = null,
        isDoingAt: String? = null,
        isAcceptedAt: String? = null,
        isDeclineAt: String? = null,
        isCheckAt: String? = null,
        isReadyAt: String? = null
    ) {
        if (logisticsId == null) return
        val client = OkHttpClient()
        val requestBodyBuilder = FormBody.Builder()
        status?.let { requestBodyBuilder.add("status", it) }
        fieldName?.let { newValue?.let { value -> requestBodyBuilder.add(it, value) } }
        isDoingAt?.let { requestBodyBuilder.add("is_doing_at", it) }
        isAcceptedAt?.let { requestBodyBuilder.add("is_accepted_at", it) }
        isDeclineAt?.let { requestBodyBuilder.add("is_decline_at", it) }
        isCheckAt?.let { requestBodyBuilder.add("is_check_at", it) }
        isReadyAt?.let { requestBodyBuilder.add("is_ready_at", it) }
        val requestBody = requestBodyBuilder.build()
        val request = Request.Builder()
            .url("http://192.168.200.250/api/update_logistic/$logisticsId")
            .post(requestBody)
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(tag, "Error updating logistics: ${e.message}")
            }
            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e(tag, "Error response from server: ${response.code}")
                    return
                }
                runOnUiThread {
                    val itemIndex = logisticsItems.indexOfFirst { it.id == logisticsId }
                    if (itemIndex != -1) {
                        status?.let { logisticsItems[itemIndex].status = it }
                        if (fieldName != null && newValue != null) {
                            when (fieldName) {
                                "is_doing_by" -> logisticsItems[itemIndex].is_doing_by = newValue
                                "is_accepted_by" -> logisticsItems[itemIndex].is_accepted_by = newValue
                                "is_decline_by" -> logisticsItems[itemIndex].is_decline_by = newValue
                                "is_check_by" -> logisticsItems[itemIndex].is_check_by = newValue
                                "is_ready_by" -> logisticsItems[itemIndex].is_ready_by = newValue
                            }
                        }
                        isDoingAt?.let { logisticsItems[itemIndex].is_doing_at = it }
                        isAcceptedAt?.let { logisticsItems[itemIndex].is_accepted_at = it }
                        isDeclineAt?.let { logisticsItems[itemIndex].is_decline_at = it }
                        isCheckAt?.let { logisticsItems[itemIndex].is_check_at = it }
                        isReadyAt?.let { logisticsItems[itemIndex].is_ready_at = it }
                        adapter.notifyItemChanged(itemIndex)
                    }
                }
            }
        })
    }
    private fun getCurrentDateTime(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return formatter.format(Date())
    }
    private fun fetchFioByMdmCode(mdmCode: String, callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val sotrudnikiInfo = getAllSotrudnikiInfo(this@DetailLogisticsActivity)
                val fio = sotrudnikiInfo.find { it.mdmcode == mdmCode }?.fio ?: "Неизвестен"
                withContext(Dispatchers.Main) {
                    callback(fio)
                }
            } catch (e: Exception) {
                Log.e(tag, "Ошибка при получении ФИО: ${e.message}")
                withContext(Dispatchers.Main) {
                    callback("Ошибка")
                }
            }
        }
    }
    suspend fun getAllSotrudnikiInfo(context: Context): List<SotrudnikiInfo> {
        val apiUrl = "http://192.168.200.250/api/get_all_sotrudniki"
        return withContext(Dispatchers.IO) {
            val sotrudniki = mutableListOf<SotrudnikiInfo>()
            val request = Request.Builder()
                .url((apiUrl))
                .build()
            val response: Response = client.newCall(request).execute()
            if (response.isSuccessful) {
                response.body?.string()?.let { responseBody ->
                    val jsonData = JSONObject(responseBody)
                    for (key in jsonData.keys()) {
                        val sotrudnikiData = jsonData.getJSONObject(key)
                        val mdmcode = sotrudnikiData.getString("mdmcode")
                        val fio = sotrudnikiData.getString("Рабочий")
                        sotrudniki.add(SotrudnikiInfo(mdmcode, fio))
                    }
                }
            } else {
                throw Exception("Ошибка при получении данных: ${response.code}")
            }
            sotrudniki
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
            val photoFile = currentPhotoFile
            photoFile?.let {
                val uri = Uri.fromFile(it)
                uploadPhotoToServer(uri, currentLogisticsId, this)
            }
        } else if (requestCode == REQUEST_GALLERY && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                uploadPhotoToServer(uri, currentLogisticsId, this)
            }
        }
    }
    private fun uploadPhotoToServer(uri: Uri, logisticsId: String?, context: Context) {
        if (logisticsId == null) return
        val filePath = getRealPathFromURI(this, uri)
        Log.d("Upload", "Полученный путь к файлу: $filePath")
        if (filePath != null) {
            val file = File(filePath)
            Log.d("Upload", "Путь к файлу: ${file.absolutePath}")
            val requestBody: RequestBody
            if (file.exists()) {
                Log.d("Upload", "Файл существует")
                requestBody = RequestBody.create("image/*".toMediaType(), file)
            } else {
                Log.e("Upload", "Файл не найден по пути: ${file.absolutePath}")
                val parcelFileDescriptor = contentResolver.openFileDescriptor(uri, "r")
                val fileInputStream = FileInputStream(parcelFileDescriptor?.fileDescriptor)
                requestBody = RequestBody.create("image/*".toMediaType(), fileInputStream.readBytes())
            }
            val part = MultipartBody.Part.createFormData("files[]", file.name, requestBody)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("http://192.168.200.250/api/upload_logistic_files/$logisticsId")
                        .post(MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addPart(part)
                            .addFormDataPart("created_by", mdmCode)
                            .build())
                        .build()
                    Log.d("Upload", "Отправка запроса на сервер...")
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        Log.d("Upload", "Фотография успешно загружена")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Фотография успешно загружена", Toast.LENGTH_SHORT).show()
                            fetchLogisticsDetail(logisticsId)
                        }
                    } else {
                        Log.e("Upload", "Ошибка загрузки фотографии: ${response.code} - ${response.message}")
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Ошибка загрузки фотографии: ${response.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Upload", "Ошибка загрузки фотографии", e)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Ошибка загрузки фотографии", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Log.e("Upload", "Не удалось получить путь к файлу из URI")
            Toast.makeText(context, "Не удалось получить путь к файлу из URI", Toast.LENGTH_SHORT).show()
        }
    }
    private fun getRealPathFromURI(context: Context, contentUri: Uri): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = context.contentResolver.query(contentUri, projection, null, null, null)
        cursor?.use {
            val columnIndex = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            it.moveToFirst()
            path = it.getString(columnIndex)
        }
        if (path == null) {
            path = contentUri.path
            Log.d("getRealPathFromURI", "Путь из Uri: $path")
        }
        return path
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            MY_READ_EXTERNAL_REQUEST -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission", "Разрешение на чтение внешнего хранилища получено")
                } else {
                    Log.d("Permission", "Разрешение на чтение внешнего хранилища не получено")
                }
            }
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("Permission", "Разрешение на использование камеры получено")
                } else {
                    Log.d("Permission", "Разрешение на использование камеры не получено")
                }
            }
        }
    }
    private fun disableUI() {
        recyclerView.isEnabled = false
        btnClose.isEnabled = false
        Toast.makeText(this, "Версия приложения устарела. Пожалуйста, обновите приложение.", Toast.LENGTH_LONG).show()
    }
    private val MY_READ_EXTERNAL_REQUEST = 1
    private val REQUEST_CAMERA_PERMISSION = 2
}