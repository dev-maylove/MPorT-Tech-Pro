package com.mporttech.pro.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mporttech.pro.features.dashboard.DashboardViewModel
import com.mporttech.pro.features.tools.LiveNetworkInfo
import com.mporttech.pro.ui.i18n.t
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private data class DashboardAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val route: String
)

@Composable
fun DashboardScreen(nav: NavController, vm: DashboardViewModel = hiltViewModel()) {
    val dash by vm.uiState.collectAsStateWithLifecycle()
    val actions = listOf(
        DashboardAction("Network Monitor", "Router, latency & traffic", Icons.Default.NetworkCheck, "network"),
        DashboardAction("WiFi Tools", "Signal, channel & networks", Icons.Default.Wifi, "wifiTools"),
        DashboardAction("Speed Test", "Download, upload & quality", Icons.Default.Speed, "speedtest"),
        DashboardAction("Technician Tools", "Diagnostics & utilities", Icons.Default.Build, "tools"),
        DashboardAction("Discovery", "LAN devices · live", Icons.Default.Search, "discovery"),
        DashboardAction("Signal", "RSSI & latency hub", Icons.Default.BarChart, "signalHub"),
        DashboardAction("MikroTik", "RouterOS session", Icons.Default.Router, "mikrotik"),
        DashboardAction("LAN Scanner", "V2 UseCase scan", Icons.Default.Search, "networkScanner"),
        DashboardAction("Jobs", "Installation & repair tasks", Icons.Default.ConfirmationNumber, "jobs")
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(
                        Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(11.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "MPorT TECH",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        t("dash.subtitle"),
                        fontSize = 9.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.Default.Notifications,
                    contentDescription = "Alerts",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { nav.navigate("alerts") }
                )
                Spacer(Modifier.width(12.dp))
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { nav.navigate("settings") }
                )
            }
        }
        item { TechnicianIdentityCard(nav) }
        item { NetworkHealthCard(dash) }
        item { DashboardOverview(nav) }
        item {
            Text(
                t("dash.bandwidth"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
        item { BandwidthCard(dash) }
        item {
            Text(
                t("dash.quick_access"),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        }
        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier.height(200.dp),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(actions) { action ->
                    QuickAccessCard(action) { nav.navigate(action.route) }
                }
            }
        }
    }
}

@Composable
private fun TechnicianIdentityCard(nav: NavController) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("Budi Santoso", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(
                    "Teknisi Lapangan",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AssistChip(
                onClick = { nav.navigate("profile") },
                label = { Text("Online", fontSize = 9.sp) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Circle,
                        contentDescription = null,
                        tint = Color(0xFF35E381),
                        modifier = Modifier.size(8.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun NetworkHealthCard(dash: DashboardUiState) {
    val score = when {
        !dash.online -> 0.15f
        dash.gatewayReachable && (dash.gatewayMs ?: 999) < 50 -> 0.95f
        dash.gatewayReachable && (dash.gatewayMs ?: 999) < 120 -> 0.8f
        dash.gatewayReachable -> 0.65f
        else -> 0.35f
    }
    val linkOnline = dash.online
    val gwOk = dash.gatewayReachable
    val gwMs = dash.gatewayMs
    val link = dash
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(t("dash.network_health"), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.weight(1f))
                Text(
                    if (link.online) link.transport else "Offline",
                    fontSize = 11.sp,
                    color = if (link.online) Color(0xFF35E381) else Color(0xFFFF5E67)
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { score },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = when {
                    score > 0.8f -> Color(0xFF35E381)
                    score > 0.5f -> Color(0xFFFFB020)
                    else -> Color(0xFFFF5E67)
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                buildString {
                    append(link.ssid?.let { "$it · " } ?: "")
                    append(link.ip ?: "No IP")
                    append(gwMs?.let { " · GW ${it}ms" } ?: "")
                },
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DashboardOverview(nav: NavController) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        MiniStat("Device\nManager", "Go", Color(0xFF35E381), Modifier.weight(1f)) {
            nav.navigate("devices")
        }
        MiniStat("Device\nScan", "LAN", Color(0xFFFF5E67), Modifier.weight(1f)) {
            nav.navigate("devices")
        }
        MiniStat("Active\nAlerts", "Live", Color(0xFFFFB547), Modifier.weight(1f)) {
            nav.navigate("alerts")
        }
    }
}

@Composable
private fun MiniStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 18.sp)
            Text(
                label,
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 11.sp
            )
        }
    }
}

@Composable
private fun BandwidthCard(dash: DashboardUiState) {
    val rx = dash.rxMbps
    val tx = dash.txMbps
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Text(String.format("%.1f Mbps", rx), fontWeight = FontWeight.Bold)
                    Text(t("dash.download"), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(Modifier.weight(1f)) {
                    Text(String.format("%.1f Mbps", tx), fontWeight = FontWeight.Bold, color = Color(0xFFB680FF))
                    Text(t("dash.upload"), fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { ((rx + tx) / 100.0).toFloat().coerceIn(0.02f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primaryContainer
            )
            Spacer(Modifier.height(7.dp))
            Text(
                t("dash.live_traffic"),
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun QuickAccessCard(action: DashboardAction, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick)
    ) {
        Column(
            Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    action.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .padding(10.dp)
                        .size(18.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                action.title,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
            Text(
                action.subtitle,
                fontSize = 8.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
