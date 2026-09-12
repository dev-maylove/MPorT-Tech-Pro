package com.mporttech.pro.features.wifi

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.net.wifi.WifiNetworkSuggestion
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * Opens the system Wi‑Fi password / connect sheet for a given SSID
 * (same system UI as Settings → Wi‑Fi → network → Password + CONNECT).
 *
 * Android 10+ uses [WifiNetworkSpecifier] + [ConnectivityManager.requestNetwork],
 * which shows the native dialog with Password, Advanced options, CANCEL, CONNECT.
 */
object WifiConnector {
    private var activeCallback: ConnectivityManager.NetworkCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * @param ssid network name
     * @param isOpen true if network has no password (Open)
     */
    fun connect(context: Context, ssid: String, isOpen: Boolean = false) {
        val clean = ssid.removePrefix("★ ").trim()
        if (clean.isEmpty() || clean.startsWith("<")) {
            openWifiSettings(context)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestWithSpecifier(context, clean, isOpen)
        } else {
            openWifiSettings(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestWithSpecifier(context: Context, ssid: String, isOpen: Boolean) {
        val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)
            ?: run {
                openWifiSettings(context)
                return
            }

        // Cancel previous request so only one system sheet is active
        activeCallback?.let {
            try {
                cm.unregisterNetworkCallback(it)
            } catch (_: Exception) {
            }
            activeCallback = null
        }

        val builder = WifiNetworkSpecifier.Builder().setSsid(ssid)
        // Open networks: do not set passphrase (system sheet may connect immediately)
        // Secured networks: omit passphrase so system asks user (Password field)

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(builder.build())
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Optional: bind process to this network while connected via specifier
                try {
                    cm.bindProcessToNetwork(network)
                } catch (_: Exception) {
                }
            }

            override fun onUnavailable() {
                cleanup(cm, this)
            }

            override fun onLost(network: Network) {
                cleanup(cm, this)
            }
        }
        activeCallback = callback
        try {
            // Shows native system dialog (Password / CONNECT) — same as image 2
            cm.requestNetwork(request, callback, mainHandler, 30_000)
        } catch (_: Exception) {
            // Fallback: suggestions panel / settings
            trySuggest(context, ssid)
        }

        // Safety unregister after 45s if still pending
        mainHandler.postDelayed({
            if (activeCallback === callback) cleanup(cm, callback)
        }, 45_000)
    }

    private fun cleanup(cm: ConnectivityManager, callback: ConnectivityManager.NetworkCallback) {
        try {
            cm.unregisterNetworkCallback(callback)
        } catch (_: Exception) {
        }
        try {
            cm.bindProcessToNetwork(null)
        } catch (_: Exception) {
        }
        if (activeCallback === callback) activeCallback = null
    }

    private fun trySuggest(context: Context, ssid: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val suggestion = WifiNetworkSuggestion.Builder()
                    .setSsid(ssid)
                    .setIsAppInteractionRequired(true)
                    .build()
                val wm = context.applicationContext.getSystemService(android.net.wifi.WifiManager::class.java)
                wm?.addNetworkSuggestions(listOf(suggestion))
            } catch (_: Exception) {
            }
        }
        openWifiSettings(context)
    }

    fun openWifiSettings(context: Context) {
        val intents = listOf(
            // Android 10+ quick panel
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                Intent(Settings.Panel.ACTION_WIFI) else null,
            Intent(Settings.ACTION_WIFI_SETTINGS),
            Intent(android.net.wifi.WifiManager.ACTION_PICK_WIFI_NETWORK)
        )
        for (intent in intents) {
            if (intent == null) continue
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
    }
}
