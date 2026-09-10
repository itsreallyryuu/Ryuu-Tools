package com.ryuutools.app

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.work.Worker
import androidx.work.WorkerParameters

class MemorySweepWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        return try {
            if (ShizukuHelper.hasPermission()) {
                sweepBackgroundApps(applicationContext)
            }
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}

fun getExcludedPackages(context: Context): Set<String> {
    val excluded = mutableSetOf(
        context.packageName,
        "android",
        "com.android.systemui"
    )
    try {
        val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        context.packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName?.let { excluded.add(it) }
    } catch (e: Exception) { }
    try {
        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
        telecomManager?.defaultDialerPackage?.let { excluded.add(it) }
    } catch (e: Exception) { }
    return excluded
}

fun sweepBackgroundApps(context: Context): Int {
    val excluded = getExcludedPackages(context)
    val apps = context.packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
    val targets = apps.filter { app ->
        val isSystemApp = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        !isSystemApp && !excluded.contains(app.packageName)
    }

    if (targets.isEmpty()) return 0

    // FIX: dipecah per 15 app per command, bukan digabung semua sekaligus
    var stoppedCount = 0
    targets.chunked(15).forEach { batch ->
        val combinedCommand = batch.joinToString(separator = " ; ") { "am force-stop ${it.packageName}" }
        val (success, _) = ShizukuHelper.runCommand(combinedCommand)
        if (success) stoppedCount += batch.size
    }

    return stoppedCount
}