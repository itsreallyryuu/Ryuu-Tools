package com.ryuutools.app

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class ThermalMonitorActivity : AppCompatActivity() {

    private var batteryReceiver: BroadcastReceiver? = null
    private var hasNotifiedOverheat = false
    private val overheatThreshold = 42f

    companion object {
        private const val CHANNEL_ID = "thermal_channel"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIF_PERMISSION_CODE = 501
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_thermal_monitor)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        createNotificationChannel()
        requestNotificationPermissionIfNeeded()
        setupTemperatureMonitor()

        findViewById<Button>(R.id.btnCoolDown).setOnClickListener {
            performCoolDown()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Thermal Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "Notifies when device temperature is too high"
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIF_PERMISSION_CODE
                )
            }
        }
    }

    private fun setupTemperatureMonitor() {
        val tvTempValue = findViewById<TextView>(R.id.tvTempValue)
        val tvTempStatus = findViewById<TextView>(R.id.tvTempStatus)
        val progressTemp = findViewById<ProgressBar>(R.id.progressTemp)

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
                if (tempTenths < 0) return

                val tempCelsius = tempTenths / 10f
                tvTempValue.text = "%.1f°C".format(tempCelsius)
                progressTemp.progress = tempCelsius.toInt().coerceIn(0, 60)

                val (statusText, statusColor) = when {
                    tempCelsius < 35f -> "Normal" to android.graphics.Color.parseColor("#00E676")
                    tempCelsius < 40f -> "Warm" to android.graphics.Color.parseColor("#FFC107")
                    tempCelsius < 45f -> "Hot" to android.graphics.Color.parseColor("#FF9800")
                    else -> "Overheating!" to android.graphics.Color.parseColor("#FF2D55")
                }
                tvTempStatus.text = statusText
                tvTempStatus.setTextColor(statusColor)

                if (tempCelsius >= overheatThreshold) {
                    if (!hasNotifiedOverheat) {
                        showOverheatNotification(tempCelsius)
                        hasNotifiedOverheat = true
                    }
                } else {
                    hasNotifiedOverheat = false
                }
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    private fun showOverheatNotification(temp: Float) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Device Overheating")
                .setContentText("Temperature reached %.1f°C. Consider cooling down your device.".format(temp))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            val manager = ContextCompat.getSystemService(this, NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Aman, gagal notifikasi tidak boleh crash app
        }
    }

    private fun performCoolDown() {
        try {
            val layoutParams = window.attributes
            layoutParams.screenBrightness = 0.1f
            window.attributes = layoutParams

            Toast.makeText(
                this,
                "Screen brightness lowered. For best results, also close other heavy apps manually.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Could not adjust brightness.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) { }
        }
    }
}