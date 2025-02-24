package com.example.semimanufactures

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.TextView
import androidx.annotation.RequiresApi
import device.common.DecodeResult
import device.common.ScanConst
import device.sdk.ScanManager

class PM84ScannerManager private constructor(private val context: Context) {
    private var mScanner: ScanManager? = null
    private var mDecodeResult: DecodeResult? = null
    private var mScanResultReceiver: ScanResultReceiver? = null
    private var textResultScan: TextView? = null

    init {
        mScanner = ScanManager()
        mDecodeResult = DecodeResult()
        mScanResultReceiver = ScanResultReceiver(context)
    }

    companion object {
        @Volatile
        private var instance: PM84ScannerManager? = null

        fun getInstance(context: Context): PM84ScannerManager {
            synchronized(this) {
                return instance ?: PM84ScannerManager(context.applicationContext).also { instance = it }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun registerScannerReceiver() {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ScanConst.INTENT_USERMSG)
        intentFilter.addAction(ScanConst.INTENT_EVENT)
        context.registerReceiver(mScanResultReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED)
    }

    fun unregisterScannerReceiver() {
        context.unregisterReceiver(mScanResultReceiver)
    }

    fun startScanning(textResultScan: TextView) {
        this.textResultScan = textResultScan
        mScanner?.aDecodeSetTriggerMode(ScanConst.TriggerMode.DCD_TRIGGER_MODE_ONESHOT)
        mScanner?.aDecodeSetTriggerOn(1)
    }

    inner class ScanResultReceiver(private val context: Context) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (mScanner != null) {
                try {
                    if (ScanConst.INTENT_USERMSG == intent.action) {
                        mScanner?.aDecodeGetResult(mDecodeResult?.recycle())
                        textResultScan?.text = mDecodeResult?.toString()
                    } else if (ScanConst.INTENT_EVENT == intent.action) {
                        val decodeBytesValue = intent.getByteArrayExtra(ScanConst.EXTRA_EVENT_DECODE_VALUE)
                        val decodeBytesLength = intent.getIntExtra(ScanConst.EXTRA_EVENT_DECODE_LENGTH, 0)
                        if (decodeBytesValue != null && decodeBytesLength > 0) {
                            val decodeValue = String(decodeBytesValue, 0, decodeBytesLength)
                            textResultScan?.text = decodeValue
                            // Вызовите метод обратного вызова здесь
                            onScanResultReceived(decodeValue)
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Добавьте более детальную обработку исключений
                }
            }
        }
    }

    interface OnScanResultListener {
        fun onScanResultReceived(result: String)
    }

    private var onScanResultListener: OnScanResultListener? = null

    fun setOnScanResultListener(listener: OnScanResultListener) {
        onScanResultListener = listener
    }

    private fun onScanResultReceived(result: String) {
        onScanResultListener?.onScanResultReceived(result)
    }
}

