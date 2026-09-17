package com.mporttech.pro.core.network

data class NetworkState(
    val isOnline: Boolean = false,
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val ssid: String? = null,
    val ip: String? = null,
    val gateway: String? = null,
    val dns: String? = null,
    val linkMbps: Int? = null
)
