package com.mporttech.pro.features.tools

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import com.mporttech.pro.features.scanner.AuthorizedNetworkScanner
import com.mporttech.pro.features.scanner.ScanHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class LiveLinkInfo(
    val online: Boolean,
    val transport: String,
    val ssid: String?,
    val ip: String?,
    val gateway: String?,
    val dns: String?,
    val linkMbps: Int?,
    val rxBytes: Long,
    val txBytes: Long
)

data class LiveDevice(
    val name: String,
    val ip: String,
    val online: Boolean,
    val latencyMs: Long?,
    val kind: String // router | ap | switch | host | gateway
)

/** Holds last selected device for detail screen (simple in-process store). */
object SelectedDeviceStore {
    @Volatile var ip: String = "192.168.1.1"
    @Volatile var name: String = "Gateway"
    @Volatile var kind: String = "gateway"
}

object LiveNetworkInfo {

    fun snapshot(context: Context): LiveLinkInfo {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val network = cm.activeNetwork
        val caps = network?.let { cm.getNetworkCapabilities(it) }
        val online = caps != null && (
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            )
        val transport = when {
            caps == null -> "Offline"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
            else -> "Other"
        }
        val ssid = try {
            @Suppress("DEPRECATION")
            wm.connectionInfo?.ssid?.removeSurrounding("\"")
                ?.takeIf { it.isNotBlank() && !it.contains("unknown", true) }
        } catch (_: Exception) {
            null
        }
        val ip = primaryIpv4()
        val gateway = defaultGateway(wm)
        val dns = firstDns(cm)
        val link = try {
            @Suppress("DEPRECATION")
            wm.connectionInfo?.linkSpeed?.takeIf { it > 0 }
        } catch (_: Exception) {
            null
        }
        return LiveLinkInfo(
            online = online,
            transport = transport,
            ssid = ssid,
            ip = ip,
            gateway = gateway,
            dns = dns,
            linkMbps = link,
            rxBytes = TrafficStats.getTotalRxBytes().coerceAtLeast(0),
            txBytes = TrafficStats.getTotalTxBytes().coerceAtLeast(0)
        )
    }

    fun interfaceNames(): List<String> {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.filter { it.isUp && !it.isLoopback }
                ?.map { ni ->
                    val addrs = ni.inetAddresses.toList()
                        .filterIsInstance<Inet4Address>()
                        .joinToString { it.hostAddress ?: "" }
                    "${ni.name}  ${if (addrs.isBlank()) "" else "· $addrs"}"
                }
                ?.sorted()
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun probe(ip: String, timeoutMs: Int = 800): Pair<Boolean, Long?> = withContext(Dispatchers.IO) {
        val start = SystemClock.elapsedRealtime()
        val ports = intArrayOf(80, 443, 22, 53, 8728, 8291, 8080)
        for (port in ports) {
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(ip, port), timeoutMs)
                    val ms = SystemClock.elapsedRealtime() - start
                    return@withContext true to ms
                }
            } catch (_: Exception) {
            }
        }
        val ok = try {
            InetAddress.getByName(ip).isReachable(timeoutMs)
        } catch (_: Exception) {
            false
        }
        val ms = if (ok) SystemClock.elapsedRealtime() - start else null
        ok to ms
    }

    /**
     * Discover live hosts on the current private /24 (requires authorized=true).
     * Always includes gateway first.
     */
    suspend fun discoverDevices(context: Context, authorized: Boolean): List<LiveDevice> =
        withContext(Dispatchers.IO) {
            val snap = snapshot(context)
            val gateway = snap.gateway
            val base = gateway?.substringBeforeLast('.')
                ?: snap.ip?.substringBeforeLast('.')
                ?: return@withContext emptyList()

            val found = LinkedHashMap<String, LiveDevice>()
            if (!gateway.isNullOrBlank()) {
                val (ok, ms) = probe(gateway, 600)
                found[gateway] = LiveDevice(
                    name = "Gateway",
                    ip = gateway,
                    online = ok,
                    latencyMs = ms,
                    kind = "gateway"
                )
            }

            if (!authorized) {
                return@withContext found.values.toList()
            }

            // Bounded scan: .1–.40 first (typical infra), then sample more if needed
            val scanner = AuthorizedNetworkScanner()
            val hosts: List<ScanHost> = try {
                scanner.scan(
                    base = base,
                    startHost = 1,
                    endHost = 40,
                    timeoutMs = 200,
                    authorized = true
                )
            } catch (_: Exception) {
                emptyList()
            }

            for (h in hosts) {
                if (found.containsKey(h.address)) continue
                val kind = guessKind(h.address, gateway)
                found[h.address] = LiveDevice(
                    name = defaultName(kind, h.address),
                    ip = h.address,
                    online = h.reachable,
                    latencyMs = h.latencyMs,
                    kind = kind
                )
            }
            found.values.sortedWith(compareByDescending<LiveDevice> { it.online }.thenBy { it.ip })
        }

    suspend fun measureTrafficDeltaMbps(sampleMs: Long = 1200L): Pair<Double, Double> =
        withContext(Dispatchers.IO) {
            val rx0 = TrafficStats.getTotalRxBytes()
            val tx0 = TrafficStats.getTotalTxBytes()
            if (rx0 < 0 || tx0 < 0) return@withContext 0.0 to 0.0
            Thread.sleep(sampleMs)
            val rx1 = TrafficStats.getTotalRxBytes()
            val tx1 = TrafficStats.getTotalTxBytes()
            val sec = sampleMs / 1000.0
            val rxMbps = ((rx1 - rx0).coerceAtLeast(0) * 8.0) / sec / 1_000_000.0
            val txMbps = ((tx1 - tx0).coerceAtLeast(0) * 8.0) / sec / 1_000_000.0
            rxMbps to txMbps
        }

    private fun primaryIpv4(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.flatMap { it.inetAddresses.toList() }
                ?.filterIsInstance<Inet4Address>()
                ?.firstOrNull { !it.isLoopbackAddress }
                ?.hostAddress
        } catch (_: Exception) {
            null
        }
    }

    private fun defaultGateway(wm: WifiManager): String? {
        return try {
            @Suppress("DEPRECATION")
            val gw = wm.dhcpInfo?.gateway ?: return null
            if (gw == 0) return null
            formatIp(gw)
        } catch (_: Exception) {
            null
        }
    }

    private fun firstDns(cm: ConnectivityManager): String? {
        return try {
            if (Build.VERSION.SDK_INT >= 23) {
                val lp = cm.getLinkProperties(cm.activeNetwork) ?: return null
                lp.dnsServers.firstOrNull()?.hostAddress
            } else null
        } catch (_: Exception) {
            null
        }
    }

    private fun formatIp(ip: Int): String =
        "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"

    private fun guessKind(ip: String, gateway: String?): String {
        val host = ip.substringAfterLast('.').toIntOrNull() ?: return "host"
        return when {
            ip == gateway || host == 1 -> "gateway"
            host in 2..9 -> "switch"
            host in 10..49 -> "ap"
            else -> "host"
        }
    }

    private fun defaultName(kind: String, ip: String): String = when (kind) {
        "gateway" -> "Gateway / Router"
        "switch" -> "Switch $ip"
        "ap" -> "Access Point $ip"
        else -> "Host $ip"
    }
}
