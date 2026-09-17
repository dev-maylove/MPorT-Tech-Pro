package com.mporttech.pro.features.diagnostics.traceroute

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import com.mporttech.pro.features.networktools.TracerouteScreen as LegacyTracerouteScreen

@Composable
fun TracerouteScreen(nav: NavController? = null) {
    LegacyTracerouteScreen(nav)
}
