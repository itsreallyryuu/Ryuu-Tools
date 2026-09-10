package com.ryuutools.app

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LiveStatsActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_stats)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnStartOverlay).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Grant 'Display over other apps' permission first.", Toast.LENGTH_LONG).show()
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                return@setOnClickListener
            }
            startService(Intent(this, LiveStatsService::class.java))
            Toast.makeText(this, "Live Stats overlay started.", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnStopOverlay).setOnClickListener {
            stopService(Intent(this, LiveStatsService::class.java))
            Toast.makeText(this, "Live Stats overlay stopped.", Toast.LENGTH_SHORT).show()
        }
    }
}