package com.mporttech.pro.domain.model

data class NetworkInfo(
    val ssid: String? = null,
    val ip: String? = null,
    val gateway: String? = null,
    val dns: String? = null,
    val linkSpeedMbps: Int? = null,
    val frequencyMhz: Int? = null,
    val rssiDbm: Int? = null
)
