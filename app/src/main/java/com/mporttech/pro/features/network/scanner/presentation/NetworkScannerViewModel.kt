package com.mporttech.pro.features.network.scanner.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.usecase.network.ScanNetworkUseCase
import com.mporttech.pro.features.network.scanner.model.ScanUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkScannerViewModel @Inject constructor(
    private val scanNetwork: ScanNetworkUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _ui.asStateFlow()

    private var scanJob: Job? = null

    fun setAuthorized(value: Boolean) {
        _ui.update { it.copy(authorized = value) }
    }

    fun scan(authorized: Boolean = _ui.value.authorized) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _ui.update { it.copy(loading = true, status = "Searching network…", error = null) }
            when (val r = scanNetwork(authorized)) {
                is Result.Success -> {
                    _ui.update {
                        it.copy(
                            loading = false,
                            devices = r.data,
                            status = "${r.data.size} devices found"
                        )
                    }
                }
                is Result.Error -> {
                    _ui.update {
                        it.copy(loading = false, error = r.message, status = r.message)
                    }
                }
                Result.Loading -> Unit
            }
        }
    }

    fun quickScan() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _ui.update { it.copy(loading = true, status = "Searching nearby…", error = null) }
            when (val quick = scanNetwork(false)) {
                is Result.Success -> {
                    _ui.update {
                        it.copy(
                            devices = quick.data,
                            status = if (it.authorized) "Expanding scan…" else "${quick.data.size} devices"
                        )
                    }
                }
                is Result.Error -> {
                    _ui.update { it.copy(loading = false, error = quick.message, status = quick.message) }
                    return@launch
                }
                Result.Loading -> Unit
            }
            if (_ui.value.authorized) {
                when (val full = scanNetwork(true)) {
                    is Result.Success -> _ui.update { it.copy(loading = false, devices = full.data, status = "${full.data.size} devices found") }
                    is Result.Error -> _ui.update { it.copy(loading = false, error = full.message, status = full.message) }
                    Result.Loading -> Unit
                }
            } else {
                _ui.update { it.copy(loading = false) }
            }
        }
    }
}
