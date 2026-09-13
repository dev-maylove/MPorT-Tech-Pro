package com.mporttech.pro.features.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mporttech.pro.core.auth.AppUser
import com.mporttech.pro.core.common.Result
import com.mporttech.pro.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val loading: Boolean = false,
    val error: String? = null,
    val successUser: AppUser? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _ui.asStateFlow()

    fun login(identity: String, password: String) {
        if (_ui.value.loading) return
        viewModelScope.launch {
            _ui.value = LoginUiState(loading = true)
            when (val result = authRepository.login(identity, password)) {
                is Result.Success -> {
                    _ui.value = LoginUiState(loading = false, successUser = result.data)
                }
                is Result.Error -> {
                    _ui.value = LoginUiState(loading = false, error = result.message)
                }
                Result.Loading -> {
                    _ui.value = LoginUiState(loading = true)
                }
            }
        }
    }

    fun consumeError() {
        _ui.value = _ui.value.copy(error = null)
    }

    fun consumeSuccess() {
        _ui.value = _ui.value.copy(successUser = null)
    }
}
