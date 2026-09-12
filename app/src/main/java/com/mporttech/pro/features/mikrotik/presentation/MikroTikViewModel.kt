package com.mporttech.pro.features.mikrotik.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mporttech.pro.core.security.CredentialManager
import com.mporttech.pro.core.security.RouterCredentials
import com.mporttech.pro.features.mikrotik.connection.RouterSession
import com.mporttech.pro.features.mikrotik.model.RouterInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MikroTikUiState(
    val host: String = "",
    val username: String = "admin",
    val password: String = "",
    val port: String = "8728",
    val connecting: Boolean = false,
    val info: RouterInfo = RouterInfo(),
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class MikroTikViewModel @Inject constructor(
    private val credentials: CredentialManager,
    private val session: RouterSession
) : ViewModel() {

    private val _ui = MutableStateFlow(MikroTikUiState())
    val uiState: StateFlow<MikroTikUiState> = _ui.asStateFlow()

    init {
        credentials.loadRouter()?.let { c ->
            _ui.update {
                it.copy(
                    host = c.host,
                    username = c.username,
                    password = c.password,
                    port = c.port.toString(),
                    info = session.info
                )
            }
        }
    }

    fun updateHost(v: String) = _ui.update { it.copy(host = v) }
    fun updateUser(v: String) = _ui.update { it.copy(username = v) }
    fun updatePass(v: String) = _ui.update { it.copy(password = v) }
    fun updatePort(v: String) = _ui.update { it.copy(port = v) }

    fun connect() {
        viewModelScope.launch {
            val state = _ui.value
            if (state.host.isBlank()) {
                _ui.update { it.copy(error = "Host wajib diisi") }
                return@launch
            }
            _ui.update { it.copy(connecting = true, error = null, message = null) }
            try {
                val c = RouterCredentials(
                    host = state.host.trim(),
                    username = state.username.trim().ifBlank { "admin" },
                    password = state.password,
                    port = state.port.toIntOrNull() ?: 8728
                )
                credentials.saveRouter(c)
                val info = session.connect(c)
                _ui.update {
                    it.copy(
                        connecting = false,
                        info = info,
                        message = "Session siap · API RouterOS menyusul"
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(connecting = false, error = e.message ?: "Connect failed")
                }
            }
        }
    }

    fun disconnect() {
        session.disconnect()
        _ui.update { it.copy(info = RouterInfo(), message = "Disconnected") }
    }

    fun clearSaved() {
        credentials.clearRouter()
        session.disconnect()
        _ui.update {
            MikroTikUiState(message = "Credentials cleared")
        }
    }
}
