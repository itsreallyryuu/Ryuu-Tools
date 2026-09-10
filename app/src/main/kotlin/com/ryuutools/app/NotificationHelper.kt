package com.ryuutools.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationHelper {
    const val THERMAL_CHANNEL_ID = "thermal_channel"
    const val UPDATE_CHANNEL_ID = "update_channel"
    const val LIVE_STATS_CHANNEL_ID = "live_stats_channel"

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)

            val thermalChannel = NotificationChannel(
                THERMAL_CHANNEL_ID,
                "Thermal Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Notifies when device temperature is too high" }

            val updateChannel = NotificationChannel(
                UPDATE_CHANNEL_ID,
                "App Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifies when a new app update is available" }
            
            val liveStatsChannel = NotificationChannel(
    LIVE_STATS_CHANNEL_ID,
    "Live Stats Overlay",
    NotificationManager.IMPORTANCE_LOW
).apply { description = "Shows the ongoing notification while the Live Stats overlay is active" }
manager?.createNotificationChannel(liveStatsChannel)

            manager?.createNotificationChannel(thermalChannel)
            manager?.createNotificationChannel(updateChannel)
        }
    }
}