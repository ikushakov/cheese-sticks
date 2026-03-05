package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.Ndef
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewAnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.semimanufactures.Auth.authToken
import com.example.semimanufactures.Auth.authTokenAPI
import com.example.semimanufactures.DetailLogisticsAdapter.Companion.REQUEST_CAMERA
import com.example.semimanufactures.DetailLogisticsAdapter.Companion.REQUEST_GALLERY
import com.example.semimanufactures.DetailLogisticsAdapter.Companion.currentPhotoFile
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.squareup.picasso.BuildConfig
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONException
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate
import kotlin.math.hypot

val version_name: String = "Версия: 9.0"

class DetailLogisticsActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DetailLogisticsAdapter
    private val logisticsItems = mutableListOf<LogisticsItem>()
    private val tag = "DetailLogisticsActivity"
    private var sendFromId: String? = null
    private var sendTo: String? = null
    private var currentLogisticsId: String? = null
    private var currentUsername: String? = null
    private var currentUserId: Int? = null
    private var currentRoleCheck: String? = null
    private var currentMdmCode: String? = null
    private var currentFio: String? = null
    private var currentDeviceInfo: String? = null
    private var currentRolesString: String? = null
    private var currentDeviceToken: String? = null
    private var currentIsAuthorized:  Boolean = false
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
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val REQUEST_NFC_PERMISSION = 1002
    private lateinit var progressBar: ProgressBar
    private lateinit var contentLayout: LinearLayout
    private companion object {

        private const val REVEAL_DURATION_MS: Long = 1200
        private const val OVERLAY_HOLD_MS: Long = 700
        private const val OVERLAY_FADE_DURATION_MS: Long = 600
        private const val FB_FADE_IN_MS: Long = 550
        private const val FB_HOLD_MS: Long = 400
        private const val FB_FADE_OUT_MS: Long = 550
    }

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
        setContentView(R.layout.activity_detail_logistics)
        contentLayout = findViewById(R.id.content_layout)
        progressBar = findViewById(R.id.status_progress)
        showLoading(true)
        checkAndRequestPermissions()
        type = intent.getStringExtra("type") ?: ""
        if (currentRolesString?.isNotEmpty() == true) {
            rolesList.addAll(currentRolesString!!.split(",").map { it.trim() })
        }
        val logisticsId = intent.getStringExtra("logistics_id") ?: run {
            showErrorAndRedirect()
            return
        }
        Log.d(tag, "User id: $currentUserId, Username: $currentUsername, Role: $currentRoleCheck, MDM Code: $currentMdmCode")
        supportActionBar?.hide()
        recyclerView = findViewById(R.id.detail_recycler_view)
        adapter = DetailLogisticsAdapter(logisticsItems, currentUsername ?: "", currentUserId ?: 0, currentRoleCheck ?: "",
            currentMdmCode ?: "", currentFio ?: "", currentDeviceInfo ?: "", currentRolesString ?: "",
            currentDeviceToken ?: "", currentIsAuthorized ?: false) { item -> showStatusUpdateDialog(this, item) }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            fetchLogisticsDetail(currentLogisticsId ?: "")
        }
        fetchLogisticsDetail(logisticsId)
        go_to_logistic = findViewById(R.id.go_to_logistic)
        btnClose = findViewById(R.id.btnClose)
        btnClose.setOnClickListener {
            // Возвращаемся с результатом, чтобы обновить данные
            val returnIntent = Intent().apply {
                putExtra("shouldRefresh", true)
            }
            setResult(Activity.RESULT_OK, returnIntent)
            finish()
        }
        setupNfcAdapter()
        main_layout = findViewById(R.id.main_layout)
    }
    private fun showLoading(show: Boolean) {
        if (show) {
            progressBar.visibility = View.VISIBLE
            contentLayout.visibility = View.GONE
        } else {
            progressBar.visibility = View.GONE
            contentLayout.visibility = View.VISIBLE
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
    private fun showErrorAndRedirect() {
        runOnUiThread {
            showLoading(false)
            Toast.makeText(this, "Заявка не найдена", Toast.LENGTH_LONG).show()
        }
        val intent = Intent(this, LogisticActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        finish()
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
//        dialog.setOnCancelListener {
//            nfcAdapter.disableForegroundDispatch(this)
//        }
        val buttonYes: Button = dialogView.findViewById(R.id.buttonYes)
        buttonYes.setOnClickListener {
            enableNfcReaderMode(isStart)
            dialog.dismiss()
        }
        val buttonNo: Button = dialogView.findViewById(R.id.buttonNo)
        buttonNo.setOnClickListener {
            nfcAdapter.disableForegroundDispatch(this)
            currentLogisticsId?.let { finishItemLoading(it) }
            dialog.dismiss()
        }
        dialog.show()
    }
    private fun showResultDialog(resultMessage: String, isSuccess: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.nfc_new_result_dialog, null)
        val messageTextView: TextView = dialogView.findViewById(R.id.messageTextView)
        val buttonClose: Button       = dialogView.findViewById(R.id.buttonClose)
        val root: LinearLayout        = dialogView.findViewById(R.id.rootLayout)
        val overlay: View             = dialogView.findViewById(R.id.stateOverlay) // новый overlay

        messageTextView.text = resultMessage

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        buttonClose.setOnClickListener { dialog.dismiss() }

        dialog.setOnShowListener {
            // Запускаем анимацию после раскладки
            root.post {
                playCircularReveal(root, overlay, isSuccess)
            }
        }

        dialog.show()
    }

    private fun playCircularReveal(root: View, overlay: View, isSuccess: Boolean) {
        val color = if (isSuccess) Color.parseColor("#CCFF00") else Color.parseColor("#FF0000")
        overlay.setBackgroundColor(color)
        overlay.alpha = 0.18f
        overlay.visibility = View.VISIBLE

        if (Build.VERSION.SDK_INT >= 21) {
            val cx = root.width / 2
            val cy = root.height / 2
            val endRadius = hypot(root.width.toDouble(), root.height.toDouble()).toFloat()

            val reveal = ViewAnimationUtils.createCircularReveal(overlay, cx, cy, 0f, endRadius)
            reveal.duration = REVEAL_DURATION_MS
            reveal.start()

            overlay.animate()
                .alpha(0f)
                .setStartDelay(OVERLAY_HOLD_MS)
                .setDuration(OVERLAY_FADE_DURATION_MS)
                .withEndAction {
                    overlay.visibility = View.GONE
                    overlay.alpha = 1f
                }
                .start()
        } else {
            // Fallback для <21: ступенчатая альфа
            overlay.alpha = 0f
            overlay.visibility = View.VISIBLE
            overlay.animate().alpha(0.18f)
                .setDuration(FB_FADE_IN_MS)
                .withEndAction {
                    overlay.animate().alpha(0f)
                        .setStartDelay(FB_HOLD_MS)
                        .setDuration(FB_FADE_OUT_MS)
                        .withEndAction {
                            overlay.visibility = View.GONE
                        }
                        .start()
                }
                .start()
        }
    }
    private fun enableNfcReaderMode(isStartWork: Boolean) {
        if (!::nfcAdapter.isInitialized) {
            Toast.makeText(this, "NFC не поддерживается или не настроен", Toast.LENGTH_SHORT).show()
            return
        }
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
    private var isNfcProcessing = false
    private fun readFromTag(tag: Tag) {
        val ndef = Ndef.get(tag)
        var resultMessage = "Ошибка чтения NFC метки"
        var isSuccess = false

        if (isNfcProcessing) return
        isNfcProcessing = true

        try {
            ndef?.connect()
            try {
                val ndefMessage = ndef?.ndefMessage
                val message = ndefMessage?.records?.joinToString("\n") { record ->
                    String(record.payload).replace(Regex("[^0-9]"), "")
                } ?: "Данные не найдены"

                val currentDateTime = getCurrentDateTime()

                if (isStartWork) {
                    // НАЧАЛО РАБОТЫ - СТАТУС "ПРИНЯТА"
                    // Проверяем, что метка соответствует send_from
                    if (message == sendFromId) {
                        isSuccess = true
                        Toast.makeText(this, "Можете приступать к работе", Toast.LENGTH_SHORT).show()

                        // Обновляем статус на "Принята"
                        updateLogistics(
                            currentLogisticsId ?: "",
                            status = "1",
                            fieldName = "is_accepted_by",
                            newValue = currentMdmCode,
                            isAcceptedAt = currentDateTime,
                            executor = currentMdmCode,
                            mdmCode = currentMdmCode,
                            versionName = version_name
                        )

                        resultMessage = "Успешно начали работу на складе с ID: $message"
                    } else {
                        isSuccess = false
                        Toast.makeText(this, "Неверный склад", Toast.LENGTH_SHORT).show()
                        resultMessage = "Ошибка: Неверный склад для начала работы. Ожидался: $sendFromId, получено: $message"
                    }
                } else {
                    // ЗАВЕРШЕНИЕ РАБОТЫ - СТАТУС "ВЫПОЛНЕНА"
                    // Для всех типов, кроме "other" - строгая проверка NFC
                    if (type == "other") {
                        // Для типа "other" - NFC не требуется, но если данные есть - проверяем
                        if (sendTo.isNullOrEmpty()) {
                            // Если нет данных о месте прибытия - просто завершаем
                            if (!hasPhotoLocal(currentLogisticsId)) {
                                isSuccess = false
                                Toast.makeText(this, "Необходимо добавить фото перед завершением.", Toast.LENGTH_SHORT).show()
                                resultMessage = "Добавьте фото к заявке и повторите подтверждение."
                            } else {
                                isSuccess = true
                                Toast.makeText(this, "Заявка на перемещение выполнена", Toast.LENGTH_SHORT).show()
                                updateLogistics(
                                    currentLogisticsId ?: "",
                                    status = "4",
                                    fieldName = "is_ready_by",
                                    newValue = currentMdmCode,
                                    isReadyAt = currentDateTime,
                                    executor = currentMdmCode,
                                    mdmCode = currentMdmCode,
                                    versionName = version_name
                                )
                                resultMessage = "Заявка на перемещение выполнена"
                            }
                        } else {
                            // Если есть sendTo - проверяем NFC
                            if (message == sendTo) {
                                if (!hasPhotoLocal(currentLogisticsId)) {
                                    isSuccess = false
                                    Toast.makeText(this, "Необходимо добавить фото перед завершением.", Toast.LENGTH_SHORT).show()
                                    resultMessage = "Добавьте фото к заявке и повторите подтверждение."
                                } else {
                                    isSuccess = true
                                    Toast.makeText(this, "Заявка на перемещение выполнена", Toast.LENGTH_SHORT).show()
                                    updateLogistics(
                                        currentLogisticsId ?: "",
                                        status = "4",
                                        fieldName = "is_ready_by",
                                        newValue = currentMdmCode,
                                        isReadyAt = currentDateTime,
                                        executor = currentMdmCode,
                                        mdmCode = currentMdmCode,
                                        versionName = version_name
                                    )
                                    resultMessage = "Заявка на перемещение выполнена на склад с ID: $message"
                                }
                            } else {
                                isSuccess = false
                                Toast.makeText(this, "Неверный склад", Toast.LENGTH_SHORT).show()
                                resultMessage = "Ошибка: Неверный склад для завершения работы. Ожидался: $sendTo, получено: $message"
                            }
                        }
                    } else {
                        // СТАНДАРТНАЯ ЛОГИКА ДЛЯ ДРУГИХ ТИПОВ
                        if (message == sendTo) {
                            if (!hasPhotoLocal(currentLogisticsId)) {
                                isSuccess = false
                                Toast.makeText(this, "Необходимо добавить фото перед завершением.", Toast.LENGTH_SHORT).show()
                                resultMessage = "Добавьте фото к заявке и повторите подтверждение."
                            } else {
                                isSuccess = true
                                Toast.makeText(this, "Заявка на перемещение выполнена", Toast.LENGTH_SHORT).show()
                                updateLogistics(
                                    currentLogisticsId ?: "",
                                    status = "4",
                                    fieldName = "is_ready_by",
                                    newValue = currentMdmCode,
                                    isReadyAt = currentDateTime,
                                    executor = currentMdmCode,
                                    mdmCode = currentMdmCode,
                                    versionName = version_name
                                )
                                resultMessage = "Заявка на перемещение выполнена на склад с ID: $message"
                            }
                        } else {
                            isSuccess = false
                            Toast.makeText(this, "Неверный склад", Toast.LENGTH_SHORT).show()
                            resultMessage = "Ошибка: Неверный склад для завершения работы. Ожидался: $sendTo, получено: $message"
                        }
                    }
                }
            } catch (e: Exception) {
                isSuccess = false
                Log.e("NFC", "Ошибка чтения NFC метки", e)
                resultMessage = "Ошибка чтения данных с NFC метки: ${e.message}"
            }
        } catch (e: IOException) {
            isSuccess = false
            Log.e("NFC", "Ошибка соединения с NFC меткой", e)
            resultMessage = "Ошибка соединения с NFC меткой"
        } finally {
            try {
                ndef?.close()
            } catch (e: IOException) {
                Log.e("NFC", "Ошибка при закрытии соединения с NFC", e)
            }
            isNfcProcessing = false
        }

        // Отключаем NFC после обработки тега
        Handler(Looper.getMainLooper()).postDelayed({
            try {
                nfcAdapter.disableForegroundDispatch(this)
            } catch (e: Exception) {
                Log.e("NFC", "Ошибка при отключении NFC", e)
            }
        }, 100)

        if (!isSuccess) {
            runOnUiThread { currentLogisticsId?.let { finishItemLoading(it) } }
        }

        showResultDialog(resultMessage, isSuccess)
    }


    override fun onResume() {
        super.onResume()
//        if (checkNfcPermission()) {
//            if (type == "prp" || type == "doc" || type == "other" || type == "stanok") {
//                nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null)
//            }
//        } else {
//            requestNfcPermission()
//        }
    }
    override fun onPause() {
        super.onPause()
        try {
            nfcAdapter.disableForegroundDispatch(this)
        } catch (e: Exception) {
            Log.e("NFC", "Ошибка при отключении NFC в onPause", e)
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("sendFromId", sendFromId)
        outState.putString("sendTo", sendTo)
        outState.putString("currentLogisticsId", currentLogisticsId)
    }
    private var isFetching = false  // Переменная класса для блокировки повторных запросов

    private fun shouldVerify(code: Int) = code == 502 || code == 503 || code == 504

    private fun verifyStatusAfterWrite(
        logisticsId: String,
        expectedStatus: String?,
        onDone: (Boolean, LogisticsItem?) -> Unit
    ) {
        if (expectedStatus == null) { onDone(false, null); return }
        CoroutineScope(Dispatchers.IO).launch {
            repeat(3) { // 2–3 попытки с небольшими паузами
                val item = fetchLogisticsSync(logisticsId)
                if (item?.status == expectedStatus) {
                    withContext(Dispatchers.Main) { onDone(true, item) }
                    return@launch
                }
                delay(1500)
            }
            withContext(Dispatchers.Main) { onDone(false, null) }
        }
    }

    private fun hasPhotoLocal(logisticsId: String?): Boolean {
        if (logisticsId.isNullOrBlank()) return false
        val item = logisticsItems.firstOrNull { it.id == logisticsId } ?: return false
        return item.hasPhotos() // Используем метод из LogisticsItem
    }

    private fun fetchLogisticsSync(id: String): LogisticsItem? {
        val urls = listOf(
            "https://api.gkmmz.ru/api/get_logistics/$id",
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_logistics/$id"
        )

        val client = (application as App).okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        loop@ for (u in urls) {
            try {
                val req = Request.Builder()
                    .url(u)
                    .post(FormBody.Builder().build())
                    .addHeader("X-Apig-AppCode", authTokenAPI)
                    .addHeader("X-Auth-Token", authToken)
                    .build()

                val r = client.newCall(req).execute()
                try {
                    if (r.isSuccessful) {
                        val body = r.body?.string().orEmpty()

                        // Используем универсальный парсер
                        val parseResult = JsonParser.parseLogisticsResponse(body)
                        if (parseResult.data != null) {
                            return parseResult.data
                        } else {
                            Log.w(tag, "fetchLogisticsSync: Failed to parse data for $id: ${parseResult.error}")
                        }
                    }

                    // на 429 пробуем следующий URL
                    if (r.code == 429) {
                        continue@loop
                    }

                    // для прочих кодов просто пробуем следующий
                    continue@loop
                } finally {
                    r.close()
                }
            } catch (e: Exception) {
                Log.e(tag, "Error in fetchLogisticsSync for $id", e)
                continue@loop
            }
        }

        return null
    }

    private fun logJsonStructure(jsonString: String) {
        try {
            Log.d(tag, "=== JSON Structure Analysis ===")

            // Пробуем как JSONObject
            try {
                val json = JSONObject(jsonString)
                Log.d(tag, "Successfully parsed as JSONObject")

                // Логируем ключи верхнего уровня
                val keys = json.keys()
                val keyList = mutableListOf<String>()
                while (keys.hasNext()) {
                    keyList.add(keys.next())
                }
                Log.d(tag, "Top-level keys: ${keyList.joinToString(", ")}")

                // Анализируем поле data
                if (json.has("data")) {
                    val data = json.get("data")
                    when (data) {
                        is JSONObject -> {
                            Log.d(tag, "Field 'data' is JSONObject")
                            Log.d(tag, "Data object keys: ${data.keys().asSequence().toList().take(10).joinToString(", ")}...")
                        }
                        is JSONArray -> {
                            Log.d(tag, "Field 'data' is JSONArray with ${data.length()} elements")
                            if (data.length() > 0) {
                                val first = data.get(0)
                                if (first is JSONObject) {
                                    Log.d(tag, "First array element keys: ${first.keys().asSequence().toList().take(10).joinToString(", ")}...")
                                }
                            }
                        }
                        else -> {
                            Log.d(tag, "Field 'data' is ${data::class.simpleName}")
                        }
                    }
                }
            } catch (e: JSONException) {
                Log.d(tag, "Not a JSONObject: ${e.message}")
            }

            // Пробуем как JSONArray
            try {
                val jsonArray = JSONArray(jsonString)
                Log.d(tag, "Successfully parsed as JSONArray with ${jsonArray.length()} elements")
            } catch (e: JSONException) {
                Log.d(tag, "Not a JSONArray either")
            }

            Log.d(tag, "=== End JSON Analysis ===")
        } catch (e: Exception) {
            Log.e(tag, "Error analyzing JSON structure", e)
        }
    }

    private fun applyServerItem(item: LogisticsItem) {
        val idx = logisticsItems.indexOfFirst { it.id == item.id }
        if (idx == -1) return
        logisticsItems[idx] = item
        adapter.notifyItemChanged(idx)
    }

    private fun fetchLogisticsDetail(id: String) {
        Log.d(tag, "Fetching details for logistics ID: $id")
        swipeRefreshLayout.isRefreshing = false
        showLoading(true)
        if (isFetching) return
        isFetching = true

        val urls = listOf(
            "https://api.gkmmz.ru/api/get_logistics/$id",
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_logistics/$id"
        )

        val client = (application as App).okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        fun tryNextUrl(index: Int) {
            if (index >= urls.size) {
                runOnUiThread {
                    swipeRefreshLayout.isRefreshing = false
                    showLoading(false)
                    Toast.makeText(this@DetailLogisticsActivity, "Ошибка соединения с сервером", Toast.LENGTH_SHORT).show()
                }
                isFetching = false
                return
            }

            val url = urls[index]
            val req = Request.Builder()
                .url(url)
                .post(FormBody.Builder().build())
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token", authToken)
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(tag, "Error fetching details from $url: ${e.message}")
                    tryNextUrl(index + 1)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        if (response.code == 429) {
                            Log.w(tag, "429 on $url, trying fallback…")
                            response.close()
                            tryNextUrl(index + 1)
                            return
                        }

                        Log.w(tag, "Response error from $url: ${response.code}")
                        runOnUiThread {
                            swipeRefreshLayout.isRefreshing = false
                            showLoading(false)
                            val errorMsg = when (response.code) {
                                404 -> "Заявка не найдена"
                                500 -> "Внутренняя ошибка сервера"
                                else -> "Ошибка сервера: ${response.code}"
                            }
                            Toast.makeText(this@DetailLogisticsActivity, errorMsg, Toast.LENGTH_SHORT).show()
                        }
                        isFetching = false
                        response.close()
                        return
                    }

                    val body = response.body?.string().orEmpty()
                    response.close()
                    logJsonStructure(body.take(2000))
                    // Логируем ответ для отладки (первые 500 символов)
                    Log.d(tag, "Response from $url: ${body.take(500)}...")

                    runOnUiThread { showLoading(false) }

                    try {
                        // Используем универсальный парсер
                        val parseResult = JsonParser.parseLogisticsResponse(body)

                        if (parseResult.data != null) {
                            val logisticsDetail = parseResult.data
                            runOnUiThread {
                                currentLogisticsId = logisticsDetail.id
                                sendFromId = logisticsDetail.send_from
                                sendTo = logisticsDetail.send_to
                                updateUI(logisticsDetail)
                                Log.d(tag, "Успешно загружено. Откуда: $sendFromId, Куда: $sendTo")
                                Log.d(tag, "Тип данных в ответе: ${if (parseResult.isDataArray) "массив" else "объект"}")

                                if (type == "doc") {
                                    fetchFioByMdmCode(logisticsDetail.created_by_name) { fio ->
                                        logisticsDetail.created_by_name = fio
                                        updateUI(logisticsDetail)
                                    }
                                }
                                swipeRefreshLayout.isRefreshing = false
                            }
                        } else {
                            Log.e(tag, "LogisticsItem is null! Parse result: $parseResult")
                            runOnUiThread {
                                val errorMessage = parseResult.error ?: "Заявка не найдена или данные некорректны"
                                Toast.makeText(this@DetailLogisticsActivity, errorMessage, Toast.LENGTH_SHORT).show()

                                // Если это не временная ошибка сервера, возвращаемся назад
                                if (response.code != 500 && response.code != 503) {
                                    val intent = Intent(this@DetailLogisticsActivity, LogisticActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                                    startActivity(intent)
                                    finish()
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(tag, "Critical error parsing JSON response: ${e.message}", e)
                        Log.e(tag, "Response body that caused error: ${body.take(1000)}")
                        runOnUiThread {
                            Toast.makeText(this@DetailLogisticsActivity, "Критическая ошибка при обработке данных", Toast.LENGTH_SHORT).show()
                        }
                    } finally {
                        isFetching = false
                    }
                }
            })
        }

        tryNextUrl(0)
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
    @SuppressLint("NotifyDataSetChanged")
    private fun updateUI(item: LogisticsItem) {
        runOnUiThread {
            showLoading(false)
            logisticsItems.clear()
            logisticsItems.add(item)
            adapter.notifyDataSetChanged()
        }
    }
    private fun showConfirmationMethodDialog(item: LogisticsItem) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.confirmation_method_dialog, null)
        dialogBuilder.setView(dialogView)
        val dialog = dialogBuilder.create()

        val nfcButton: Button = dialogView.findViewById(R.id.nfcButton)
        val codeButton: Button = dialogView.findViewById(R.id.codeButton)

        // Для типа "other" - показываем NFC только если ЕСТЬ sendTo
        if (type == "other" && sendTo.isNullOrEmpty()) {
            // Если нет данных о месте прибытия - скрываем NFC
            nfcButton.visibility = View.GONE
        }

        nfcButton.setOnClickListener {
            dialog.dismiss()
            showNfcDialog("Сканируйте NFC метку склада назначения для завершения работы", false)
        }

        codeButton.setOnClickListener {
            dialog.dismiss()
            requestConfirmationCode(item)
        }

        dialog.show()
    }

    private fun showCodeInputDialog(item: LogisticsItem) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.code_input_dialog, null)
        dialogBuilder.setView(dialogView)
        val dialog = dialogBuilder.create()
        val codeInput: EditText = dialogView.findViewById(R.id.codeInput)
        val submitButton: Button = dialogView.findViewById(R.id.submitButton)
        submitButton.setOnClickListener {
            val code = codeInput.text.toString()
            if (code.isNotEmpty()) {
                checkConfirmationCode(item, code, dialog)
            } else {
                Toast.makeText(this, "Введите код подтверждения", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }
    private fun requestConfirmationCode(item: LogisticsItem) {
        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/create_delivery_code",
            "https://api.gkmmz.ru/api/create_delivery_code"
        )

        val client = (application as App).okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        fun attempt(i: Int) {
            if (i >= urls.size) {
                runOnUiThread {
                    Toast.makeText(this@DetailLogisticsActivity, "Ошибка при запросе кода подтверждения", Toast.LENGTH_SHORT).show()
                    finishItemLoading(item.id)
                }
                return
            }

            val req = Request.Builder()
                .url(urls[i])
                .post(FormBody.Builder().add("id", item.id).build())
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token", authToken)
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    attempt(i + 1)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.close()
                        runOnUiThread { showCodeInputDialog(item) }
                    } else {
                        val code = response.code
                        response.close()
                        if (code == 429) {
                            Log.w("RequestConfirmationCode", "429 on ${urls[i]}, try fallback")
                            attempt(i + 1)
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@DetailLogisticsActivity, "Ошибка при запросе кода подтверждения", Toast.LENGTH_SHORT).show()
                                finishItemLoading(item.id)
                            }
                        }
                    }
                }
            })
        }

        attempt(0)
    }
    private fun checkConfirmationCode(item: LogisticsItem, code: String, dialog: AlertDialog) {
        if (item.status == "4") {
            Toast.makeText(this, "Статус уже 'Выполнена'", Toast.LENGTH_SHORT).show()
            return
        }

        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/check_delivery_code",
            "https://api.gkmmz.ru/api/check_delivery_code"
        )

        val client = (application as App).okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        fun attempt(i: Int) {
            if (i >= urls.size) {
                runOnUiThread {
                    Toast.makeText(this@DetailLogisticsActivity, "Ошибка при проверке кода подтверждения", Toast.LENGTH_SHORT).show()
                    finishItemLoading(item.id)
                }
                return
            }

            val req = Request.Builder()
                .url(urls[i])
                .post(
                    FormBody.Builder()
                        .add("id", item.id)
                        .add("code", code)
                        .build()
                )
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token", authToken)
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    attempt(i + 1)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.close()
                        runOnUiThread {
                            updateLogistics(
                                item.id,
                                status = "4",
                                fieldName = "is_ready_by",
                                newValue = currentMdmCode,
                                isReadyAt = getCurrentDateTime(),
                                executor = currentMdmCode,
                                mdmCode = currentMdmCode,
                                versionName = version_name
                            )
                            dialog.dismiss()
                            Toast.makeText(this@DetailLogisticsActivity, "Код подтвержден, статус обновлен", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val codeResp = response.code
                        response.close()
                        if (codeResp == 429) {
                            Log.w("CheckConfirmationCode", "429 on ${urls[i]}, try fallback")
                            attempt(i + 1)
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@DetailLogisticsActivity, "Неверный код подтверждения", Toast.LENGTH_SHORT).show()
                                Log.d("Код подтверждения", "Неверный код подтверждения")
                                finishItemLoading(item.id)
                            }
                        }
                    }
                }
            })
        }

        attempt(0)
    }
    private fun showStatusUpdateDialog(context: Context, item: LogisticsItem) {
        val statuses: Array<String>
        val statusValues: Array<String>
        val icons: Array<Int>

        // Админ (роль 2) может все статусы в любом порядке
        if (rolesList.contains("2")) {
            statuses = arrayOf("Принята", "Аннулирована", "Выполняется", "На подтверждении", "Выполнена")
            statusValues = arrayOf("1", "-1", "2", "3", "4")
            icons = arrayOf(
                R.drawable.icon_accepted,
                R.drawable.icon_cancelled,
                R.drawable.icon_in_progress,
                R.drawable.icon_pending,
                R.drawable.icon_completed
            )
        } else if (rolesList.contains("64")) {
            // Диспетчер ОВЛ (роль 64) - те же права, что и у администратора для статусов 1, 2, 3, 4
            // + сохраняет право аннулировать заявки со статусами 1, 2, 3
            when (item.status) {
                "0" -> { // Зарегистрирована
                    statuses = arrayOf("Принята", "Аннулирована")
                    statusValues = arrayOf("1", "-1")
                    icons = arrayOf(R.drawable.icon_accepted, R.drawable.icon_cancelled)
                }
                "1" -> { // Принята
                    statuses = arrayOf("Выполняется", "Аннулирована")
                    statusValues = arrayOf("2", "-1")
                    icons = arrayOf(R.drawable.icon_in_progress, R.drawable.icon_cancelled)
                }
                "2" -> { // Выполняется
                    statuses = arrayOf("На подтверждении", "Аннулирована")
                    statusValues = arrayOf("3", "-1")
                    icons = arrayOf(R.drawable.icon_pending, R.drawable.icon_cancelled)
                }
                "3" -> { // На подтверждении
                    statuses = arrayOf("Выполнена", "Аннулирована")
                    statusValues = arrayOf("4", "-1")
                    icons = arrayOf(R.drawable.icon_completed, R.drawable.icon_cancelled)
                }
                "-1" -> { // Аннулирована
                    statuses = arrayOf()
                    statusValues = arrayOf()
                    icons = arrayOf()
                }
                "4" -> { // Выполнена
                    statuses = arrayOf()
                    statusValues = arrayOf()
                    icons = arrayOf()
                }
                else -> {
                    statuses = arrayOf()
                    statusValues = arrayOf()
                    icons = arrayOf()
                }
            }
        } else {
            // Для остальных ролей - последовательная логика
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
                else -> {
                    statuses = arrayOf()
                    statusValues = arrayOf()
                    icons = arrayOf()
                }
            }
        }

        // Проверяем, есть ли доступные статусы для выбора
        if (statuses.isEmpty()) {
            Toast.makeText(context, "Нет доступных действий для этого статуса", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = StatusAdapter(context, statuses, icons)
        val bottomSheetDialog = BottomSheetDialog(context)
        val bottomSheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_layout, null)
        val listView = bottomSheetView.findViewById<ListView>(R.id.status_list_view)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, position, _ ->
            val selectedStatus = statusValues[position]
            val currentDateTime = getCurrentDateTime()
            val holder = recyclerView.findViewHolderForItemId(item.id.hashCode().toLong())
                    as? DetailLogisticsAdapter.DetailLogisticsViewHolder
            when (selectedStatus) {
                "1" -> {
                    holder?.let { adapter.onStatusUpdateStarted(item.id.toInt(), it) }
                    handleStatus1(item, currentDateTime)
                }
                "-1" -> {
                    holder?.let { adapter.onStatusUpdateStarted(item.id.toInt(), it) }
                    handleStatusMinus1(item, currentDateTime)
                }
                "2" -> {
                    holder?.let { adapter.onStatusUpdateStarted(item.id.toInt(), it) }
                    handleStatus2(item, currentDateTime)
                }
                "3" -> {
                    holder?.let { adapter.onStatusUpdateStarted(item.id.toInt(), it) }
                    handleStatus3(item, currentDateTime)
                }
                "4" -> {
                    holder?.let { adapter.onStatusUpdateStarted(item.id.toInt(), it) }
                    handleStatus4(item, currentDateTime)
                }
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

    private fun handleStatus1(item: LogisticsItem, currentDateTime: String) {
        if (item.status == "1") {
            Toast.makeText(this, "Статус уже 'Принята'", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentUsername == "T.Test") {
            Toast.makeText(this, "У вас недостаточно прав для совершения данной операции", Toast.LENGTH_LONG).show()
            return
        }

        // Админ может устанавливать без ограничений
        if (rolesList.contains("2")) {
            updateLogistics(
                item.id,
                status = "1",
                fieldName = "is_accepted_by",
                newValue = currentMdmCode,
                isAcceptedAt = currentDateTime,
                executor = currentMdmCode,
                mdmCode = currentMdmCode,
                versionName = version_name
            )
            return
        }

        if (item.sklad_from_resp_name == currentFio) {
            updateLogistics(
                item.id,
                status = "1",
                fieldName = "is_accepted_by",
                newValue = currentMdmCode,
                isAcceptedAt = currentDateTime,
                executor = currentMdmCode,
                mdmCode = currentMdmCode,
                versionName = version_name
            )
            return
        }

        // Для типа "other" - можно без NFC
        if (type == "other") {
            // Для транспортировщиков (65) и диспетчеров ОВЛ (64)
            if (rolesList.contains("65") || rolesList.contains("64")) {
                updateLogistics(
                    item.id,
                    status = "1",
                    fieldName = "is_accepted_by",
                    newValue = currentMdmCode,
                    isAcceptedAt = currentDateTime,
                    executor = currentMdmCode,
                    mdmCode = currentMdmCode,
                    versionName = version_name
                )
            } else {
                Toast.makeText(this, "Недостаточно прав для принятия заявки", Toast.LENGTH_LONG).show()
                finishItemLoading(item.id)
            }
        } else {
            // Для других типов - через NFC
            showNfcDialog("Сканируйте NFC метку для подтверждения принятия заявки", true)
        }
    }
    private fun handleStatusMinus1(item: LogisticsItem, currentDateTime: String) {
        if (item.status == "-1") {
            Toast.makeText(this, "Статус уже 'Аннулирована'", Toast.LENGTH_SHORT).show()
            return
        }

        // Админ может всегда
        if (rolesList.contains("2")) {
            updateLogistics(
                item.id,
                status = "-1",
                fieldName = "is_decline_by",
                newValue = currentMdmCode,
                isDeclineAt = currentDateTime,
                executor = currentMdmCode,
                mdmCode = currentMdmCode,
                versionName = version_name
            )
            return
        }

        // Только диспетчеры ОВЛ (роль 64)
        if (rolesList.contains("64")) {
            updateLogistics(
                item.id,
                status = "-1",
                fieldName = "is_decline_by",
                newValue = currentMdmCode,
                isDeclineAt = currentDateTime,
                executor = currentMdmCode,
                mdmCode = currentMdmCode,
                versionName = version_name
            )
        } else {
            Toast.makeText(this, "Недостаточно прав для аннулирования заявки", Toast.LENGTH_LONG).show()
            finishItemLoading(item.id)
        }
    }
    private fun handleStatus2(item: LogisticsItem, currentDateTime: String) {
        if (item.status == "2") {
            Toast.makeText(this, "Статус уже 'Выполняется'", Toast.LENGTH_SHORT).show()
            return
        }

        // Админ может всегда
        if (rolesList.contains("2")) {
            updateLogistics(
                item.id,
                status = "2",
                fieldName = "is_doing_by",
                newValue = currentMdmCode,
                isDoingAt = currentDateTime,
                executor = currentMdmCode,
                mdmCode = currentMdmCode,
                versionName = version_name
            )
            return
        }

        // Транспортировщики (65) и диспетчеры ОВЛ (64)
        if (rolesList.contains("65") || rolesList.contains("64")) {
            updateLogistics(
                item.id,
                status = "2",
                fieldName = "is_doing_by",
                newValue = currentMdmCode,
                isDoingAt = currentDateTime,
                executor = currentMdmCode,
                mdmCode = currentMdmCode,
                versionName = version_name
            )
        } else {
            Toast.makeText(this, "Недостаточно прав для установки статуса", Toast.LENGTH_LONG).show()
            finishItemLoading(item.id)
        }
    }

    private fun handleStatus3(item: LogisticsItem, currentDateTime: String) {
        if (item.status == "3") {
            Toast.makeText(this, "Статус уже 'На подтверждении'", Toast.LENGTH_SHORT).show()
            return
        }

        // Админ может всегда
        if (rolesList.contains("2")) {
            updateLogistics(
                item.id,
                status = "3",
                fieldName = "is_check_by",
                newValue = currentMdmCode,
                isCheckAt = currentDateTime,
                executor = currentMdmCode,
                mdmCode = currentMdmCode,
                versionName = version_name
            )
            return
        }

        // Транспортировщики (65) и диспетчеры ОВЛ (64)
        if (rolesList.contains("65") || rolesList.contains("64")) {
            updateLogistics(
                item.id,
                status = "3",
                fieldName = "is_check_by",
                newValue = currentMdmCode,
                isCheckAt = currentDateTime,
                executor = currentMdmCode,
                mdmCode = currentMdmCode,
                versionName = version_name
            )
        } else {
            Toast.makeText(this, "Недостаточно прав для установки статуса", Toast.LENGTH_LONG).show()
            finishItemLoading(item.id)
        }
    }
    private fun handleStatus4(item: LogisticsItem, currentDateTime: String) {
        if (item.status == "4") {
            Toast.makeText(this, "Статус уже 'Выполнена'", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверяем наличие фото (обязательное условие)
        if (!hasPhotoLocal(item.id)) {
            Toast.makeText(this, "Необходимо добавить фото перед установкой статуса «Выполнена».", Toast.LENGTH_LONG).show()
            finishItemLoading(item.id)
            return
        }

        // Админ может закрывать автоматически
        if (rolesList.contains("2")) {
            updateLogistics(
                item.id,
                status = "4",
                fieldName = "is_ready_by",
                newValue = currentMdmCode,
                isReadyAt = currentDateTime,
                executor = currentMdmCode,
                mdmCode = currentMdmCode,
                versionName = version_name
            )
        } else {
            // Все остальные - только через NFC или код
            showConfirmationMethodDialog(item)
        }
    }
    private fun isNfcScanned(): Boolean {
        return isNfcScanned
    }
    private var isUpdating = false

    private fun updateLogistics(
        logisticsId: String?,
        status: String? = null,
        fieldName: String? = null,
        newValue: String? = null,
        isDoingAt: String? = null,
        isAcceptedAt: String? = null,
        isDeclineAt: String? = null,
        isCheckAt: String? = null,
        isReadyAt: String? = null,
        executor: String? = null,
        mdmCode: String? = currentMdmCode,
        versionName: String = version_name,
        skipPhotoCheck: Boolean = false
    ) {
        // Недопускаем параллельные апдейты и пустой id
        if (logisticsId == null || isUpdating) return

        // Единый гейт: статус "Выполнена" (4) запрещён без фото
        if (!skipPhotoCheck && status == "4") {
            if (!hasPhotoLocal(logisticsId)) {
                CoroutineScope(Dispatchers.IO).launch {
                    val fresh = fetchLogisticsSync(logisticsId)
                    val hasRemotePhoto = fresh?.hasPhotos() == true
                    withContext(Dispatchers.Main) {
                        if (hasRemotePhoto) {
                            updateLogistics(
                                logisticsId = logisticsId,
                                status = status,
                                fieldName = fieldName,
                                newValue = newValue,
                                isDoingAt = isDoingAt,
                                isAcceptedAt = isAcceptedAt,
                                isDeclineAt = isDeclineAt,
                                isCheckAt = isCheckAt,
                                isReadyAt = isReadyAt,
                                executor = executor,
                                mdmCode = mdmCode,
                                versionName = versionName,
                                skipPhotoCheck = true
                            )
                        } else {
                            // фото нет ни локально, ни на сервере — запрещаем и снимаем лоадер
                            Toast.makeText(
                                this@DetailLogisticsActivity,
                                "Необходимо добавить фото перед установкой статуса «Выполнена».",
                                Toast.LENGTH_LONG
                            ).show()
                            logisticsId?.let { finishItemLoading(it) }
                        }
                    }
                }
                return
            }
        }


        isUpdating = true
        // показываем спиннер ТОЛЬКО у этой строки
        startItemLoading(logisticsId)

        val urls = listOf(
            "https://api.gkmmz.ru/api/update_logistic_status/$logisticsId",
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/update_logistic_status/$logisticsId"
        )

        val client = (application as App).okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        fun buildBody(): FormBody {
            val b = FormBody.Builder()
                .add("mdm_code", mdmCode ?: "")
                .add("version_name", versionName)
            status?.let { b.add("status", it) }
            fieldName?.let { fn -> newValue?.let { v -> b.add(fn, v) } }
            isDoingAt?.let   { b.add("is_doing_at", it) }
            isAcceptedAt?.let{ b.add("is_accepted_at", it) }
            isDeclineAt?.let { b.add("is_decline_at", it) }
            isCheckAt?.let   { b.add("is_check_at", it) }
            isReadyAt?.let   { b.add("is_ready_at", it) }
            executor?.let    { b.add("executor", it) }
            return b.build()
        }

        fun finishWithError(msg: String) {
            runOnUiThread {
                finishItemLoading(logisticsId)
                Toast.makeText(this@DetailLogisticsActivity, msg, Toast.LENGTH_SHORT).show()
            }
        }

        fun tryUpdate(i: Int) {
            if (i >= urls.size) {
                finishWithError("Ошибка обновления статуса. Не удалось подключиться к серверам.")
                return
            }

            val req = Request.Builder()
                .url(urls[i])
                .post(buildBody()) // новое тело на каждую попытку
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token", authToken)
                .build()

            client.newCall(req).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // Проверим, не применился ли статус на сервере
                    verifyStatusAfterWrite(logisticsId, status) { ok, fresh ->
                        if (ok && fresh != null) {
                            finishItemLoading(logisticsId)
                            applyServerItem(fresh)
                            Toast.makeText(this@DetailLogisticsActivity, "Статус изменён", Toast.LENGTH_SHORT).show()
                        } else {
                            // если нет — пробуем следующий URL
                            tryUpdate(i + 1)
                        }
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use { r ->
                        if (!r.isSuccessful) {
                            if (r.code == 429) {
                                tryUpdate(i + 1)
                                return
                            }

                            if (shouldVerify(r.code)) {
                                // 502/503/504 — проверяем фактическое состояние
                                verifyStatusAfterWrite(logisticsId, status) { ok, fresh ->
                                    finishItemLoading(logisticsId)
                                    if (ok && fresh != null) {
                                        applyServerItem(fresh)
                                        Toast.makeText(this@DetailLogisticsActivity, "Статус изменён", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(this@DetailLogisticsActivity, "Ошибка сервера: ${r.code}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                finishWithError("Ошибка сервера: ${r.code}")
                            }
                            return
                        }

                        // успех
                        runOnUiThread {
                            // снимаем лоадер строки
                            finishItemLoading(logisticsId)

                            // обновляем локальную модель
                            val idx = logisticsItems.indexOfFirst { it.id == logisticsId }
                            if (idx != -1) {
                                status?.let { logisticsItems[idx].status = it }
                                if (fieldName != null && newValue != null) {
                                    when (fieldName) {
                                        "is_doing_by"    -> logisticsItems[idx].is_doing_by = newValue
                                        "is_accepted_by" -> logisticsItems[idx].is_accepted_by = newValue
                                        "is_decline_by"  -> logisticsItems[idx].is_decline_by = newValue
                                        "is_check_by"    -> logisticsItems[idx].is_check_by = newValue
                                        "is_ready_by"    -> logisticsItems[idx].is_ready_by = newValue
                                    }
                                }
                                isDoingAt?.let   { logisticsItems[idx].is_doing_at   = it }
                                isAcceptedAt?.let{ logisticsItems[idx].is_accepted_at = it }
                                isDeclineAt?.let { logisticsItems[idx].is_decline_at  = it }
                                isCheckAt?.let   { logisticsItems[idx].is_check_at    = it }
                                isReadyAt?.let   { logisticsItems[idx].is_ready_at    = it }
                                executor?.let    { logisticsItems[idx].executor       = it }

                                adapter.notifyItemChanged(idx)

                                val statusText = when (status) {
                                    "1"  -> "Принята"
                                    "-1" -> "Аннулирована"
                                    "2"  -> "Выполняется"
                                    "3"  -> "На подтверждении"
                                    "4"  -> "Выполнена"
                                    else -> null
                                }
                                statusText?.let {
                                    Toast.makeText(this@DetailLogisticsActivity, "Статус изменен на: $it", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }
            })
        }

        tryUpdate(0)
    }



    private fun startItemLoading(logisticsId: String) {
        val idx = logisticsItems.indexOfFirst { it.id == logisticsId }
        if (idx != -1) {
            adapter.statusLoadingStates[logisticsId] = true
            adapter.notifyItemChanged(idx)
        }
    }

    private fun finishItemLoading(logisticsId: String) {
        isUpdating = false
        adapter.resetLoadingState(logisticsId) // сам найдёт позицию и обновит
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
    // где-то в репозитории/утилитах (есть Context)
    suspend fun getAllSotrudnikiInfo(context: Context): List<SotrudnikiInfo> = withContext(Dispatchers.IO) {
        val urls = listOf(
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/get_all_sotrudniki",
            "https://api.gkmmz.ru/api/get_all_sotrudniki"
        )

        val client = (context.applicationContext as App).okHttpClient.newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val result = mutableListOf<SotrudnikiInfo>()
        var success = false

        for (url in urls) {
            val req = Request.Builder()
                .url(url)
                .addHeader("X-Apig-AppCode", authTokenAPI)
                .addHeader("X-Auth-Token", authToken)
                .build()

            val resp = try { client.newCall(req).execute() } catch (e: Exception) {
                Log.e("GetAllSotrudnikiInfo", "call failed for $url", e)
                null
            } ?: continue

            try {
                if (!resp.isSuccessful) {
                    if (resp.code == 429) {
                        Log.w("GetAllSotrudnikiInfo", "429 on $url, trying fallback…")
                        continue
                    }
                    Log.e("GetAllSotrudnikiInfo", "HTTP ${resp.code} - ${resp.message}")
                    continue
                }
                val body = resp.body?.string().orEmpty()
                val json = JSONObject(body)
                for (key in json.keys()) {
                    val obj = json.getJSONObject(key)
                    val mdmcode = obj.getString("mdmcode")
                    val fio = obj.getString("Рабочий")
                    result.add(SotrudnikiInfo(mdmcode, fio))
                }
                success = true
                break
            } catch (t: Throwable) {
                Log.e("GetAllSotrudnikiInfo", "parse error", t)
            } finally {
                resp.close()
            }
        }

        if (!success) throw Exception("Не удалось получить данные сотрудников")
        result
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (currentUsername == "T.Test") {
            Toast.makeText(this, "У вас недостаточно прав для совершения данной операции", Toast.LENGTH_LONG).show()
        }
        else {
            if (requestCode == REQUEST_CAMERA && resultCode == Activity.RESULT_OK) {
                val photoFile = currentPhotoFile
                photoFile?.let {
                    val uri = Uri.fromFile(it)
                    uploadPhotoToServer(uri, currentLogisticsId, this)
                }
            } else if ((requestCode == REQUEST_GALLERY || requestCode == pickImageRequestCode) && resultCode == Activity.RESULT_OK) {
                data?.data?.let { uri ->
                    uploadPhotoToServer(uri, currentLogisticsId, this)
                }
            }
        }
    }
    // загрузка одного фото
    // загрузка одного фото
    private fun uploadPhotoToServer(uri: Uri, logisticsId: String?, context: Context) {
        if (logisticsId == null) return

        // Готовим имя файла и байты
        val (fileName, fileBytes) = run {
            val filePath = getRealPathFromURI(context, uri)
            if (filePath != null) {
                val f = File(filePath)
                if (f.exists()) {
                    f.name to f.readBytes()
                } else {
                    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    val bytes = FileInputStream(pfd?.fileDescriptor).use { it.readBytes() }
                    (f.name.ifEmpty { "photo.jpg" }) to bytes
                }
            } else {
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                val bytes = FileInputStream(pfd?.fileDescriptor).use { it.readBytes() }
                "photo.jpg" to bytes
            }
        }

        // диалог прогресса
        fun createPhotoProgressDialog(ctx: Context, msg: String): AlertDialog {
            val v = LayoutInflater.from(ctx).inflate(R.layout.photo_progress_dialog, null)
            v.findViewById<TextView>(R.id.message).text = msg
            return AlertDialog.Builder(ctx)
                .setView(v)
                .setCancelable(false)
                .create()
        }

        val urls = listOf(
            "https://api.gkmmz.ru/api/upload_files/$logisticsId",
            "https://09f2befcf01d4dd39cbe7e54717e28af.apicapis.ru-moscow-1.hc.sbercloud.ru/api/upload_files/$logisticsId"
        )

        CoroutineScope(Dispatchers.IO).launch {
            val client = (application as App).okHttpClient.newBuilder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            var idx = 0
            var success = false
            var retry = 0
            val maxRetries = 3

            // ВАЖНО: создавать и показывать диалог только на Main
            var progressDialog: AlertDialog? = null
            withContext(Dispatchers.Main) {
                val act = this@DetailLogisticsActivity
                if (!act.isFinishing && !(Build.VERSION.SDK_INT >= 17 && act.isDestroyed)) {
                    progressDialog = createPhotoProgressDialog(act, "Загрузка фото…")
                    progressDialog?.show()
                }
            }

            try {
                while (!success && idx < urls.size) {
                    val imageBody = RequestBody.create("image/*".toMediaType(), fileBytes)
                    val multipart = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("files[]", fileName, imageBody)
                        .addFormDataPart("created_by", currentMdmCode ?: "")
                        .addFormDataPart("type", "logistics")
                        .build()

                    val req = Request.Builder()
                        .url(urls[idx])
                        .post(multipart)
                        .addHeader("X-Apig-AppCode", authTokenAPI)
                        .addHeader("X-Auth-Token", authToken)
                        .build()

                    Log.d("Upload", "Запрос: ${urls[idx]} (попытка ${retry + 1})")

                    val resp: Response? = try {
                        client.newCall(req).execute()
                    } catch (e: Exception) {
                        Log.e("Upload", "call failed for ${urls[idx]}", e)
                        null
                    }

                    if (resp == null) {
                        idx++; retry = 0
                        continue
                    }

                    var needBreak = false
                    try {
                        when {
                            resp.isSuccessful -> {
                                Log.d("Upload", "Фотография успешно загружена")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Фотография успешно загружена", Toast.LENGTH_SHORT).show()
                                    fetchLogisticsDetail(logisticsId)
                                }
                                success = true
                            }
                            resp.code == 429 -> {
                                Log.w("Upload", "429 на ${urls[idx]}")
                                if (idx < urls.size - 1) {
                                    idx++; retry = 0
                                } else {
                                    if (retry < maxRetries - 1) {
                                        retry++
                                        delay(1000L * retry)
                                    } else {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Сервер перегружен, попробуйте позже", Toast.LENGTH_SHORT).show()
                                        }
                                        needBreak = true
                                    }
                                }
                            }
                            else -> {
                                Log.e("Upload", "Ошибка загрузки: ${resp.code} - ${resp.message}")
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "Ошибка загрузки фотографии: ${resp.message}", Toast.LENGTH_SHORT).show()
                                }
                                idx++; retry = 0
                            }
                        }
                    } finally {
                        resp.close()
                    }

                    if (needBreak) break
                    if (!success && idx >= urls.size && retry in 1 until maxRetries) {
                        delay(1000L * retry)
                        idx = urls.lastIndex
                    }
                }

                if (!success && idx >= urls.size) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Не удалось загрузить фотографию ни на один сервер", Toast.LENGTH_SHORT).show()
                    }
                }
            } finally {
                withContext(Dispatchers.Main) {
                    progressDialog?.let { if (it.isShowing) it.dismiss() }
                }
            }
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
            REQUEST_CODE_GALLERY_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Разрешение предоставлено, можно открыть галерею
                    currentLogisticsId?.let { adapter.notifyGalleryPermissionGranted(this, it) }
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            REQUEST_NFC_PERMISSION -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Log.d(tag, "NFC permission granted")
                    setupNfcAdapter()
                } else {
                    Log.d(tag, "NFC permission denied")
                    Toast.makeText(this, "Разрешение на использование NFC отклонено", Toast.LENGTH_SHORT).show()
                }
                return
            }
            REQUEST_CALL_PHONE -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // Разрешение предоставлено, можно совершать звонок
                } else {
                    Toast.makeText(this, "Разрешение на звонок отклонено", Toast.LENGTH_SHORT).show()
                }
                return
            }
            REQUEST_CODE_PERMISSIONS -> {
                for (i in permissions.indices) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        Log.d("MainActivity", "Разрешение предоставлено: ${permissions[i]}")
                    } else {
                        Log.d("MainActivity", "Разрешение отклонено: ${permissions[i]}")
                    }
                }
                return
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
            Toast.makeText(this, "Для работы с NFC необходимо разрешение", Toast.LENGTH_SHORT).show()
        }
        ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.NFC), REQUEST_NFC_PERMISSION)
    }
    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add( android.Manifest.permission.CAMERA)
        }
        if (ContextCompat.checkSelfPermission(this,  android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add( android.Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (ContextCompat.checkSelfPermission(this,  android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add( android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,  android.Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add( android.Manifest.permission.READ_MEDIA_IMAGES)
            }
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }
    private val MY_READ_EXTERNAL_REQUEST = 1
    private val REQUEST_CAMERA_PERMISSION = 2
    private val pickImageRequestCode = 1001
    private val REQUEST_CODE_GALLERY_PERMISSION = 1003
    private val REQUEST_CALL_PHONE = 1004
    private val REQUEST_CODE_PERMISSIONS = 1005
}