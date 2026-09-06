package com.ryuutools.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL
import java.util.Collections

class IPCheckerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ip_checker)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<Button>(R.id.btnRefresh).setOnClickListener { loadData() }

        loadData()
    }

    private fun loadData() {
        findViewById<TextView>(R.id.tvLocalIp).text = getLocalIpAddress()
        fetchPublicIpInfo()
    }

    private fun getLocalIpAddress(): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr.hostAddress?.contains(":") == false) {
                        return addr.hostAddress ?: "Unknown"
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("IPChecker", "Local IP error: ${e.message}")
        }
        return "Unknown"
    }

    private fun fetchPublicIpInfo() {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        Thread {
            try {
                val url = URL("https://ipwho.is/")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                val success = json.optBoolean("success", false)

                runOnUiThread {
                    progressBar.visibility = View.GONE
                    if (success) {
                        findViewById<TextView>(R.id.tvPublicIp).text = json.optString("ip", "--")
                        findViewById<TextView>(R.id.tvCity).text = json.optString("city", "--")
                        findViewById<TextView>(R.id.tvRegion).text = json.optString("region", "--")
                        findViewById<TextView>(R.id.tvCountry).text = json.optString("country", "--")

                        val connectionInfo = json.optJSONObject("connection")
                        val isp = connectionInfo?.optString("isp", "--") ?: "--"
                        findViewById<TextView>(R.id.tvIsp).text = isp
                    } else {
                        Toast.makeText(this, "Could not fetch public IP info", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("IPChecker", "Fetch error: ${e.message}")
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Failed to fetch. Check your internet connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }
}