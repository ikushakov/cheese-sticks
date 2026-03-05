package com.example.semimanufactures.service_mode

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import com.example.semimanufactures.R
import com.example.semimanufactures.App
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ServiceModeActivity : ComponentActivity() {

    private val manager by lazy { (application as App).serviceModeManager }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("MissingInflatedId", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_service_mode)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)
        val tvSubtitle = findViewById<TextView>(R.id.tvSubtitle)
        val tvUntil = findViewById<TextView>(R.id.tvUntil)
        val btnRetry = findViewById<Button>(R.id.btnRetry)
        tvTitle.text = """
            Проведение
            Технических работ
            """.trimIndent()

        tvSubtitle.text = """
            Приносим извинения
            за временные неудобства
            - скоро все будет как прежде
        """.trimIndent()
        val state = manager.getState()
        tvUntil.text = state.until?.let {
            val fmt = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
                .withZone(ZoneId.systemDefault())
            "Окончание: ${fmt.format(it)}"
        } ?: "Окончание: неизвестно"
        btnRetry.setOnClickListener { finish() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onStart() {
        super.onStart()
        manager.markScreenVisible(true)

        val st = manager.getState()
        if (!st.active || (st.until?.isBefore(Instant.now()) == true)) {
            finish()
        }
    }

    override fun onStop() {
        manager.markScreenVisible(false)
        super.onStop()
    }
}