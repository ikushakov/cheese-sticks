package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException

class TgAuthorizationActivity : AppCompatActivity() {
    private lateinit var codeOne: EditText
    private lateinit var codeTwo: EditText
    private lateinit var codeThree: EditText
    private lateinit var codeFour: EditText
    private lateinit var codeFive: EditText
    private lateinit var the_button_for_check_code: Button
    private lateinit var button_back: Button
    private lateinit var go_to_logistic: ImageView
    private var username: String = ""
    private val client = OkHttpClient()
//    private lateinit var go_to_authorization: ImageView
    private lateinit var go_to_search_and_add: ImageView
    private lateinit var go_to_issue: ImageView
    private lateinit var go_to_users_settings: ImageView
    private lateinit var go_to_send_notification: ImageView
    private val rolesList: MutableList<String> = mutableListOf()
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tg_authorization)
        supportActionBar?.hide()
        button_back = findViewById(R.id.button_back)
        button_back.setOnClickListener {
            finish()
        }
        codeOne = findViewById(R.id.codeOne)
        codeTwo = findViewById(R.id.codeTwo)
        codeThree = findViewById(R.id.codeThree)
        codeFour = findViewById(R.id.codeFour)
        codeFive = findViewById(R.id.codeFive)
        the_button_for_check_code = findViewById(R.id.the_button_for_check_code)
        go_to_logistic = findViewById(R.id.go_to_logistic)
        go_to_logistic.setOnClickListener {
            showPopupMenu(it)
        }
        username = intent.getStringExtra("username").toString()
        setupCodeInputListeners()
        the_button_for_check_code.setOnClickListener {
            sendAuthCode()
        }
        codeFive.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE ||
                (event != null && event.keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_DOWN)) {
                sendAuthCode()
                true
            } else {
                false
            }
        }
//        go_to_authorization = findViewById(R.id.go_to_authorization)
//        go_to_authorization.setOnClickListener {
//            Toast.makeText(this, "Вы находитесь в окне авторизации", Toast.LENGTH_LONG).show()
//        }
        go_to_search_and_add = findViewById(R.id.go_to_search_and_add)
        go_to_issue = findViewById(R.id.go_to_issue)
        go_to_users_settings = findViewById(R.id.go_to_users_settings)
        go_to_send_notification = findViewById(R.id.go_to_send_notification)
        go_to_search_and_add.setOnClickListener {
            Toast.makeText(this, "Для начала работы авторизируйтесь", Toast.LENGTH_LONG).show()
        }
        go_to_users_settings.setOnClickListener {
            Toast.makeText(this, "Для начала работы авторизируйтесь", Toast.LENGTH_LONG).show()
        }
        go_to_issue.setOnClickListener {
            Toast.makeText(this, "Для начала работы авторизируйтесь", Toast.LENGTH_LONG).show()
        }
        go_to_send_notification.setOnClickListener {
            Toast.makeText(this, "Появится позже\uD83D\uDE01)))", Toast.LENGTH_LONG).show()
        }
    }
    private fun setupCodeInputListeners() {
        val editTexts = arrayOf(codeOne, codeTwo, codeThree, codeFour, codeFive)
        for (i in editTexts.indices) {
            editTexts[i].addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {
                    if (s.toString().length == 1) {
                        if (i < editTexts.size - 1) {
                            editTexts[i + 1].requestFocus()
                        }
                    } else if (s.toString().isEmpty() && i > 0) {
                        editTexts[i - 1].requestFocus()
                    }
                }
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            })
        }
    }
    private fun sendAuthCode() {
        val code = "${codeOne.text}${codeTwo.text}${codeThree.text}${codeFour.text}${codeFive.text}"
        if (code.length == 5) {
            CoroutineScope(Dispatchers.IO).launch {
                val url = "http://192.168.200.250/api/check_auth_code"
                val formBody = FormBody.Builder()
                    .add("login", username)
                    .add("code", code)
                    .build()
                val request = Request.Builder()
                    .url(url)
                    .post(formBody)
                    .build()
                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        response.body?.string()?.let { responseData ->
                            try {
                                val jsonArray = JSONArray(responseData)
                                if (jsonArray.length() > 0) {
                                    val jsonObject = jsonArray.getJSONObject(0)
                                    val userId = jsonObject.getString("id") ?: ""
                                    val roleId = jsonObject.getString("role_id") ?: ""
                                    val mdmCode = jsonObject.getString("sotrudnik_mdm_code") ?: ""
                                    val fio = jsonObject.getString("fio") ?: ""
                                    val rolesString = intent.getStringExtra("rolesString") ?: ""
                                    rolesList.addAll(rolesString.split(",").map { it.trim() })
                                    rolesList.forEach { role ->
                                        Log.d("Список ролей", "Роль: $role")
                                    }
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(this@TgAuthorizationActivity, "Вы успешно авторизовались", Toast.LENGTH_LONG).show()
                                        startActivity(Intent(this@TgAuthorizationActivity, FeaturesOfTheFunctionalityActivity::class.java).apply {
                                            putExtra("username", username)
                                            putExtra("userId", userId.toIntOrNull() ?: 0)
                                            putExtra("roleCheck", roleId.toIntOrNull() ?: 0)
                                            putExtra("mdmCode", mdmCode)
                                            putExtra("fio", fio)
                                            putExtra("deviceInfo", Build.MODEL)
                                            putExtra("rolesString", rolesString)
                                        })
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(
                                            this@TgAuthorizationActivity,
                                            "Нет данных для авторизации",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            } catch (e: JSONException) {
                                e.printStackTrace();
                            }
                        }
                        return@launch
                    } else { throw IOException("") }
                } catch (e: IOException) { Log.e("", "", e); }
            }
        } else { Log.e("", "Invalid code length"); }
    }
    @SuppressLint("ResourceType")
    private fun showPopupMenu(view: View) {
        val popupView = layoutInflater.inflate(R.layout.info_popup_menu, null)
        val popupWindow = PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        popupWindow.elevation = 10f
        val popupTextView: TextView = popupView.findViewById(R.id.popup_item_text)
        popupTextView.text = getString(R.string.info_about_app)
        popupTextView.setOnClickListener {
            popupWindow.dismiss()
        }
        popupWindow.isFocusable = true
        popupWindow.showAsDropDown(view)
    }
}