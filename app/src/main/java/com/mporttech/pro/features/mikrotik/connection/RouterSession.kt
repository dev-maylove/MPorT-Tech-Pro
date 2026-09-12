package com.mporttech.pro.features.mikrotik.connection

import com.mporttech.pro.core.security.RouterCredentials
import com.mporttech.pro.features.mikrotik.model.RouterInfo
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory session after successful auth.
 * Actual RouterOS API (port 8728) to be wired in Phase D+.
 */
@Singleton
class RouterSession @Inject constructor() {
    @Volatile var credentials: RouterCredentials? = null
        private set
    @Volatile var info: RouterInfo = RouterInfo()
        private set

    fun connect(credentials: RouterCredentials): RouterInfo {
        this.credentials = credentials
        // Placeholder: mark connected; real API handshake later
        info = RouterInfo(
            identity = credentials.host,
            boardName = "RouterOS",
            version = "pending API",
            uptime = "—",
            connected = true
        )
        return info
    }

    fun disconnect() {
        credentials = null
        info = RouterInfo()
    }

    val isConnected: Boolean get() = info.connected
}
