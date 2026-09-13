package com.mporttech.pro.features.discovery

import android.annotation.SuppressLint
import android.content.Context
import android.net.wifi.WifiManager
import android.os.SystemClock
import com.mporttech.pro.features.tools.LiveNetworkInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap

data class DiscoveredNode(
    val ip: String,
    val name: String,
    val vendor: String?,
    val kind: String, // gateway | ap | host | me
    val latencyMs: Long?,
    val online: Boolean,
    val isMe: Boolean = false
)

data class LatencySample(
    val target: String,
    val label: String,
    val ip: String?,
    val ms: Long?,
    val ok: Boolean,
    val history: List<Long>
)

data class SignalSample(
    val rssi: Int,
    val linkMbps: Int?,
    val ssid: String?,
    val bssid: String?,
    val frequency: Int?,
    val timestamp: Long = System.currentTimeMillis()
)

object VendorLookup {
    private val oui = mapOf(
        "00:0C:42" to "MikroTik",
        "4C:5E:0C" to "MikroTik",
        "48:A9:8A" to "TP-Link",
        "50:C7:BF" to "TP-Link",
        "14:CC:20" to "TP-Link",
        "C0:25:E9" to "TP-Link",
        "B0:4E:26" to "TP-Link",
        "A0:F3:C1" to "TP-Link",
        "EC:08:6B" to "TP-Link",
        "18:D6:C7" to "TP-Link",
        "F4:F2:6D" to "TP-Link",
        "00:1A:2B" to "Huawei",
        "00:25:9E" to "Huawei",
        "C8:D7:79" to "Huawei",
        "28:6E:D4" to "Huawei",
        "00:22:15" to "Huawei",
        "E4:46:DA" to "Huawei",
        "00:17:7C" to "Ubiquiti",
        "24:A4:3C" to "Ubiquiti",
        "44:D9:E7" to "Ubiquiti",
        "FC:EC:DA" to "Ubiquiti",
        "B4:FB:E4" to "Ubiquiti",
        "18:E8:29" to "Ubiquiti",
        "00:15:6D" to "Ubiquiti",
        "78:8A:20" to "Ubiquiti",
        "00:1E:8C" to "Samsung",
        "8C:79:F5" to "Samsung",
        "AC:5A:FC" to "Intel",
        "3C:22:FB" to "Apple",
        "F0:18:98" to "Apple",
        "A4:83:E7" to "Apple",
        "00:1A:11" to "Google",
        "54:60:09" to "Google"
    )

    fun fromMac(mac: String?): String? {
        if (mac.isNullOrBlank()) return null
        val norm = mac.uppercase().replace('-', ':')
        val prefix = norm.split(":").take(3).joinToString(":")
        return oui[prefix]
    }

    fun guessFromName(name: String): String? {
        val n = name.lowercase()
        return when {
            "tp-link" in n || "tl-" in n || "archer" in n -> "TP-Link"
            "mikrotik" in n || "hap" in n || "ccr" in n || "rb" in n -> "MikroTik"
            "huawei" in n || "b860" in n || "hg8" in n -> "Huawei"
            "unifi" in n || "ubiquiti" in n || "uap" in n -> "Ubiquiti"
            "android" in n || "pixel" in n || "galaxy" in n -> "Phone"
            else -> null
        }
    }
}

object DiscoveryEngine {

    private val latencyHistory = ConcurrentHashMap<String, ArrayDeque<Long>>()

    fun pushHistory(key: String, ms: Long, max: Int = 24) {
        val q = latencyHistory.getOrPut(key) { ArrayDeque() }
        synchronized(q) {
            q.addLast(ms)
            while (q.size > max) q.removeFirst()
        }
    }

    fun history(key: String): List<Long> =
        latencyHistory[key]?.toList() ?: emptyList()

    suspend fun resolveName(ip: String): String = withContext(Dispatchers.IO) {
        try {
            val host = InetAddress.getByName(ip).canonicalHostName
            if (host.isNullOrBlank() || host == ip) ip else host.substringBefore('.')
        } catch (_: Exception) {
            ip
        }
    }

    suspend fun tcpPing(host: String, port: Int = 443, timeoutMs: Int = 1200): Long? =
        withContext(Dispatchers.IO) {
            val start = SystemClock.elapsedRealtime()
            try {
                Socket().use { s ->
                    s.connect(InetSocketAddress(host, port), timeoutMs)
                    SystemClock.elapsedRealtime() - start
                }
            } catch (_: Exception) {
                try {
                    val ok = InetAddress.getByName(host).isReachable(timeoutMs)
                    if (ok) SystemClock.elapsedRealtime() - start else null
                } catch (_: Exception) {
                    null
                }
            }
        }

    suspend fun discover(context: Context, authorized: Boolean): List<DiscoveredNode> =
        withContext(Dispatchers.IO) {
            val snap = LiveNetworkInfo.snapshot(context)
            val myIp = snap.ip
            val gateway = snap.gateway
            val base = gateway?.substringBeforeLast('.')
                ?: myIp?.substringBeforeLast('.')
                ?: return@withContext emptyList()

            val nodes = LinkedHashMap<String, DiscoveredNode>()

            suspend fun add(ip: String, kind: String, preferName: String? = null) {
                if (nodes.containsKey(ip)) return
                val ms = tcpPing(ip, 80, 600) ?: tcpPing(ip, 443, 500)
                val online = ms != null
                if (ms != null) pushHistory(ip, ms)
                val resolved = preferName ?: try {
                    resolveName(ip)
                } catch (_: Exception) {
                    ip
                }
                val name = if (resolved == ip) {
                    when (kind) {
                        "gateway" -> "Gateway"
                        "me" -> android.os.Build.MODEL ?: "This device"
                        else -> "Host ${ip.substringAfterLast('.')}"
                    }
                } else resolved
                val vendor = VendorLookup.guessFromName(name)
                nodes[ip] = DiscoveredNode(
                    ip = ip,
                    name = name,
                    vendor = vendor,
                    kind = kind,
                    latencyMs = ms,
                    online = online,
                    isMe = kind == "me"
                )
            }

            if (!gateway.isNullOrBlank()) add(gateway, "gateway", preferName = null)
            if (!myIp.isNullOrBlank()) add(myIp, "me", preferName = android.os.Build.MODEL)

            if (authorized) {
                val live = LiveNetworkInfo.discoverDevices(context, authorized = true)
                for (d in live) {
                    if (nodes.containsKey(d.ip)) continue
                    val name = try {
                        resolveName(d.ip)
                    } catch (_: Exception) {
                        d.name
                    }
                    if (d.latencyMs != null) pushHistory(d.ip, d.latencyMs)
                    nodes[d.ip] = DiscoveredNode(
                        ip = d.ip,
                        name = if (name == d.ip) d.name else name,
                        vendor = VendorLookup.guessFromName(name),
                        kind = d.kind,
                        latencyMs = d.latencyMs,
                        online = d.online
                    )
                }
            }

            nodes.values.sortedWith(
                compareByDescending<DiscoveredNode> { it.kind == "gateway" }
                    .thenByDescending { it.isMe }
                    .thenByDescending { it.online }
                    .thenBy { it.ip }
            )
        }

    @SuppressLint("MissingPermission")
    fun signalFlow(context: Context): Flow<SignalSample> = flow {
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        while (true) {
            val sample = try {
                @Suppress("DEPRECATION")
                val info = wm.connectionInfo
                SignalSample(
                    rssi = info?.rssi ?: -100,
                    linkMbps = info?.linkSpeed?.takeIf { it > 0 },
                    ssid = info?.ssid?.removeSurrounding("\"")?.takeIf {
                        it.isNotBlank() && !it.contains("unknown", true)
                    },
                    bssid = info?.bssid,
                    frequency = info?.frequency?.takeIf { it > 0 }
                )
            } catch (_: Exception) {
                SignalSample(-100, null, null, null, null)
            }
            emit(sample)
            delay(1000)
        }
    }

    suspend fun probeTargets(context: Context): List<LatencySample> = coroutineScope {
        val snap = LiveNetworkInfo.snapshot(context)
        val targets = listOf(
            Triple("google.com", "Google", 443),
            Triple("facebook.com", "Facebook", 443),
            Triple("x.com", "X", 443),
            Triple(snap.gateway ?: "1.1.1.1", "Gateway", 80)
        )
        targets.map { (host, label, port) ->
            async {
                val ms = tcpPing(host, port, 1500)
                if (ms != null) pushHistory(host, ms)
                val resolvedIp = try {
                    InetAddress.getByName(host).hostAddress
                } catch (_: Exception) {
                    null
                }
                LatencySample(
                    target = host,
                    label = label,
                    ip = if (label == "Gateway") host else resolvedIp,
                    ms = ms,
                    ok = ms != null,
                    history = history(host)
                )
            }
        }.awaitAll()
    }
}
