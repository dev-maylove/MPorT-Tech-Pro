package com.mporttech.pro.core.network

import android.content.Context
import com.mporttech.pro.features.tools.LiveNetworkInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkInfoProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun snapshot(): NetworkState {
        val s = LiveNetworkInfo.snapshot(context)
        return NetworkState(
            isOnline = s.online,
            isWifi = s.transport.contains("Wi", ignoreCase = true),
            isCellular = s.transport.contains("Cellular", ignoreCase = true),
            ssid = s.ssid,
            ip = s.ip,
            gateway = s.gateway,
            dns = s.dns,
            linkMbps = s.linkMbps
        )
    }
}
