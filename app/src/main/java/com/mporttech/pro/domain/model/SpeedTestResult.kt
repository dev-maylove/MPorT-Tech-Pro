package com.mporttech.pro.domain.model

data class SpeedTestResult(
    val timestamp: Long,
    val serverName: String,
    val location: String,
    val downloadMbps: Double,
    val uploadMbps: Double,
    val pingMs: Double,
    val jitterMs: Double,
    val lossPct: Double
)
