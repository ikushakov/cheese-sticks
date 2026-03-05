package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ProgressBar
import com.example.semimanufactures.lazy_loading_photo_delete_cache.SmartImageLoader

class FullScreenImageDialog(
    private val context: Context,
    private val primaryUrl: String,
    private val fallbackUrl: String? = null,
    private val authTokenAPI: String,
    private val authToken: String,
    private val s3UnavailableUrl: String? = null
) : Dialog(context) {

    private val smartImageLoader = SmartImageLoader.getInstance(context)
    private var currentUrl: String = primaryUrl
    private var fallbackTried = false
    private var s3UnavailableTried = false

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setFlags(
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS,
            WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
        )

        setContentView(R.layout.dialog_full_screen_image)

        val zoomImageView = findViewById<ZoomableImageView>(R.id.full_screen_image)
        val progressBar = findViewById<ProgressBar>(R.id.full_screen_progress)
        val closeButton = findViewById<ImageView>(R.id.close_button)
        val dimBackground = findViewById<View>(R.id.dim_background)
        val errorIndicator = findViewById<ImageView>(R.id.error_indicator)

        progressBar.visibility = View.VISIBLE
        zoomImageView.visibility = View.INVISIBLE
        errorIndicator.visibility = View.GONE

        // Загружаем изображение с авторизацией
        loadImageWithAuth(zoomImageView, progressBar, errorIndicator)

        setupClickListeners(closeButton, zoomImageView, dimBackground, errorIndicator)
    }

    private fun loadImageWithAuth(
        imageView: ZoomableImageView,
        progressBar: ProgressBar,
        errorIndicator: ImageView
    ) {
        Log.d("FullScreenImageDialog", "Loading image with auth: $currentUrl")

        progressBar.visibility = View.VISIBLE
        imageView.visibility = View.INVISIBLE
        errorIndicator.visibility = View.GONE

        // Используем новый метод для загрузки с кастомными заголовками
        smartImageLoader.loadImageWithProgressAndCustomAuth(
            url = currentUrl,
            imageView = imageView,
            progressBar = progressBar,
            authTokenAPI = authTokenAPI,
            authToken = authToken,
            errorRes = R.drawable.error,
            onSuccess = {
                Log.d("FullScreenImageDialog", "Image loaded successfully from: $currentUrl")
                imageView.visibility = View.VISIBLE
                errorIndicator.visibility = View.GONE
            },
            onError = {
                Log.w("FullScreenImageDialog", "Failed to load image from: $currentUrl")
                imageView.visibility = View.VISIBLE

                // Сначала пробуем s3_unavailable URL, если есть и еще не пробовали
                if (!s3UnavailableTried && !s3UnavailableUrl.isNullOrEmpty() && currentUrl != s3UnavailableUrl) {
                    Log.d("FullScreenImageDialog", "Trying s3_unavailable URL: $s3UnavailableUrl")
                    s3UnavailableTried = true
                    currentUrl = s3UnavailableUrl!!
                    loadImageWithAuth(imageView, progressBar, errorIndicator)
                }
                // Затем пробуем обычный fallback URL если есть и еще не пробовали
                else if (!fallbackTried && !fallbackUrl.isNullOrEmpty() && currentUrl != fallbackUrl) {
                    Log.d("FullScreenImageDialog", "Trying fallback URL: $fallbackUrl")
                    fallbackTried = true
                    currentUrl = fallbackUrl!!
                    loadImageWithAuth(imageView, progressBar, errorIndicator)
                } else {
                    // Все попытки исчерпаны, показываем индикатор ошибки
                    errorIndicator.visibility = View.VISIBLE
                    errorIndicator.setOnClickListener {
                        // Перезагружаем с основной URL
                        fallbackTried = false
                        s3UnavailableTried = false
                        currentUrl = primaryUrl
                        loadImageWithAuth(imageView, progressBar, errorIndicator)
                    }
                }
            }
        )
    }

    private fun setupClickListeners(
        closeButton: ImageView,
        imageView: ZoomableImageView,
        dimBackground: View,
        errorIndicator: ImageView
    ) {
        closeButton.setOnClickListener {
            dismiss()
        }

        dimBackground.setOnClickListener {
            dismiss()
        }

        errorIndicator.setOnClickListener {
            // Перезагрузка при нажатии на индикатор ошибки
            fallbackTried = false
            s3UnavailableTried = false
            currentUrl = primaryUrl
            val progressBar = findViewById<ProgressBar>(R.id.full_screen_progress)
            loadImageWithAuth(imageView, progressBar, errorIndicator)
        }

        // Одиночный клик на фото закрывает диалог только если не увеличен
        imageView.setOnClickListener {
            if (imageView.scaleFactor <= 1.1f) {
                dismiss()
            }
        }
    }

    override fun show() {
        super.show()
        window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
    }
}