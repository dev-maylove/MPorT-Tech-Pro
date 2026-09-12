package com.mporttech.pro.navigation

/**
 * Type-safe route catalog (Full Core V2).
 * Existing AppNavigation still uses string routes; migrate gradually.
 */
sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Network : Screen("network")
    data object Tools : Screen("tools")
    data object Alerts : Screen("alerts")
    data object Profile : Screen("profile")

    data object Discovery : Screen("discovery")
    data object SignalHub : Screen("signalHub")
    data object LatencyMonitor : Screen("latencyMonitor")
    data object DeviceDetail : Screen("deviceDetailRich")

    data object SpeedTest : Screen("speedtest")
    data object SpeedResults : Screen("speedResults")

    data object Ping : Screen("ping")
    data object Dns : Screen("dns")
    data object Traceroute : Screen("traceroute")
    data object WifiAnalyzer : Screen("wifiAnalyzer")
    data object WifiTools : Screen("wifiTools")

    data object History : Screen("history")
    data object Settings : Screen("settings")
    data object About : Screen("about")
    data object MikroTik : Screen("mikrotik")
    data object NetworkScanner : Screen("networkScanner")
}
