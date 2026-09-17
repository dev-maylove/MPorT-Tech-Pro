package com.mporttech.pro.features.network.scanner.model

import com.mporttech.pro.domain.model.NetworkDevice

data class ScanUiState(
    val loading: Boolean = false,
    val authorized: Boolean = false,
    val devices: List<NetworkDevice> = emptyList(),
    val status: String = "",
    val error: String? = null
)
