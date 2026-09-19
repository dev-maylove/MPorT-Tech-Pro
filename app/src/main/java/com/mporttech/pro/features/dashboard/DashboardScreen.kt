package com.mporttech.pro.features.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Router
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mporttech.pro.core.auth.SessionManager
import com.mporttech.pro.features.tools.LiveNetworkInfo
import com.mporttech.pro.features.tools.AlertBadgeStore
import com.mporttech.pro.ui.i18n.t
import kotlinx.coroutines.delay

private data class DashboardAction(
    val title: String,
    val icon: ImageVector,
    val route: String,
    val color: Color
)

@Composable
fun DashboardScreen(nav: NavController, vm: DashboardViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val dash by vm.uiState.collectAsStateWithLifecycle()
    val isStaff = SessionManager.isStaff(context)
    val isGuest = SessionManager.isGuest(context)

    var onlineCount by remember { mutableIntStateOf(0) }
    var offlineCount by remember { mutableIntStateOf(0) }
    var alertCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(dash.online, dash.gateway) {
        while (true) {
            try {
                val devices = LiveNetworkInfo.discoverDevices(context, authorized = true, endHost = 40)
                onlineCount = devices.count { it.online }
                offlineCount = devices.count { !it.online }
                alertCount = offlineCount + if (!dash.gatewayReachable && dash.online) 1 else 0
                AlertBadgeStore.count = alertCount
            } catch (_: Exception) { }
            delay(15_000)
        }
    }

    val healthScore = when {
        !dash.online -> 15
        dash.gatewayReachable && (dash.gatewayMs ?: 999) < 30 -> 95
        dash.gatewayReachable && (dash.gatewayMs ?: 999) < 60 -> 88
        dash.gatewayReachable && (dash.gatewayMs ?: 999) < 120 -> 75
        dash.gatewayReachable -> 60
        else -> 35
    }
    val healthLabel = when {
        healthScore >= 90 -> "Excellent"
        healthScore >= 75 -> "Good"
        healthScore >= 50 -> "Fair"
        else -> "Poor"
    }
    val healthSub = when {
        !dash.online -> "Offline"
        dash.gatewayReachable -> "Devices detected"
        else -> "Gateway tidak merespons"
    }

    val actions = buildList {
        add(DashboardAction("Network\nMonitor", Icons.Default.NetworkCheck, "network", Color(0xFF00F0FF)))
        add(DashboardAction("WiFi Tools", Icons.Default.Wifi, "wifiTools", Color(0xFF00F0FF)))
        add(DashboardAction("Speed Test", Icons.Default.Speed, "speedtest", Color(0xFF00F0FF)))
        add(DashboardAction("Devices", Icons.Default.Devices, "devices", Color(0xFF00F0FF)))
        if (isStaff) {
            add(DashboardAction("Technician\nTools", Icons.Default.Build, "tools", Color(0xFF00F0FF)))
            add(DashboardAction("Jobs", Icons.Default.ConfirmationNumber, "jobs", Color(0xFF00F0FF)))
            add(DashboardAction("Reports", Icons.Default.BarChart, "reports", Color(0xFF00F0FF)))
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            brush = Brush.linearGradient(listOf(Color(0xFF00F0FF), Color(0xFF1E6FFF))),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Wifi, null, tint = Color(0xFF03060F), modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("MPorT TECH", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFE8FBFF), letterSpacing = 1.sp)
                    Text("Professional network technician toolkit", fontSize = 10.sp, color = Color(0xFF5EC8E8))
                }
                Icon(Icons.Default.Notifications, "Alerts", tint = Color(0xFF9EE8FF),
                    modifier = Modifier.size(22.dp).clickable { nav.navigate("alerts") })
                Spacer(Modifier.width(12.dp))
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    Icon(Icons.Default.MoreVert, "Menu", tint = Color(0xFF9EE8FF),
                        modifier = Modifier.clickable { menuOpen = true })
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        if (isGuest || !isStaff) {
                            DropdownMenuItem(text = { Text(t("common.login")) },
                                onClick = { menuOpen = false; nav.navigate("login") })
                        }
                        DropdownMenuItem(text = { Text(t("common.settings")) },
                            onClick = { menuOpen = false; nav.navigate("settings") })
                        DropdownMenuItem(text = { Text(t("screen.about")) },
                            onClick = { menuOpen = false; nav.navigate("about") })
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1528)),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1A4A60), RoundedCornerShape(20.dp))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Network Health", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE8FBFF))
                        Spacer(Modifier.weight(1f))
                        Text(
                            if (dash.online) dash.transport else "Offline",
                            fontSize = 11.sp,
                            color = if (dash.online) Color(0xFF39FF14) else Color(0xFFFF2E63)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                            Canvas(Modifier.size(100.dp)) {
                                val stroke = 10.dp.toPx()
                                drawArc(Color(0xFF122438), -90f, 360f, false, style = Stroke(stroke, cap = StrokeCap.Round))
                                drawArc(
                                    brush = Brush.sweepGradient(listOf(Color(0xFF00F0FF), Color(0xFF39FF14), Color(0xFF00F0FF))),
                                    startAngle = -90f,
                                    sweepAngle = 360f * (healthScore / 100f),
                                    useCenter = false,
                                    style = Stroke(stroke, cap = StrokeCap.Round)
                                )
                            }
                            Text("$healthScore%", fontWeight = FontWeight.Black, fontSize = 22.sp, color = Color(0xFF00F0FF))
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(healthLabel, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF39FF14))
                            Text(healthSub, fontSize = 11.sp, color = Color(0xFF5EC8E8))
                            val detail = buildString {
                                append(dash.ssid ?: dash.transport)
                                dash.ip?.let { append(" · $it") }
                                dash.gatewayMs?.let { append(" · GW ${it}ms") }
                            }
                            Text(detail, fontSize = 10.sp, color = Color(0xFF5EC8E8))
                            Spacer(Modifier.height(6.dp))
                            Text("View Details →", fontSize = 11.sp, color = Color(0xFF00F0FF), fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { nav.navigate("network") })
                        }
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatPill(Icons.Default.Router, "$onlineCount", "Device Online", Color(0xFF39FF14), Modifier.weight(1f)) { nav.navigate("devices") }
                StatPill(Icons.Default.WifiOff, "$offlineCount", "Device Offline", Color(0xFFFF2E63), Modifier.weight(1f)) { nav.navigate("devices") }
                StatPill(Icons.Default.Notifications, "$alertCount", "Active Alerts", Color(0xFFFFD60A), Modifier.weight(1f)) { nav.navigate("alerts") }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1528)),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF1A4A60), RoundedCornerShape(16.dp))
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("Bandwidth Usage", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE8FBFF))
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text("↑ Download", fontSize = 10.sp, color = Color(0xFF39FF14))
                            Text(String.format("%.1f Mbps", dash.rxMbps), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF39FF14))
                        }
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                            Text("↓ Upload", fontSize = 10.sp, color = Color(0xFFB14DFF))
                            Text(String.format("%.1f Mbps", dash.txMbps), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFFB14DFF))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Canvas(Modifier.fillMaxWidth().height(36.dp)) {
                        val mid = size.height / 2f
                        val pts = 24
                        for (i in 0 until pts - 1) {
                            val x1 = size.width * i / pts
                            val x2 = size.width * (i + 1) / pts
                            val y1 = mid - (dash.rxMbps.toFloat() * 0.15f * (1 + (i % 5) * 0.2f)).coerceAtMost(mid - 2)
                            val y2 = mid - (dash.rxMbps.toFloat() * 0.15f * (1 + ((i + 1) % 5) * 0.2f)).coerceAtMost(mid - 2)
                            drawLine(Color(0xFF00F0FF).copy(alpha = 0.7f), Offset(x1, y1), Offset(x2, y2), strokeWidth = 2.dp.toPx())
                        }
                    }
                    Text(
                        listOfNotNull(dash.ssid, dash.ip, dash.gateway?.let { "GW $it" }).joinToString(" · ").ifBlank { "Live traffic" },
                        fontSize = 10.sp, color = Color(0xFF5EC8E8)
                    )
                }
            }
        }

        item { Text("Quick Access", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE8FBFF)) }
        item {
            val rows = ((actions.size + 3) / 4).coerceAtLeast(1)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.height((rows * 88).dp),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(actions) { action ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { nav.navigate(action.route) }.padding(4.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(52.dp)
                                .background(Color(0xFF0A2038), RoundedCornerShape(14.dp))
                                .border(1.dp, Color(0xFF1A5A70), RoundedCornerShape(14.dp))
                        ) {
                            Icon(action.icon, null, tint = action.color, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(action.title, fontSize = 9.sp, color = Color(0xFF9EE8FF), lineHeight = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1528)),
        modifier = modifier.border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp)).clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(32.dp).background(accent.copy(alpha = 0.15f), CircleShape)
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp, color = accent)
            Text(label, fontSize = 9.sp, color = Color(0xFF5EC8E8), lineHeight = 11.sp)
        }
    }
}
