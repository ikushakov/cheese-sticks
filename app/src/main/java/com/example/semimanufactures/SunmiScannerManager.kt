package com.example.semimanufactures

import android.app.Service
import android.content.*
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.view.KeyEvent

class SunmiScannerManager private constructor(private val activity: Context) : IScannerManager {
    private val handler = Handler(Looper.getMainLooper())
    private var serviceIntent: Intent? = null
    private var listener: SupporterManager.IScanListener? = null
    private var singleScanFlag = false
    private var isProcessing: Boolean = false

    private val conn: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(componentName: ComponentName, service: IBinder) {
            Log.d("SunmiScannerManager", "Scanner service connected")
            listener?.onScannerServiceConnected()
            scanInterface = IScanInterface.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Log.d("SunmiScannerManager", "Scanner service disconnected")
            listener?.onScannerServiceDisconnected()
            scanInterface = null
        }
    }

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        private var lastScannedResult: String? = null

        override fun onReceive(context: Context, intent: Intent) {
            handler.post {
                val code = intent.getStringExtra(DATA)
                Log.d("SunmiScannerManager", "Data received: $code")

                if (code != null && code.isNotEmpty() && code != lastScannedResult && !isProcessing) {
                    lastScannedResult = code
                    isProcessing = true
                    listener?.onScannerResultChange(code)
                    Log.d("SunmiScannerManager", "Scanner result changed: $code")

                    handler.postDelayed({
                        isProcessing = false
                        lastScannedResult = null
                    }, 300)

                    if (singleScanFlag) {
                        singleScanFlag = false
                        try {
                            Log.d("SunmiScannerManager", "Stopping scan after single scan")
                            scanInterface?.stop()
                        } catch (e: RemoteException) {
                            Log.e("SunmiScannerManager", "Error stopping scan", e)
                        }
                    }
                }
            }
        }
    }

    override fun init() {
        Log.d("SunmiScannerManager", "Initializing scanner manager")
        bindService()
        registerReceiver()
    }

    override fun recycle() {
        Log.d("SunmiScannerManager", "Recycling scanner manager")
        activity.stopService(serviceIntent)
        activity.unregisterReceiver(receiver)
        listener = null
    }

    override fun setScannerListener(listener: SupporterManager.IScanListener) {
        Log.d("SunmiScannerManager", "Setting scanner listener")
        this.listener = listener
    }

    override fun sendKeyEvent(key: KeyEvent?) {
        try {
            Log.d("SunmiScannerManager", "Sending key event: $key")
            scanInterface?.sendKeyEvent(key)
        } catch (e: Exception) {
            Log.e("SunmiScannerManager", "Error sending key event", e)
        }
    }

    override val scannerModel: Int?
        get() {
            return try {
                val model = scanInterface?.scannerModel
                Log.d("SunmiScannerManager", "Getting scanner model: $model")
                model
            } catch (e: Exception) {
                Log.e("SunmiScannerManager", "Error getting scanner model", e)
                -1
            }
        }

    override fun scannerEnable(enable: Boolean) {
        Log.d("SunmiScannerManager", "This device does not support the method!")
    }

    override fun setScanMode(mode: String?) {
        Log.d("SunmiScannerManager", "Setting scan mode: $mode")
    }

    override fun setDataTransferType(type: String?) {
        Log.d("SunmiScannerManager", "Setting data transfer type: $type")
    }

    override fun singleScan(bool: Boolean) {
        try {
            Log.d("SunmiScannerManager", "Setting single scan to: $bool")
            if (bool) {
                scanInterface?.scan()
                singleScanFlag = true
            }
        } catch (e: Exception) {
            Log.e("SunmiScannerManager", "Error starting single scan", e)
        }
    }

    @Synchronized
    override fun continuousScan(bool: Boolean) {
        try {
            Log.d("SunmiScannerManager", "Setting continuous scan to: $bool")
            if (bool) {
                scanInterface?.scan()
            } else {
                scanInterface?.stop()
            }
        } catch (e: Exception) {
            Log.e("SunmiScannerManager", "Error in continuous scan operation", e)
        }
    }

    private fun bindService() {
        serviceIntent = Intent().apply {
            setPackage("com.sunmi.scanner")
            action = "com.sunmi.scanner.IScanInterface"
        }

        Log.d("SunmiScannerManager", "Binding to scanner service")
        activity.bindService(serviceIntent!!, conn, Service.BIND_AUTO_CREATE)
    }

    private fun registerReceiver() {
        val intentFilter = IntentFilter().apply { addAction(ACTION_DATA_CODE_RECEIVED) }

        Log.d("SunmiScannerManager", "Registering receiver for action: $ACTION_DATA_CODE_RECEIVED")
        activity.registerReceiver(receiver, intentFilter)
    }

    companion object {
        const val ACTION_DATA_CODE_RECEIVED = "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED"
        private const val DATA = "data"

        private var scanInterface: IScanInterface? = null

        private var instance: SunmiScannerManager? = null

        fun getInstance(context: Context): SunmiScannerManager? {
            if (instance == null) {
                synchronized(SunmiScannerManager::class.java) {
                    if (instance == null) {
                        instance = SunmiScannerManager(context)
                    }
                }
            }
            return instance
        }
    }
}