package com.mporttech.pro.domain.model

data class NetworkDevice(
    val name: String,
    val ip: String,
    val mac: String? = null,
    val vendor: String? = null,
    val online: Boolean = false,
    val latencyMs: Long? = null,
    val kind: String = "host",
    val openPorts: List<Int> = emptyList()
)
