package com.ryuutools.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) { "Unknown" }
        findViewById<TextView>(R.id.tvAboutVersion).text = "Version $versionName"

        findViewById<Button>(R.id.btnAboutDonate).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://saweria.co/itsmeryuu")))
        }
        
    }
}