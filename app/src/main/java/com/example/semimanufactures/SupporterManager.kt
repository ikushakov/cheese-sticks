package com.example.semimanufactures

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.KeyEvent

class SupporterManager(context: Context, listener: IScanListener) {
    private var scannerManager: SunmiScannerManager? = null

    enum class ScannerSupporter {
        SUNMI
    }

    private val supporter: ScannerSupporter
        get() {
            for (supporter in ScannerSupporter.values()) {
                if (supporter.name == Build.MODEL || supporter.name == Build.MANUFACTURER) {
                    return supporter
                }
            }
            return ScannerSupporter.SUNMI
        }

    init {
        val scannerSupporter = supporter
        Log.d("SupporterManager", "Initialized with supporter: $scannerSupporter")
        scannerManager = SunmiScannerManager.getInstance(context)
        scannerManager?.setScannerListener(listener)
        scannerManager?.init()
    }

    fun recycle() {
        Log.d("SupporterManager", "Recycling scanner manager.")
        scannerManager?.recycle()
    }

    fun setScannerListener(listener: IScanListener) {
        Log.d("SupporterManager", "Setting new scanner listener.")
        scannerManager?.setScannerListener(listener)
    }

    fun sendKeyEvent(key: KeyEvent?) {
        Log.d("SupporterManager", "Sending key event: $key")
        scannerManager?.sendKeyEvent(key)
    }

    val scannerModel: Int?
        get() {
            val model = scannerManager?.scannerModel
            Log.d("SupporterManager", "Getting scanner model: $model")
            return model
        }

    fun scannerEnable(enable: Boolean?) {
        Log.d("SupporterManager", "Setting scanner enable to: $enable")
        scannerManager?.scannerEnable(enable!!)
    }

    fun setScanMode(mode: String?) {
        Log.d("SupporterManager", "Setting scan mode to: $mode")
        scannerManager?.setScanMode(mode)
    }

    fun setDataTransferType(type: String?) {
        Log.d("SupporterManager", "Setting data transfer type to: $type")
        scannerManager?.setDataTransferType(type)
    }

    fun singleScan(bool: Boolean?) {
        Log.d("SupporterManager", "Setting single scan to: $bool")
        scannerManager?.singleScan(bool!!)
    }

    fun continuousScan(bool: Boolean?) {
        Log.d("SupporterManager", "Setting continuous scan to: $bool")
        scannerManager?.continuousScan(bool!!)
    }

    interface IScanListener {
        fun onScannerResultChange(result: String?)
        fun onScannerServiceConnected()
        fun onScannerServiceDisconnected()
        fun onScannerInitFail()
    }
}