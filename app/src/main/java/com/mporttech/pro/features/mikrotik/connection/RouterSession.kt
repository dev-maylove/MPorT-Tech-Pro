package com.mporttech.pro.features.mikrotik.connection

import com.mporttech.pro.core.security.RouterCredentials
import com.mporttech.pro.features.mikrotik.model.RouterInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Minimal RouterOS API session (plain API, default TCP 8728).
 * The session is intentionally short-lived: credentials are authenticated and
 * router metadata is read, then the socket is closed. This avoids leaking a
 * socket when the UI/ViewModel is destroyed.
 */
@Singleton
class RouterSession @Inject constructor() {
    @Volatile var credentials: RouterCredentials? = null
        private set
    @Volatile var info: RouterInfo = RouterInfo()
        private set

    suspend fun connect(credentials: RouterCredentials): RouterInfo = withContext(Dispatchers.IO) {
        require(credentials.host.isNotBlank()) { "Host tidak valid" }
        require(credentials.port in 1..65535) { "Port tidak valid" }

        val socket = Socket()
        try {
            socket.tcpNoDelay = true
            socket.connect(InetSocketAddress(credentials.host, credentials.port), CONNECT_TIMEOUT_MS)
            socket.soTimeout = READ_TIMEOUT_MS

            BufferedInputStream(socket.getInputStream()).use { input ->
                BufferedOutputStream(socket.getOutputStream()).use { output ->
                    val api = RouterOsApi(input, output)
                    api.login(credentials.username, credentials.password)

                    val identity = api.printOne("/system/identity/print")["name"]
                        ?: credentials.host
                    val resource = api.printOne("/system/resource/print")
                    val result = RouterInfo(
                        identity = identity,
                        boardName = resource["board-name"] ?: resource["platform"] ?: "RouterOS",
                        version = resource["version"] ?: "Unknown",
                        uptime = resource["uptime"] ?: "Unknown",
                        connected = true
                    )
                    this@RouterSession.credentials = credentials
                    this@RouterSession.info = result
                    result
                }
            }
        } catch (e: CancellationException) {
            // Cancellation is lifecycle control flow, not a connection failure.
            // Do not swallow it; finally still closes the socket.
            throw e
        } catch (e: Exception) {
            this@RouterSession.credentials = null
            this@RouterSession.info = RouterInfo()
            throw when (e) {
                is RouterOsApiException -> e
                is IOException -> IOException("Router tidak dapat dihubungi: ${e.message ?: "I/O error"}", e)
                else -> e
            }
        } finally {
            try { socket.close() } catch (_: IOException) { }
        }
    }

    fun disconnect() {
        credentials = null
        info = RouterInfo()
    }

    val isConnected: Boolean get() = info.connected

    private class RouterOsApi(
        private val input: BufferedInputStream,
        private val output: BufferedOutputStream
    ) {
        fun login(username: String, password: String) {
            send("/login", "=name=$username", "=password=$password")
            val reply = readReply()
            if (reply.trap != null) throw RouterOsApiException("Authentication gagal: ${reply.trap}")
            if (!reply.done) throw RouterOsApiException("RouterOS tidak menyelesaikan proses login")
        }

        fun printOne(command: String): Map<String, String> {
            send(command)
            val reply = readReply()
            if (reply.trap != null) throw RouterOsApiException("RouterOS error: ${reply.trap}")
            return reply.records.firstOrNull() ?: emptyMap()
        }

        private fun send(vararg words: String) {
            words.forEach { writeWord(it) }
            writeWord("")
            output.flush()
        }

        private fun readReply(): Reply {
            val records = mutableListOf<Map<String, String>>()
            var trap: String? = null
            while (true) {
                val sentence = mutableListOf<String>()
                while (true) {
                    val word = readWord()
                    if (word.isEmpty()) break
                    sentence += word
                }
                if (sentence.isEmpty()) continue
                when (sentence.first()) {
                    "!re" -> records += parseAttributes(sentence.drop(1))
                    "!trap", "!fatal" -> trap = parseAttributes(sentence.drop(1))["message"] ?: sentence.joinToString(" ")
                    "!done" -> return Reply(records, trap, true)
                }
            }
        }

        private fun parseAttributes(words: List<String>): Map<String, String> = buildMap {
            words.forEach { word ->
                if (word.startsWith("=") || word.startsWith(".")) {
                    val body = word.substring(1)
                    val i = body.indexOf('=')
                    if (i >= 0) put(body.substring(0, i), body.substring(i + 1))
                }
            }
        }

        private fun writeWord(word: String) {
            val bytes = word.toByteArray(Charsets.UTF_8)
            writeLength(bytes.size)
            output.write(bytes)
        }

        private fun readWord(): String {
            val length = readLength()
            if (length == 0) return ""
            if (length < 0 || length > MAX_WORD_BYTES) throw RouterOsApiException("RouterOS API word terlalu besar")
            val bytes = ByteArray(length)
            var offset = 0
            while (offset < length) {
                val n = input.read(bytes, offset, length - offset)
                if (n < 0) throw IOException("Router menutup koneksi")
                offset += n
            }
            return String(bytes, Charsets.UTF_8)
        }

        private fun writeLength(length: Int) {
            require(length >= 0)
            when {
                length < 0x80 -> output.write(length)
                length < 0x4000 -> { output.write((length shr 8) or 0x80); output.write(length and 0xFF) }
                length < 0x20_0000 -> { output.write((length shr 16) or 0xC0); output.write((length shr 8) and 0xFF); output.write(length and 0xFF) }
                length < 0x1000_0000 -> { output.write((length shr 24) or 0xE0); output.write((length shr 16) and 0xFF); output.write((length shr 8) and 0xFF); output.write(length and 0xFF) }
                else -> { output.write(0xF0); output.write(length shr 24); output.write(length shr 16); output.write(length shr 8); output.write(length) }
            }
        }

        private fun readLength(): Int {
            val first = input.read()
            if (first < 0) throw IOException("Router menutup koneksi")
            return when {
                first and 0x80 == 0 -> first
                first and 0xC0 == 0x80 -> ((first and 0x3F) shl 8) or readByte()
                first and 0xE0 == 0xC0 -> ((first and 0x1F) shl 16) or (readByte() shl 8) or readByte()
                first and 0xF0 == 0xE0 -> ((first and 0x0F) shl 24) or (readByte() shl 16) or (readByte() shl 8) or readByte()
                first == 0xF0 -> (readByte() shl 24) or (readByte() shl 16) or (readByte() shl 8) or readByte()
                else -> throw RouterOsApiException("RouterOS API length tidak valid")
            }
        }

        private fun readByte(): Int {
            val value = input.read()
            if (value < 0) throw IOException("Router menutup koneksi")
            return value
        }
    }

    private data class Reply(val records: List<Map<String, String>>, val trap: String?, val done: Boolean)

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 8_000
        const val MAX_WORD_BYTES = 4 * 1024 * 1024
    }
}

class RouterOsApiException(message: String) : IOException(message)
