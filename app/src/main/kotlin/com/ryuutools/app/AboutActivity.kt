package com.ryuutools.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) { "Unknown" }
        findViewById<TextView>(R.id.tvAboutVersion).text = "Version $versionName"

        findViewById<Button>(R.id.btnAboutWhatsapp).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://whatsapp.com/channel/0029VbC4xiq3wtbC3VsYt127")))
        }
        findViewById<Button>(R.id.btnAboutTiktok).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW,
                Uri.parse("https://vm.tiktok.com/ZS9S8ftJeT1dt-imXTA/")))
        }
    }
}