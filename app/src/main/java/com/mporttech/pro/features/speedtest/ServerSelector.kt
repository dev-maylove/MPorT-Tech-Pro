package com.mporttech.pro.features.speedtest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Selects and probes speed-test servers (port of MPorT-Tes-Speed ServerService).
 * Optimized: parallel batches, short timeouts, early exit on low latency.
 */
object ServerSelector {
    @Volatile
    var selected: TestServer = TestServer.haansiro()
        private set

    init {
        selected.applyToConfig()
    }

    fun select(server: TestServer) {
        selected = server
        server.applyToConfig()
    }

    fun catalog(): List<TestServer> = TestServer.catalog()

    /**
     * Probe up to [limit] nearest servers (by distanceKm), pick lowest latency.
     * Stops early if a server responds under [earlyExitMs].
     */
    suspend fun selectNearest(limit: Int = 12, earlyExitMs: Double = 15.0): TestServer =
        withContext(Dispatchers.IO) {
            val ordered = TestServer.catalog()
                .sortedWith(
                    compareBy<TestServer> { it.distanceKm ?: 999 }
                        .thenByDescending { it.isDefault }
                )
                .take(limit.coerceIn(3, 20))

            var best: TestServer? = null
            var bestLat = Double.POSITIVE_INFINITY

            // Larger parallel batches = faster nearest selection on cellular
            ordered.chunked(6).forEach { batch ->
                coroutineScope {
                    val results = batch.map { s ->
                        async { s to probeLatencyFast(s) }
                    }.awaitAll()
                    results.forEach { (s, lat) ->
                        if (lat != null && lat < bestLat) {
                            bestLat = lat
                            best = s.copyLatency(lat)
                        }
                    }
                }
                if (best != null && bestLat < earlyExitMs) {
                    return@withContext best.also { select(it) }
                }
            }

            val chosen = best ?: TestServer.haansiro()
            select(chosen)
            chosen
        }

    private fun probeLatencyFast(server: TestServer): Double? {
        val base = server.baseUrl.trimEnd('/')
        val paths = listOf(
            server.pingPath,
            "/speedtest/latency.txt",
            "/speedtest/download?size=0",
            "/"
        ).filter { it.isNotBlank() }.distinct()

        val samples = mutableListOf<Double>()
        // Two rounds, first successful path per round
        repeat(2) {
            for (path in paths) {
                val normalized = if (path.startsWith("/")) path else "/$path"
                val url = "$base$normalized${if (normalized.contains("?")) "&" else "?"}n=${System.nanoTime()}"
                val ms = timedRequest(url)
                if (ms != null) {
                    samples += ms
                    break
                }
            }
        }
        return samples.minOrNull()
    }

    /** Follows HTTP→HTTPS redirects; short timeouts for selection probe. */
    private fun timedRequest(urlStr: String): Double? {
        return try {
            val start = System.nanoTime()
            var current = urlStr
            var redirects = 0
            while (redirects <= 4) {
                val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 2000
                    readTimeout = 2000
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "MPorT-TesSpeed/1.0")
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("Connection", "close")
                    setRequestProperty("Accept-Encoding", "identity")
                }
                try {
                    val code = conn.responseCode
                    if (code in 300..399) {
                        val loc = conn.getHeaderField("Location")
                        if (loc.isNullOrBlank()) return null
                        current = if (loc.startsWith("http")) loc else URL(URL(current), loc).toString()
                        redirects++
                        continue
                    }
                    if (code in 200..399) {
                        try {
                            conn.inputStream?.use { ins ->
                                val buf = ByteArray(256)
                                while (ins.read(buf) > 0) { /* drain tiny body */ }
                            }
                        } catch (_: Exception) { }
                        return (System.nanoTime() - start) / 1_000_000.0
                    }
                    return null
                } finally {
                    try { conn.disconnect() } catch (_: Exception) { }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
