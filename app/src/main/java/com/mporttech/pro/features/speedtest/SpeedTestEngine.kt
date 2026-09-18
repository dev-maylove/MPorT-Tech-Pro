package com.mporttech.pro.features.speedtest

import com.mporttech.pro.core.network.TcpTuning

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * Port of MPorT-Tes-Speed engine (Flutter) to Kotlin.
 *
 * Server default: http://ookla.haansiro.net:8080 (aligned with MPorT-Tes-Speed)
 * - Download multi-thread with grace period
 * - Upload multi-thread with accepted-byte scoring
 * - HTTP RTT ping (HEAD / ranged GET)
 */
object ServerConfig {
    // Defaults aligned with MPorT-Tes-Speed (Flutter) ServerConfig
    @Volatile var baseUrl: String = "http://ookla.haansiro.net:8080"
    /** Original hostname for HTTP Host header when baseUrl uses an IP. */
    @Volatile var originalHostname: String = "ookla.haansiro.net"
    /** After first HTTP→HTTPS redirect, stick to this origin for the rest of the test. */
    @Volatile var resolvedBaseUrl: String? = null
    @Volatile var downloadPath: String = "/speedtest/download"
    @Volatile var uploadPath: String = "/speedtest/upload.php"
    @Volatile var pingPath: String = "/speedtest/latency.txt"
    @Volatile var serverName: String = "HaaNSirO"

    // Matched to MPorT-Tes-Speed lib/core/constants/server_config.dart
    const val downloadDurationSeconds = 15
    const val uploadDurationSeconds = 15
    const val gracePeriodSeconds = 3
    const val downloadThreads = 6
    const val uploadThreads = 2
    const val uploadPayloadMegabytes = 1
    const val pingSamples = 12
    const val pingWarmup = 2
    const val pingIntervalMs = 20
    const val pingTrimCount = 4
    const val overheadAdjustment = 1.04
    const val connectTimeoutMs = 10_000
    const val readTimeoutMs = 20_000
    const val sampleIntervalMs = 200L
    /** Ping probe timeouts (cellular RTT can be 50–200 ms+). */
    const val pingConnectTimeoutMs = 8_000
    const val pingReadTimeoutMs = 6_000

    fun effectiveBase(): String = resolvedBaseUrl?.takeIf { it.isNotBlank() } ?: baseUrl

    fun downloadUrlBusted(): String {
        val n = System.nanoTime()
        val base = "${effectiveBase()}$downloadPath"
        return if (downloadPath.contains("speedtest")) {
            "$base?size=35000000&n=$n"
        } else {
            "$base?ckSize=100&n=$n"
        }
    }

    fun uploadUrlBusted(): String {
        val n = System.nanoTime()
        return "${effectiveBase()}$uploadPath?n=$n"
    }

    fun pingUrl(): String = "${effectiveBase()}$pingPath"
}

data class SpeedResult(
    val pingMs: Double,
    val jitterMs: Double,
    val avgPingMs: Double,
    val maxPingMs: Double,
    val packetLossPercent: Double,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val downloadBytes: Long,
    val uploadBytes: Long,
    val server: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class PhaseProgress(
    val phase: Phase,
    val mbps: Double = 0.0,
    val pingMs: Double = 0.0
)

enum class Phase { PING, DOWNLOAD, UPLOAD, COMPLETED, ERROR }

class SpeedTestEngine {
    @Volatile private var cancelled = false
    private val cancelFlag = AtomicBoolean(false)

    fun cancel() {
        cancelled = true
        cancelFlag.set(true)
    }

    fun resetCancel() {
        cancelled = false
        cancelFlag.set(false)
    }

    /**
     * Open GET/POST connection for speed-test payloads.
     * Manually follows HTTP→HTTPS redirects (Android does not auto-follow those).
     */
    // ── Connection layer ─────────────────────────────────────────────────────

    private fun isIpv4(host: String): Boolean =
        host.matches(Regex("""^\d{1,3}(\.\d{1,3}){3}$"""))

    private fun originOf(urlStr: String): String {
        val u = URL(urlStr)
        // Keep non-default ports (Ookla uses :8080 even on HTTPS)
        val portPart = when {
            u.port != -1 && !(u.protocol == "https" && u.port == 443) &&
                !(u.protocol == "http" && u.port == 80) -> ":${u.port}"
            else -> ""
        }
        return "${u.protocol}://${u.host}$portPart"
    }

    private fun applyCommonHeaders(conn: HttpURLConnection, urlStr: String) {
        conn.connectTimeout = ServerConfig.connectTimeoutMs
        conn.readTimeout = ServerConfig.readTimeoutMs
        conn.instanceFollowRedirects = false
        conn.useCaches = false
        conn.doInput = true
        conn.setRequestProperty("Accept-Encoding", "identity")
        conn.setRequestProperty("User-Agent", "MPorT-TesSpeed/1.0")
        conn.setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
        conn.setRequestProperty("Pragma", "no-cache")
        conn.setRequestProperty("Connection", "keep-alive")
        // Virtual-host only when connecting by raw IP (Haansiro)
        val hn = ServerConfig.originalHostname.trim()
        val urlHost = try { URL(urlStr).host } catch (_: Exception) { "" }
        if (hn.isNotEmpty() && !isIpv4(hn) && isIpv4(urlHost)) {
            conn.setRequestProperty("Host", hn)
        }
    }

    /**
     * Resolve the real origin before the timed test.
     * Handles servers that answer on HTTP and those that 307 → HTTPS Ookla CDN.
     */
    private fun resolveOriginViaRedirect() {
        val candidates = linkedSetOf<String>()
        val primary = ServerConfig.baseUrl.trimEnd('/')
        candidates += primary
        // Also try https on same host:port (some ISPs only expose TLS)
        if (primary.startsWith("http://")) {
            candidates += "https://" + primary.removePrefix("http://")
        }
        for (base in candidates) {
            val probeUrl = "$base${ServerConfig.downloadPath}?size=65536&n=${System.nanoTime()}"
            try {
                val finalUrl = followRedirects(probeUrl, method = "GET", drainBody = true)
                if (finalUrl != null) {
                    ServerConfig.resolvedBaseUrl = originOf(finalUrl)
                    return
                }
            } catch (_: Exception) {
                // try next candidate
            }
        }
    }

    /**
     * Follow redirects (including HTTP→HTTPS). Returns the final URL that yielded 2xx,
     * or null if none. Optionally drains a small body so keep-alive is happy.
     */
    private fun followRedirects(
        startUrl: String,
        method: String = "GET",
        maxRedirects: Int = 5,
        drainBody: Boolean = false
    ): String? {
        var current = startUrl
        var redirects = 0
        while (redirects <= maxRedirects) {
            val conn = (URL(current).openConnection() as HttpURLConnection)
            try {
                conn.requestMethod = method
                applyCommonHeaders(conn, current)
                val code = conn.responseCode
                if (code in 300..399) {
                    val loc = conn.getHeaderField("Location") ?: return null
                    current = if (loc.startsWith("http://") || loc.startsWith("https://")) {
                        loc
                    } else {
                        URL(URL(current), loc).toString()
                    }
                    redirects++
                    continue
                }
                if (code in 200..299) {
                    if (drainBody) {
                        try {
                            conn.inputStream?.use { ins ->
                                val buf = ByteArray(8 * 1024)
                                var total = 0
                                while (total < 64 * 1024) {
                                    val n = ins.read(buf)
                                    if (n < 0) break
                                    total += n
                                }
                            }
                        } catch (_: Exception) { }
                    }
                    return current
                }
                return null
            } finally {
                try { conn.disconnect() } catch (_: Exception) { }
            }
        }
        return null
    }

    /** Open GET/POST against urlStr. GET follows redirects once if origin not yet sticky. */
    private fun openSpeedConnection(urlStr: String, method: String = "GET"): HttpURLConnection {
        val target = when {
            method != "GET" -> urlStr
            // Already on sticky origin — no redirect chase
            ServerConfig.resolvedBaseUrl != null &&
                urlStr.startsWith(ServerConfig.resolvedBaseUrl!!) -> urlStr
            else -> followRedirects(urlStr, method = "GET", drainBody = false) ?: urlStr
        }
        val conn = (URL(target).openConnection() as HttpURLConnection)
        conn.requestMethod = method
        applyCommonHeaders(conn, target)
        if (method == "POST") {
            conn.doOutput = true
        }
        return conn
    }

    suspend fun run(
        multiConnection: Boolean = true,
        onProgress: (PhaseProgress) -> Unit = {}
    ): SpeedResult = withContext(Dispatchers.IO) {
        resetCancel()
        ensureNotCancelled()
        // HTTP keep-alive + DNS/TCP warm-up (cuts first-byte delay on cellular)
        TcpTuning.applyHttpSystemProperties()
        // Resolve final origin first (HTTP or HTTPS after 307) so ping/dl/ul share one base
        resolveOriginViaRedirect()
        warmConnection()

        onProgress(PhaseProgress(Phase.PING))
        val ping = measurePing()
        ensureNotCancelled()
        onProgress(PhaseProgress(Phase.PING, pingMs = ping.ping))

        onProgress(PhaseProgress(Phase.DOWNLOAD))
        val dlThreads = if (multiConnection) ServerConfig.downloadThreads else 1
        val download = measureDownload(dlThreads) { live ->
            onProgress(PhaseProgress(Phase.DOWNLOAD, mbps = live))
        }
        ensureNotCancelled()
        onProgress(PhaseProgress(Phase.DOWNLOAD, mbps = download.mbps))

        onProgress(PhaseProgress(Phase.UPLOAD))
        val ulThreads = if (multiConnection) ServerConfig.uploadThreads else 1
        val upload = measureUpload(ulThreads) { live ->
            onProgress(PhaseProgress(Phase.UPLOAD, mbps = live))
        }
        ensureNotCancelled()
        onProgress(PhaseProgress(Phase.UPLOAD, mbps = upload.mbps))

        val result = SpeedResult(
            pingMs = ping.ping,
            jitterMs = ping.jitter,
            avgPingMs = ping.avgPing,
            maxPingMs = ping.maxPing,
            packetLossPercent = ping.packetLossPercent,
            downloadMbps = download.mbps,
            uploadMbps = upload.mbps,
            downloadBytes = download.bytes,
            uploadBytes = upload.bytes,
            server = ServerConfig.serverName
        )
        onProgress(PhaseProgress(Phase.COMPLETED, mbps = result.downloadMbps))
        result
    }

    private fun ensureNotCancelled() {
        if (cancelled || cancelFlag.get()) throw SpeedTestCancelledException()
    }

    // ── Ping (HTTP RTT) ──────────────────────────────────────────────────────

    private data class PingMetrics(
        val ping: Double,
        val jitter: Double,
        val avgPing: Double,
        val maxPing: Double,
        val packetLossPercent: Double,
        val sampleCount: Int
    )

    private fun measurePing(): PingMetrics {
        val samplesMs = mutableListOf<Double>()
        var attempts = 0
        var failures = 0
        val total = ServerConfig.pingSamples + ServerConfig.pingWarmup

        for (i in 0 until total) {
            ensureNotCancelled()
            val ms = onePing()
            if (i >= ServerConfig.pingWarmup) {
                attempts++
                if (ms != null && ms > 0) samplesMs += ms else failures++
            }
            if (i < total - 1) {
                Thread.sleep(ServerConfig.pingIntervalMs.toLong())
            }
        }

        if (samplesMs.isEmpty()) {
            throw IllegalStateException("Ping timed out — server did not respond (${ServerConfig.serverName} @ ${ServerConfig.baseUrl})")
        }

        val sorted = samplesMs.sorted()
        var working = sorted
        val trim = ServerConfig.pingTrimCount
        if (working.size > trim + 2) {
            working = working.subList(0, working.size - trim)
        }

        val ping = max(0.1, sorted.first())
        val maxPing = max(0.1, sorted.last())
        val avgPing = max(0.1, working.average())

        val p80Idx = ((sorted.size * 0.80).toInt()).coerceIn(0, sorted.lastIndex)
        val threshold = sorted[p80Idx]
        val stable = samplesMs.filter { it <= threshold }
        val diffs = mutableListOf<Double>()
        for (i in 1 until stable.size) {
            diffs += abs(stable[i] - stable[i - 1])
        }
        val jitter = if (diffs.isEmpty()) 0.0 else diffs.average()
        val loss = if (attempts > 0) (failures.toDouble() / attempts) * 100.0 else 0.0

        return PingMetrics(ping, jitter, avgPing, maxPing, loss, samplesMs.size)
    }

    private fun onePing(): Double? {
        val paths = linkedSetOf(
            ServerConfig.pingPath.ifBlank { "/speedtest/latency.txt" },
            "/speedtest/latency.txt",
            "/speedtest/download?size=0",
            "/"
        )
        val bases = linkedSetOf(ServerConfig.effectiveBase(), ServerConfig.baseUrl)
        for (base in bases) {
            for (path in paths) {
                val normalizedPath = if (path.startsWith('/')) path else "/$path"
                val url = "$base$normalizedPath${if ('?' in normalizedPath) "&" else "?"}n=${System.nanoTime()}"
                onePingAttempt(url, "GET")?.let { return it }
                onePingAttempt(url, "HEAD")?.let { return it }
            }
        }
        return null
    }

    private fun onePingAttempt(url: String, mode: String): Double? {
        return try {
            val start = System.nanoTime()
            var current = url
            var redirects = 0
            while (redirects <= 5) {
                val conn = (URL(current).openConnection() as HttpURLConnection)
                try {
                    conn.requestMethod = if (mode == "HEAD") "HEAD" else "GET"
                    applyCommonHeaders(conn, current)
                    conn.connectTimeout = ServerConfig.pingConnectTimeoutMs
                    conn.readTimeout = ServerConfig.pingReadTimeoutMs
                    if (mode == "GET_RANGE") {
                        conn.setRequestProperty("Range", "bytes=0-0")
                    }
                    val code = conn.responseCode
                    if (code in 300..399) {
                        val loc = conn.getHeaderField("Location") ?: return null
                        current = if (loc.startsWith("http")) loc else URL(URL(current), loc).toString()
                        redirects++
                        continue
                    }
                    if (code !in 200..399 && code != 404) return null
                    if (mode != "HEAD") {
                        try {
                            conn.inputStream?.use { ins ->
                                val buf = ByteArray(512)
                                while (ins.read(buf) > 0) { /* drain */ }
                            }
                        } catch (_: Exception) { }
                    }
                    if (redirects > 0) {
                        try { ServerConfig.resolvedBaseUrl = originOf(current) } catch (_: Exception) { }
                    }
                    return (System.nanoTime() - start) / 1_000_000.0
                } finally {
                    try { conn.disconnect() } catch (_: Exception) { }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }


    private fun warmConnection() {
        try {
            val base = ServerConfig.effectiveBase()
            val hostPort = base
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
            val host = hostPort.substringBefore(":")
            val port = hostPort.substringAfter(":", "").toIntOrNull()
                ?: if (base.startsWith("https")) 443 else 80
            // DNS
            java.net.InetAddress.getAllByName(host)
            // TCP connect with congestion-friendly buffer / NODELAY tuning
            java.net.Socket().use { sock ->
                sock.connect(java.net.InetSocketAddress(host, port), ServerConfig.pingConnectTimeoutMs)
                TcpTuning.apply(sock, lowLatency = true)
            }
            // One cheap HTTP warm request (not counted in samples)
            onePingAttempt("${ServerConfig.pingUrl()}?warm=${System.nanoTime()}", "GET")
        } catch (_: Exception) {
            // ignore — actual test still runs
        }
    }

    // ── Download ─────────────────────────────────────────────────────────────

    private data class TransferResult(val mbps: Double, val bytes: Long)

    private suspend fun measureDownload(
        threads: Int,
        onProgress: (Double) -> Unit
    ): TransferResult = coroutineScope {
        val durationMs = ServerConfig.downloadDurationSeconds * 1000L
        val graceMs = ServerConfig.gracePeriodSeconds * 1000L
        val startMs = System.currentTimeMillis()
        val totalBytes = AtomicLong(0)
        val running = AtomicBoolean(true)
        val intervalSamples = mutableListOf<Double>()
        var graceBytes = 0L
        var graceEnded = false
        var lastTickMs = graceMs
        var lastTickBytes = 0L
        var activeMs = 0L

        fun endGraceIfNeeded() {
            val elapsed = System.currentTimeMillis() - startMs
            if (!graceEnded && elapsed >= graceMs) {
                graceEnded = true
                graceBytes = totalBytes.get()
                lastTickBytes = totalBytes.get()
                lastTickMs = elapsed
            }
        }

        fun sampleTick(force: Boolean = false) {
            endGraceIfNeeded()
            if (!graceEnded || cancelled) return
            val now = System.currentTimeMillis() - startMs
            val deltaMs = now - lastTickMs
            val deltaBytes = totalBytes.get() - lastTickBytes
            if (deltaMs > 0 && deltaBytes > 0) {
                activeMs += deltaMs
                val sample = measuredMbps(deltaBytes, deltaMs / 1000.0)
                synchronized(intervalSamples) {
                    intervalSamples += sample
                    if (intervalSamples.size > 12) intervalSamples.removeAt(0)
                }
            }
            lastTickMs = now
            lastTickBytes = totalBytes.get()
            if (!force) {
                val live = robustLiveMbps(synchronized(intervalSamples) { intervalSamples.toList() })
                if (live > 0) onProgress(live)
            }
        }

        val workers = (1..threads.coerceIn(1, 8)).map {
            async(Dispatchers.IO) {
                var failures = 0
                val buffer = ByteArray(32 * 1024)
                while (running.get() && !cancelled) {
                    val elapsed = System.currentTimeMillis() - startMs
                    if (elapsed >= durationMs) {
                        running.set(false)
                        break
                    }
                    try {
                        val conn = openSpeedConnection(ServerConfig.downloadUrlBusted(), "GET")
                        try {
                            val code = conn.responseCode
                            if (code !in 200..299) {
                                failures++
                                if (failures >= 5) break
                                delay(80)
                            } else {
                                failures = 0
                                val stream = try {
                                    conn.inputStream
                                } catch (_: Exception) {
                                    conn.errorStream
                                }
                                if (stream == null) {
                                    failures++
                                } else {
                                    // Avoid break/continue inside inline use{} (Kotlin experimental)
                                    val input = BufferedInputStream(stream)
                                    try {
                                        while (running.get() && !cancelled && failures < 5) {
                                            if (System.currentTimeMillis() - startMs >= durationMs) {
                                                running.set(false)
                                                break
                                            }
                                            val read = input.read(buffer)
                                            if (read < 0) break
                                            if (read > 0) totalBytes.addAndGet(read.toLong())
                                        }
                                    } finally {
                                        try { input.close() } catch (_: Exception) {}
                                    }
                                }
                            }
                        } finally {
                            try { conn.disconnect() } catch (_: Exception) {}
                        }
                        if (failures >= 5) break
                    } catch (_: Exception) {
                        failures++
                        if (failures >= 6) break
                        delay(80)
                    }
                }
            }
        }

        // Progress sampler
        val sampler = async(Dispatchers.Default) {
            while (running.get() && !cancelled) {
                sampleTick()
                delay(ServerConfig.sampleIntervalMs)
            }
        }

        // Duration watchdog
        async {
            delay(durationMs + 600)
            running.set(false)
        }

        workers.awaitAll()
        running.set(false)
        sampler.cancel()
        sampleTick(force = true)

        ensureNotCancelled()
        endGraceIfNeeded()
        val total = totalBytes.get()
        var measuredBytes = max(0L, total - graceBytes)
        // If all data arrived during grace window, still score using full transfer
        if (measuredBytes <= 0 && total > 0) {
            measuredBytes = total
            graceBytes = 0L
        }
        val wallSec = max(
            0.001,
            (System.currentTimeMillis() - startMs - (if (graceBytes > 0) ServerConfig.gracePeriodSeconds * 1000L else 0L))
                .coerceAtLeast(200L) / 1000.0
        )
        val sampleSec = activeMs / 1000.0
        val seconds = when {
            sampleSec > 0.2 -> sampleSec
            measuredBytes > 0 -> wallSec
            else -> 0.0
        }
        if (measuredBytes <= 0 || seconds <= 0) {
            // Last-resort: single sequential download probe (helps flaky multi-thread / cellular)
            val fallback = sequentialDownloadProbe()
            if (fallback.bytes > 0) {
                onProgress(fallback.mbps)
                return@coroutineScope fallback
            }
            throw IllegalStateException("Download timed out or no data was received")
        }
        val mbps = measuredMbps(measuredBytes, seconds)
        onProgress(mbps)
        TransferResult(mbps, measuredBytes)
    }

    /**
     * Short single-stream download used when multi-thread measurement collected no bytes.
     * Downloads up to ~8s or ~12MB — enough to compute a valid Mbps figure.
     */
    private fun sequentialDownloadProbe(): TransferResult {
        val buffer = ByteArray(64 * 1024)
        val t0 = System.currentTimeMillis()
        var bytes = 0L
        val limitMs = 8_000L
        val limitBytes = 12L * 1024 * 1024
        try {
            val conn = openSpeedConnection(ServerConfig.downloadUrlBusted(), "GET")
            try {
                if (conn.responseCode !in 200..299) return TransferResult(0.0, 0)
                val input = BufferedInputStream(conn.inputStream)
                try {
                    while (bytes < limitBytes && System.currentTimeMillis() - t0 < limitMs) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        bytes += n
                    }
                } finally {
                    try { input.close() } catch (_: Exception) {}
                }
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            return TransferResult(0.0, 0)
        }
        val sec = max(0.2, (System.currentTimeMillis() - t0) / 1000.0)
        if (bytes <= 0) return TransferResult(0.0, 0)
        return TransferResult(measuredMbps(bytes, sec), bytes)
    }

    // ── Upload ───────────────────────────────────────────────────────────────

    private suspend fun measureUpload(
        threads: Int,
        onProgress: (Double) -> Unit
    ): TransferResult = coroutineScope {
        val durationMs = ServerConfig.uploadDurationSeconds * 1000L
        val graceMs = ServerConfig.gracePeriodSeconds * 1000L
        val startMs = System.currentTimeMillis()
        val payload = ByteArray(ServerConfig.uploadPayloadMegabytes * 1024 * 1024).also {
            Random.nextBytes(it)
        }
        val acceptedBytes = AtomicLong(0)
        val wireBytes = AtomicLong(0)
        val running = AtomicBoolean(true)
        val intervalSamples = mutableListOf<Double>()
        var graceEnded = false
        var lastTickMs = 0L
        var previousWire = 0L

        fun sampleTick() {
            if (cancelled) return
            val now = System.currentTimeMillis() - startMs
            if (!graceEnded && now >= graceMs) {
                graceEnded = true
                lastTickMs = now
            }
            if (!graceEnded) return
            val dt = now - lastTickMs
            val delta = wireBytes.get() - previousWire
            previousWire = wireBytes.get()
            if (dt > 0 && delta > 0) {
                val sample = measuredMbps(delta, dt / 1000.0)
                synchronized(intervalSamples) {
                    intervalSamples += sample
                    if (intervalSamples.size > 12) intervalSamples.removeAt(0)
                }
                val live = robustLiveMbps(synchronized(intervalSamples) { intervalSamples.toList() })
                if (live > 0) onProgress(live)
            }
            lastTickMs = now
        }

        val workers = (1..threads.coerceIn(1, 6)).map {
            async(Dispatchers.IO) {
                var failures = 0
                while (running.get() && !cancelled && System.currentTimeMillis() - startMs < durationMs) {
                    try {
                        val conn = openSpeedConnection(ServerConfig.uploadUrlBusted(), "POST").apply {
                            setRequestProperty("Content-Type", "application/octet-stream")
                            setFixedLengthStreamingMode(payload.size)
                        }
                        try {
                            val out = conn.outputStream
                            try {
                                var offset = 0
                                val chunk = 64 * 1024
                                while (offset < payload.size && running.get() && !cancelled) {
                                    if (System.currentTimeMillis() - startMs >= durationMs) {
                                        running.set(false)
                                        break
                                    }
                                    val end = min(offset + chunk, payload.size)
                                    out.write(payload, offset, end - offset)
                                    wireBytes.addAndGet((end - offset).toLong())
                                    offset = end
                                }
                                out.flush()
                            } finally {
                                try { out.close() } catch (_: Exception) {}
                            }
                            val code = conn.responseCode
                            // Drain response
                            try {
                                conn.inputStream?.use { it.readBytes() }
                            } catch (_: Exception) {
                                conn.errorStream?.use { it.readBytes() }
                            }
                            if (code !in 200..299) {
                                failures++
                                if (failures >= 3) break
                                continue
                            }
                            failures = 0
                            if (running.get() && !cancelled && System.currentTimeMillis() - startMs <= durationMs) {
                                acceptedBytes.addAndGet(payload.size.toLong())
                            }
                        } finally {
                            conn.disconnect()
                        }
                    } catch (_: Exception) {
                        failures++
                        if (failures >= 4) break
                        delay(120)
                    }
                }
            }
        }

        val sampler = async(Dispatchers.Default) {
            while (running.get() && !cancelled) {
                sampleTick()
                delay(ServerConfig.sampleIntervalMs)
            }
        }

        async {
            delay(durationMs + 700)
            running.set(false)
        }

        workers.awaitAll()
        running.set(false)
        sampler.cancel()
        sampleTick()

        ensureNotCancelled()
        var bytes = max(0L, acceptedBytes.get())
        if (bytes <= 0) {
            // Server may not echo Content-Length; fall back to bytes written on the wire
            bytes = max(0L, wireBytes.get())
        }
        val activeSeconds = max(
            0.001,
            (min(System.currentTimeMillis() - startMs, durationMs) - graceMs).coerceAtLeast(1) / 1000.0
        )
        if (bytes <= 0) {
            throw IllegalStateException("Upload timed out or no data was accepted")
        }
        val mbps = measuredMbps(bytes, activeSeconds)
        onProgress(mbps)
        TransferResult(mbps, bytes)
    }

    // ── Math helpers (from throughput_math.dart) ─────────────────────────────

    private fun measuredMbps(bytes: Long, seconds: Double): Double {
        if (bytes <= 0 || !seconds.isFinite() || seconds <= 0) return 0.0
        val value = bytes * 8.0 / seconds / 1e6 * ServerConfig.overheadAdjustment
        return if (value.isFinite() && value > 0) value else 0.0
    }

    private fun robustLiveMbps(samples: List<Double>): Double {
        val valid = samples.filter { it.isFinite() && it > 0 }
        if (valid.isEmpty()) return 0.0
        if (valid.size < 3) return valid.last()
        val sorted = valid.sorted()
        val median = sorted[sorted.size / 2]
        val deviations = sorted.map { abs(it - median) }.sorted()
        val mad = deviations[deviations.size / 2]
        val tolerance = max(median * 0.35, mad * 3.0)
        val filtered = valid.filter { abs(it - median) <= tolerance }
        if (filtered.isEmpty()) return median
        return filtered.average()
    }

    class SpeedTestCancelledException(message: String = "Speed test cancelled") : Exception(message)
}
