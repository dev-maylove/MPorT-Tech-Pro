package com.mporttech.pro.domain.model

data class PingSample(
    val seq: Int,
    val success: Boolean,
    val latencyMs: Long? = null,
    val message: String = ""
)

data class PingResult(
    val host: String,
    val sent: Int,
    val received: Int,
    val lost: Int,
    val lossPct: Double,
    val minMs: Long? = null,
    val avgMs: Double? = null,
    val maxMs: Long? = null,
    val samples: List<PingSample> = emptyList()
)
