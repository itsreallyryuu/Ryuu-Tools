package com.ryuutools.app

import android.content.Context
import android.os.Build

object ChangelogHelper {
    // Update peta ini tiap rilis baru — key-nya versionCode, isinya daftar poin update
    private val changelogs = mapOf(
        7 to listOf(
            "Added System Boost: Root Mode, Gaming Mode, Memory Sweep, Live Stats, App Manager, Terminal",
            "Added Calculator with trig, log, memory, and history",
            "Removed all WhatsApp links",
            "Download progress popup added to TikTok Downloader",
            "Renamed Thermal Monitor to Device Temperature"
        ),
        8 to listOf(
    "Added Terminal with real ADB shell access (via Root Mode)",
    "Added scientific Calculator with history",
    "Added Brat text generator",
    "Added optional background music with mute and custom song support",
    "Added switchable video banners, including custom video upload",
    "Gaming Mode now supports a game launcher and more optimization options",
    "Added System Boost on/off toggle",
    "Fixed TikTok Downloader preview sizing to match the video's real aspect ratio",
    "Moved About and Feedback into Settings"
),
    )

    private fun getVersionCode(context: Context): Int {
        return try {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt()
            else @Suppress("DEPRECATION") info.versionCode
        } catch (e: Exception) { -1 }
    }

    fun getEntriesForCurrentVersion(context: Context): List<String>? = changelogs[getVersionCode(context)]

    fun shouldShow(context: Context): Boolean {
        val prefs = context.getSharedPreferences("ryuu_prefs", Context.MODE_PRIVATE)
        val lastShown = prefs.getInt("last_changelog_shown", -1)
        val current = getVersionCode(context)
        return lastShown != current && changelogs.containsKey(current)
    }

    fun markShown(context: Context) {
        val prefs = context.getSharedPreferences("ryuu_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt("last_changelog_shown", getVersionCode(context)).apply()
    }
}