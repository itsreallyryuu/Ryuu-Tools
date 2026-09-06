package com.ryuutools.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class RyuuApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createChannels(this)
        scheduleBackgroundWork()
    }

    private fun scheduleBackgroundWork() {
        val prefs = getSharedPreferences("ryuu_prefs", MODE_PRIVATE)

        if (prefs.getBoolean("thermal_alert_enabled", true)) {
            val thermalRequest = PeriodicWorkRequestBuilder<ThermalCheckWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "thermal_check_work", ExistingPeriodicWorkPolicy.KEEP, thermalRequest
            )
        }

        if (prefs.getBoolean("auto_update_check", true)) {
            val updateRequest = PeriodicWorkRequestBuilder<UpdateCheckWorker>(12, TimeUnit.HOURS).build()
            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "update_check_work", ExistingPeriodicWorkPolicy.KEEP, updateRequest
            )
        }
    }
}

