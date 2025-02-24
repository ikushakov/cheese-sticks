package com.example.semimanufactures

import android.view.KeyEvent

interface IScannerManager {
    fun init()
    fun recycle()
    fun setScannerListener(listener: SupporterManager.IScanListener)
    fun sendKeyEvent(key: KeyEvent?)
    val scannerModel: Int?
    fun scannerEnable(enable: Boolean)
    fun setScanMode(mode: String?)
    fun setDataTransferType(type: String?)
    fun singleScan(bool: Boolean)
    fun continuousScan(bool: Boolean)
}