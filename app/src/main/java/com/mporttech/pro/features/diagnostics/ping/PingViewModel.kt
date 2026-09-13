package com.mporttech.pro.features.diagnostics.ping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mporttech.pro.core.common.Result
import com.mporttech.pro.domain.model.PingResult
import com.mporttech.pro.domain.usecase.diagnostics.PingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class PingUiState(
    val host: String = "8.8.8.8",
    val running: Boolean = false,
    val result: PingResult? = null,
    val liveLog: List<String> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class PingViewModel @Inject constructor(
    private val pingUseCase: PingUseCase
) : ViewModel() {

    private val _ui = MutableStateFlow(PingUiState())
    val uiState: StateFlow<PingUiState> = _ui.asStateFlow()
    private val runFlag = AtomicBoolean(false)
    private var job: Job? = null

    fun setHost(host: String) {
        _ui.update { it.copy(host = host.trim()) }
    }

    fun startContinuous() {
        if (runFlag.get()) return
        val host = _ui.value.host
        if (host.isBlank()) {
            _ui.update { it.copy(error = "Host kosong") }
            return
        }
        runFlag.set(true)
        _ui.update {
            it.copy(running = true, error = null, liveLog = emptyList(), result = null)
        }
        job = viewModelScope.launch {
            var seq = 0
            val logs = mutableListOf<String>()
            var sent = 0
            var received = 0
            var sum = 0.0
            var minMs: Long? = null
            var maxMs: Long? = null
            while (isActive && runFlag.get()) {
                seq++
                when (val r = pingUseCase(host, count = 1)) {
                    is Result.Success -> {
                        val sample = r.data.samples.firstOrNull()
                        sent++
                        val ok = sample?.success == true
                        val ms = sample?.latencyMs
                        if (ok && ms != null) {
                            received++
                            sum += ms
                            if (minMs == null || ms < minMs) minMs = ms
                            if (maxMs == null || ms > maxMs) maxMs = ms
                        }
                        val line = sample?.message?.ifBlank {
                            if (ok) "seq=$seq time=${ms}ms" else "seq=$seq timeout"
                        } ?: "seq=$seq no response"
                        logs.add(line)
                        if (logs.size > 40) logs.removeAt(0)
                        _ui.update {
                            it.copy(
                                liveLog = logs.toList(),
                                result = PingResult(
                                    host = host,
                                    sent = sent,
                                    received = received,
                                    lost = sent - received,
                                    lossPct = if (sent > 0) (sent - received) * 100.0 / sent else 0.0,
                                    minMs = minMs,
                                    avgMs = if (received > 0) sum / received else null,
                                    maxMs = maxMs,
                                    samples = r.data.samples
                                )
                            )
                        }
                    }
                    is Result.Error -> {
                        logs.add("seq=$seq error: ${r.message}")
                        if (logs.size > 40) logs.removeAt(0)
                        sent++
                        _ui.update { it.copy(liveLog = logs.toList()) }
                    }
                    Result.Loading -> Unit
                }
                delay(800)
            }
            _ui.update { it.copy(running = false) }
        }
    }

    fun stop() {
        runFlag.set(false)
        _ui.update { it.copy(running = false) }
    }

    override fun onCleared() {
        runFlag.set(false)
        job?.cancel()
        super.onCleared()
    }
}
