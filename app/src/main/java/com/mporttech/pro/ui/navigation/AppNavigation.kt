package com.mporttech.pro.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.mporttech.pro.ui.i18n.t
import com.mporttech.pro.features.about.AboutScreen
import com.mporttech.pro.features.customers.CustomerScreen
import com.mporttech.pro.features.dashboard.DashboardScreen
import com.mporttech.pro.features.discovery.DiscoveryHubScreen
import com.mporttech.pro.features.discovery.SignalHubScreen
import com.mporttech.pro.features.discovery.LatencyHubScreen
import com.mporttech.pro.features.discovery.DeviceDetailRichScreen
import com.mporttech.pro.features.speedtest.SpeedTestResultsScreen
import com.mporttech.pro.features.diagnostic.DiagnosticScreen
import com.mporttech.pro.features.networktools.DnsLookupScreen
import com.mporttech.pro.features.networktools.PingToolScreen
import com.mporttech.pro.features.networktools.PortCheckerScreen
import com.mporttech.pro.features.networktools.TracerouteScreen
import com.mporttech.pro.features.tickets.TicketScreen
import com.mporttech.pro.features.tools.*

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val route = nav.currentBackStackEntryAsState().value?.destination?.route ?: "dashboard"
    val topLevel = setOf("dashboard", "network", "tools", "alerts", "profile")

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (route in topLevel) {
                Surface(tonalElevation = 8.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(Color(0xFF08111F)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomItem(
                            selected = route == "dashboard",
                            onClick = {
                                nav.navigate("dashboard") {
                                    launchSingleTop = true
                                    popUpTo("dashboard") { inclusive = false }
                                }
                            },
                            icon = Icons.Default.Home,
                            label = t("nav.home"),
                            modifier = Modifier.weight(1f)
                        )
                        BottomItem(
                            selected = route == "network",
                            onClick = { nav.navigate("network") { launchSingleTop = true } },
                            icon = Icons.Default.NetworkCheck,
                            label = t("nav.network"),
                            modifier = Modifier.weight(1f)
                        )
                        BottomItem(
                            selected = route == "tools",
                            onClick = { nav.navigate("tools") { launchSingleTop = true } },
                            icon = Icons.Default.Build,
                            label = t("nav.tools"),
                            modifier = Modifier.weight(1f)
                        )
                        BottomItem(
                            selected = route == "alerts",
                            onClick = { nav.navigate("alerts") { launchSingleTop = true } },
                            icon = Icons.Default.Notifications,
                            label = t("nav.alerts"),
                            modifier = Modifier.weight(1f)
                        )
                        BottomItem(
                            selected = route == "profile",
                            onClick = { nav.navigate("profile") { launchSingleTop = true } },
                            icon = Icons.Default.Person,
                            label = t("nav.profile"),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = "dashboard",
            modifier = Modifier.padding(padding)
        ) {
            composable("dashboard") { DashboardScreen(nav) }
            composable("network") { NetworkMonitorScreen(nav) }
            composable("tools") { TechnicianToolsScreen(nav) }
            composable("alerts") { AlertsScreen(nav) }
            composable("profile") { ProfileScreen(nav) }
            composable("wifiTools") { WifiToolsScreen(nav) }
            composable("wifi") { WifiAnalyzerScreen(nav) }
            composable("scanner") { NetworkScannerScreen(nav) }
            composable("speedtest") { SpeedTestScreen(nav) }
            composable("mikrotik") { MikroTikScreen(nav) }
            composable("activity") { ActivityScreen(nav) }
            composable("customers") { CustomerScreen(nav) }
            composable("tickets") { TicketScreen(nav) }
            composable("jobs") { JobsScreen(nav) }
            composable("diagnostic") { DiagnosticScreen(nav) }
            composable("ping") { PingToolScreen(nav) }
            composable("traceroute") { TracerouteScreen(nav) }
            composable("dns") { DnsLookupScreen(nav) }
            composable("portcheck") { PortCheckerScreen(nav) }
            composable("devices") { DeviceManagerScreen(nav) }
            composable("deviceDetail") { DeviceDetailScreen(nav) }
            composable("alertDetail") { AlertDetailScreen(nav) }
            composable("reports") { ReportsScreen(nav) }
            composable("settings") { SettingsScreen(nav) }
            composable("about") { AboutScreen(nav) }
            composable("discovery") { DiscoveryHubScreen(nav) }
            composable("signalHub") { SignalHubScreen(nav) }
            composable("latencyHub") { LatencyHubScreen(nav) }
            composable("deviceDetailRich") { DeviceDetailRichScreen(nav) }
            composable("speedResults") { SpeedTestResultsScreen(nav) }
        }
    }
}

@Composable
private fun BottomItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier
) {
    val tint = if (selected) Color(0xFF24B8FF) else Color(0xFF93A6BE)
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(19.dp))
        Spacer(Modifier.height(3.dp))
        Text(text = label, color = tint, fontSize = 8.sp)
    }
}
