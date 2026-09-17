package com.mporttech.pro.features.diagnostics.ping

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.mporttech.pro.features.networktools.PingToolScreen

/**
 * V2 entry — delegates to existing premium Ping UI.
 * Continuous logic also available via [PingViewModel] for future redesign.
 */
@Composable
fun PingScreen(nav: NavController? = null) {
    PingToolScreen(nav)
}
