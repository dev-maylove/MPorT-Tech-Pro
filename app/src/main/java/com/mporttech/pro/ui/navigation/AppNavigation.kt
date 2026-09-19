package com.mporttech.pro.ui.navigation

import kotlinx.coroutines.delay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.compose.ui.platform.LocalContext
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
import com.mporttech.pro.features.networktools.PortCheckerScreen
import com.mporttech.pro.features.tickets.TicketScreen
import com.mporttech.pro.features.tools.*
import com.mporttech.pro.features.tools.AlertBadgeStore
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mporttech.pro.features.auth.TechnicianAdminScreen
import com.mporttech.pro.features.auth.LoginScreen
import com.mporttech.pro.core.auth.SessionManager
import com.mporttech.pro.features.mikrotik.presentation.MikroTikScreen
import com.mporttech.pro.features.network.scanner.presentation.NetworkScannerScreen
import com.mporttech.pro.features.diagnostics.ping.PingScreen
import com.mporttech.pro.features.diagnostics.dns.DnsScreen
import com.mporttech.pro.features.diagnostics.traceroute.TracerouteScreen as DiagnosticsTracerouteScreen

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val route = nav.currentBackStackEntryAsState().value?.destination?.route ?: "dashboard"
    // Re-read session when route changes (e.g. after login) so StaffOnly gates update
    val isStaff = SessionManager.isStaff(context)
    val isGuest = SessionManager.isGuest(context)
    val topLevel = setOf("dashboard", "network", "tools", "alerts", "profile")
    var alertBadge by remember { mutableIntStateOf(AlertBadgeStore.count) }
    // Single long-lived poller — do not key on route (avoids cancel/restart storms)
    LaunchedEffect(Unit) {
        while (true) {
            alertBadge = AlertBadgeStore.count
            kotlinx.coroutines.delay(3000)
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            if (route in topLevel) {
                Surface(
                    tonalElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .background(MaterialTheme.colorScheme.surface),
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
                        if (isStaff) {
                            BottomItem(
                                selected = route == "tools",
                                onClick = { nav.navigate("tools") { launchSingleTop = true } },
                                icon = Icons.Default.Build,
                                label = t("nav.tools"),
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            BottomItem(
                                selected = route == "diagnostic" || route == "ping",
                                onClick = { nav.navigate("diagnostic") { launchSingleTop = true } },
                                icon = Icons.Default.Build,
                                label = t("nav.diagnostic"),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        BottomItem(
                            selected = route == "alerts",
                            onClick = { nav.navigate("alerts") { launchSingleTop = true } },
                            icon = Icons.Default.Notifications,
                            label = t("nav.alerts"),
                            modifier = Modifier.weight(1f),
                            badgeCount = alertBadge
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
            composable("tools") { StaffOnly(nav) { TechnicianToolsScreen(nav) } }
            composable("alerts") { AlertsScreen(nav) }
            composable("profile") { ProfileScreen(nav) }
            composable("wifiTools") { WifiToolsScreen(nav) }
            composable("wifi") { WifiAnalyzerScreen(nav) }
            composable("scanner") { NetworkScannerScreen(nav) }
            composable("networkScanner") { NetworkScannerScreen(nav) }
            composable("speedtest") { SpeedTestScreen(nav) }
            composable("mikrotik") { StaffOnly(nav) { MikroTikScreen(nav) } }
            composable("activity") { ActivityScreen(nav) }
            composable("customers") { AdminOnly(nav) { CustomerScreen(nav) } }
            composable("tickets") { StaffOnly(nav) { TicketScreen(nav) } }
            composable("jobs") { StaffOnly(nav) { JobsScreen(nav) } }
            composable("diagnostic") { DiagnosticScreen(nav) }
            composable("ping") { PingScreen(nav) }
            composable("traceroute") { DiagnosticsTracerouteScreen(nav) }
            composable("dns") { DnsScreen(nav) }
            composable("portcheck") { PortCheckerScreen(nav) }
            composable("devices") { DeviceManagerScreen(nav) }
            composable("deviceDetail") { DeviceDetailScreen(nav) }
            composable("alertDetail") { AlertDetailScreen(nav) }
            composable("reports") { AdminOnly(nav) { ReportsScreen(nav) } }
            composable("settings") { SettingsScreen(nav) }
            composable("about") { AboutScreen(nav) }
            composable("techAdmin") {
                if (SessionManager.isAdmin(LocalContext.current)) TechnicianAdminScreen(nav)
                else {
                    // non-admin redirected
                    androidx.compose.runtime.LaunchedEffect(Unit) { nav.popBackStack() }
                }
            }
            composable("login") {
                LoginScreen(
                    onLoggedIn = {
                        nav.navigate("dashboard") {
                            popUpTo("dashboard") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onBack = { nav.popBackStack() }
                )
            }
            composable("discovery") { DiscoveryHubScreen(nav) }
            composable("signalHub") { SignalHubScreen(nav) }
            composable("latencyHub") { LatencyHubScreen(nav) }
            composable("deviceDetailRich") { DeviceDetailRichScreen(nav) }
            composable("speedResults") { SpeedTestResultsScreen(nav) }
        }
    }
}


@Composable
private fun StaffOnly(nav: NavController, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    if (SessionManager.isStaff(ctx)) {
        content()
    } else {
        LaunchedEffect(Unit) {
            nav.popBackStack()
        }
    }
}

@Composable
private fun AdminOnly(nav: NavController, content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    if (SessionManager.isAdmin(ctx)) {
        content()
    } else {
        LaunchedEffect(Unit) {
            nav.popBackStack()
        }
    }
}

@Composable
private fun BottomItem(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier,
    badgeCount: Int = 0
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box {
            Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(19.dp))
            if (badgeCount > 0) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-4).dp)
                        .size(14.dp)
                        .background(Color(0xFFFF2E63), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (badgeCount > 9) "9+" else badgeCount.toString(),
                        color = Color.White,
                        fontSize = 7.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(3.dp))
        Text(text = label, color = tint, fontSize = 8.sp)
    }
}
