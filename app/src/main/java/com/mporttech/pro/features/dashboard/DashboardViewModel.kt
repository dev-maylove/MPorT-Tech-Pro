package com.mporttech.pro.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.usecase.network.GetNetworkInfoUseCase
import com.mporttech.pro.features.tools.LiveNetworkInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getNetworkInfo: GetNetworkInfoUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _ui.asStateFlow()

    private var loopJob: Job? = null

    init {
        startMonitoring()
    }

    fun startMonitoring() {
        if (loopJob?.isActive == true) return
        loopJob = viewModelScope.launch {
            while (true) {
                refreshOnce()
                // traffic sample (~1.2s)
                val (rx, tx) = LiveNetworkInfo.measureTrafficDeltaMbps(1200)
                _ui.update { it.copy(rxMbps = rx, txMbps = tx) }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { refreshOnce() }
    }

    private suspend fun refreshOnce() {
        when (val r = getNetworkInfo()) {
            is Result.Success -> {
                val n = r.data
                var gwMs: Long? = null
                var gwOk = false
                val gw = n.gateway
                if (!gw.isNullOrBlank()) {
                    val (ok, ms) = LiveNetworkInfo.probe(gw, 800)
                    gwOk = ok
                    gwMs = ms
                }
                _ui.update {
                    it.copy(
                        loading = false,
                        online = n.ip != null,
                        transport = if (n.ssid != null) "Wi‑Fi" else "Network",
                        ssid = n.ssid,
                        ip = n.ip,
                        gateway = n.gateway,
                        dns = n.dns,
                        linkMbps = n.linkSpeedMbps,
                        gatewayMs = gwMs,
                        gatewayReachable = gwOk,
                        error = null
                    )
                }
            }
            is Result.Error -> {
                _ui.update { it.copy(loading = false, error = r.message) }
            }
            Result.Loading -> {
                _ui.update { it.copy(loading = true) }
            }
        }
    }

    override fun onCleared() {
        loopJob?.cancel()
        super.onCleared()
    }
}
