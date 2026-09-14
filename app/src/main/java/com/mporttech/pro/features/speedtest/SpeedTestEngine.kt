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
 * Server default: http://165.99.194.173:8080 (ookla.haansiro.net IP)
 * - Download multi-thread with grace period
 * - Upload multi-thread with accepted-byte scoring
 * - HTTP RTT ping (HEAD / ranged GET)
 */
object ServerConfig {
    @Volatile var baseUrl: String = "http://165.99.194.173:8080"
    /** Original hostname for HTTP Host header when baseUrl uses an IP. */
    @Volatile var originalHostname: String = "ookla.haansiro.net"
    @Volatile var downloadPath: String = "/speedtest/download"
    @Volatile var uploadPath: String = "/speedtest/upload.php"
    @Volatile var pingPath: String = "/speedtest/latency.txt"
    @Volatile var serverName: String = "HaaNSirO"

    // Tuned for cellular (higher RTT, jitter, intermittent loss) while still
    // working well on Wi‑Fi.
    const val downloadDurationSeconds = 12
    const val uploadDurationSeconds = 12
    const val gracePeriodSeconds = 1
    const val downloadThreads = 3
    const val uploadThreads = 1
    const val uploadPayloadMegabytes = 1
    const val pingSamples = 8
    const val pingWarmup = 1
    const val pingIntervalMs = 60
    const val pingTrimCount = 2
    const val overheadAdjustment = 1.04
    const val connectTimeoutMs = 12_000
    const val readTimeoutMs = 25_000
    const val sampleIntervalMs = 250L
    /** Ping probe timeouts (cellular RTT can be 50–200 ms+). */
    const val pingConnectTimeoutMs = 5_000
    const val pingReadTimeoutMs = 4_000

    fun downloadUrlBusted(): String {
        val n = System.nanoTime()
        val base = "$baseUrl$downloadPath"
        return if (downloadPath.contains("speedtest")) {
            "$base?size=35000000&n=$n"
        } else {
            "$base?ckSize=100&n=$n"
        }
    }

    fun uploadUrlBusted(): String {
        val n = System.nanoTime()
        return "$baseUrl$uploadPath?n=$n"
    }

    fun pingUrl(): String = "$baseUrl$pingPath"
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

    suspend fun run(
        multiConnection: Boolean = true,
        onProgress: (PhaseProgress) -> Unit = {}
    ): SpeedResult = withContext(Dispatchers.IO) {
        resetCancel()
        ensureNotCancelled()
        // HTTP keep-alive + DNS/TCP warm-up (cuts first-byte delay on cellular)
        TcpTuning.applyHttpSystemProperties()
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
            throw IllegalStateException("Ping timed out — server did not respond")
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
        val url = "${ServerConfig.pingUrl()}?n=${System.nanoTime()}"
        // Prefer lightweight GET of latency.txt (Ookla). HEAD first can hang on some CGNAT paths.
        onePingAttempt(url, "GET")?.let { return it }
        onePingAttempt(url, "HEAD")?.let { return it }
        // Ranged GET fallback
        return onePingAttempt(url, "GET_RANGE")
    }

    private fun onePingAttempt(url: String, mode: String): Double? {
        return try {
            val start = System.nanoTime()
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = if (mode == "HEAD") "HEAD" else "GET"
                connectTimeout = ServerConfig.pingConnectTimeoutMs
                readTimeout = ServerConfig.pingReadTimeoutMs
                instanceFollowRedirects = false
                useCaches = false
                doInput = true
                setRequestProperty("Accept-Encoding", "identity")
                setRequestProperty("Connection", "keep-alive")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("User-Agent", "MPorT-TesSpeed/1.0")
                if (mode == "GET_RANGE") {
                    setRequestProperty("Range", "bytes=0-0")
                }
                val hn = ServerConfig.originalHostname.trim()
                if (hn.isNotEmpty() && !hn.matches(Regex("""^\d{1,3}(\.\d{1,3}){3}$"""))) {
                    setRequestProperty("Host", hn)
                }
            }
            try {
                val code = conn.responseCode
                if (code !in 200..399 && code != 404) return null
                // Drain tiny body so connection can be reused (keep-alive)
                if (mode != "HEAD") {
                    try {
                        conn.inputStream?.let { ins ->
                            try {
                                val buf = ByteArray(512)
                                while (ins.read(buf) > 0) { /* drain */ }
                            } finally {
                                try { ins.close() } catch (_: Exception) {}
                            }
                        }
                    } catch (_: Exception) { }
                }
                (System.nanoTime() - start) / 1_000_000.0
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    /** DNS + TCP warm-up so the first timed ping is not dominated by resolve/connect. */
    private fun warmConnection() {
        try {
            val hostPort = ServerConfig.baseUrl
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
            val host = hostPort.substringBefore(":")
            val port = hostPort.substringAfter(":", "").toIntOrNull()
                ?: if (ServerConfig.baseUrl.startsWith("https")) 443 else 80
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
                        val conn = (URL(ServerConfig.downloadUrlBusted()).openConnection() as HttpURLConnection).apply {
                            requestMethod = "GET"
                            connectTimeout = ServerConfig.connectTimeoutMs
                            readTimeout = ServerConfig.readTimeoutMs
                            instanceFollowRedirects = true
                            useCaches = false
                            doInput = true
                            setRequestProperty("Accept-Encoding", "identity")
                            val _hn = ServerConfig.originalHostname.trim()
                            if (_hn.isNotEmpty() && !_hn.matches(Regex("""^\d{1,3}(\.\d{1,3}){3}$"""))) {
                                setRequestProperty("Host", _hn)
                            }
                            setRequestProperty("User-Agent", "MPorT-TesSpeed/1.0")
                            setRequestProperty("Cache-Control", "no-cache, no-store, must-revalidate")
                            setRequestProperty("Pragma", "no-cache")
                            setRequestProperty("Connection", "keep-alive")
                        }
                        try {
                            val code = conn.responseCode
                            if (code !in 200..299) {
                                failures++
                                if (failures >= 5) {
                                    // stop this worker
                                } else {
                                    delay(80)
                                }
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
            val conn = (URL(ServerConfig.downloadUrlBusted()).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = ServerConfig.connectTimeoutMs
                readTimeout = ServerConfig.readTimeoutMs
                useCaches = false
                doInput = true
                instanceFollowRedirects = true
                setRequestProperty("Accept-Encoding", "identity")
                val hn = ServerConfig.originalHostname.trim()
                if (hn.isNotEmpty() && !hn.matches(Regex("""^\d{1,3}(\.\d{1,3}){3}$"""))) {
                    setRequestProperty("Host", hn)
                }
                setRequestProperty("User-Agent", "MPorT-TesSpeed/1.0")
                setRequestProperty("Cache-Control", "no-cache")
            }
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
                        val conn = (URL(ServerConfig.uploadUrlBusted()).openConnection() as HttpURLConnection).apply {
                            requestMethod = "POST"
                            doOutput = true
                            doInput = true
                            useCaches = false
                            connectTimeout = ServerConfig.connectTimeoutMs
                            readTimeout = ServerConfig.readTimeoutMs
                            setRequestProperty("Content-Type", "application/octet-stream")
                            setRequestProperty("Accept-Encoding", "identity")
                            val _hn = ServerConfig.originalHostname.trim()
                            if (_hn.isNotEmpty() && !_hn.matches(Regex("""^\d{1,3}(\.\d{1,3}){3}$"""))) {
                                setRequestProperty("Host", _hn)
                            }
                            setRequestProperty("User-Agent", "MPorT-TesSpeed/1.0")
                            setRequestProperty("Cache-Control", "no-cache")
                            setRequestProperty("Connection", "keep-alive")
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
