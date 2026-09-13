package com.mporttech.pro.core.common

/**
 * One-shot UI events (snackbar, navigation, toast).
 */
sealed class UiEvent {
    data class Message(val text: String) : UiEvent()
    data class Navigate(val route: String) : UiEvent()
    data object NavigateBack : UiEvent()
}
