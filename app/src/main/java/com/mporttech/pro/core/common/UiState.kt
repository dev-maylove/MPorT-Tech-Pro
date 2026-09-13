package com.mporttech.pro.core.common

/**
 * Generic UI state for feature screens (Compose + ViewModel).
 */
data class UiState<T>(
    val data: T? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val empty: Boolean = false
) {
    companion object {
        fun <T> loading(): UiState<T> = UiState(loading = true)
        fun <T> success(data: T): UiState<T> = UiState(data = data)
        fun <T> error(message: String): UiState<T> = UiState(error = message)
        fun <T> empty(): UiState<T> = UiState(empty = true)
    }
}
