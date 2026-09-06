package com.ryuutools.app

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters

class ThermalCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        private const val NOTIFICATION_ID = 2001
        private const val OVERHEAT_THRESHOLD = 43f
    }

    override fun doWork(): Result {
        return try {
            val batteryIntent = applicationContext.registerReceiver(
                null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )
            val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1

            if (tempTenths >= 0) {
                val tempCelsius = tempTenths / 10f
                if (tempCelsius >= OVERHEAT_THRESHOLD) {
                    showNotification(tempCelsius)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun showNotification(temp: Float) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.THERMAL_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Device Overheating")
                .setContentText("Temperature reached %.1f°C. Tap to open Thermal Monitor.".format(temp))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            val manager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Aman
        }
    }
}