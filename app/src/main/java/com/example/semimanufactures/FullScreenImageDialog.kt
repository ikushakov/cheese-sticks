package com.example.semimanufactures

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.widget.ImageView
import com.bumptech.glide.Glide

class FullScreenImageDialog(private val context: Context, private val url: String) : Dialog(context) {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_full_screen_image)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        val imageView = findViewById<ImageView>(R.id.full_screen_image)

        Glide.with(context)
            .load(url)
            .into(imageView)

        window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
    }
}