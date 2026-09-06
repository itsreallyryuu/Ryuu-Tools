package com.ryuutools.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val versionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: Exception) { "?" }
        findViewById<TextView>(R.id.tvVersion).text = "v$versionName"

        findViewById<Button>(R.id.btnCheckUpdate).setOnClickListener { checkForUpdatesNow() }

        val prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)

        val switchAutoUpdate = findViewById<Switch>(R.id.switchAutoUpdate)
        switchAutoUpdate.isChecked = prefs.getBoolean("auto_update_check", true)
        switchAutoUpdate.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("auto_update_check", isChecked).apply()
            if (isChecked) {
                val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS).build()
                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "update_check_work", ExistingPeriodicWorkPolicy.REPLACE, request
                )
            } else {
                WorkManager.getInstance(this).cancelUniqueWork("update_check_work")
            }
        }

        val switchThermalAlert = findViewById<Switch>(R.id.switchThermalAlert)
        switchThermalAlert.isChecked = prefs.getBoolean("thermal_alert_enabled", true)
        switchThermalAlert.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("thermal_alert_enabled", isChecked).apply()
            if (isChecked) {
                val request = PeriodicWorkRequestBuilder<ThermalCheckWorker>(15, TimeUnit.MINUTES).build()
                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "thermal_check_work", ExistingPeriodicWorkPolicy.REPLACE, request
                )
            } else {
                WorkManager.getInstance(this).cancelUniqueWork("thermal_check_work")
            }
        }

        findViewById<Button>(R.id.btnAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        findViewById<Button>(R.id.btnFeedback).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://whatsapp.com/channel/0029VbC4xiq3wtbC3VsYt127")))
            } catch (e: Exception) {
                Toast.makeText(this, "Unable to open link.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkForUpdatesNow() {
        val btn = findViewById<Button>(R.id.btnCheckUpdate)
        val progress = findViewById<ProgressBar>(R.id.progressCheckUpdate)
        val statusText = findViewById<TextView>(R.id.tvUpdateStatus)

        btn.isEnabled = false
        progress.visibility = View.VISIBLE
        statusText.text = "Checking for updates..."

        val request = OneTimeWorkRequestBuilder<UpdateCheckWorker>().build()
        WorkManager.getInstance(this).enqueue(request)

        WorkManager.getInstance(this).getWorkInfoByIdLiveData(request.id)
            .observe(this) { workInfo ->
                if (workInfo != null && workInfo.state.isFinished) {
                    progress.visibility = View.GONE
                    btn.isEnabled = true
                    if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                        val updateAvailable = workInfo.outputData.getBoolean("update_available", false)
                        val latestVersion = workInfo.outputData.getString("latest_version") ?: ""
                        statusText.text = if (updateAvailable) {
                            "Update available: v$latestVersion"
                        } else {
                            "You're up to date"
                        }
                    } else {
                        statusText.text = "Failed to check for updates. Try again later."
                    }
                }
            }
    }
}