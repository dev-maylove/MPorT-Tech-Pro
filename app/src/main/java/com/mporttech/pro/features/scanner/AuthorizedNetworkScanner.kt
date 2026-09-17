package com.mporttech.pro.features.scanner

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

data class ScanHost(val address: String, val reachable: Boolean, val latencyMs: Long?)

/**
 * Scanner limited to authorized private (RFC1918) subnets.
 * Accepts base "192.168.1" OR full IP "192.168.1.1" (normalized to /24 base).
 */
class AuthorizedNetworkScanner {
    suspend fun scan(
        base: String,
        startHost: Int = 1,
        endHost: Int = 254,
        timeoutMs: Int = 250,
        authorized: Boolean
    ): List<ScanHost> = withContext(Dispatchers.IO) {
        require(authorized) { "Authorization confirmation is required." }
        val normalized = normalizePrivateBase(base)
            ?: throw IllegalArgumentException(
                "Only private RFC1918 IPv4 ranges are accepted (contoh: 192.168.1 atau 192.168.1.1)"
            )
        require(startHost in 1..254 && endHost in startHost..254)

        // Parallel in batches (avoid 254 concurrent sockets)
        val results = ArrayList<ScanHost>()
        val hosts = (startHost..endHost).toList()
        val batchSize = 32
        for (i in hosts.indices step batchSize) {
            val batch = hosts.subList(i, minOf(i + batchSize, hosts.size))
            val part = coroutineScope {
                batch.map { host ->
                    async(Dispatchers.IO) {
                        val ip = "$normalized.$host"
                        probe(ip, timeoutMs)
                    }
                }.awaitAll()
            }
            results.addAll(part.filter { it.reachable })
        }
        results
    }

    private fun probe(ip: String, timeoutMs: Int): ScanHost {
        val started = System.nanoTime()
        val ports = intArrayOf(80, 443, 22, 53, 8080, 8728, 8291)
        for (port in ports) {
            try {
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), timeoutMs)
                    val latency = (System.nanoTime() - started) / 1_000_000
                    return ScanHost(ip, true, latency)
                }
            } catch (_: Exception) {
            }
        }
        val ok = try {
            InetAddress.getByName(ip).isReachable(timeoutMs)
        } catch (_: Exception) {
            false
        }
        val latency = if (ok) (System.nanoTime() - started) / 1_000_000 else null
        return ScanHost(ip, ok, latency)
    }

    companion object {
        /**
         * "192.168.1" → "192.168.1"
         * "192.168.1.1" → "192.168.1"
         * "10.0.0.5" → "10.0.0"
         */
        fun normalizePrivateBase(input: String): String? {
            val raw = input.trim().removePrefix("/").substringBefore("/")
            val parts = raw.split('.').map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.size !in 3..4) return null
            val nums = parts.map { it.toIntOrNull() ?: return null }
            if (nums.any { it !in 0..255 }) return null
            val a = nums[0]
            val b = nums[1]
            val c = nums[2]
            val private = a == 10 || (a == 192 && b == 168) || (a == 172 && b in 16..31)
            if (!private) return null
            return "$a.$b.$c"
        }

        fun isPrivateV4Base(base: String): Boolean = normalizePrivateBase(base) != null
    }
}
