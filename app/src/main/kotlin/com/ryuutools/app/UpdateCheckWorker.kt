package com.ryuutools.app

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class UpdateCheckWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    companion object {
        // TODO WAJIB DIGANTI: isi dengan repo GitHub asli kamu, format "username/nama-repo"
        // Belum ada repo GitHub? Aman dibiarkan dulu — cuma hasilnya selalu "gagal cek" (tidak crash)
        private const val GITHUB_REPO = "itsreallyryuu/Ryuu-Tools"
        private const val NOTIFICATION_ID = 3001
    }

    override fun doWork(): Result {
        return try {
            val connection = URL("https://api.github.com/repos/$GITHUB_REPO/releases/latest")
                .openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            val reader = BufferedReader(InputStreamReader(connection.inputStream))
            val response = reader.readText()
            reader.close()

            val json = JSONObject(response)
            val latestTag = json.optString("tag_name", "").removePrefix("v")

            // Cari asset yang benar-benar .apk, bukan asal ambil asset pertama
            // (release GitHub bisa punya source zip/tarball juga)
            val assets = json.optJSONArray("assets")
            var apkDownloadUrl: String? = null
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i)
                    val name = asset?.optString("name", "") ?: ""
                    if (name.endsWith(".apk", ignoreCase = true)) {
                        apkDownloadUrl = asset?.optString("browser_download_url")
                        break
                    }
                }
            }

            val currentVersion = applicationContext.packageManager
                .getPackageInfo(applicationContext.packageName, 0).versionName ?: "0"

            val isNewer = latestTag.isNotBlank() && isNewerVersion(latestTag, currentVersion)
            if (isNewer && apkDownloadUrl != null) {
                showUpdateNotification(latestTag, apkDownloadUrl)
            }

            val output = workDataOf(
                "update_available" to isNewer,
                "latest_version" to latestTag
            )
            Result.success(output)
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun isNewerVersion(remote: String, local: String): Boolean {
        return try {
            val remoteParts = remote.split(".").map { it.toIntOrNull() ?: 0 }
            val localParts = local.split(".").map { it.toIntOrNull() ?: 0 }
            for (i in 0 until maxOf(remoteParts.size, localParts.size)) {
                val r = remoteParts.getOrElse(i) { 0 }
                val l = localParts.getOrElse(i) { 0 }
                if (r != l) return r > l
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    private fun showUpdateNotification(version: String, apkDownloadUrl: String) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            val intent = Intent(applicationContext, UpdateDownloadActivity::class.java).apply {
                putExtra("apk_url", apkDownloadUrl)
                putExtra("version", version)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.UPDATE_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Update Available — v$version")
                .setContentText("Tap to download and install the latest version of Ryuu Tools.")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            val manager = ContextCompat.getSystemService(applicationContext, NotificationManager::class.java)
            manager?.notify(NOTIFICATION_ID, notification)
        } catch (e: Exception) {
            // Aman
        }
    }
}