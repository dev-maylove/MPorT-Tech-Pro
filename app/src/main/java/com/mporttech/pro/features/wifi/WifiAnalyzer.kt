package com.mporttech.pro.features.wifi

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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
 * High-performance WiFi analyzer:
 * 1) Instant path: system scanResults / memory cache (0–20 ms)
 * 2) Throttled startScan (Android rate-limit ~4 / 2 min)
 * 3) Short await timeout; never block full 6s if results already present
 * 4) Single-pass BSSID dedupe + channel stats
 * 5) Stale-while-revalidate for UI snappiness
 */
@SuppressLint("MissingPermission")
class WifiAnalyzer(private val context: Context) {
    private val wifi: WifiManager =
        context.applicationContext.getSystemService(WifiManager::class.java)
            ?: throw IllegalStateException("Wi-Fi service unavailable")


    private fun hasScanPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            val nearby = ContextCompat.checkSelfPermission(
                context, Manifest.permission.NEARBY_WIFI_DEVICES
            ) == PackageManager.PERMISSION_GRANTED
            if (nearby) return true
        }
        val fine = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /** Lint-safe scanResults access — checks permission + catches SecurityException. */
    @SuppressLint("MissingPermission")
    private fun safeScanResults(): List<ScanResult> {
        if (!hasScanPermission()) return emptyList()
        return try {
            wifi.scanResults ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
    }


    companion object {
        /** Serve memory cache without radio work */
        private const val CACHE_TTL_MS = 12_000L
        /** Minimum gap between startScan() calls */
        private const val MIN_SCAN_INTERVAL_MS = 8_000L
        /** Max wait for SCAN_RESULTS_AVAILABLE when we already have partial data */
        private const val FAST_TIMEOUT_MS = 2_800L
        /** Max wait on forced cold scan */
        private const val FORCE_TIMEOUT_MS = 4_500L
        private val lastScanAt = AtomicLong(0L)
        private val cacheRef = AtomicReference<WifiScanSnapshot?>(null)
        private val preferred24 = listOf(1, 6, 11)
        private val preferred5 = listOf(36, 40, 44, 48, 149, 153, 157, 161)
    }

    /**
     * @param force ignore TTL and try startScan if throttle allows
     * @param preferCache if true and cache exists, return it immediately (UI first paint)
     */
    suspend fun scan(
        force: Boolean = false,
        preferCache: Boolean = false,
        timeoutMs: Long? = null
    ): WifiScanSnapshot = withContext(Dispatchers.IO) {
        val started = SystemClock.elapsedRealtime()
        val now = started
        val cached = cacheRef.get()
        val age = if (cached != null) now - lastScanAt.get() else Long.MAX_VALUE

        // 1) Prefer memory cache for instant UI
        if (preferCache && cached != null && cached.networks.isNotEmpty()) {
            return@withContext cached.copy(fromCache = true, scanDurationMs = 0)
        }
        if (!force && cached != null && age < CACHE_TTL_MS && cached.networks.isNotEmpty()) {
            return@withContext cached.copy(fromCache = true, scanDurationMs = 0)
        }

        val connectedBssid = currentBssid()
        val connectedSsid = currentSsid()

        // 2) Instant system buffer (last OS scan) — often non-empty without startScan
        @Suppress("DEPRECATION")
        var results: List<ScanResult> = safeScanResults()

        val sinceLast = now - lastScanAt.get()
        val throttleOk = force || sinceLast >= MIN_SCAN_INTERVAL_MS
        val needRadio = force || results.isEmpty() || age >= CACHE_TTL_MS

        if (needRadio && throttleOk) {
            val wait = timeoutMs ?: if (results.isEmpty()) FORCE_TIMEOUT_MS else FAST_TIMEOUT_MS
            val fresh = withTimeoutOrNull(wait) { awaitScanResults() }
            if (!fresh.isNullOrEmpty()) {
                results = fresh
                lastScanAt.set(SystemClock.elapsedRealtime())
            } else if (results.isEmpty()) {
                // last resort: re-read system buffer after failed/timeout scan
                @Suppress("DEPRECATION")
                results = safeScanResults()
            }
        } else if (results.isEmpty() && cached != null) {
            return@withContext cached.copy(
                fromCache = true,
                scanDurationMs = SystemClock.elapsedRealtime() - started
            )
        }

        val networks = mapAndEnrich(results, connectedBssid, connectedSsid)
        if (networks.isEmpty() && cached != null) {
            return@withContext cached.copy(
                fromCache = true,
                scanDurationMs = SystemClock.elapsedRealtime() - started
            )
        }

        val ch24 = buildChannelStats(networks, Band.GHZ_24)
        val ch5 = buildChannelStats(networks, Band.GHZ_5)
        val snap = WifiScanSnapshot(
            networks = networks,
            channels24 = ch24,
            channels5 = ch5,
            recommended24 = pickBestChannel(ch24, preferred24),
            recommended5 = pickBestChannel(ch5, preferred5),
            fromCache = false,
            scanDurationMs = SystemClock.elapsedRealtime() - started
        )
        if (networks.isNotEmpty()) {
            cacheRef.set(snap)
            if (lastScanAt.get() == 0L) lastScanAt.set(SystemClock.elapsedRealtime())
        }
        snap
    }

    /** Non-suspending peek for first frame (main thread safe). */
    fun peekCache(): WifiScanSnapshot? = cacheRef.get()

    fun cachedSnapshot(): WifiScanSnapshot? = cacheRef.get()

    @SuppressLint("MissingPermission")
    fun currentConnection(): WifiNetworkInfo? {
        if (!hasScanPermission()) return null
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

    @SuppressLint("MissingPermission")
    private fun currentBssid(): String? {
        if (!hasScanPermission()) return null
        return try {
            @Suppress("DEPRECATION")
            wifi.connectionInfo?.bssid
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    private fun currentSsid(): String? {
        if (!hasScanPermission()) return null
        return try {
            @Suppress("DEPRECATION")
            wifi.connectionInfo?.ssid?.removeSurrounding("\"")
                ?.takeIf { it.isNotBlank() && it != "<unknown ssid>" }
        } catch (_: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
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
                        cont.resume(safeScanResults())
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
                    if (hasScanPermission()) {
                        @Suppress("DEPRECATION")
                        @SuppressLint("MissingPermission")
                        val started = wifi.startScan()
                    }
                } catch (_: Exception) {
                }
            } catch (_: Exception) {
                if (cont.isActive) cont.resume(safeScanResults())
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
        // Single-pass BSSID dedupe (keep strongest RSSI)
        val best = LinkedHashMap<String, ScanResult>(results.size * 2)
        for (r in results) {
            val key = r.BSSID ?: continue
            val prev = best[key]
            if (prev == null || r.level > prev.level) best[key] = r
        }
        val out = ArrayList<WifiNetworkInfo>(best.size)
        for (result in best.values) {
            val freq = result.frequency
            val band = bandOf(freq)
            val width = channelWidthMhz(result)
            val rssi = result.level
            val bssid = result.BSSID.orEmpty()
            val rawSsid = result.SSID
            val ssid = if (rawSsid.isNullOrBlank()) "<Hidden SSID>" else rawSsid
            val connected = when {
                connectedBssid != null && bssid.equals(connectedBssid, ignoreCase = true) -> true
                connectedSsid != null && ssid == connectedSsid -> true
                else -> false
            }
            out.add(
                WifiNetworkInfo(
                    ssid = ssid,
                    bssid = bssid,
                    rssiDbm = rssi,
                    frequencyMhz = freq,
                    channel = channelFromFrequency(freq),
                    security = simplifySecurity(result.capabilities.orEmpty()),
                    widthMhz = width,
                    band = band,
                    qualityScore = qualityFromRssi(rssi),
                    estimatedMbps = estimateMbps(rssi, band, width),
                    isConnected = connected
                )
            )
        }
        out.sortWith(
            compareByDescending<WifiNetworkInfo> { it.isConnected }
                .thenByDescending { it.rssiDbm }
        )
        return out
    }

    private fun buildChannelStats(networks: List<WifiNetworkInfo>, band: Band): List<ChannelStat> {
        // One pass: channel -> (count, strongest)
        val counts = HashMap<Int, Int>(16)
        val strongestMap = HashMap<Int, Int>(16)
        for (n in networks) {
            if (n.band != band || n.channel < 0) continue
            val ch = n.channel
            counts[ch] = (counts[ch] ?: 0) + 1
            val prev = strongestMap[ch]
            if (prev == null || n.rssiDbm > prev) strongestMap[ch] = n.rssiDbm
        }
        if (counts.isEmpty()) return emptyList()
        val out = ArrayList<ChannelStat>(counts.size)
        for ((ch, cnt) in counts) {
            val strongest = strongestMap[ch] ?: -100
            val congestion = min(100, cnt * 18 + max(0, strongest + 70))
            out.add(ChannelStat(ch, band, cnt, strongest, congestion))
        }
        out.sortBy { it.channel }
        return out
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
