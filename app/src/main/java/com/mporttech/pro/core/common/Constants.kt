package com.mporttech.pro.core.common

import com.mporttech.pro.BuildConfig

object Constants {
    const val APP_NAME = "MPorT Tech Pro"
    const val PACKAGE_NAME = "com.mporttech.pro"

    // Network defaults
    const val DEFAULT_PING_TIMEOUT_SEC = 2
    const val DEFAULT_PORT_TIMEOUT_MS = 800
    const val LAN_SCAN_HOST_END = 40
    const val SPEED_HISTORY_MAX = 30

    // Prefs
    const val PREFS_APP = "mport_app"
    const val PREFS_SPEED = "mport_speed_history"
    const val PREFS_LANG = "mport_lang"

    /**
     * Backend base URL — production API (override via BuildConfig / -PapiBaseUrl).
     * Must end with trailing slash for Retrofit.
     * Override per build via BuildConfig if needed.
     */
    val API_BASE_URL: String = BuildConfig.API_BASE_URL.ifBlank {
        "https://api.mandalanet.id/"
    }

    /** Offline demo login (debug). */
    val ALLOW_OFFLINE_DEMO_LOGIN: Boolean = BuildConfig.ALLOW_OFFLINE_DEMO_LOGIN

    val ENABLE_CERT_PINNING: Boolean = BuildConfig.ENABLE_CERT_PINNING
}
