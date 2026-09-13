package com.mporttech.pro.core.common

import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import retrofit2.HttpException

object NetworkErrors {
    fun userMessage(t: Throwable?): String {
        if (t == null) return "Terjadi kesalahan. Coba lagi."
        return when (t) {
            is SocketTimeoutException -> "Koneksi timeout. Periksa jaringan lalu coba lagi."
            is UnknownHostException -> "Server tidak ditemukan (${Constants.API_BASE_URL}). Pastikan HP satu WiFi dengan server."
            is ConnectException -> "Tidak dapat terhubung ke ${Constants.API_BASE_URL}. Cek server online & firewall."
            is HttpException -> httpMessage(t.code())
            else -> {
                val msg = t.message.orEmpty()
                when {
                    msg.contains("401") || msg.contains("Unauthorized", true) ->
                        "Sesi berakhir. Silakan masuk lagi."
                    msg.contains("timeout", true) ->
                        "Koneksi timeout. Coba lagi."
                    msg.contains("Unable to resolve host", true) ->
                        "Tidak ada koneksi internet."
                    msg.contains("Failed to connect", true) ->
                        "Tidak dapat terhubung ke server."
                    else -> msg.take(120).ifBlank { "Terjadi kesalahan. Coba lagi." }
                }
            }
        }
    }

    fun httpMessage(code: Int): String = when (code) {
        401 -> "Sesi berakhir. Silakan masuk lagi."
        403 -> "Akses ditolak."
        404 -> "Data tidak ditemukan."
        408, 504 -> "Koneksi timeout. Coba lagi."
        429 -> "Terlalu banyak permintaan. Tunggu sebentar."
        in 500..599 -> "Server sedang bermasalah. Coba lagi nanti."
        else -> "Gagal memproses permintaan ($code)."
    }

    fun fromHttpCode(code: Int): String = httpMessage(code)
}
