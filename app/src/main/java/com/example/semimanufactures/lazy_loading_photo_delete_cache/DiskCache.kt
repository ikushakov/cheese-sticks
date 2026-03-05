package com.example.semimanufactures.lazy_loading_photo_delete_cache

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest

class DiskCache(private val context: Context) {
    private val cacheDir = File(context.cacheDir, "smart_image_cache")
    private val maxSize = 100 * 1024 * 1024L // 100MB
    private val prefs = context.getSharedPreferences("disk_cache", Context.MODE_PRIVATE)

    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    fun put(key: String, data: ByteArray) {
        val file = File(cacheDir, generateFileName(key))

        try {
            FileOutputStream(file).use { fos ->
                fos.write(data)
            }
            // Сохраняем время создания
            prefs.edit().putLong(file.name, System.currentTimeMillis()).apply()
            trimToSize(maxSize)
        } catch (e: IOException) {
            Log.e("DiskCache", "Error writing to cache", e)
        }
    }

    fun get(key: String): ByteArray? {
        val file = File(cacheDir, generateFileName(key))

        return if (file.exists() && isValid(file)) {
            try {
                FileInputStream(file).use { fis ->
                    fis.readBytes()
                }
            } catch (e: IOException) {
                null
            }
        } else {
            // Удаляем невалидный файл
            file.delete()
            prefs.edit().remove(file.name).apply()
            null
        }
    }

    private fun isValid(file: File): Boolean {
        val createTime = prefs.getLong(file.name, 0L)
        // Файл валиден 30 дней
        return createTime > 0 && System.currentTimeMillis() - createTime < 30 * 24 * 60 * 60 * 1000L
    }

    fun remove(key: String) {
        val file = File(cacheDir, generateFileName(key))
        if (file.exists()) {
            file.delete()
            prefs.edit().remove(file.name).apply()
        }
    }

    fun clear() {
        cacheDir.listFiles()?.forEach { it.delete() }
        prefs.edit().clear().apply()
    }

    fun getSize(): Long {
        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    fun cleanupOldFiles(maxAgeDays: Int = 14) {
        val maxAgeMillis = maxAgeDays * 24 * 60 * 60 * 1000L
        val currentTime = System.currentTimeMillis()

        cacheDir.listFiles()?.forEach { file ->
            val createTime = prefs.getLong(file.name, 0L)
            if (createTime > 0 && currentTime - createTime > maxAgeMillis) {
                file.delete()
                prefs.edit().remove(file.name).apply()
            }
        }

        trimToSize(maxSize)
    }

    private fun generateFileName(key: String): String {
        return MD5.hash(key)
    }

    private fun trimToSize(maxSize: Long) {
        var currentSize = getSize()

        if (currentSize > maxSize) {
            // Сортируем файлы по времени последнего доступа (в нашем случае - создания)
            val files = cacheDir.listFiles()?.map { file ->
                file to prefs.getLong(file.name, 0L)
            }?.sortedBy { it.second }

            files?.forEach { (file, _) ->
                if (currentSize <= maxSize) return

                val fileSize = file.length()
                if (file.delete()) {
                    prefs.edit().remove(file.name).apply()
                    currentSize -= fileSize
                }
            }
        }
    }
}

// Утилита для MD5 хеширования
object MD5 {
    fun hash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}