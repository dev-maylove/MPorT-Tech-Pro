package com.mporttech.pro.data.repository

import android.content.Context
import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.model.NetworkDevice
import com.mporttech.pro.domain.model.NetworkInfo
import com.mporttech.pro.domain.repository.NetworkRepository
import com.mporttech.pro.features.tools.LiveNetworkInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : NetworkRepository {

    override suspend fun getNetworkInfo(): Result<NetworkInfo> = try {
        val s = LiveNetworkInfo.snapshot(context)
        Result.Success(
            NetworkInfo(
                online = s.online,
                transport = s.transport,
                ssid = s.ssid,
                ip = s.ip,
                gateway = s.gateway,
                dns = s.dns,
                linkSpeedMbps = s.linkMbps,
                frequencyMhz = null,
                rssiDbm = null
            )
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(e.message ?: "Network info failed", e)
    }

    override suspend fun scanDevices(authorized: Boolean): Result<List<NetworkDevice>> = try {
        val list = LiveNetworkInfo.discoverDevices(context, authorized)
        Result.Success(
            list.map {
                NetworkDevice(
                    name = it.name,
                    ip = it.ip,
                    mac = it.mac,
                    vendor = it.vendor,
                    online = it.online,
                    latencyMs = it.latencyMs,
                    kind = it.kind
                )
            }
        )
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.Error(e.message ?: "Scan failed", e)
    }
}
