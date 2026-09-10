package com.ryuutools.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

class UpdateDownloadActivity : BaseActivity() {

    private var downloadId: Long = -1L
    private lateinit var downloadManager: DownloadManager
    private val handler = Handler(Looper.getMainLooper())
    private var pollingProgress = false

    private lateinit var apkUrl: String
    private lateinit var apkFile: File

    private val downloadCompleteReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                pollingProgress = false
                onDownloadFinished()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update_download)

        val version = intent.getStringExtra("version") ?: "latest"
        apkUrl = intent.getStringExtra("apk_url") ?: ""

        findViewById<TextView>(R.id.tvUpdateVersion).text = "Version $version"

        if (apkUrl.isBlank()) {
            findViewById<TextView>(R.id.tvUpdateStatus).text = "Update link not found. Please try again later."
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            findViewById<TextView>(R.id.tvUpdateStatus).text =
                "Please allow \"Install unknown apps\" for Ryuu Tools, then reopen this screen."
            findViewById<Button>(R.id.btnCancelUpdate).apply {
                visibility = View.VISIBLE
                text = "Open Settings"
                setOnClickListener { openInstallPermissionSettings() }
            }
            return
        }

        startDownload()
    }

    private fun openInstallPermissionSettings() {
        try {
            val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't open settings. Please enable it manually.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startDownload() {
        downloadManager = getSystemService(DOWNLOAD_SERVICE) as DownloadManager
        apkFile = File(getExternalFilesDir(null), "RyuuTools_update.apk")
        if (apkFile.exists()) apkFile.delete()

        val request = DownloadManager.Request(Uri.parse(apkUrl))
            .setTitle("Ryuu Tools Update")
            .setDescription("Downloading the latest version...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
            .setDestinationUri(Uri.fromFile(apkFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        registerReceiver(
            downloadCompleteReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            Context.RECEIVER_NOT_EXPORTED.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU } ?: 0
        )

        downloadId = downloadManager.enqueue(request)
        pollingProgress = true
        findViewById<TextView>(R.id.tvUpdateStatus).text = "Downloading..."
        pollProgress()
    }

    private fun pollProgress() {
        if (!pollingProgress) return

        val query = DownloadManager.Query().setFilterById(downloadId)
        downloadManager.query(query)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                val downloaded = if (bytesDownloadedIndex >= 0) cursor.getLong(bytesDownloadedIndex) else 0L
                val total = if (bytesTotalIndex >= 0) cursor.getLong(bytesTotalIndex) else -1L
                val status = if (statusIndex >= 0) cursor.getInt(statusIndex) else -1

                if (total > 0) {
                    val percent = ((downloaded * 100) / total).toInt()
                    findViewById<ProgressBar>(R.id.progressUpdate).progress = percent
                    findViewById<TextView>(R.id.tvUpdatePercent).text = "$percent%"
                }

                if (status == DownloadManager.STATUS_FAILED) {
                    pollingProgress = false
                    findViewById<TextView>(R.id.tvUpdateStatus).text = "Download failed. Please try again."
                    return
                }
            }
        }

        if (pollingProgress) {
            handler.postDelayed({ pollProgress() }, 500)
        }
    }

    private fun onDownloadFinished() {
        findViewById<ProgressBar>(R.id.progressUpdate).progress = 100
        findViewById<TextView>(R.id.tvUpdatePercent).text = "100%"
        findViewById<TextView>(R.id.tvUpdateStatus).text = "Download complete. Opening installer..."

        if (!apkFile.exists()) {
            findViewById<TextView>(R.id.tvUpdateStatus).text = "Downloaded file not found. Please try again."
            return
        }

        try {
            val apkUri = FileProvider.getUriForFile(this, "com.ryuutools.app.fileprovider", apkFile)
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            startActivity(installIntent)
            finish()
        } catch (e: Exception) {
            findViewById<TextView>(R.id.tvUpdateStatus).text = "Couldn't open installer: ${e.message}"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingProgress = false
        try { unregisterReceiver(downloadCompleteReceiver) } catch (e: Exception) { }
    }
}