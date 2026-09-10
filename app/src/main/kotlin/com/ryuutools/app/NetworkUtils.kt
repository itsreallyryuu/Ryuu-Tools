package com.ryuutools.app

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.widget.Toast

object NetworkUtils {

    /** True if there's an active network with general internet capability. */
    fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** True if the active network is specifically WiFi (used for local network scanning). */
    fun isOnWifi(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun showOfflineWarning(context: Context) {
        Toast.makeText(context, "You're offline, connect to a network to continue", Toast.LENGTH_LONG).show()
    }

    fun showWifiRequiredWarning(context: Context) {
        Toast.makeText(context, "Connect to a WiFi network to scan your local network", Toast.LENGTH_LONG).show()
    }
}