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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.sin
import kotlin.math.cos
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
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
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
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
                Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
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
                                    Toast.makeText(pageContext, com.mporttech.pro.ui.i18n.Str.get("menu.open_location_toast", com.mporttech.pro.ui.i18n.loadSavedLanguage(pageContext)), Toast.LENGTH_SHORT).show()
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
                            text = { Text(t("menu.to_tools")) },
                            onClick = {
                                menuOpen = false
                                nav?.navigate("tools") { launchSingleTop = true }
                            },
                            leadingIcon = { Icon(Icons.Default.Build, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(t("menu.to_home")) },
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
            Text(
                title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(9.dp))
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurface
            ) {
                content()
            }
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
internal fun DeviceCard(
    name: String,
    ip: String,
    online: Boolean,
    mac: String? = null,
    vendor: String? = null,
    onClick: () -> Unit
) {
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
                if (!mac.isNullOrBlank()) {
                    Text("MAC  $mac", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!vendor.isNullOrBlank()) {
                    Text(vendor, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                }
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
            Text(t("net.no_iface"), fontSize = 12.sp)
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
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(
                title,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
            Icon(
                Icons.Default.ChevronRight,
                null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
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
            Text(
                title,
                modifier = Modifier.weight(1f),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
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


@Composable
internal fun SpeedMetricCard(
    title: String,
    value: String,
    unit: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A))
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(title, fontSize = 12.sp, color = Color(0xFF8BA3B8), fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    unit,
                    fontSize = 12.sp,
                    color = Color(0xFF8BA3B8),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
internal fun SpeedChip(
    label: String,
    value: String,
    unit: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1B2A))
    ) {
        Column(
            Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 10.sp, color = Color(0xFF8BA3B8))
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE8F4FF)
            )
            Text(unit, fontSize = 9.sp, color = Color(0xFF8BA3B8))
        }
    }
}

/**
 * Neon circular speed gauge — ported from MPorT-Tes-Speed (Flutter speed_gauge.dart).
 * Center shows only speed + unit — no logo.
 *
 * [fraction] is 0..1 (speed / maxSpeed). Needle sweeps 270° from 135° (bottom-left).
 */
@Composable
internal fun SpeedDialGauge(
    fraction: Float,
    displayMbps: Double,
    modifier: Modifier = Modifier,
    progressColor: Color = Color(0xFF00E5A0),
    progressColorLight: Color = Color(0xFF5CFFC9),
) {
    val frac = fraction.coerceIn(0f, 1f)
    val gold = progressColor
    val goldLight = progressColorLight
    val purple = Color(0xFF9B7BFF)
    val purpleMid = Color(0xFF7B5CFF)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val center = Offset(cx, cy)
            val outerR = size.minDimension / 2f - 6.dp.toPx()
            val trackR = outerR - 30.dp.toPx()

            // Radial fill background (dark core)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0C1524), Color(0xFF05070E)),
                    center = center,
                    radius = outerR
                ),
                radius = outerR - 1.dp.toPx(),
                center = center
            )

            // 60 tick marks around full circle; major every 5
            val tickCount = 60
            for (i in 0 until tickCount) {
                val t = i / tickCount.toFloat()
                // Flutter: angle = -pi/2 + t * 2pi  →  start at top, clockwise in standard math
                // Compose: 0° = east, positive = clockwise. Convert:
                // math angle from +x axis: same as Flutter (cos/sin of math angle)
                val angle = (-Math.PI / 2.0 + t * Math.PI * 2.0)
                val isMajor = i % 5 == 0
                val purpleBlend = if (t > 0.5f && t < 0.92f) {
                    ((t - 0.5f) / 0.42f).coerceIn(0f, 1f)
                } else 0f
                val base = lerpColor(gold, purple, purpleBlend * 0.85f)
                val color = base.copy(alpha = if (isMajor) 0.95f else 0.4f)
                val inner = outerR - if (isMajor) 20.dp.toPx() else 12.dp.toPx()
                val outerTick = outerR - 4.dp.toPx()
                val cosA = cos(angle).toFloat()
                val sinA = sin(angle).toFloat()
                drawLine(
                    color = color,
                    start = Offset(cx + cosA * inner, cy + sinA * inner),
                    end = Offset(cx + cosA * outerTick, cy + sinA * outerTick),
                    strokeWidth = if (isMajor) 2.4.dp.toPx() else 1.15.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Outer sweep-gradient ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(gold, goldLight, purpleMid, gold),
                    center = center
                ),
                radius = outerR,
                center = center,
                style = Stroke(width = 3.4.dp.toPx())
            )

            // Needle: arcStart = 0.75*pi (135°), arcSweep = 1.5*pi (270°)
            val arcStart = Math.PI * 0.75
            val arcSweep = Math.PI * 1.5
            val angle = arcStart + arcSweep * frac
            val tip = Offset(
                cx + cos(angle).toFloat() * (trackR - 8.dp.toPx()),
                cy + sin(angle).toFloat() * (trackR - 8.dp.toPx())
            )
            // soft glow under needle
            drawLine(
                color = gold.copy(alpha = 0.3f),
                start = center,
                end = tip,
                strokeWidth = 5.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = gold,
                start = center,
                end = tip,
                strokeWidth = 2.6.dp.toPx(),
                cap = StrokeCap.Round
            )
            // hub
            drawCircle(color = Color(0xFF0A1628), radius = 8.dp.toPx(), center = center)
            drawCircle(
                color = gold,
                radius = 8.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Center value only — no MPorT GO logo
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                if (displayMbps > 0) String.format(Locale.US, "%.1f", displayMbps) else "0.0",
                fontSize = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                lineHeight = 44.sp
            )
            Text(
                "Mbps",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = progressColorLight
            )
        }
    }
}

/** Linear color blend (same idea as Flutter Color.lerp). */
private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val x = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * x,
        green = a.green + (b.green - a.green) * x,
        blue = a.blue + (b.blue - a.blue) * x,
        alpha = a.alpha + (b.alpha - a.alpha) * x
    )
}
