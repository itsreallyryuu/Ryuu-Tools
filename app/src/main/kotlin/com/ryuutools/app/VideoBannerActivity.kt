package com.ryuutools.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts

class VideoBannerActivity : BaseActivity() {

    private lateinit var prefs: android.content.SharedPreferences

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) { }
            prefs.edit()
                .putString("video_banner_type", "custom")
                .putString("video_banner_custom_uri", it.toString())
                .apply()
            Toast.makeText(this, "Custom banner saved.", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_banner)

        prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.rowBanner1).setOnClickListener { selectBuiltin("builtin1") }
        findViewById<TextView>(R.id.rowBanner2).setOnClickListener { selectBuiltin("builtin2") }
        findViewById<TextView>(R.id.rowBanner3).setOnClickListener { selectBuiltin("builtin3") }

        findViewById<Button>(R.id.btnUploadCustom).setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }
    }

    private fun selectBuiltin(type: String) {
        prefs.edit().putString("video_banner_type", type).apply()
        Toast.makeText(this, "Banner updated.", Toast.LENGTH_SHORT).show()
        finish()
    }
}