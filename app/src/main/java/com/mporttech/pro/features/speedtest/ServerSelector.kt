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
        val base = "${server.scheme}://${server.host}"
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
        // HEAD
        try {
            val start = System.nanoTime()
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "HEAD"
                connectTimeout = 1500
                readTimeout = 1500
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "MPorT-TesSpeed/1.0")
                setRequestProperty("Cache-Control", "no-cache")
                setRequestProperty("Connection", "close")
            }
            try {
                val code = conn.responseCode
                if (code in 200..399) return (System.nanoTime() - start) / 1_000_000.0
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
        }
        // GET fallback
        try {
            val start = System.nanoTime()
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 1500
                readTimeout = 1500
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "MPorT-TesSpeed/1.0")
                setRequestProperty("Connection", "close")
            }
            try {
                val code = conn.responseCode
                conn.inputStream?.use { it.readBytes() }
                if (code in 200..399) return (System.nanoTime() - start) / 1_000_000.0
            } finally {
                conn.disconnect()
            }
        } catch (_: Exception) {
        }
        return null
    }
}
