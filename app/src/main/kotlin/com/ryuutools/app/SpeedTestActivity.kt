package com.ryuutools.app

import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedInputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.random.Random

class SpeedTestActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_speed_test)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnStartTest).setOnClickListener {
    if (!NetworkUtils.isOnline(this)) {
        NetworkUtils.showOfflineWarning(this)
    } else {
        runSpeedTest()
    }
}
    }

    private fun runSpeedTest() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvPing = findViewById<TextView>(R.id.tvPing)
        val tvDownload = findViewById<TextView>(R.id.tvDownload)
        val tvUpload = findViewById<TextView>(R.id.tvUpload)
        val btnStart = findViewById<Button>(R.id.btnStartTest)

        btnStart.isEnabled = false
        progressBar.visibility = View.VISIBLE
        tvPing.text = "--"
        tvDownload.text = "--"
        tvUpload.text = "--"

        Thread {
            runOnUiThread { tvStatus.text = "Testing ping..." }
            val pingMs = measurePing()
            runOnUiThread { tvPing.text = if (pingMs >= 0) "$pingMs ms" else "Failed" }

            runOnUiThread { tvStatus.text = "Testing download speed..." }
            val downloadMbps = measureDownloadSpeed()
            runOnUiThread {
                tvDownload.text = if (downloadMbps >= 0) "%.2f Mbps".format(downloadMbps) else "Failed"
            }

            runOnUiThread { tvStatus.text = "Testing upload speed..." }
            val uploadMbps = measureUploadSpeed()
            runOnUiThread {
                tvUpload.text = if (uploadMbps >= 0) "%.2f Mbps".format(uploadMbps) else "Failed"
                tvStatus.text = "Test complete"
                progressBar.visibility = View.GONE
                btnStart.isEnabled = true
            }
        }.start()
    }

    private fun measurePing(): Long {
        return try {
            val start = SystemClock.elapsedRealtime()
            val connection = URL("https://www.google.com").openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            connection.responseCode
            val end = SystemClock.elapsedRealtime()
            connection.disconnect()
            end - start
        } catch (e: Exception) {
            Log.e("SpeedTest", "Ping error: ${e.message}")
            -1L
        }
    }

    private fun measureDownloadSpeed(): Double {
        return try {
            val connection = URL("https://speed.cloudflare.com/__down?bytes=5000000")
                .openConnection() as HttpURLConnection
            connection.connectTimeout = 8000
            connection.readTimeout = 20000
            connection.connect()

            val input = BufferedInputStream(connection.inputStream)
            val buffer = ByteArray(8192)
            var totalBytes = 0L
            val startTime = SystemClock.elapsedRealtime()

            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                totalBytes += read
            }
            val endTime = SystemClock.elapsedRealtime()
            input.close()
            connection.disconnect()

            val elapsedSeconds = (endTime - startTime) / 1000.0
            if (elapsedSeconds <= 0) return -1.0
            (totalBytes * 8) / (elapsedSeconds * 1_000_000)
        } catch (e: Exception) {
            Log.e("SpeedTest", "Download error: ${e.message}")
            -1.0
        }
    }

    private fun measureUploadSpeed(): Double {
        return try {
            val dataSize = 2 * 1024 * 1024
            val data = Random.nextBytes(dataSize)

            val connection = URL("https://httpbin.org/post").openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/octet-stream")
            connection.connectTimeout = 8000
            connection.readTimeout = 20000

            val startTime = SystemClock.elapsedRealtime()
            connection.outputStream.use { it.write(data) }
            connection.responseCode
            val endTime = SystemClock.elapsedRealtime()
            connection.disconnect()

            val elapsedSeconds = (endTime - startTime) / 1000.0
            if (elapsedSeconds <= 0) return -1.0
            (dataSize * 8) / (elapsedSeconds * 1_000_000)
        } catch (e: Exception) {
            Log.e("SpeedTest", "Upload error: ${e.message}")
            -1.0
        }
    }
}