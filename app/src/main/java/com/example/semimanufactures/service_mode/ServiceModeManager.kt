package com.example.semimanufactures.service_mode

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.annotation.RequiresApi
import java.lang.ref.WeakReference
import java.time.Instant

data class ServiceModeState(val active: Boolean, val until: Instant?)

class ServiceModeManager(private val app: Application) {

    private val prefs: SharedPreferences =
        app.getSharedPreferences("service_mode_prefs", Context.MODE_PRIVATE)

    @RequiresApi(Build.VERSION_CODES.O)
    @Volatile private var state: ServiceModeState = loadFromPrefs()
    @Volatile private var currentActivityRef: WeakReference<Activity?> = WeakReference(null)
    @Volatile private var screenVisible: Boolean = false

    @RequiresApi(Build.VERSION_CODES.O)
    fun getState(): ServiceModeState = state

    fun setCurrentActivity(activity: Activity?) {
        currentActivityRef = WeakReference(activity)
    }

    fun markScreenVisible(visible: Boolean) {
        screenVisible = visible
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun isExpired(now: Instant = Instant.now()): Boolean =
        state.until?.isBefore(now) == true

    @RequiresApi(Build.VERSION_CODES.O)
    private fun clearActive(reason: String) {
        android.util.Log.e("SM-MGR", "clearActive: $reason")
        state = ServiceModeState(false, null)
        saveToPrefs(state)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun update(active: Boolean, until: Instant?) {
        android.util.Log.e("SM-MGR", "update active=$active until=$until")
        val newState = if (active && until?.isBefore(Instant.now()) == true)
            ServiceModeState(false, null) else ServiceModeState(active, until)
        if (state == newState) return
        state = newState
        saveToPrefs(state)
        if (state.active) maybeOpenScreen()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun ensureShownIfNeeded() {
        if (state.active && isExpired()) {
            clearActive("expired by until")
            return
        }
        if (state.active) maybeOpenScreen()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun maybeOpenScreen() {
        if (screenVisible || isExpired()) return
        val act = currentActivityRef.get()
        if (act is ServiceModeActivity) {
            screenVisible = true
            return
        }

        val ctx = act ?: app
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            val intent = Intent(ctx, ServiceModeActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
                if (ctx === app) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try { ctx.startActivity(intent) }
            catch (t: Throwable) { android.util.Log.e("SM-MGR", "startActivity failed", t) }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadFromPrefs(): ServiceModeState {
        val active = prefs.getBoolean("active", false)
        val untilStr = prefs.getString("until", null)
        val until = try { untilStr?.let { Instant.parse(it) } } catch (_: Exception) { null }
        return ServiceModeState(active, until)
    }

    private fun saveToPrefs(s: ServiceModeState) {
        prefs.edit()
            .putBoolean("active", s.active)
            .putString("until", s.until?.toString())
            .apply()
    }
}