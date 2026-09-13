package com.mporttech.pro.features.tools

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mporttech.pro.ui.i18n.t

@Composable
internal fun Page(
    title: String,
    icon: ImageVector,
    nav: NavController?,
    content: @Composable ColumnScope.() -> Unit
) {
    val pageContext = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (nav != null) {
                    IconButton(onClick = {
                        if (nav.previousBackStackEntry != null) nav.popBackStack()
                        else nav.navigate("dashboard") {
                            launchSingleTop = true
                            popUpTo("dashboard") { inclusive = false }
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text(t("common.refresh")) },
                            onClick = {
                                menuOpen = false
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(t("net.location_settings")) },
                            onClick = {
                                menuOpen = false
                                try {
                                    pageContext.startActivity(
                                        Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    )
                                } catch (_: Exception) {
                                    Toast.makeText(pageContext, "Buka Settings → Location", Toast.LENGTH_SHORT).show()
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(t("net.share_page")) },
                            onClick = {
                                menuOpen = false
                                val share = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, "MPorT Tech — $title")
                                }
                                pageContext.startActivity(Intent.createChooser(share, "Share"))
                            },
                            leadingIcon = { Icon(Icons.Default.Share, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Go to Tools") },
                            onClick = {
                                menuOpen = false
                                nav?.navigate("tools") { launchSingleTop = true }
                            },
                            leadingIcon = { Icon(Icons.Default.Build, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Go to Home") },
                            onClick = {
                                menuOpen = false
                                nav?.navigate("dashboard") {
                                    launchSingleTop = true
                                    popUpTo("dashboard") { inclusive = false }
                                }
                            },
                            leadingIcon = { Icon(Icons.Default.Home, null) }
                        )
                    }
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

@Composable
internal fun InteractiveTabStrip(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items.forEachIndexed { index, label ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (index == selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSelect(index) }
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                    fontSize = 9.sp,
                    color = if (index == selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }
    }
}

@Composable
internal fun RouterHero() {
    val context = LocalContext.current
    val link = remember { LiveNetworkInfo.snapshot(context) }
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Router, null, tint = Color(0xFF4EDCFF), modifier = Modifier.size(36.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (link.online) "Uplink · ${link.transport}" else "Uplink offline",
                    fontWeight = FontWeight.Bold
                )
                Text(
                    listOfNotNull(link.ssid, link.ip, link.gateway?.let { "GW $it" }).joinToString(" · ").ifBlank { "—" },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun CardBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            Spacer(Modifier.height(9.dp))
            content()
        }
    }
}

@Composable
internal fun MetricBox(label: String, value: String, color: Color, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(10.dp)) {
            Text(value, color = color, fontWeight = FontWeight.Black, fontSize = 15.sp)
            Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun ToolTile(tile: Tile, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxSize().clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(12.dp)) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(tile.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
            }
            Spacer(Modifier.weight(1f))
            Text(tile.title, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            Text(tile.subtitle, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
        }
    }
}

@Composable
internal fun WifiSignalBars(rssi: Int, modifier: Modifier = Modifier) {
    val bars = when {
        rssi >= -55 -> 4
        rssi >= -67 -> 3
        rssi >= -78 -> 2
        rssi >= -88 -> 1
        else -> 0
    }
    val color = when {
        rssi >= -65 -> Color(0xFF39FF14) // kuat hijau
        rssi >= -78 -> Color(0xFFFFD60A) // menengah kuning
        else -> Color(0xFFFF2E63) // lemah merah
    }
    Row(
        modifier = modifier.height(18.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        val heights = listOf(6.dp, 10.dp, 14.dp, 18.dp)
        heights.forEachIndexed { i, h ->
            Box(
                Modifier
                    .width(4.dp)
                    .height(h)
                    .background(
                        if (i < bars) color else color.copy(alpha = 0.2f),
                        RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

@Composable
internal fun WifiRow(
    name: String,
    detail: String,
    signal: String,
    rssi: Int = -80,
    onClick: (() -> Unit)? = null
) {
    val color = when {
        rssi >= -65 -> Color(0xFF39FF14)
        rssi >= -78 -> Color(0xFFFFD60A)
        else -> Color(0xFFFF2E63)
    }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            WifiSignalBars(rssi)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name.removePrefix("★ ").trim(), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(detail, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(signal, fontSize = 10.sp, color = color, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun DeviceRow(name: String, ip: String, online: Boolean) {
    val color = if (online) Color(0xFF39FF14) else Color(0xFFFF2E63)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            Text("●", color = color)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(ip, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun DeviceCard(name: String, ip: String, online: Boolean, onClick: () -> Unit) {
    val color = if (online) Color(0xFF39FF14) else Color(0xFFFF2E63)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Router, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(ip, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(if (online) "Online" else "Offline", color = color, fontSize = 9.sp)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun InterfaceList() {
    val context = LocalContext.current
    val ifaces = remember { LiveNetworkInfo.interfaceNames() }
    CardBlock("Interfaces") {
        if (ifaces.isEmpty()) {
            Text("Tidak ada interface aktif", fontSize = 12.sp)
        } else {
            ifaces.take(10).forEach {
                Text(it, fontSize = 11.sp, lineHeight = 18.sp)
            }
        }
    }
}

@Composable
internal fun AlertCard(title: String, detail: String, time: String, color: Color, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = color.copy(alpha = 0.15f)) {
                Icon(Icons.Default.Warning, null, tint = color, modifier = Modifier.padding(9.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, color = color, fontSize = 11.sp)
                Text(detail, fontSize = 9.sp)
                Text(time, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun JobCard(tag: String, title: String, detail: String, color: Color, onClick: () -> Unit = {}) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(20.dp), color = color.copy(alpha = 0.16f)) {
                Icon(Icons.Default.Build, null, tint = color, modifier = Modifier.padding(9.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(tag, fontSize = 9.sp, color = color)
                Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(detail, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun ReportRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (selected) {
                Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            } else {
                Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
internal fun HistoryRow(title: String, detail: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            Icon(Icons.Default.History, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(detail, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun SettingsRow(title: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(title, modifier = Modifier.weight(1f), fontSize = 11.sp)
            Icon(Icons.Default.ChevronRight, null, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
internal fun SwitchRow(title: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit = {}) {
    var state by remember(checked) { mutableStateOf(checked) }
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(title, modifier = Modifier.weight(1f), fontSize = 11.sp)
            Switch(checked = state, onCheckedChange = {
                state = it
                onCheckedChange(it)
            })
        }
    }
}

internal fun formatIp(ip: Int): String {
    return "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
}

internal fun openWifiConnectDialog(context: android.content.Context, ssid: String, security: String = "") {
    val isOpen = security.equals("Open", ignoreCase = true)
    // Native system sheet: Password + Advanced options + CANCEL / CONNECT
    com.mporttech.pro.features.wifi.WifiConnector.connect(context, ssid, isOpen = isOpen)
}


