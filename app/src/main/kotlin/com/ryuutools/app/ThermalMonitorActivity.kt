package com.ryuutools.app

import android.Manifest
import android.app.AlertDialog
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
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

class ThermalMonitorActivity : BaseActivity() {

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

    /**
     * Step 1: lower brightness (instant, always safe).
     * Step 2: force-stop heavy background apps (needs Shizuku/Root Mode).
     * Step 3: radio reduction (WiFi/data off) is now OPT-IN via a confirmation
     * dialog — NOT automatic. Turning off networking while the user is gaming
     * online or using internet apps would be actively disruptive, so we ask
     * first and clearly warn them to skip it in that situation.
     */
    private fun performCoolDown() {
        val btnCoolDown = findViewById<Button>(R.id.btnCoolDown)
        btnCoolDown.isEnabled = false

        try {
            val layoutParams = window.attributes
            layoutParams.screenBrightness = 0.1f
            window.attributes = layoutParams
        } catch (e: Exception) {
            // Aman, brightness gagal diubah tidak boleh crash app
        }

        Thread {
            val shizukuConnected = ShizukuHelper.isAvailable() && ShizukuHelper.hasPermission()
            var stoppedCount = 0

            if (shizukuConnected) {
                stoppedCount = sweepBackgroundApps(this)
            }

            Thread.sleep(400)

            runOnUiThread {
                if (shizukuConnected) {
                    askAboutRadioReduction(stoppedCount)
                } else {
                    btnCoolDown.isEnabled = true
                    Toast.makeText(
                        this,
                        "Brightness lowered. Connect Root Mode in System Boost to also close background apps.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    private fun askAboutRadioReduction(stoppedCount: Int) {
        val btnCoolDown = findViewById<Button>(R.id.btnCoolDown)

        AlertDialog.Builder(this)
            .setTitle("Reduce Radio Activity Too?")
            .setMessage(
                "Turning off WiFi and mobile data can help cool the device a bit " +
                "faster, but it will disconnect you from the internet.\n\n" +
                "Skip this if you're currently gaming online or using anything " +
                "that needs a connection."
            )
            .setPositiveButton("Turn Off WiFi/Data") { _, _ -> runRadioReduction(stoppedCount) }
            .setNegativeButton("Skip") { _, _ ->
                btnCoolDown.isEnabled = true
                Toast.makeText(
                    this,
                    "Brightness lowered. Closed $stoppedCount background app(s).",
                    Toast.LENGTH_LONG
                ).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun runRadioReduction(stoppedCount: Int) {
        val btnCoolDown = findViewById<Button>(R.id.btnCoolDown)

        Thread {
            val (wifiOk, _) = ShizukuHelper.runCommand("svc wifi disable")
            val (dataOk, _) = ShizukuHelper.runCommand("svc data disable")
            val radioReduced = wifiOk || dataOk

            runOnUiThread {
                btnCoolDown.isEnabled = true
                val message = if (radioReduced) {
                    "Brightness lowered. Closed $stoppedCount background app(s). WiFi/mobile data turned off."
                } else {
                    "Brightness lowered. Closed $stoppedCount background app(s). Couldn't reduce radio activity."
                }
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()

                if (radioReduced) {
                    Toast.makeText(
                        this,
                        "Note: turn WiFi/data back on manually when you're done.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        batteryReceiver?.let {
            try { unregisterReceiver(it) } catch (e: Exception) { }
        }
    }
}