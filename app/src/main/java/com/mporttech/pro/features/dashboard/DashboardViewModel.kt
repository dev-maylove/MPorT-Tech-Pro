package com.mporttech.pro.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.usecase.network.GetNetworkInfoUseCase
import com.mporttech.pro.features.tools.LiveNetworkInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
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
                try {
                    refreshOnce()
                    val (rx, tx) = LiveNetworkInfo.measureTrafficDeltaMbps(1200)
                    _ui.update { it.copy(rxMbps = rx, txMbps = tx) }
                    // Avoid a tight continuous polling loop when the dashboard stays open.
                    delay(800)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _ui.update { it.copy(error = e.message, loading = false) }
                    delay(2000)
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                refreshOnce()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message, loading = false) }
            }
        }
    }

    private suspend fun refreshOnce() {
        when (val r = getNetworkInfo()) {
            is Result.Success -> {
                val n = r.data
                var gwMs: Long? = null
                var gwOk = false
                val gw = n.gateway
                if (!gw.isNullOrBlank() && n.online) {
                    val (ok, ms) = LiveNetworkInfo.probe(gw, 800)
                    gwOk = ok
                    gwMs = ms
                }
                _ui.update {
                    it.copy(
                        loading = false,
                        online = n.online,
                        transport = n.transport,
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
                _ui.update { it.copy(loading = false, error = r.message, online = false) }
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
