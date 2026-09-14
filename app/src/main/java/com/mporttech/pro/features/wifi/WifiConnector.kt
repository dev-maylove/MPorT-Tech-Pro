package com.mporttech.pro.features.wifi

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.annotation.RequiresApi

/**
 * Wi-Fi connection helper.
 *
 * Android does not expose a generic API that lets a third-party app submit an
 * unknown WPA password to the system settings UI. For secured networks we
 * therefore hand off to Settings. Open networks may use WifiNetworkSpecifier.
 */
object WifiConnector {
    private var activeCallback: ConnectivityManager.NetworkCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun connect(context: Context, ssid: String, isOpen: Boolean = false) {
        val clean = ssid.removePrefix("★ ").trim()
        if (clean.isEmpty() || clean.startsWith("<") || !isOpen) {
            openWifiSettings(context)
            return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requestOpenNetwork(context, clean)
        } else {
            openWifiSettings(context)
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestOpenNetwork(context: Context, ssid: String) {
        val cm = context.applicationContext.getSystemService(ConnectivityManager::class.java)
            ?: run { openWifiSettings(context); return }

        activeCallback?.let { previous ->
            runCatching { cm.unregisterNetworkCallback(previous) }
            activeCallback = null
        }

        val specifier = WifiNetworkSpecifier.Builder().setSsid(ssid).build()
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .setNetworkSpecifier(specifier)
            .build()

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                // Do not bind the whole process: that can unexpectedly route all
                // application traffic through a local-only specifier network.
            }
            override fun onUnavailable() = cleanup(cm, this)
            override fun onLost(network: Network) = cleanup(cm, this)
        }
        activeCallback = callback
        try {
            cm.requestNetwork(request, callback, mainHandler, 30_000)
        } catch (_: Exception) {
            cleanup(cm, callback)
            openWifiSettings(context)
            return
        }
        mainHandler.postDelayed {
            if (activeCallback === callback) cleanup(cm, callback)
        }, 45_000)
    }

    private fun cleanup(cm: ConnectivityManager, callback: ConnectivityManager.NetworkCallback) {
        runCatching { cm.unregisterNetworkCallback(callback) }
        if (activeCallback === callback) activeCallback = null
    }

    fun openWifiSettings(context: Context) {
        val intents = listOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) Intent(Settings.Panel.ACTION_WIFI) else null,
            Intent(Settings.ACTION_WIFI_SETTINGS),
            Intent(android.net.wifi.WifiManager.ACTION_PICK_WIFI_NETWORK)
        )
        for (intent in intents) {
            if (intent == null) continue
            try {
                if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                return
            } catch (_: Exception) {
            }
        }
    }
}
