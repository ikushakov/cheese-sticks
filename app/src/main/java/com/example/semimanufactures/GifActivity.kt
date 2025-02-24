package com.example.semimanufactures

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide

class GifActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gif)
        supportActionBar?.hide()
        val gifImageView = findViewById<ImageView>(R.id.gifImageView)
        Glide.with(this)
            .asGif()
            .load(R.drawable.drift_rc_truck)
            .into(gifImageView)
        Handler().postDelayed({
            val intent = Intent(this, LogisticActivity::class.java).apply {
                putExtra("userId", intent.getIntExtra("userId", 0))
                putExtra("username", intent.getStringExtra("username"))
                putExtra("roleCheck", intent.getStringExtra("roleCheck"))
                putExtra("mdmCode", intent.getStringExtra("mdmCode"))
                putExtra("fio", intent.getStringExtra("fio"))
                putExtra("deviceInfo", intent.getStringExtra("deviceInfo"))
            }
            startActivity(intent)
            finish()
        }, 7000)
    }
}