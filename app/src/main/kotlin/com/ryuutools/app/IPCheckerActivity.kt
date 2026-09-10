package com.ryuutools.app

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.NetworkInterface
import java.net.URL
import java.util.Collections

class IPCheckerActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ip_checker)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        val localIp = getLocalIpAddress()
        findViewById<TextView>(R.id.tvLocalIpRow).text = "Your device's local IP: $localIp"

        findViewById<Button>(R.id.btnUseMyIp).setOnClickListener {
            findViewById<EditText>(R.id.etIpInput).text.clear()
            findViewById<TextView>(R.id.tvIpLabel).text = "Your Public IP Address"
            loadMyIp()
        }

        findViewById<Button>(R.id.btnCheckIp).setOnClickListener {
            val input = findViewById<EditText>(R.id.etIpInput).text.toString().trim()
            if (input.isEmpty()) {
                Toast.makeText(this, "Enter an IP address first", Toast.LENGTH_SHORT).show()
            } else if (!isValidIp(input)) {
                Toast.makeText(this, "That doesn't look like a valid IP address", Toast.LENGTH_SHORT).show()
            } else if (!NetworkUtils.isOnline(this)) {
                NetworkUtils.showOfflineWarning(this)
            } else {
                findViewById<TextView>(R.id.tvIpLabel).text = "Looked-up IP Address"
                lookupIp(input)
            }
        }

        // Default: check own public IP on open
        if (NetworkUtils.isOnline(this)) {
            loadMyIp()
        } else {
            NetworkUtils.showOfflineWarning(this)
        }
    }

    private fun isValidIp(ip: String): Boolean {
        return Patterns.IP_ADDRESS.matcher(ip).matches()
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

    private fun loadMyIp() {
        if (!NetworkUtils.isOnline(this)) {
            NetworkUtils.showOfflineWarning(this)
            return
        }
        fetchIpInfo("https://ipwho.is/")
    }

    private fun lookupIp(ip: String) {
        fetchIpInfo("https://ipwho.is/$ip")
    }

    private fun fetchIpInfo(urlString: String) {
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)
        progressBar.visibility = View.VISIBLE

        Thread {
            try {
                val url = URL(urlString)
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

                        val timezoneInfo = json.optJSONObject("timezone")
                        val timezone = timezoneInfo?.optString("id", "--") ?: "--"
                        findViewById<TextView>(R.id.tvTimezone).text = timezone

                        val ipType = json.optString("type", "--")
                        findViewById<TextView>(R.id.tvIpType).text = ipType.ifBlank { "--" }
                    } else {
                        val message = json.optString("message", "Could not fetch info for that IP")
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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