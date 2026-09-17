package com.mporttech.pro.features.dashboard

data class DashboardUiState(
    val loading: Boolean = true,
    val online: Boolean = false,
    val transport: String = "—",
    val ssid: String? = null,
    val ip: String? = null,
    val gateway: String? = null,
    val dns: String? = null,
    val linkMbps: Int? = null,
    val gatewayMs: Long? = null,
    val gatewayReachable: Boolean = false,
    val rxMbps: Double = 0.0,
    val txMbps: Double = 0.0,
    val error: String? = null
)
