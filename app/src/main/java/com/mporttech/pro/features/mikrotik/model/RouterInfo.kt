package com.mporttech.pro.features.mikrotik.model

data class RouterInfo(
    val identity: String = "",
    val boardName: String = "",
    val version: String = "",
    val uptime: String = "",
    val connected: Boolean = false
)

data class RouterInterface(
    val name: String,
    val type: String = "",
    val running: Boolean = false,
    val rxBytes: Long = 0,
    val txBytes: Long = 0
)
