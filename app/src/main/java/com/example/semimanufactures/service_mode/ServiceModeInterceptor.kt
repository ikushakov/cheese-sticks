package com.example.semimanufactures.service_mode

import android.os.Build
import androidx.annotation.RequiresApi
import okhttp3.Interceptor
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ServiceModeException(val until: Instant?) : IOException("Service mode active")

class ServiceModeInterceptor(
    private val manager: ServiceModeManager
) : Interceptor {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun intercept(chain: Interceptor.Chain): Response {
        val req = chain.request()
        val resp = chain.proceed(req)
        if (resp.code == 503) {
            manager.update(active = true, until = null)
            resp.close()
            throw ServiceModeException(null)
        }
        val bodyStr = resp.peekBody(1024 * 1024).string()
        try {
            val json = JSONObject(bodyStr)
            if (json.has("service_mode")) {
                val active = isServiceModeActive(json.opt("service_mode"))
                if (active) {
                    val until = json.optString("service_time", null)?.let { parseServiceTime(it) }
                    manager.update(active = true, until = until)
                    resp.close()
                    throw ServiceModeException(until)
                } else {
                    manager.update(active = false, until = null)
                }
            } else {
                manager.update(active = false, until = null)
            }
        } catch (_: JSONException) {
            manager.update(active = false, until = null)
        }
        return resp
    }

    private fun isServiceModeActive(raw: Any?): Boolean = when (raw) {
        is Boolean -> raw
        is Number  -> raw.toInt() != 0
        is String  -> raw.equals("true", true) || raw == "1" || raw.equals("yes", true)
        else       -> false
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseServiceTime(raw: String): Instant? {
        val s = raw.trim().replace('T', ' ')
        val zone = ZoneId.systemDefault()
        val patterns = listOf(
            "dd.MM.yyyy HH:mm",
            "dd.MM.yyyy HH:mm:ss",
            "d.M.yyyy H:mm",
            "d.M.yyyy H:mm:ss"
        )
        for (p in patterns) {
            try {
                val fmt = DateTimeFormatter.ofPattern(p)
                return LocalDateTime.parse(s, fmt).atZone(zone).toInstant()
            } catch (_: Exception) {}
        }
        return try { Instant.parse(raw.trim()) } catch (_: Exception) { null }
    }
}