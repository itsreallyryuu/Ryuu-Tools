package com.ryuutools.app

import android.app.ActivityManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MemorySweepActivity : BaseActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memory_sweep)

        prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        updateRamInfo()

        findViewById<Button>(R.id.btnCleanNow).setOnClickListener { runCleanNow() }

        val switchAutoSweep = findViewById<Switch>(R.id.switchAutoSweep)
        switchAutoSweep.isChecked = prefs.getBoolean("auto_sweep_enabled", false)
        switchAutoSweep.setOnCheckedChangeListener { _, checked ->
            prefs.edit().putBoolean("auto_sweep_enabled", checked).apply()
            if (checked) {
                val request = PeriodicWorkRequestBuilder<MemorySweepWorker>(2, TimeUnit.HOURS).build()
                WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                    "memory_sweep_work", ExistingPeriodicWorkPolicy.REPLACE, request
                )
            } else {
                WorkManager.getInstance(this).cancelUniqueWork("memory_sweep_work")
            }
        }
    }

    private fun getAvailRamGb(): Double {
        val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        return memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
    }

    private fun updateRamInfo() {
        val actManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        actManager.getMemoryInfo(memInfo)
        val totalGb = memInfo.totalMem / (1024.0 * 1024.0 * 1024.0)
        val availGb = memInfo.availMem / (1024.0 * 1024.0 * 1024.0)
        findViewById<TextView>(R.id.tvRamInfo).text = "RAM: %.1f / %.1f GB free".format(availGb, totalGb)
    }

    private fun runCleanNow() {
        if (!ShizukuHelper.hasPermission()) {
            Toast.makeText(this, "Connect Root Mode in System Boost first.", Toast.LENGTH_LONG).show()
            return
        }
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val btnClean = findViewById<Button>(R.id.btnCleanNow)
        val tvResult = findViewById<TextView>(R.id.tvResult)

        progressBar.visibility = View.VISIBLE
        btnClean.isEnabled = false
        tvResult.text = ""

        val ramBeforeGb = getAvailRamGb()

        Thread {
            val stoppedCount = sweepBackgroundApps(this)

            // Sample RAM multiple times over ~2.5s instead of once after a fixed 800ms.
            // Android reclaims freed memory into cache almost immediately, so the
            // number right after force-stopping keeps drifting for a bit — we want
            // the highest free value we actually observe (the real peak the sweep
            // achieved), not whatever the number happens to be at one fixed instant.
            var peakAvailGb = ramBeforeGb
            repeat(5) {
                Thread.sleep(500)
                val sample = getAvailRamGb()
                if (sample > peakAvailGb) peakAvailGb = sample
            }

            val freedGb = (peakAvailGb - ramBeforeGb).coerceAtLeast(0.0)

            Handler(Looper.getMainLooper()).post {
                progressBar.visibility = View.GONE
                btnClean.isEnabled = true
                tvResult.text = if (freedGb >= 0.05) {
                    "Closed $stoppedCount background apps — freed up to %.2f GB".format(freedGb)
                } else {
                    "Closed $stoppedCount background apps — RAM was already mostly free, so the gain was minimal (this is normal)"
                }
                updateRamInfo()
            }
        }.start()
    }
}