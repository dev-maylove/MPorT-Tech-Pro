package com.mporttech.pro.core.network

import java.lang.reflect.Method
import java.net.Socket

/**
 * Application-level TCP tuning for high BDP links (cellular 4G/5G).
 *
 * Note: The kernel congestion-control algorithm (CUBIC/BBR/…) is controlled by
 * the OS and cannot be switched from a normal app without root. What we *can*
 * do is:
 *  - disable Nagle (TCP_NODELAY) for lower latency on small RPC / ping
 *  - enlarge SO_RCVBUF / SO_SNDBUF for better throughput on high-RTT paths
 *  - enable keep-alive with sane idle/interval when the platform allows it
 *  - prefer TCP_QUICKACK on Linux (best-effort via reflection)
 */
object TcpTuning {

    /** ~512 KiB — enough for ~40 ms RTT × 100 Mbps without stalling. */
    const val RECV_BUFFER = 512 * 1024
    const val SEND_BUFFER = 512 * 1024

    /** Apply best-effort socket options. Safe to call on any Socket. */
    fun apply(socket: Socket, lowLatency: Boolean = true) {
        try {
            if (!socket.isClosed) {
                socket.tcpNoDelay = lowLatency // disable Nagle
                socket.keepAlive = true
                socket.reuseAddress = true
                try {
                    socket.receiveBufferSize = RECV_BUFFER.coerceAtLeast(socket.receiveBufferSize)
                } catch (_: Exception) { }
                try {
                    socket.sendBufferSize = SEND_BUFFER.coerceAtLeast(socket.sendBufferSize)
                } catch (_: Exception) { }
                // Traffic class: low-delay (IPTOS_LOWDELAY = 0x10)
                if (lowLatency) {
                    try {
                        socket.trafficClass = 0x10
                    } catch (_: Exception) { }
                }
                tryQuickAck(socket)
                tryKeepAliveParams(socket)
            }
        } catch (_: Exception) {
            // Never break the caller
        }
    }

    /**
     * Linux TCP_QUICKACK — reduces delayed-ACK stalls after small writes.
     * Best-effort; ignored on non-Linux / blocked reflection.
     */
    private fun tryQuickAck(socket: Socket) {
        try {
            val impl = socket.javaClass.methods
                .firstOrNull { it.name == "getFileDescriptor$" || it.name == "getFileDescriptor" }
                ?: return
            // Not portable on all Android ART versions — skip if unavailable
            val so = Class.forName("android.system.Os")
            val osConstants = Class.forName("android.system.OsConstants")
            val fd = try {
                val getFd: Method = socket.javaClass.getDeclaredMethod("getFileDescriptor\$")
                getFd.isAccessible = true
                getFd.invoke(socket)
            } catch (_: Exception) {
                null
            } ?: return
            val tcpQuickack = try {
                osConstants.getField("TCP_QUICKACK").getInt(null)
            } catch (_: Exception) {
                // Typical Linux value
                12
            }
            val ipprotoTcp = try {
                osConstants.getField("IPPROTO_TCP").getInt(null)
            } catch (_: Exception) {
                6
            }
            val setsockoptInt = so.getMethod(
                "setsockoptInt",
                Class.forName("java.io.FileDescriptor"),
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType,
                Int::class.javaPrimitiveType
            )
            setsockoptInt.invoke(null, fd, ipprotoTcp, tcpQuickack, 1)
        } catch (_: Exception) {
            // optional
        }
    }

    /** Android 8+ Socket keep-alive idle/interval via reflection when present. */
    private fun tryKeepAliveParams(socket: Socket) {
        try {
            // android.net.SocketKeepalive is for ConnectivityManager; skip.
            // Some runtimes expose setOption(SocketOption)
            val std = Class.forName("java.net.StandardSocketOptions")
            val tcpKeepIdle = std.getField("TCP_KEEPIDLE").get(null)
            val tcpKeepInterval = std.getField("TCP_KEEPINTERVAL").get(null)
            val setOption = Socket::class.java.getMethod(
                "setOption",
                Class.forName("java.net.SocketOption"),
                Any::class.java
            )
            // idle 30s, probe every 15s — gentle on mobile radios
            setOption.invoke(socket, tcpKeepIdle, 30)
            setOption.invoke(socket, tcpKeepInterval, 15)
        } catch (_: Exception) {
            // SocketOption TCP_KEEP* not on all API levels
        }
    }

    /** System-level HTTP client defaults that improve connection reuse. */
    fun applyHttpSystemProperties() {
        try {
            System.setProperty("http.keepAlive", "true")
            System.setProperty("http.maxConnections", "12")
            // Keep idle pooled connections a bit longer (ms) — helps speed-test phases
            System.setProperty("http.keepAliveDuration", "30000")
        } catch (_: Exception) { }
    }
}
