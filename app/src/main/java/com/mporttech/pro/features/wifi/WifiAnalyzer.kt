package com.mporttech.pro.features.wifi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.os.SystemClock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class WifiNetworkInfo(
    val ssid: String,
    val bssid: String,
    val rssiDbm: Int,
    val frequencyMhz: Int,
    val channel: Int,
    val security: String,
    val widthMhz: Int = 20,
    val band: Band = Band.UNKNOWN,
    val qualityScore: Int = 0,          // 0..100
    val estimatedMbps: Int = 0,         // rough PHY estimate from RSSI/band/width
    val isConnected: Boolean = false
)

enum class Band { GHZ_24, GHZ_5, GHZ_6, UNKNOWN }

data class ChannelStat(
    val channel: Int,
    val band: Band,
    val networkCount: Int,
    val strongestRssi: Int,
    val congestionScore: Int            // higher = more congested
)

data class WifiScanSnapshot(
    val networks: List<WifiNetworkInfo>,
    val channels24: List<ChannelStat>,
    val channels5: List<ChannelStat>,
    val recommended24: Int?,
    val recommended5: Int?,
    val fromCache: Boolean,
    val scanDurationMs: Long
)

/**
 * Optimized WiFi analyzer:
 * - scan throttle + in-memory cache (Android rate-limits startScan)
 * - fast path: return cache if fresh, refresh in background pattern
 * - BSSID dedupe, quality scoring, channel congestion
 * - estimated link speed from RSSI / band / channel width
 */
class WifiAnalyzer(private val context: Context) {
    private val wifi: WifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

    companion object {
        private const val CACHE_TTL_MS = 8_000L
        private const val MIN_SCAN_INTERVAL_MS = 6_000L
        private const val DEFAULT_TIMEOUT_MS = 6_500L
        private val lastScanAt = AtomicLong(0L)
        private val cacheRef = AtomicReference<WifiScanSnapshot?>(null)
    }

    suspend fun scan(
        force: Boolean = false,
        timeoutMs: Long = DEFAULT_TIMEOUT_MS
    ): WifiScanSnapshot = withContext(Dispatchers.IO) {
        val started = SystemClock.elapsedRealtime()
        val now = SystemClock.elapsedRealtime()
        val cached = cacheRef.get()
        val age = cached?.let { now - lastScanAt.get() } ?: Long.MAX_VALUE

        // Fast path: fresh cache
        if (!force && cached != null && age < CACHE_TTL_MS) {
            return@withContext cached.copy(fromCache = true, scanDurationMs = 0)
        }

        // Throttle startScan — Android limits ~4 scans / 2 minutes on many devices
        val sinceLast = now - lastScanAt.get()
        val canStart = force || sinceLast >= MIN_SCAN_INTERVAL_MS

        val connectedBssid = currentBssid()
        val connectedSsid = currentSsid()

        var results: List<ScanResult> = emptyList()
        if (canStart) {
            results = withTimeoutOrNull(timeoutMs) { awaitScanResults() } ?: emptyList()
            if (results.isNotEmpty()) lastScanAt.set(SystemClock.elapsedRealtime())
        }
        if (results.isEmpty()) {
            results = wifi.scanResults ?: emptyList()
        }

        val networks = mapAndEnrich(results, connectedBssid, connectedSsid)
        val ch24 = buildChannelStats(networks, Band.GHZ_24)
        val ch5 = buildChannelStats(networks, Band.GHZ_5)
        val snap = WifiScanSnapshot(
            networks = networks,
            channels24 = ch24,
            channels5 = ch5,
            recommended24 = pickBestChannel(ch24, preferred = listOf(1, 6, 11)),
            recommended5 = pickBestChannel(ch5, preferred = listOf(36, 40, 44, 48, 149, 153, 157, 161)),
            fromCache = results.isEmpty() && cached != null,
            scanDurationMs = SystemClock.elapsedRealtime() - started
        )
        if (networks.isNotEmpty()) {
            cacheRef.set(snap)
            if (lastScanAt.get() == 0L) lastScanAt.set(SystemClock.elapsedRealtime())
        }
        snap
    }

    fun cachedSnapshot(): WifiScanSnapshot? = cacheRef.get()

    fun currentConnection(): WifiNetworkInfo? {
        return try {
            @Suppress("DEPRECATION")
            val info = wifi.connectionInfo ?: return null
            val ssid = info.ssid?.removeSurrounding("\"")
                ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" } ?: return null
            val freq = info.frequency
            val rssi = info.rssi
            val band = bandOf(freq)
            val ch = channelFromFrequency(freq)
            val width = 20
            WifiNetworkInfo(
                ssid = ssid,
                bssid = info.bssid ?: "",
                rssiDbm = rssi,
                frequencyMhz = freq,
                channel = ch,
                security = "Connected",
                widthMhz = width,
                band = band,
                qualityScore = qualityFromRssi(rssi),
                estimatedMbps = estimateMbps(rssi, band, width),
                isConnected = true
            )
        } catch (_: Exception) {
            null
        }
    }

    fun isWifiEnabled(): Boolean = try {
        wifi.isWifiEnabled
    } catch (_: Exception) {
        false
    }

    @Suppress("DEPRECATION")
    fun setWifiEnabled(enabled: Boolean): Boolean = try {
        wifi.isWifiEnabled = enabled
        true
    } catch (_: Exception) {
        false
    }

    private fun currentBssid(): String? = try {
        @Suppress("DEPRECATION")
        wifi.connectionInfo?.bssid
    } catch (_: Exception) {
        null
    }

    private fun currentSsid(): String? = try {
        @Suppress("DEPRECATION")
        wifi.connectionInfo?.ssid?.removeSurrounding("\"")
            ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
    } catch (_: Exception) {
        null
    }

    private suspend fun awaitScanResults(): List<ScanResult> =
        suspendCancellableCoroutine { cont ->
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context?, intent: Intent?) {
                    if (intent?.action != WifiManager.SCAN_RESULTS_AVAILABLE_ACTION) return
                    try {
                        context.applicationContext.unregisterReceiver(this)
                    } catch (_: Exception) {
                    }
                    if (cont.isActive) {
                        cont.resume(wifi.scanResults ?: emptyList())
                    }
                }
            }
            val appCtx = context.applicationContext
            try {
                val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
                if (Build.VERSION.SDK_INT >= 33) {
                    appCtx.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    appCtx.registerReceiver(receiver, filter)
                }
                try {
                    @Suppress("DEPRECATION")
                    wifi.startScan()
                } catch (_: Exception) {
                }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(wifi.scanResults ?: emptyList())
            }
            cont.invokeOnCancellation {
                try {
                    appCtx.unregisterReceiver(receiver)
                } catch (_: Exception) {
                }
            }
        }

    private fun mapAndEnrich(
        results: List<ScanResult>,
        connectedBssid: String?,
        connectedSsid: String?
    ): List<WifiNetworkInfo> {
        if (results.isEmpty()) return emptyList()
        // Dedupe by BSSID, keep strongest
        val best = LinkedHashMap<String, ScanResult>(results.size)
        for (r in results) {
            val key = r.BSSID ?: continue
            val prev = best[key]
            if (prev == null || r.level > prev.level) best[key] = r
        }
        return best.values.map { result ->
            val freq = result.frequency
            val band = bandOf(freq)
            val width = channelWidthMhz(result)
            val rssi = result.level
            val bssid = result.BSSID ?: ""
            val ssid = result.SSID?.ifBlank { "<Hidden SSID>" } ?: "<Hidden SSID>"
            WifiNetworkInfo(
                ssid = ssid,
                bssid = bssid,
                rssiDbm = rssi,
                frequencyMhz = freq,
                channel = channelFromFrequency(freq),
                security = simplifySecurity(result.capabilities ?: ""),
                widthMhz = width,
                band = band,
                qualityScore = qualityFromRssi(rssi),
                estimatedMbps = estimateMbps(rssi, band, width),
                isConnected = (connectedBssid != null && bssid.equals(connectedBssid, true)) ||
                    (connectedSsid != null && ssid == connectedSsid)
            )
        }.sortedWith(
            compareByDescending<WifiNetworkInfo> { it.isConnected }
                .thenByDescending { it.rssiDbm }
        )
    }

    private fun buildChannelStats(networks: List<WifiNetworkInfo>, band: Band): List<ChannelStat> {
        val grouped = networks.filter { it.band == band }.groupBy { it.channel }
        return grouped.map { (ch, list) ->
            val strongest = list.maxOf { it.rssiDbm }
            // congestion: count + strong neighbors weight
            val congestion = min(100, list.size * 18 + max(0, strongest + 70))
            ChannelStat(ch, band, list.size, strongest, congestion)
        }.sortedBy { it.channel }
    }

    private fun pickBestChannel(stats: List<ChannelStat>, preferred: List<Int>): Int? {
        if (stats.isEmpty()) return preferred.firstOrNull()
        val occupied = stats.associateBy { it.channel }
        // Prefer classic non-overlapping channels with lowest congestion
        val candidates = preferred.ifEmpty { stats.map { it.channel } }
        return candidates.minByOrNull { ch ->
            val self = occupied[ch]?.congestionScore ?: 0
            // adjacent channel interference (2.4 mainly)
            val adj = listOf(ch - 1, ch + 1).sumOf { occupied[it]?.congestionScore ?: 0 } / 2
            self * 2 + adj
        }
    }

    private fun channelWidthMhz(result: ScanResult): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                when (result.channelWidth) {
                    ScanResult.CHANNEL_WIDTH_20MHZ -> 20
                    ScanResult.CHANNEL_WIDTH_40MHZ -> 40
                    ScanResult.CHANNEL_WIDTH_80MHZ -> 80
                    ScanResult.CHANNEL_WIDTH_160MHZ -> 160
                    ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> 80
                    else -> 20
                }
            } else 20
        } catch (_: Exception) {
            20
        }
    }

    private fun simplifySecurity(cap: String): String {
        val c = cap.uppercase()
        return when {
            "WPA3" in c -> "WPA3"
            "WPA2" in c -> "WPA2"
            "WPA" in c -> "WPA"
            "WEP" in c -> "WEP"
            "EAP" in c -> "Enterprise"
            else -> "Open"
        }
    }

    private fun bandOf(f: Int): Band = when {
        f in 2400..2500 -> Band.GHZ_24
        f in 4900..5900 -> Band.GHZ_5
        f in 5925..7125 -> Band.GHZ_6
        else -> Band.UNKNOWN
    }

    private fun channelFromFrequency(f: Int): Int = when {
        f in 2412..2484 -> if (f == 2484) 14 else (f - 2407) / 5
        f in 5000..5900 -> (f - 5000) / 5
        f in 5955..7115 -> (f - 5955) / 5 + 1
        else -> -1
    }

    private fun qualityFromRssi(rssi: Int): Int {
        // Map -90..-30 → 0..100
        val q = ((rssi + 90) * (100.0 / 60.0)).toInt()
        return q.coerceIn(0, 100)
    }

    /**
     * Rough application-layer estimate (not true PHY rate).
     * Used to show expected throughput before running full speed test.
     */
    private fun estimateMbps(rssi: Int, band: Band, widthMhz: Int): Int {
        val base = when {
            rssi >= -45 -> 1.0
            rssi >= -55 -> 0.78
            rssi >= -65 -> 0.55
            rssi >= -75 -> 0.32
            rssi >= -85 -> 0.15
            else -> 0.06
        }
        val bandCap = when (band) {
            Band.GHZ_24 -> 90.0
            Band.GHZ_5 -> 320.0
            Band.GHZ_6 -> 480.0
            Band.UNKNOWN -> 120.0
        }
        val widthFactor = when {
            widthMhz >= 160 -> 1.9
            widthMhz >= 80 -> 1.45
            widthMhz >= 40 -> 1.2
            else -> 1.0
        }
        return (bandCap * base * widthFactor).toInt().coerceAtLeast(1)
    }
}
