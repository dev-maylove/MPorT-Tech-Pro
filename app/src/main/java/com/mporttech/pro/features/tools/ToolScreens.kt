package com.mporttech.pro.features.tools

import kotlinx.coroutines.launch
import android.content.Context
import android.net.wifi.WifiManager
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Devices
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mporttech.pro.features.scanner.AuthorizedNetworkScanner
import com.mporttech.pro.features.scanner.ScanHost
import com.mporttech.pro.features.speedtest.Phase
import com.mporttech.pro.features.speedtest.ServerConfig
import com.mporttech.pro.features.speedtest.ServerSelector
import com.mporttech.pro.features.speedtest.TestServer
import com.mporttech.pro.features.speedtest.SpeedTestEngine
import com.mporttech.pro.features.speedtest.SpeedTestHistoryStore
import com.mporttech.pro.features.speedtest.SpeedTestRecord
import com.mporttech.pro.features.wifi.Band
import com.mporttech.pro.features.wifi.WifiAnalyzer
import com.mporttech.pro.features.wifi.WifiScanSnapshot
import com.mporttech.pro.features.wifi.WifiNetworkInfo
import com.mporttech.pro.ui.i18n.AppLanguage
import com.mporttech.pro.ui.i18n.LocalAppLanguage
import com.mporttech.pro.ui.i18n.saveLanguage
import com.mporttech.pro.ui.i18n.t
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.io.File

internal data class Tile(val title: String, val subtitle: String, val icon: ImageVector, val route: String = "")

@Composable
fun NetworkMonitorScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf(t("screen.overview"), t("screen.devices"), t("screen.graph"))
    var link by remember { mutableStateOf(LiveNetworkInfo.snapshot(context)) }
    var rxMbps by remember { mutableStateOf(0.0) }
    var txMbps by remember { mutableStateOf(0.0) }
    var rxHistory by remember { mutableStateOf<List<Double>>(emptyList()) }
    var txHistory by remember { mutableStateOf<List<Double>>(emptyList()) }
    var gatewayMs by remember { mutableStateOf<Long?>(null) }
    var gatewayOk by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf<List<LiveDevice>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var ifaces by remember { mutableStateOf(LiveNetworkInfo.interfaceNames()) }

    fun refreshLink() {
        link = LiveNetworkInfo.snapshot(context)
        ifaces = LiveNetworkInfo.interfaceNames()
    }

    LaunchedEffect(Unit) {
        refreshLink()
        while (true) {
            val (rx, tx) = LiveNetworkInfo.measureTrafficDeltaMbps(1000)
            rxMbps = rx
            txMbps = tx
            // Functional update avoids stale list capture
            rxHistory = (rxHistory + rx).takeLast(30)
            txHistory = (txHistory + tx).takeLast(30)
            refreshLink()
            val gw = link.gateway
            if (!gw.isNullOrBlank()) {
                val (ok, ms) = LiveNetworkInfo.probe(gw, 800)
                gatewayOk = ok
                gatewayMs = ms
            }
            // measureTraffic already waited ~1s; short pause before next cycle
            delay(500)
        }
    }

    LaunchedEffect(selectedTab) {
        if (selectedTab == 1 && devices.isEmpty() && !scanning) {
            scanning = true
            try {
                devices = LiveNetworkInfo.discoverDevices(context, authorized = true, endHost = 254)
            } catch (_: Exception) {
            } finally {
                scanning = false
            }
        }
    }

    Page(t("screen.network_monitor"), Icons.Default.NetworkCheck, nav) {
        InteractiveTabStrip(tabs, selectedTab) { selectedTab = it }
        when (selectedTab) {
            0 -> {
                CardBlock(
                    if (link.online) "Link  ·  ${link.transport}  ·  Online"
                    else "Link  ·  Offline"
                ) {
                    Text(
                        buildString {
                            append(link.ssid?.let { "SSID  $it\n" } ?: "")
                            append("IP  ${link.ip ?: "—"}\n")
                            append("Gateway  ${link.gateway ?: "—"}\n")
                            append("DNS  ${link.dns ?: "—"}\n")
                            append(link.linkMbps?.let { "Link speed  $it Mbps" } ?: "Link speed  —")
                        },
                        fontSize = 12.sp,
                        lineHeight = 20.sp
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricBox(
                        "RX",
                        String.format("%.1f", rxMbps),
                        Color(0xFF39FF14),
                        Modifier.weight(1f)
                    )
                    MetricBox(
                        "TX",
                        String.format("%.1f", txMbps),
                        Color(0xFF4EDCFF),
                        Modifier.weight(1f)
                    )
                    MetricBox(
                        "GW ms",
                        gatewayMs?.toString() ?: "—",
                        if (gatewayOk) Color(0xFF39FF14) else Color(0xFFFF2E63),
                        Modifier.weight(1f)
                    )
                }
                CardBlock("Traffic (Mbps, live)") {
                    Text(
                        "↑ RX  ${String.format("%.2f", rxMbps)} Mbps     ↓ TX  ${String.format("%.2f", txMbps)} Mbps",
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (rxMbps / 100.0).toFloat().coerceIn(0.02f, 1f) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                CardBlock("Network interfaces") {
                    if (ifaces.isEmpty()) {
                        Text("Tidak ada interface aktif", fontSize = 12.sp)
                    } else {
                        ifaces.take(8).forEach {
                            Text(it, fontSize = 11.sp, lineHeight = 18.sp)
                        }
                    }
                }
            }
            1 -> {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (scanning) "Scanning LAN…" else "Perangkat di segmen lokal",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        enabled = !scanning,
                        onClick = {
                            scanning = true
                            scope.launch {
                                try {
                                    devices = LiveNetworkInfo.discoverDevices(context, authorized = true, endHost = 254)
                                    Toast.makeText(context, "${devices.size} host ditemukan", Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    Toast.makeText(context, e.message ?: "Scan gagal", Toast.LENGTH_LONG).show()
                                } finally {
                                    scanning = false
                                }
                            }
                        }
                    ) { Text(if (scanning) "…" else t("common.scan")) }
                }
                if (devices.isEmpty() && !scanning) {
                    CardBlock("Belum ada data") {
                        Text(
                            "Ketuk SCAN untuk probe gateway + host RFC1918 (.1–.40). Hanya jaringan yang Anda kelola.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                devices.forEach { d ->
                    DeviceCard(d.name, d.ip, d.online, d.mac, d.vendor) {
                        SelectedDeviceStore.ip = d.ip
                        SelectedDeviceStore.name = d.name
                        SelectedDeviceStore.kind = d.kind
                        SelectedDeviceStore.mac = d.mac
                        SelectedDeviceStore.vendor = d.vendor
                        nav.navigate("deviceDetailRich")
                    }
                }
            }
            else -> {
                CardBlock(t("net.throughput_chart")) {
                    val maxY = maxOf((rxHistory + txHistory).maxOrNull() ?: 1.0, 1.0)
                    Text(
                        "RX ${String.format("%.2f", rxMbps)}  ·  TX ${String.format("%.2f", txMbps)} Mbps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    ) {
                        val w = size.width
                        val h = size.height
                        fun plot(data: List<Double>, color: Color) {
                            if (data.size < 2) return
                            val step = w / (data.size - 1).coerceAtLeast(1)
                            var prev = Offset(0f, h)
                            data.forEachIndexed { i, v ->
                                val x = i * step
                                val y = h - ((v / maxY).toFloat().coerceIn(0f, 1f) * h * 0.92f)
                                if (i > 0) {
                                    drawLine(color, prev, Offset(x, y), strokeWidth = 3f, cap = StrokeCap.Round)
                                }
                                prev = Offset(x, y)
                            }
                        }
                        plot(rxHistory, Color(0xFF39FF14))
                        plot(txHistory, Color(0xFF00F0FF))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Peak RX ${String.format("%.2f", (rxHistory.maxOrNull() ?: 0.0))}  ·  Peak TX ${String.format("%.2f", (txHistory.maxOrNull() ?: 0.0))}  ·  n=${rxHistory.size}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CardBlock(t("net.gateway_latency")) {
                    Text(
                        when {
                            link.gateway == null -> t("net.gateway_missing")
                            gatewayMs != null && gatewayOk -> "Ping ${link.gateway}  ·  ${gatewayMs} ms  ·  ${t("net.reachable")}"
                            else -> "Ping ${link.gateway}  ·  ${t("net.no_response")}"
                        },
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TechnicianToolsScreen(nav: NavController) {
    val tiles = listOf(
        Tile(t("tools.tile_ping"), t("tools.tile_ping_sub"), Icons.Default.NetworkCheck, "ping"),
        Tile(t("tools.tile_trace"), t("tools.tile_trace_sub"), Icons.Default.Timeline, "traceroute"),
        Tile(t("tools.tile_dns"), t("tools.tile_dns_sub"), Icons.Default.Search, "dns"),
        Tile(t("tools.tile_wifi"), t("tools.tile_wifi_sub"), Icons.Default.Wifi, "wifi"),
        Tile(t("tools.tile_port"), t("tools.tile_port_sub"), Icons.Default.Security, "portcheck"),
        Tile(t("tools.tile_speed"), t("tools.tile_speed_sub"), Icons.Default.Speed, "speedtest"),
        Tile(t("tools.tile_scan"), t("tools.tile_scan_sub"), Icons.Default.Router, "scanner"),
        Tile(t("tools.tile_discovery"), t("tools.tile_discovery_sub"), Icons.Default.Search, "discovery")
    )
    Page(t("tools.technician"), Icons.Default.Build, nav) {
        // Non-lazy 2-column grid so parent Page (LazyColumn) can scroll the full list
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            tiles.chunked(2).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowTiles.forEach { tile ->
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1528)),
                            modifier = Modifier
                                .weight(1f)
                                .border(1.dp, Color(0xFF1A4A60), RoundedCornerShape(16.dp))
                                .clickable { if (tile.route.isNotBlank()) nav.navigate(tile.route) }
                        ) {
                            Column(Modifier.padding(14.dp)) {
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .background(Color(0xFF0A2840), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        tile.icon,
                                        null,
                                        tint = Color(0xFF00F0FF),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    tile.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    tile.subtitle,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    // Odd last row: keep grid alignment
                    if (rowTiles.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2038)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1A5A70), RoundedCornerShape(14.dp))
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, null, tint = Color(0xFF00F0FF))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Semua tools dalam satu layar",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "Kerja lebih cepat, lebih efisien",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        // Extra bottom padding so last tiles clear the bottom nav
        Spacer(Modifier.height(24.dp))
    }
}



@Composable
fun WifiToolsScreen(nav: NavController) {
    val context = LocalContext.current
    val analyzer = remember { WifiAnalyzer(context) }
    var connected by remember { mutableStateOf(analyzer.currentConnection()) }
    var quickCount by remember { mutableStateOf(analyzer.cachedSnapshot()?.networks?.size ?: 0) }
    LaunchedEffect(Unit) {
        connected = analyzer.currentConnection()
        quickCount = analyzer.cachedSnapshot()?.networks?.size ?: 0
    }
    val tiles = listOf(
        Tile(t("tools.tile_wifi_scan"), t("tools.tile_wifi_scan_sub"), Icons.Default.Wifi, "wifi"),
        Tile(t("tools.tile_signal"), t("tools.tile_signal_sub"), Icons.Default.NetworkCheck, "signalHub"),
        Tile(t("tools.tile_channel"), t("tools.tile_channel_sub"), Icons.Default.BarChart, "wifi"),
        Tile(t("tools.tile_netinfo"), t("tools.tile_netinfo_sub"), Icons.Default.Info, "wifi"),
        Tile(t("tools.tile_connected"), t("tools.tile_connected_sub"), Icons.Default.Groups, "discovery"),
        Tile("Speed Test", "Tes kecepatan penuh", Icons.Default.Speed, "speedtest")
    )
    Page(t("tools.wifi"), Icons.Default.Wifi, nav) {
        CardBlock(connected?.ssid ?: t("wifi.not_connected")) {
            if (connected != null) {
                Text(
                    "RSSI ${(connected?.rssiDbm ?: 0)} dBm • Q${(connected?.qualityScore ?: 0)}/100 • ~${(connected?.estimatedMbps ?: 0)} Mbps",
                    color = Color(0xFF39FF14),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(t("wifi.open_analyzer"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (quickCount > 0) {
                Spacer(Modifier.height(4.dp))
                Text("$quickCount AP di cache lokal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tiles.chunked(2).forEach { rowTiles ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowTiles.forEach { tile ->
                        Box(Modifier.weight(1f)) {
                            ToolTile(tile) { nav.navigate(tile.route) }
                        }
                    }
                    if (rowTiles.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { nav.navigate("wifi") }, modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )) {
                Text(t("tools.tile_wifi"), fontSize = 12.sp, color = Color(0xFF000000), fontWeight = FontWeight.Bold)
            }
            Button(onClick = { nav.navigate("speedtest") }, modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )) {
                Text(t("tools.tile_speed"), fontSize = 12.sp, color = Color(0xFF000000), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun WifiAnalyzerScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var snapshot by remember { mutableStateOf<WifiScanSnapshot?>(null) }
    var networks by remember { mutableStateOf<List<WifiNetworkInfo>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf("Siap scan — cache optimasi aktif") }
    var permissionHint by remember { mutableStateOf<String?>(null) }
    var miniSpeed by remember { mutableStateOf<String?>(null) }
    var miniRunning by remember { mutableStateOf(false) }

    val analyzer = remember { WifiAnalyzer(context) }
    var connected by remember { mutableStateOf(analyzer.currentConnection()) }

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    fun hasNearbyWifiPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.NEARBY_WIFI_DEVICES) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    fun doScan(force: Boolean = false) {
        if (scanning) return
        permissionHint = null
        // Instant paint from memory cache (no spinner if we already have data)
        if (!force) {
            analyzer.peekCache()?.let { cached ->
                if (cached.networks.isNotEmpty()) {
                    snapshot = cached
                    networks = cached.networks
                    status = "Cache • ${cached.networks.size} AP • instant"
                }
            }
        }
        scanning = true
        status = if (force) "Force scan…" else "Refreshing…"
        scope.launch {
            try {
                if (!analyzer.isWifiEnabled()) {
                    status = "WiFi mati — mengaktifkan…"
                    analyzer.setWifiEnabled(true)
                    delay(400)
                }
                val snap = withContext(Dispatchers.IO) {
                    analyzer.scan(force = force, preferCache = false)
                }
                snapshot = snap
                networks = snap.networks
                connected = analyzer.currentConnection()
                status = buildString {
                    append(if (snap.fromCache) "Cache " else "Live ")
                    append("• ${snap.networks.size} AP")
                    append(" • ${snap.scanDurationMs} ms")
                    snap.recommended24?.let { append(" • Best 2.4: CH $it") }
                    snap.recommended5?.let { append(" • Best 5: CH $it") }
                }
            } catch (e: Exception) {
                status = com.mporttech.pro.ui.i18n.Str.get("common.error_prefix", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)) + ": ${e.message}"
                Toast.makeText(context, e.message ?: "Scan gagal", Toast.LENGTH_LONG).show()
            } finally {
                scanning = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        if (result.values.any { it }) doScan(force = true) else {
            permissionHint = com.mporttech.pro.ui.i18n.Str.get("wifi.perm_denied", com.mporttech.pro.ui.i18n.loadSavedLanguage(context))
            status = com.mporttech.pro.ui.i18n.Str.get("wifi.perm_needed", com.mporttech.pro.ui.i18n.loadSavedLanguage(context))
        }
    }

    fun requestAndScan(force: Boolean = true) {
        val needed = mutableListOf<String>()
        if (!hasLocationPermission()) {
            needed += Manifest.permission.ACCESS_FINE_LOCATION
            needed += Manifest.permission.ACCESS_COARSE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= 33 && !hasNearbyWifiPermission()) {
            needed += Manifest.permission.NEARBY_WIFI_DEVICES
        }
        if (needed.isNotEmpty()) {
            status = "Meminta izin..."
            permissionLauncher.launch(needed.toTypedArray())
        } else doScan(force)
    }

    // First frame: peek cache, then background refresh (no forced radio if cache warm)
    LaunchedEffect(Unit) {
        analyzer.peekCache()?.let { cached ->
            if (cached.networks.isNotEmpty()) {
                snapshot = cached
                networks = cached.networks
                status = "Cache • ${cached.networks.size} AP"
            }
        }
        connected = analyzer.currentConnection()
        if (hasLocationPermission()) doScan(force = false)
    }

    Page("WiFi Analyzer", Icons.Default.Wifi, nav) {
        // Connected + estimated speed + run speed test
        CardBlock(connected?.ssid ?: "Belum terhubung WiFi") {
            if (connected != null) {
                Text(
                    "RSSI ${(connected?.rssiDbm ?: 0)} dBm  •  CH ${(connected?.channel ?: 0)}  •  ${(connected?.frequencyMhz ?: 0)} MHz",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Kualitas ${(connected?.qualityScore ?: 0)}/100  •  Estimasi link ~${(connected?.estimatedMbps ?: 0)} Mbps",
                    color = Color(0xFF39FF14),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text(
                    "Hubungkan ke WiFi untuk estimasi link & speed test terintegrasi.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        miniRunning = true
                        miniSpeed = null
                        scope.launch {
                            try {
                                val engine = SpeedTestEngine()
                                // short integrated ping+download sample using default server
                                var lastDl = 0.0
                                val result = withContext(Dispatchers.IO) {
                                    engine.run { phase ->
                                        if (phase.phase == Phase.DOWNLOAD) lastDl = phase.mbps
                                    }
                                }
                                miniSpeed =
                                    "Ping ${"%.0f".format(result.pingMs)} ms  •  ↓ ${"%.1f".format(result.downloadMbps)}  •  ↑ ${"%.1f".format(result.uploadMbps)} Mbps"
                            } catch (e: Exception) {
                                miniSpeed = "Speed test gagal: ${e.message}"
                            } finally {
                                miniRunning = false
                            }
                        }
                    },
                    enabled = !miniRunning,
                    modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )) {
                    if (miniRunning) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF000000))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (miniRunning) "…" else t("wifi.speed_test"), fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { nav?.navigate("speedtest") },
                    modifier = Modifier.weight(1f)
                ) { Text(t("wifi.full_test"), fontSize = 11.sp) }
            }
            miniSpeed?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4EDCFF))
            }
        }

        InteractiveTabStrip(listOf("2.4 GHz", "5 GHz", "All"), selectedTab) { selectedTab = it }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { requestAndScan(force = true) },
                enabled = !scanning,
                modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )) {
                if (scanning) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color(0xFF000000))
                    Spacer(Modifier.width(6.dp))
                }
                Text(if (scanning) "…" else t("wifi.scan"))
            }
            OutlinedButton(
                onClick = { doScan(force = false) },
                enabled = !scanning,
                modifier = Modifier.weight(1f)
            ) { Text(t("wifi.cache")) }
        }


        Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        permissionHint?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }

        snapshot?.let { snap ->
            CardBlock(t("wifi.channel_rec_title")) {
                Text(
                    "2.4 GHz → CH ${snap.recommended24 ?: "-"}    |    5 GHz → CH ${snap.recommended5 ?: "-"}",
                    color = Color(0xFF39FF14),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                val busy24 = snap.channels24.sortedByDescending { it.congestionScore }.take(3)
                if (busy24.isNotEmpty()) {
                    Text(
                        t("wifi.busy_24") + " " + busy24.joinToString { "CH ${it.channel}(${it.networkCount})" },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Visual channel occupancy (real scan stats)
            if (snap.channels24.isNotEmpty() || snap.channels5.isNotEmpty()) {
                CardBlock(t("wifi.channel_occ")) {
                    val bars = (snap.channels24 + snap.channels5)
                        .sortedByDescending { it.networkCount }
                        .take(10)
                    val maxCnt = (bars.maxOfOrNull { it.networkCount } ?: 1).coerceAtLeast(1)
                    bars.forEach { ch ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "CH ${ch.channel}",
                                modifier = Modifier.width(52.dp),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(10.dp)
                                    .background(Color(0xFF0A2840), RoundedCornerShape(4.dp))
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth((ch.networkCount.toFloat() / maxCnt).coerceIn(0.05f, 1f))
                                        .background(
                                            if (ch.band.name.contains("24")) Color(0xFF00F0FF) else Color(0xFFB680FF),
                                            RoundedCornerShape(4.dp)
                                        )
                                )
                            }
                            Text(
                                "${ch.networkCount}",
                                modifier = Modifier.width(28.dp),
                                fontSize = 11.sp,
                                color = Color(0xFF39FF14),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }

        val filtered = when (selectedTab) {
            0 -> networks.filter { it.band == Band.GHZ_24 }
            1 -> networks.filter { it.band == Band.GHZ_5 || it.band == Band.GHZ_6 }
            else -> networks
        }

        if (filtered.isEmpty() && !scanning) {
            Text(
                t("wifi.no_results"),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            filtered.take(40).forEach { net ->
                WifiRow(
                    name = (if (net.isConnected) "★ " else "") + net.ssid,
                    detail = "CH ${net.channel} • ${net.frequencyMhz} MHz • ${net.security} • ${net.widthMhz}MHz • Q${net.qualityScore} • ~${net.estimatedMbps}Mbps",
                    signal = "${net.rssiDbm} dBm",
                    rssi = net.rssiDbm,
                    onClick = { openWifiConnectDialog(context, net.ssid, net.security) }
                )
            }
        }
    }
}

@Composable
fun LegacyNetworkScannerScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var allowed by remember { mutableStateOf(false) }
    var subnet by remember { mutableStateOf("192.168.1") }
    var scanning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<ScanHost>>(emptyList()) }
    var status by remember { mutableStateOf("Siap memindai subnet privat terotorisasi") }
    var progress by remember { mutableStateOf(0f) }

    Page("Network Scanner", Icons.Default.NetworkCheck, nav) {
        CardBlock(t("scan.authorized")) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = allowed, onCheckedChange = { allowed = it })
                Text(
                    t("scan.consent"),
                    fontSize = 11.sp
                )
            }
        }
        OutlinedTextField(
            value = subnet,
            onValueChange = { subnet = it.filter { c -> c.isDigit() || c == '.' }.take(15) },
            label = { Text("Private subnet base (contoh 192.168.1)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (scanning) {
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
        Button(
            onClick = {
                if (!allowed) {
                    Toast.makeText(context, com.mporttech.pro.ui.i18n.Str.get("scan.need_consent", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)), Toast.LENGTH_SHORT).show()
                    return@Button
                }
                scanning = true
                results = emptyList()
                status = "Memindai $subnet.0/24 ..."
                progress = 0f
                scope.launch {
                    try {
                        val scanner = AuthorizedNetworkScanner()
                        // Scan limited range for responsiveness
                        val base = AuthorizedNetworkScanner.normalizePrivateBase(subnet.trim())
                            ?: throw IllegalArgumentException(
                                "Only private RFC1918 IPv4 ranges are accepted (contoh: 192.168.1 atau 192.168.1.1)"
                            )
                        status = "Memindai $base.0/24 (host 1–40)..."
                        val hosts = withContext(Dispatchers.IO) {
                            scanner.scan(
                                base = base,
                                startHost = 1,
                                endHost = 40,
                                timeoutMs = 180,
                                authorized = allowed
                            )
                        }
                        results = hosts
                        status = if (hosts.isEmpty()) {
                            "Selesai. Tidak ada host yang merespons di rentang 1–30."
                        } else {
                            "Selesai. ${hosts.size} host ditemukan."
                        }
                        progress = 1f
                    } catch (e: Exception) {
                        status = com.mporttech.pro.ui.i18n.Str.get("common.error_prefix", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)) + ": ${e.message}"
                        Toast.makeText(context, e.message ?: "Scan gagal", Toast.LENGTH_LONG).show()
                    } finally {
                        scanning = false
                    }
                }
            },
            enabled = allowed && !scanning,
            modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )) {
            if (scanning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF000000))
                Spacer(Modifier.width(8.dp))
            }
            Text(if (scanning) "SCANNING..." else t("scan.start"))
        }

        if (results.isEmpty() && !scanning) {
            Text("Tekan MULAI SCAN untuk menemukan perangkat di LAN (data real, bukan demo).", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            results.forEach { host ->
                DeviceRow(host.address, "Latency ${host.latencyMs ?: "-"} ms", host.reachable)
            }
        }
    }
}

@Composable
fun SpeedTestScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine = remember { SpeedTestEngine() }

    var running by remember { mutableStateOf(false) }
    var probing by remember { mutableStateOf(false) }
    var phase by remember { mutableStateOf("Siap") }
    var currentPhase by remember { mutableStateOf<Phase?>(null) }
    var downloadMbps by remember { mutableStateOf(0.0) }
    var uploadMbps by remember { mutableStateOf(0.0) }
    var pingMs by remember { mutableStateOf(0.0) }
    var jitterMs by remember { mutableStateOf(0.0) }
    var lossPct by remember { mutableStateOf(0.0) }
    var progress by remember { mutableStateOf(0f) }
    var selected by remember { mutableStateOf(ServerSelector.selected) }
    var status by remember { mutableStateOf("Server: ${selected.name} • ${selected.location}") }
    var showServerPicker by remember { mutableStateOf(false) }
    var history by remember {
        mutableStateOf(
            SpeedTestHistoryStore.load(context).map { r ->
                val df = java.text.SimpleDateFormat("dd MMM yyyy HH:mm", java.util.Locale.getDefault())
                "${df.format(java.util.Date(r.timestamp))}  ${"%.1f".format(r.downloadMbps)} / ${"%.1f".format(r.uploadMbps)} Mbps  ${r.location.ifBlank { r.serverName }}"
            }.ifEmpty { listOf(com.mporttech.pro.ui.i18n.Str.get("speed.empty_history", com.mporttech.pro.ui.i18n.loadSavedLanguage(context))) }
        )
    }
    val catalog = remember { TestServer.catalog() }

    // Apply selected server to engine config
    LaunchedEffect(selected) {
        ServerSelector.select(selected)
        status = "${selected.displayName} • ${selected.displaySubtitle}"
    }

    Page(t("speed.title"), Icons.Default.Speed, nav) {
        // Server resource card (from MPorT-Tes-Speed catalog)
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().clickable(enabled = !running) { showServerPicker = !showServerPicker }
        ) {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(10.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(selected.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        selected.displaySubtitle,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    selected.latencyMs?.let {
                        Text("Latency ~ ${String.format(Locale.US, "%.0f", it)} ms", fontSize = 10.sp, color = Color(0xFF39FF14))
                    }
                }
                Icon(if (showServerPicker) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null)
            }
        }

        if (showServerPicker && !running) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                probing = true
                                status = "Mencari server terdekat..."
                                scope.launch {
                                    try {
                                        val nearest = withContext(Dispatchers.IO) { ServerSelector.selectNearest(10) }
                                        selected = nearest
                                        status = "Nearest: ${nearest.name} (${nearest.latencyMs?.let { String.format(Locale.US, "%.0f ms", it) } ?: "-"})"
                                    } catch (e: Exception) {
                                        status = "Probe gagal: ${e.message}"
                                    } finally {
                                        probing = false
                                    }
                                }
                            },
                            enabled = !probing,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (probing) {
                                CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(if (probing) "PROBING..." else t("speed.auto_nearest"), fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                selected = TestServer.haansiro()
                                showServerPicker = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text(t("speed.default_server"), fontSize = 10.sp) }
                    }
                    Text("Katalog server (${catalog.size})", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    catalog.take(12).forEach { s ->
                        val active = s.id == selected.id
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth().clickable {
                                selected = s
                                showServerPicker = false
                            }
                        ) {
                            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(s.name, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    val ipHint = try {
                                        TestServer.resolveHostToIp(s.host).substringBefore(":")
                                    } catch (_: Exception) {
                                        s.host.substringBefore(":")
                                    }
                                    Text("${s.location} • $ipHint • ${s.distanceKm ?: "-"} km", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (active) Text("●", color = Color(0xFF39FF14))
                            }
                        }
                    }
                    if (catalog.size > 12) {
                        Text("+${catalog.size - 12} server lainnya di katalog", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(
                Modifier.fillMaxWidth().padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val resolvedIp = remember(selected.host) {
                    try {
                        TestServer.resolveHostToIp(selected.host).substringBefore(":")
                    } catch (_: Exception) {
                        selected.host.substringBefore(":")
                    }
                }
                Text("${selected.displayName}  •  $resolvedIp", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (selected.sponsor.isNotBlank() && selected.sponsor != selected.displayName) {
                    Text(selected.sponsor, fontSize = 9.sp, color = MaterialTheme.colorScheme.primary)
                }
                Text(phase, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(180.dp)) {
                    CircularProgressIndicator(
                        progress = {
                            when {
                                running -> progress.coerceIn(0.05f, 1f)
                                downloadMbps > 0 -> (downloadMbps / 200.0).toFloat().coerceIn(0.05f, 1f)
                                else -> 0.05f
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 12.dp,
                        color = Color(0xFF00F0FF),
                        trackColor = Color(0xFF0A2840)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val main = when {
                            running && currentPhase == Phase.UPLOAD -> uploadMbps
                            running && currentPhase == Phase.DOWNLOAD -> downloadMbps
                            downloadMbps > 0 -> downloadMbps
                            else -> 0.0
                        }
                        Text(
                            if (main > 0) String.format(Locale.US, "%.1f", main) else "—",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text("Mbps", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Text(
                        "Download\n${if (downloadMbps > 0) String.format(Locale.US, "%.1f", downloadMbps) else "—"} Mbps",
                        color = Color(0xFF4EDCFF),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Upload\n${if (uploadMbps > 0) String.format(Locale.US, "%.1f", uploadMbps) else "—"} Mbps",
                        color = Color(0xFFB680FF),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MetricBox("Ping", if (pingMs > 0) String.format(Locale.US, "%.0f ms", pingMs) else "—", Color(0xFF4EDCFF), Modifier.weight(1f))
            MetricBox("Jitter", if (jitterMs > 0) String.format(Locale.US, "%.1f ms", jitterMs) else "—", Color(0xFFB680FF), Modifier.weight(1f))
            MetricBox("Loss", if (pingMs > 0) String.format(Locale.US, "%.0f%%", lossPct) else "—", Color(0xFF39FF14), Modifier.weight(1f))
        }
        Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (running) return@Button
                    running = true
                    showServerPicker = false
                    phase = com.mporttech.pro.ui.i18n.Str.get("speed.phase_ping", com.mporttech.pro.ui.i18n.loadSavedLanguage(context))
                    progress = 0.05f
                    ServerSelector.select(selected)
                    val ipHint = try {
                        TestServer.resolveHostToIp(selected.host).substringBefore(":")
                    } catch (_: Exception) { selected.host.substringBefore(":") }
                    status = "${selected.displayName} · $ipHint"
                    downloadMbps = 0.0; uploadMbps = 0.0; pingMs = 0.0; jitterMs = 0.0; lossPct = 0.0
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                engine.run(multiConnection = true) { p ->
                                    when (p.phase) {
                                        Phase.PING -> {
                                            currentPhase = Phase.PING
                                            phase = com.mporttech.pro.ui.i18n.Str.get("speed.phase_ping", com.mporttech.pro.ui.i18n.loadSavedLanguage(context))
                                            progress = 0.1f
                                            if (p.pingMs > 0) pingMs = p.pingMs
                                        }
                                        Phase.DOWNLOAD -> {
                                            currentPhase = Phase.DOWNLOAD
                                            phase = com.mporttech.pro.ui.i18n.Str.get("speed.phase_download", com.mporttech.pro.ui.i18n.loadSavedLanguage(context))
                                            progress = (0.15f + (p.mbps / 300.0).toFloat() * 0.4f).coerceIn(0.15f, 0.55f)
                                            if (p.mbps > 0) downloadMbps = p.mbps
                                        }
                                        Phase.UPLOAD -> {
                                            currentPhase = Phase.UPLOAD
                                            phase = com.mporttech.pro.ui.i18n.Str.get("speed.phase_upload", com.mporttech.pro.ui.i18n.loadSavedLanguage(context))
                                            progress = (0.55f + (p.mbps / 150.0).toFloat() * 0.4f).coerceIn(0.55f, 0.95f)
                                            if (p.mbps > 0) uploadMbps = p.mbps
                                        }
                                        Phase.COMPLETED -> {
                                            currentPhase = Phase.COMPLETED
                                            phase = com.mporttech.pro.ui.i18n.Str.get("speed.done", com.mporttech.pro.ui.i18n.loadSavedLanguage(context))
                                            progress = 1f
                                        }
                                        Phase.ERROR -> {
                                            currentPhase = Phase.ERROR
                                            phase = com.mporttech.pro.ui.i18n.Str.get("common.error", com.mporttech.pro.ui.i18n.loadSavedLanguage(context))
                                        }
                                    }
                                }
                            }
                            downloadMbps = result.downloadMbps
                            uploadMbps = result.uploadMbps
                            pingMs = result.pingMs
                            jitterMs = result.jitterMs
                            lossPct = result.packetLossPercent
                            phase = com.mporttech.pro.ui.i18n.Str.get("speed.done", com.mporttech.pro.ui.i18n.loadSavedLanguage(context))
                            progress = 1f
                            status = "Selesai • ${result.server} • DL ${String.format(Locale.US, "%.1f", result.downloadMbps)} / UL ${String.format(Locale.US, "%.1f", result.uploadMbps)} Mbps"
                            val ts = SimpleDateFormat("dd MMM HH:mm", Locale.getDefault()).format(Date())
                            history = listOf(
                                "$ts — ${String.format(Locale.US, "%.1f", result.downloadMbps)} / ${String.format(Locale.US, "%.1f", result.uploadMbps)} Mbps  •  ${selected.displayName}"
                            ) + history.filterNot { it.startsWith("—") }.take(9)
                            SpeedTestHistoryStore.add(
                                context,
                                SpeedTestRecord(
                                    timestamp = System.currentTimeMillis(),
                                    serverName = selected.displayName,
                                    location = selected.location,
                                    downloadMbps = result.downloadMbps,
                                    uploadMbps = result.uploadMbps,
                                    pingMs = result.pingMs,
                                    jitterMs = result.jitterMs,
                                    lossPct = result.packetLossPercent
                                )
                            )
                        } catch (e: SpeedTestEngine.SpeedTestCancelledException) {
                            phase = com.mporttech.pro.ui.i18n.Str.get("speed.cancelled", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)); status = com.mporttech.pro.ui.i18n.Str.get("speed.cancel_status", com.mporttech.pro.ui.i18n.loadSavedLanguage(context))
                        } catch (e: Exception) {
                            phase = com.mporttech.pro.ui.i18n.Str.get("speed.failed", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)); status = com.mporttech.pro.ui.i18n.Str.get("common.error_prefix", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)) + ": ${e.message}"
                            Toast.makeText(context, e.message ?: "Speed test gagal", Toast.LENGTH_LONG).show()
                        } finally {
                            running = false
                        }
                    }
                },
                enabled = !running && !probing,
                modifier = Modifier.weight(1f).height(48.dp),
                colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )
            ) {
                if (running) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color(0xFF000000))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (running) t("speed.testing") else t("speed.start"),
                    color = Color(0xFF000000),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
            if (running) {
                OutlinedButton(
                    onClick = { engine.cancel(); phase = com.mporttech.pro.ui.i18n.Str.get("speed.cancelling", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)); status = com.mporttech.pro.ui.i18n.Str.get("speed.cancelling", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)) },
                    modifier = Modifier.weight(0.45f)
                ) { Text(t("speed.stop")) }
            }
        }
        Text(t("speed.history"), fontWeight = FontWeight.Bold)
        history.forEach { line ->
            val parts = line.split(" — ")
            HistoryRow(parts.getOrElse(0) { line }, parts.getOrElse(1) { "" })
        }
        OutlinedButton(
            onClick = { nav?.navigate("speedResults") },
            modifier = Modifier.fillMaxWidth()
        ) { Text(t("speed.view_all")) }
        CardBlock(t("speed.engine_info")) {
            Text(
                "Katalog: ${catalog.size} server\n" +
                    "Default: ${TestServer.haansiro().displayName} · ${TestServer.haansiro().location}\n" +
                    "Engine: multi-thread DL ${ServerConfig.downloadThreads} / UL ${ServerConfig.uploadThreads}\n" +
                    "Paths: download • upload.php • latency.txt",
                fontSize = 11.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DeviceManagerScreen(nav: NavController) {
    val context = LocalContext.current
    var devices by remember { mutableStateOf<List<LiveDevice>>(emptyList()) }
    var filter by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    val filters = listOf("All", "Routers", "AP", "Switches")

    LaunchedEffect(Unit) {
        loading = true
        devices = try {
            LiveNetworkInfo.discoverDevices(context, authorized = true, endHost = 50)
        } catch (_: Exception) { emptyList() }
        loading = false
    }

    val filtered = when (filter) {
        1 -> devices.filter { it.kind in listOf("router", "gateway") }
        2 -> devices.filter { it.kind == "ap" }
        3 -> devices.filter { it.kind == "switch" }
        else -> devices
    }

    Page(t("tools.devices"), Icons.Default.Devices, nav) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            filters.forEachIndexed { i, label ->
                val count = when (i) {
                    1 -> devices.count { it.kind in listOf("router", "gateway") }
                    2 -> devices.count { it.kind == "ap" }
                    3 -> devices.count { it.kind == "switch" }
                    else -> devices.size
                }
                val selected = filter == i
                Box(
                    modifier = Modifier
                        .background(
                            if (selected) Color(0xFF00F0FF) else Color(0xFF0A1528),
                            RoundedCornerShape(20.dp)
                        )
                        .border(1.dp, if (selected) Color(0xFF00F0FF) else Color(0xFF1A4A60), RoundedCornerShape(20.dp))
                        .clickable { filter = i }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "$label ($count)",
                        fontSize = 11.sp,
                        color = if (selected) Color(0xFF03060F) else Color(0xFF9EE8FF),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        if (loading) Text("Scanning LAN…", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!loading && filtered.isEmpty()) Text("Tidak ada perangkat ditemukan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        filtered.forEach { d ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1528)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF1A4A60), RoundedCornerShape(14.dp))
                    .clickable {
                        SelectedDeviceStore.ip = d.ip
                        SelectedDeviceStore.name = d.name
                        SelectedDeviceStore.kind = d.kind
                        SelectedDeviceStore.mac = d.mac
                        SelectedDeviceStore.vendor = d.vendor
                        nav.navigate("deviceDetail")
                    }
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(40.dp).background(Color(0xFF0A2840), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when (d.kind) {
                                "router", "gateway" -> Icons.Default.Router
                                "ap" -> Icons.Default.Wifi
                                else -> Icons.Default.Devices
                            },
                            null, tint = Color(0xFF00F0FF), modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(d.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                        Text(d.ip, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        if (d.online) "Online" else "Offline",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (d.online) Color(0xFF39FF14) else Color(0xFFFF2E63)
                    )
                }
            }
        }
    }
}



@Composable
fun DeviceDetailScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val ip = SelectedDeviceStore.ip
    val name = SelectedDeviceStore.name
    val kind = SelectedDeviceStore.kind
    val mac = SelectedDeviceStore.mac
    val vendor = SelectedDeviceStore.vendor
    var pingResult by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    var online by remember { mutableStateOf(false) }
    var latency by remember { mutableStateOf<Long?>(null) }
    var openPorts by remember { mutableStateOf<List<Int>>(emptyList()) }
    var logLines by remember { mutableStateOf(listOf("Siap probe $ip")) }

    fun appendLog(line: String) {
        logLines = (listOf(line) + logLines).take(30)
    }

    LaunchedEffect(ip) {
        busy = true
        val (ok, ms) = LiveNetworkInfo.probe(ip, 1000)
        online = ok
        latency = ms
        appendLog(
            if (ok) "Reachable $ip ${ms ?: "?"}ms"
            else "No response from $ip"
        )
        // port sweep common management ports
        val ports = listOf(22, 53, 80, 443, 8728, 8291, 8080, 8291)
        val open = mutableListOf<Int>()
        withContext(Dispatchers.IO) {
            for (port in ports.distinct()) {
                try {
                    java.net.Socket().use { s ->
                        s.connect(java.net.InetSocketAddress(ip, port), 400)
                        open.add(port)
                    }
                } catch (_: Exception) {
                }
            }
        }
        openPorts = open
        if (open.isNotEmpty()) appendLog("Open ports: ${open.joinToString()}")
        busy = false
    }

    Page(t("screen.device_detail"), Icons.Default.Router, nav) {
        CardBlock("$name     ${if (online) "Online" else "Offline"}") {
            Text(ip, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            if (!mac.isNullOrBlank()) {
                Text("MAC  $mac", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (!vendor.isNullOrBlank()) {
                Text(vendor, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text(
                "Jenis  $kind${latency?.let { "  ·  ${it} ms" } ?: ""}",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        InteractiveTabStrip(listOf("Info", "Ports", "Log"), selectedTab) { selectedTab = it }
        when (selectedTab) {
            0 -> CardBlock("Live probe") {
                Text(
                    "IP             $ip\n" +
                        "MAC            ${mac ?: "—"}\n" +
                        "Manufacturer   ${vendor ?: "—"}\n" +
                        "Status         ${if (online) "Reachable" else "Unreachable"}\n" +
                        "Latency        ${latency?.let { "$it ms" } ?: "—"}\n" +
                        "Kind           $kind\n" +
                        "Open ports     ${if (openPorts.isEmpty()) "—" else openPorts.joinToString()}",
                    lineHeight = 22.sp,
                    fontSize = 11.sp
                )
            }
            1 -> CardBlock("Common ports") {
                val labels = mapOf(
                    22 to "SSH", 53 to "DNS", 80 to "HTTP", 443 to "HTTPS",
                    8728 to "RouterOS API", 8291 to "Winbox", 8080 to "HTTP-alt"
                )
                if (openPorts.isEmpty()) {
                    Text(
                        if (busy) "Probing…" else "Tidak ada port manajemen terbuka (atau difilter firewall)",
                        fontSize = 12.sp
                    )
                } else {
                    openPorts.forEach { port ->
                        Text(
                            "●  $port  ${labels[port] ?: ""}",
                            fontSize = 12.sp,
                            color = Color(0xFF39FF14)
                        )
                    }
                }
            }
            else -> CardBlock("Probe log") {
                logLines.forEach {
                    Text(it, fontSize = 11.sp, lineHeight = 18.sp)
                }
            }
        }
        pingResult?.let {
            Text(it, fontSize = 12.sp, color = Color(0xFF39FF14))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    scope.launch {
                        val (ok, ms) = LiveNetworkInfo.probe(ip, 1500)
                        online = ok
                        latency = ms
                        pingResult = if (ok) "Reachable ${ms}ms" else "No response"
                        pingResult?.let(::appendLog)
                        busy = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (busy) "…" else "PING") }
            Button(
                onClick = {
                    nav?.navigate("diagnostic")
                },
                modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )) { Text("DIAGNOSTIC") }
        }
    }
}

@Composable
fun AlertsScreen(nav: NavController) {
    val context = LocalContext.current
    var devices by remember { mutableStateOf<List<LiveDevice>>(emptyList()) }
    var filter by remember { mutableIntStateOf(0) }
    var link by remember { mutableStateOf(LiveNetworkInfo.snapshot(context)) }
    var gatewayMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        link = LiveNetworkInfo.snapshot(context)
        devices = try {
            LiveNetworkInfo.discoverDevices(context, authorized = true, endHost = 40)
        } catch (_: Exception) {
            emptyList()
        }
        val gw = link.gateway
        if (!gw.isNullOrBlank()) {
            val (ok, ms) = LiveNetworkInfo.probe(gw, 800)
            gatewayMs = if (ok) ms else null
        }
    }

    val items = buildList {
        devices.filter { !it.online }.forEach { d ->
            add(AlertUiItem(t("alert.device_offline"), "${d.name}\n${d.ip}", com.mporttech.pro.ui.i18n.Str.get("common.refresh", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)).let { "—" }, Color(0xFFFF2E63), "Critical"))
        }
        gatewayMs?.let { ms ->
            if (ms > 150) {
                add(
                    AlertUiItem(
                        t("alert.high_latency_id"),
                        "Gateway ${link.gateway}\nLatency: $ms ms",
                        "Baru saja",
                        Color(0xFFFFD60A),
                        "Warning"
                    )
                )
            }
        }
        if (!link.online) {
            add(AlertUiItem(t("alert.network_offline"), t("net.link_offline"), com.mporttech.pro.ui.i18n.Str.get("common.refresh", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)).let { "—" }, Color(0xFFFF2E63), "Critical"))
        }
    }
    val filtered = when (filter) {
        1 -> items.filter { it.severity == "Critical" }
        2 -> items.filter { it.severity == "Warning" }
        else -> items
    }

    Page(t("tools.alerts"), Icons.Default.Notifications, nav) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                t("alert.filter_all") + " (${items.size})",
                t("alert.filter_critical") + " (${items.count { it.severity == "Critical" }})",
                t("alert.filter_warning") + " (${items.count { it.severity == "Warning" }})"
            ).forEachIndexed { i, label ->
                val selected = filter == i
                Box(
                    Modifier
                        .background(
                            if (selected) Color(0xFF00F0FF) else Color(0xFF0A1528),
                            RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (selected) Color(0xFF00F0FF) else Color(0xFF1A4A60),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { filter = i }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        label,
                        fontSize = 11.sp,
                        color = if (selected) Color(0xFF03060F) else Color(0xFF9EE8FF),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            Text(t("alert.none"), fontSize = 12.sp, color = Color(0xFF39FF14))
        }
        filtered.forEach { a ->
            AlertCard(a.title, a.detail, a.time, a.color) {
                SelectedAlertStore.set(
                    title = a.title,
                    detail = a.detail,
                    time = a.time,
                    severity = a.severity,
                    colorArgb = a.color.value.toInt()
                )
                nav.navigate("alertDetail")
            }
        }
    }
    // Sync bottom-nav badge
    LaunchedEffect(items.size) {
        AlertBadgeStore.count = items.size
    }
}



private data class AlertUiItem(val title: String, val detail: String, val time: String, val color: Color, val severity: String)

@Composable
fun AlertDetailScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val link = remember { LiveNetworkInfo.snapshot(context) }
    val title = SelectedAlertStore.title.ifBlank { "Detail Peringatan" }
    val detail = SelectedAlertStore.detail.ifBlank {
        "Tidak ada alert terpilih. Kembali ke Alerts dan pilih item."
    }
    val severity = SelectedAlertStore.severity.ifBlank { "Info" }
    val time = SelectedAlertStore.time.ifBlank { "—" }
    Page(t("screen.alert_detail"), Icons.Default.Notifications, nav) {
        CardBlock(title) {
            Text(
                "Severity  $severity\n" +
                    "Waktu  $time\n\n" +
                    detail,
                fontSize = 12.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        CardBlock(t("alert.context")) {
            Text(
                "Transport  ${link.transport}\n" +
                    "IP lokal  ${link.ip ?: "—"}\n" +
                    "Gateway  ${link.gateway ?: "—"}\n" +
                    "SSID  ${link.ssid ?: "—"}",
                fontSize = 12.sp,
                lineHeight = 20.sp
            )
        }
        CardBlock(t("alert.actions")) {
            Text(
                "1. Pastikan Wi‑Fi/ethernet tersambung\n" +
                    "2. Probe gateway dari Device Manager\n" +
                    "3. Jalankan Diagnostic / Ping ke target yang diizinkan\n" +
                    "4. Cek izin lokasi jika SSID kosong",
                fontSize = 12.sp,
                lineHeight = 20.sp
            )
        }
        Button(
            onClick = { nav?.navigate("diagnostic") },
            modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )) { Text(t("alert.open_diag")) }
    }
}

@Composable
fun JobsScreen(nav: NavController) {
    val context = LocalContext.current
    var tickets by remember { mutableStateOf<List<com.mporttech.pro.core.database.TicketEntity>>(emptyList()) }
    var filter by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        try {
            val entry = dagger.hilt.android.EntryPointAccessors.fromApplication(
                context.applicationContext,
                com.mporttech.pro.di.AuthEntryPoint::class.java
            )
            entry.ticketRepository().observe().collect { list ->
                tickets = list
            }
        } catch (_: Exception) {
            tickets = emptyList()
        }
    }
    val filtered = when (filter) {
        1 -> tickets.filter {
            it.priority.equals("high", true) || it.priority.equals("urgent", true) ||
                it.status.equals("OPEN", true)
        }
        2 -> tickets.filter {
            it.status.equals("IN_PROGRESS", true) || it.status.equals("ASSIGNED", true)
        }
        else -> tickets
    }
    Page(t("tools.jobs"), Icons.Default.ConfirmationNumber, nav) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(
                t("jobs.filter_all") + " (${tickets.size})",
                t("jobs.filter_urgent") + " (${tickets.count { it.priority.equals("high", true) || it.priority.equals("urgent", true) }})",
                t("jobs.filter_open") + " (${tickets.count { it.status.equals("OPEN", true) }})"
            ).forEachIndexed { i, label ->
                val selected = filter == i
                Box(
                    Modifier
                        .background(if (selected) Color(0xFF00F0FF) else Color(0xFF0A1528), RoundedCornerShape(20.dp))
                        .border(1.dp, if (selected) Color(0xFF00F0FF) else Color(0xFF1A4A60), RoundedCornerShape(20.dp))
                        .clickable { filter = i }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        label,
                        fontSize = 11.sp,
                        color = if (selected) Color(0xFF03060F) else Color(0xFF9EE8FF),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        if (filtered.isEmpty()) {
            Text(
                t("jobs.empty"),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            filtered.forEach { tkt ->
                val color = when {
                    tkt.priority.equals("high", true) || tkt.priority.equals("urgent", true) -> Color(0xFFFF2E63)
                    tkt.status.equals("OPEN", true) -> Color(0xFFFFD60A)
                    else -> Color(0xFF39FF14)
                }
                JobCard(
                    tag = tkt.status.ifBlank { "TICKET" },
                    title = tkt.title.ifBlank { tkt.ticketNumber.ifBlank { "Tiket #${tkt.id}" } },
                    detail = buildString {
                        append(tkt.customerName.ifBlank { "Customer —" })
                        append(" · ")
                        append(tkt.priority)
                        if (tkt.description.isNotBlank()) append("\n").append(tkt.description.take(80))
                    },
                    color = color
                ) {
                    nav.navigate("tickets")
                }
            }
        }
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1528)),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF1A4A60), RoundedCornerShape(14.dp))
                .clickable { nav.navigate("tickets") }
        ) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ConfirmationNumber, null, tint = Color(0xFF00F0FF))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t("jobs.manage"), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text(t("jobs.manage_sub"), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("→", color = Color(0xFF00F0FF), fontSize = 18.sp)
            }
        }
    }
}




@Composable
fun ReportsScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val history = remember { com.mporttech.pro.features.speedtest.SpeedTestHistoryStore.load(context) }
    val link = remember { LiveNetworkInfo.snapshot(context) }

    val rows = listOf(
        Triple("Network Performance", "Status: ${if (link.online) "Online" else "Offline"} · ${link.transport}", Icons.Default.BarChart),
        Triple("Device Uptime", "Device uptime sejak boot", Icons.Default.Timeline),
        Triple("Bandwidth Usage", "Rx/Tx live dari TrafficStats", Icons.Default.Speed),
        Triple("Speed Test History", "${history.size} hasil tersimpan", Icons.Default.Speed),
        Triple("Technician Activity", "Aktivitas sesi staff", Icons.Default.Person),
        Triple("Incident Report", "Dari alert & offline scan", Icons.Default.Notifications)
    )

    Page("Reports", Icons.Default.BarChart, nav) {
        rows.forEach { (title, sub, icon) ->
            ReportRow(title, sub, icon) {
                when {
                    title.contains("Speed") -> nav?.navigate("speedResults")
                    title.contains("Network") || title.contains("Bandwidth") -> nav?.navigate("network")
                    title.contains("Incident") -> nav?.navigate("alerts")
                    else -> { }
                }
            }
        }
        CardBlock("Periode") {
            Text("7 Hari Terakhir (data lokal device)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF00F0FF), RoundedCornerShape(14.dp))
                .clickable {
                    android.widget.Toast.makeText(
                        context,
                        "Laporan disusun dari data live + history lokal",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Generate Report", fontWeight = FontWeight.Bold, color = Color(0xFF03060F))
        }
    }
}



@Composable
fun ProfileScreen(nav: NavController) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var notifications by remember { mutableStateOf(true) }
    var biometrics by remember { mutableStateOf(false) }
    val sessionUser = remember { com.mporttech.pro.core.auth.SessionManager.currentUser(context) }
    val isAdmin = com.mporttech.pro.core.auth.SessionManager.isAdmin(context)
    val isGuest = com.mporttech.pro.core.auth.SessionManager.isGuest(context) ||
        sessionUser?.role == com.mporttech.pro.core.auth.UserRole.GUEST ||
        sessionUser == null
    val isStaff = com.mporttech.pro.core.auth.SessionManager.isStaff(context)
    var displayName by remember {
        mutableStateOf(sessionUser?.name ?: "User")
    }
    val roleLabel = when (sessionUser?.role) {
        com.mporttech.pro.core.auth.UserRole.ADMIN -> t("profile.role_admin")
        com.mporttech.pro.core.auth.UserRole.TECHNICIAN -> t("profile.role_tech")
        com.mporttech.pro.core.auth.UserRole.GUEST -> t("profile.role_guest")
        null -> t("profile.role_guest")
    }
    var editRole by remember { mutableStateOf(roleLabel) }
    var phone by remember { mutableStateOf("0812-3456-7890") }
    var email by remember {
        mutableStateOf(sessionUser?.username?.let { "$it@mport.tech" } ?: "user@mport.tech")
    }
    var showEditProfile by remember { mutableStateOf(false) }
    var showSecurity by remember { mutableStateOf(false) }
    var showServer by remember { mutableStateOf(false) }
    var serverHost by remember { mutableStateOf("api.mport.tech") }
    var serverPort by remember { mutableStateOf("443") }
    var pinEnabled by remember { mutableStateOf(true) }

    Page(t("screen.profile"), Icons.Default.Person, nav) {
        if (!isGuest) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                displayName.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(displayName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(roleLabel, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(email, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("● Online", color = Color(0xFF39FF14), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("Field Unit", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            SettingsRow(t("profile.edit"), Icons.Default.Person) { editRole = roleLabel; showEditProfile = true }
        } else {
            // Guest: no "Pengguna Umum" identity — show staff sign-in CTA only
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.clickable {
                    nav.navigate("login") {
                        launchSingleTop = true
                    }
                }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t("login.staff_button"), fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            t("login.subtitle"),
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        val langState = LocalAppLanguage.current
        CardBlock(t("profile.language")) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = langState.value == AppLanguage.INDONESIAN,
                    onClick = {
                        langState.value = AppLanguage.INDONESIAN
                        saveLanguage(context, AppLanguage.INDONESIAN)
                    },
                    label = { Text(t("profile.language_id")) }
                )
                FilterChip(
                    selected = langState.value == AppLanguage.ENGLISH,
                    onClick = {
                        langState.value = AppLanguage.ENGLISH
                        saveLanguage(context, AppLanguage.ENGLISH)
                    },
                    label = { Text(t("profile.language_en")) }
                )
            }
        }
        SwitchRow(t("common.notifications"), Icons.Default.Notifications, notifications) {
            notifications = it
        }
        SettingsRow(t("profile.security"), Icons.Default.Security) { showSecurity = true }
        if (isAdmin) SettingsRow(t("profile.mikrotik"), Icons.Default.Router) { nav.navigate("mikrotik") }
        if (isAdmin) SettingsRow(t("profile.server"), Icons.Default.Cloud) { showServer = true }
        if (isAdmin) SettingsRow(t("screen.customers"), Icons.Default.People) { nav.navigate("customers") }
        if (isStaff) SettingsRow(t("screen.tickets"), Icons.Default.ConfirmationNumber) { nav.navigate("tickets") }
        if (isAdmin) SettingsRow(t("screen.reports"), Icons.Default.BarChart) { nav.navigate("reports") }
        if (isAdmin) SettingsRow(t("profile.backup"), Icons.Default.Backup) {
            try {
                val dbFile = context.getDatabasePath("mport_tech.db")
                val outDir = context.getExternalFilesDir(null) ?: context.filesDir
                val stamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                val out = java.io.File(outDir, "mport_backup_$stamp.db")
                if (dbFile.exists()) {
                    dbFile.copyTo(out, overwrite = true)
                    for (sfx in listOf("-wal", "-shm")) {
                        val side = java.io.File(dbFile.path + sfx)
                        if (side.exists()) side.copyTo(java.io.File(out.path + sfx), overwrite = true)
                    }
                    Toast.makeText(context, "Backup OK:\n${out.absolutePath}", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, "Database belum ada — buat ticket/pelanggan dulu", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Backup gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        if (isAdmin) SettingsRow(t("profile.restore"), Icons.Default.Restore) {
            try {
                val outDir = context.getExternalFilesDir(null) ?: context.filesDir
                val latest = outDir.listFiles()?.filter { it.name.startsWith("mport_backup_") && it.name.endsWith(".db") }
                    ?.maxByOrNull { it.lastModified() }
                if (latest == null) {
                    Toast.makeText(context, "Tidak ada file backup", Toast.LENGTH_SHORT).show()
                } else {
                    val dbFile = context.getDatabasePath("mport_tech.db")
                    // close isn't available here — copy over; app restart recommended
                    latest.copyTo(dbFile, overwrite = true)
                    for (sfx in listOf("-wal", "-shm")) {
                        val side = java.io.File(latest.path + sfx)
                        val dest = java.io.File(dbFile.path + sfx)
                        if (side.exists()) side.copyTo(dest, overwrite = true) else if (dest.exists()) dest.delete()
                    }
                    Toast.makeText(context, "Restore dari ${latest.name}. Restart app agar Room memuat ulang.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Restore gagal: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
        if (com.mporttech.pro.core.auth.SessionManager.isAdmin(context)) {
            SettingsRow(t("screen.tech_admin"), Icons.Default.People) { nav.navigate("techAdmin") }
        }
        if (!isGuest) {
            SettingsRow(t("common.logout"), Icons.Default.ExitToApp) {
                // Prefer AuthRepository.logout (server invalidate + clear tokens)
                try {
                    val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        com.mporttech.pro.di.AuthEntryPoint::class.java
                    )
                    scope.launch {
                        entryPoint.authRepository().logout()
                        (context as? android.app.Activity)?.recreate()
                    }
                } catch (_: Exception) {
                    com.mporttech.pro.core.auth.SessionManager.logout(context)
                    (context as? android.app.Activity)?.recreate()
                }
            }
        }
        SettingsRow(t("screen.about"), Icons.Default.Info) { nav.navigate("about") }
    }

    if (showEditProfile) {
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text(t("profile.edit"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(displayName, { displayName = it }, label = { Text("Nama") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(editRole, { editRole = it }, label = { Text("Jabatan") }, singleLine = true, modifier = Modifier.fillMaxWidth(), enabled = isAdmin)
                    OutlinedTextField(phone, { phone = it }, label = { Text("Telepon") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    showEditProfile = false
                    Toast.makeText(context, "Profil diperbarui", Toast.LENGTH_SHORT).show()
                },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text(t("common.cancel")) }
            }
        )
    }

    if (showSecurity) {
        AlertDialog(
            onDismissRequest = { showSecurity = false },
            title = { Text(t("profile.security"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("App PIN lock", modifier = Modifier.weight(1f))
                        Switch(checked = pinEnabled, onCheckedChange = { pinEnabled = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Biometric unlock", modifier = Modifier.weight(1f))
                        Switch(checked = biometrics, onCheckedChange = { biometrics = it })
                    }
                    Text(
                        "Di production, gunakan EncryptedSharedPreferences + BiometricPrompt. Jangan simpan kredensial RouterOS di APK.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    showSecurity = false
                    Toast.makeText(
                        context,
                        "Security: PIN ${if (pinEnabled) "ON" else "OFF"} • Bio ${if (biometrics) "ON" else "OFF"}",
                        Toast.LENGTH_SHORT
                    ).show()
                },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showSecurity = false }) { Text(t("common.close")) }
            }
        )
    }

    if (showServer) {
        AlertDialog(
            onDismissRequest = { showServer = false },
            title = { Text(t("profile.server"), fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(serverHost, { serverHost = it }, label = { Text("API Host") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(serverPort, { serverPort = it }, label = { Text("Port") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("TLS wajib untuk production. Endpoint lokal hanya untuk debug.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(onClick = {
                    showServer = false
                    Toast.makeText(context, "Server: $serverHost:$serverPort disimpan", Toast.LENGTH_SHORT).show()
                },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = {
                    Toast.makeText(context, "Testing $serverHost:$serverPort … OK (simulasi)", Toast.LENGTH_SHORT).show()
                }) { Text("Test") }
            }
        )
    }


}

@Composable
fun LegacyMikroTikScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snap = remember { LiveNetworkInfo.snapshot(context) }
    var host by remember { mutableStateOf(snap.gateway ?: "192.168.88.1") }
    var checking by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var openApi by remember { mutableStateOf(false) }
    var openWinbox by remember { mutableStateOf(false) }
    var openWww by remember { mutableStateOf(false) }

    Page(t("screen.mikrotik"), Icons.Default.Router, nav) {
        CardBlock(t("mt.probe_title")) {
            Text(
                t("mt.probe_hint"),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = host,
                onValueChange = { host = it },
                label = { Text(t("mt.host")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Button(
            enabled = !checking && host.isNotBlank(),
            onClick = {
                checking = true
                result = null
                scope.launch {
                    val h = host.trim()
                    fun portOpen(port: Int): Boolean {
                        return try {
                            java.net.Socket().use { s ->
                                s.connect(java.net.InetSocketAddress(h, port), 1200)
                                true
                            }
                        } catch (_: Exception) {
                            false
                        }
                    }
                    val api = withContext(Dispatchers.IO) { portOpen(8728) }
                    val winbox = withContext(Dispatchers.IO) { portOpen(8291) }
                    val www = withContext(Dispatchers.IO) { portOpen(80) || portOpen(443) }
                    openApi = api
                    openWinbox = winbox
                    openWww = www
                    result = buildString {
                        append(if (api) "● API 8728 terbuka\n" else "○ API 8728 tertutup/filter\n")
                        append(if (winbox) "● Winbox 8291 terbuka\n" else "○ Winbox 8291 tertutup/filter\n")
                        append(if (www) "● Web 80/443 terbuka" else "○ Web 80/443 tertutup/filter")
                    }
                    checking = false
                }
            },
            modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )) { Text(if (checking) t("common.loading") else t("mt.check")) }

        result?.let {
            CardBlock("Hasil probe $host") {
                Text(it, fontSize = 12.sp, lineHeight = 20.sp)
            }
        }

        CardBlock(t("mt.checklist")) {
            Text(
                "1. Pastikan management IP reachable\n" +
                    "2. API (8728) hanya di jaringan trusted\n" +
                    "3. Jangan simpan password di APK / chat\n" +
                    "4. Gunakan Winbox/SSH terpisah untuk konfigurasi",
                fontSize = 12.sp,
                lineHeight = 20.sp
            )
        }
        OutlinedButton(
            onClick = { nav?.navigate("diagnostic") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("PING / DIAGNOSTIC") }
    }
}

@Composable
fun ActivityScreen(nav: NavController? = null) {
    val context = LocalContext.current
    var link by remember { mutableStateOf(LiveNetworkInfo.snapshot(context)) }
    Page(t("screen.activity"), Icons.Default.History, nav) {
        CardBlock("Sesi perangkat saat ini") {
            Text(
                "Transport  ${link.transport}\n" +
                    "SSID  ${link.ssid ?: "—"}\n" +
                    "IP  ${link.ip ?: "—"}\n" +
                    "Gateway  ${link.gateway ?: "—"}",
                fontSize = 12.sp,
                lineHeight = 20.sp
            )
        }
        CardBlock("Pintasan riwayat") {
            Text(
                "Riwayat ping tersimpan di modul Diagnostic (database lokal Room).\n" +
                    "Tiket lapangan tersimpan di modul Tickets.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { nav?.navigate("diagnostic") }) { Text("Diagnostic") }
                OutlinedButton(onClick = { nav?.navigate("tickets") }) { Text("Tickets") }
            }
        }
        OutlinedButton(
            onClick = { link = LiveNetworkInfo.snapshot(context) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(t("common.refresh")) }
    }
}

@Composable
fun SettingsScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val link = remember { LiveNetworkInfo.snapshot(context) }
    Page(t("screen.settings"), Icons.Default.Settings, nav) {
        CardBlock(t("settings.app")) {
            Text("MPorT Tech Pro", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp)
            Text(t("settings.app_desc"), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CardBlock(t("settings.device_network")) {
            Text(
                "${link.transport}  ·  ${link.ip ?: "—"}\nGW ${link.gateway ?: "—"}",
                fontSize = 12.sp
            )
        }
        SettingsRow(t("screen.profile"), Icons.Default.Person) { nav?.navigate("profile") }
        if (com.mporttech.pro.core.auth.SessionManager.isAdmin(context)) {
            SettingsRow(t("screen.customers"), Icons.Default.People) { nav?.navigate("customers") }
        }
        if (com.mporttech.pro.core.auth.SessionManager.isStaff(context)) {
            SettingsRow(t("screen.tickets"), Icons.Default.ConfirmationNumber) { nav?.navigate("tickets") }
        }
        SettingsRow(t("screen.diagnostic"), Icons.Default.NetworkCheck) { nav?.navigate("diagnostic") }
        SettingsRow(t("screen.wifi"), Icons.Default.Wifi) { nav?.navigate("wifi") }
        SettingsRow(t("screen.about"), Icons.Default.Info) { nav?.navigate("about") }
        CardBlock(t("settings.privacy")) {
            Text(
                t("settings.privacy_body"),
                fontSize = 12.sp,
                lineHeight = 20.sp
            )
        }
    }
}
