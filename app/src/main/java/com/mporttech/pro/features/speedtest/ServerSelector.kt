package com.mporttech.pro.features.speedtest

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Port of ServerService from MPorT-Tes-Speed.
 * Probes nearest servers from the resource catalog.
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
     */
    suspend fun selectNearest(limit: Int = 10): TestServer = withContext(Dispatchers.IO) {
        val ordered = TestServer.catalog()
            .sortedWith(compareBy<TestServer> { it.distanceKm ?: 999 }.thenByDescending { it.isDefault })
            .take(limit)

        var best: TestServer? = null
        var bestLat = Double.POSITIVE_INFINITY

        // Probe in batches of 4
        ordered.chunked(4).forEach { batch ->
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
            if (best != null && bestLat < 12.0) return@withContext best.also { select(it) }
        }

        val chosen = best ?: TestServer.haansiro()
        select(chosen)
        chosen
    }

    private fun probeLatencyFast(server: TestServer): Double? {
        val base = server.baseUrl
        val paths = listOf(server.pingPath, "/speedtest/latency.txt", "/").filter { it.isNotBlank() }
        val samples = mutableListOf<Double>()
        repeat(2) {
            for (path in paths) {
                val ms = oneShot(base, path)
                if (ms != null) {
                    samples += ms
                    break
                }
            }
        }
        return samples.minOrNull()
    }

    private fun oneShot(base: String, path: String): Double? {
        val url = "$base$path${if (path.contains("?")) "&" else "?"}n=${System.nanoTime()}"
        return timedRequest(url, "HEAD") ?: timedRequest(url, "GET")
    }

    /** Follows HTTP→HTTPS redirects (Android does not auto-follow those). */
    private fun timedRequest(urlStr: String, method: String): Double? {
        return try {
            val start = System.nanoTime()
            var current = urlStr
            var redirects = 0
            while (redirects <= 4) {
                val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = 2500
                    readTimeout = 2500
                    instanceFollowRedirects = false
                    setRequestProperty("User-Agent", "MPorT-TesSpeed/1.0")
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("Connection", "close")
                }
                try {
                    val code = conn.responseCode
                    if (code in 300..399 && redirects < 4) {
                        val loc = conn.getHeaderField("Location")
                        conn.disconnect()
                        if (loc.isNullOrBlank()) return null
                        current = if (loc.startsWith("http")) loc else URL(URL(current), loc).toString()
                        redirects++
                        continue
                    }
                    if (code in 200..399) {
                        if (method == "GET") {
                            try { conn.inputStream?.use { it.readBytes() } } catch (_: Exception) {}
                        }
                        return (System.nanoTime() - start) / 1_000_000.0
                    }
                    return null
                } finally {
                    try { conn.disconnect() } catch (_: Exception) {}
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }
}
