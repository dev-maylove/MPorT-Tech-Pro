package com.mporttech.pro.features.tools

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
import androidx.compose.ui.graphics.Color
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
import com.mporttech.pro.ui.theme.LocalThemeMode
import com.mporttech.pro.ui.theme.ThemeMode
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

private data class Tile(val title: String, val subtitle: String, val icon: ImageVector, val route: String = "")

@Composable
fun NetworkMonitorScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Overview", "Devices", "Grafik")
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
            rxHistory = (rxHistory + rx).takeLast(30)
            txHistory = (txHistory + tx).takeLast(30)
            refreshLink()
            val gw = link.gateway
            if (!gw.isNullOrBlank()) {
                val (ok, ms) = LiveNetworkInfo.probe(gw, 800)
                gatewayOk = ok
                gatewayMs = ms
            }
            delay(2000)
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
                        Color(0xFF35E381),
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
                        if (gatewayOk) Color(0xFF35E381) else Color(0xFFFF5E67),
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
                                    devices = LiveNetworkInfo.discoverDevices(context, authorized = true)
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
                    DeviceCard(d.name, d.ip, d.online) {
                        SelectedDeviceStore.ip = d.ip
                        SelectedDeviceStore.name = d.name
                        SelectedDeviceStore.kind = d.kind
                        nav.navigate("deviceDetailRich")
                    }
                }
            }
            else -> {
                CardBlock("Throughput live") {
                    Text(
                        "RX ${String.format("%.2f", rxMbps)} Mbps  •  TX ${String.format("%.2f", txMbps)} Mbps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Peak RX ${String.format("%.2f", (rxHistory.maxOrNull() ?: 0.0))}  ·  Peak TX ${String.format("%.2f", (txHistory.maxOrNull() ?: 0.0))} Mbps",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { ((rxMbps + txMbps) / 100.0).toFloat().coerceIn(0.02f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Avg RX ${String.format("%.2f", if (rxHistory.isEmpty()) 0.0 else rxHistory.average())}  ·  samples ${rxHistory.size}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                CardBlock("Gateway latency") {
                    Text(
                        when {
                            link.gateway == null -> "Gateway tidak terdeteksi"
                            gatewayMs != null && gatewayOk -> "Ping ${link.gateway}  ·  ${gatewayMs} ms  ·  reachable"
                            else -> "Ping ${link.gateway}  ·  no response"
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
    val tools = listOf(
        Tile("Discovery", "LAN devices · WiFiman style", Icons.Default.Search, "discovery"),
        Tile("Signal Hub", "RSSI · latency live", Icons.Default.BarChart, "signalHub"),
        Tile("Network Latency", "Google · FB · X · GW", Icons.Default.Speed, "latencyHub"),
        Tile("Ping Tool", "Uji latency & loss", Icons.Default.NetworkCheck, "ping"),
        Tile("Traceroute", "Path & hop latency", Icons.Default.Timeline, "traceroute"),
        Tile("DNS Lookup", "Resolve A/AAAA", Icons.Default.Search, "dns"),
        Tile("WiFi Analyzer", "Analisa WiFi", Icons.Default.Wifi, "wifi"),
        Tile("Port Checker", "TCP port scan", Icons.Default.Security, "portcheck"),
        Tile("Speed Test", "Tes kecepatan", Icons.Default.Speed, "speedtest"),
        Tile("IP Scanner", "Scan perangkat", Icons.Default.Router, "scanner"),
        Tile("Customers", "Data pelanggan", Icons.Default.People, "customers"),
        Tile("Tickets", "Work orders", Icons.Default.ConfirmationNumber, "tickets"),
        Tile("Reports", "Laporan jaringan", Icons.Default.BarChart, "reports")
    )
    Page("Technician Tools", Icons.Default.Build, nav) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(420.dp),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tools) { tool ->
                ToolTile(tool) {
                    if (tool.route.isNotBlank()) nav.navigate(tool.route)
                }
            }
        }
        CardBlock("Semua tools dalam satu layar") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Build, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text("Kerja lebih cepat, lebih efisien", fontSize = 11.sp)
            }
        }
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
        Tile("WiFi Scanner", "Scan AP sekitar (optimized)", Icons.Default.Wifi, "wifi"),
        Tile("Signal Strength", "Kualitas & estimasi link", Icons.Default.NetworkCheck, "signalHub"),
        Tile("Channel Analyzer", "Rekomendasi channel", Icons.Default.BarChart, "wifi"),
        Tile("Network Information", "Info koneksi aktif", Icons.Default.Info, "wifi"),
        Tile("Connected Devices", "Perangkat terhubung", Icons.Default.Groups, "discovery"),
        Tile("Speed Test", "Tes kecepatan penuh", Icons.Default.Speed, "speedtest")
    )
    Page("WiFi Tools", Icons.Default.Wifi, nav) {
        CardBlock(connected?.ssid ?: "WiFi tidak terhubung") {
            if (connected != null) {
                Text(
                    "RSSI ${(connected?.rssiDbm ?: 0)} dBm • Q${(connected?.qualityScore ?: 0)}/100 • ~${(connected?.estimatedMbps ?: 0)} Mbps",
                    color = Color(0xFF35E381),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Text("Buka analyzer untuk scan & speed test", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (quickCount > 0) {
                Spacer(Modifier.height(4.dp))
                Text("$quickCount AP di cache lokal", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.height(340.dp),
            userScrollEnabled = false,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tiles) { ToolTile(it) { nav.navigate(it.route) } }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(onClick = { nav.navigate("wifi") }, modifier = Modifier.weight(1f)) {
                Text("WIFI ANALYZER", fontSize = 12.sp)
            }
            Button(onClick = { nav.navigate("speedtest") }, modifier = Modifier.weight(1f)) {
                Text("SPEED TEST", fontSize = 12.sp)
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
        scanning = true
        status = if (force) "Force scan..." else "Scanning (optimized)..."
        permissionHint = null
        scope.launch {
            try {
                if (!analyzer.isWifiEnabled()) {
                    status = "WiFi mati — mengaktifkan..."
                    analyzer.setWifiEnabled(true)
                    delay(600)
                }
                val snap = withContext(Dispatchers.IO) { analyzer.scan(force = force) }
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
                status = "Gagal: ${e.message}"
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
            permissionHint = "Izin lokasi / Nearby WiFi ditolak."
            status = "Izin diperlukan untuk scan WiFi"
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

    // Auto-scan once when opened (uses cache if fresh)
    LaunchedEffect(Unit) {
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
                    color = Color(0xFF35E381),
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
                    modifier = Modifier.weight(1f)
                ) {
                    if (miniRunning) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (miniRunning) "TESTING..." else "SPEED TEST", fontSize = 11.sp)
                }
                OutlinedButton(
                    onClick = { nav?.navigate("speedtest") },
                    modifier = Modifier.weight(1f)
                ) { Text("FULL TEST", fontSize = 11.sp) }
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
                modifier = Modifier.weight(1f)
            ) {
                if (scanning) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(6.dp))
                }
                Text(if (scanning) "SCAN..." else "SCAN WIFI")
            }
            OutlinedButton(
                onClick = { doScan(force = false) },
                enabled = !scanning,
                modifier = Modifier.weight(1f)
            ) { Text("CACHE/REFRESH") }
        }

        OutlinedButton(
            onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                } catch (_: Exception) {
                    Toast.makeText(context, "Buka Settings → Location", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("BUKA LOCATION SETTINGS") }

        Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        permissionHint?.let { Text(it, fontSize = 11.sp, color = MaterialTheme.colorScheme.error) }

        snapshot?.let { snap ->
            CardBlock("Channel Recommendation") {
                Text(
                    "2.4 GHz → CH ${snap.recommended24 ?: "-"}    |    5 GHz → CH ${snap.recommended5 ?: "-"}",
                    color = Color(0xFF35E381),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                val busy24 = snap.channels24.sortedByDescending { it.congestionScore }.take(3)
                if (busy24.isNotEmpty()) {
                    Text(
                        "Padat 2.4: " + busy24.joinToString { "CH ${it.channel}(${it.networkCount})" },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
                "Belum ada hasil. Pastikan Location ON, lalu SCAN WIFI.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            filtered.take(40).forEach { net ->
                WifiRow(
                    name = (if (net.isConnected) "★ " else "") + net.ssid,
                    detail = "CH ${net.channel} • ${net.frequencyMhz} MHz • ${net.security} • ${net.widthMhz}MHz • Q${net.qualityScore} • ~${net.estimatedMbps}Mbps",
                    signal = "${net.rssiDbm} dBm",
                    onClick = { openWifiConnectDialog(context, net.ssid, net.security) }
                )
            }
        }
    }
}

@Composable
fun NetworkScannerScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var allowed by remember { mutableStateOf(false) }
    var subnet by remember { mutableStateOf("192.168.1") }
    var scanning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<ScanHost>>(emptyList()) }
    var status by remember { mutableStateOf("Siap memindai subnet privat terotorisasi") }
    var progress by remember { mutableStateOf(0f) }

    Page("Network Scanner", Icons.Default.NetworkCheck, nav) {
        CardBlock("Authorized network scanning") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = allowed, onCheckedChange = { allowed = it })
                Text(
                    "Saya memiliki izin untuk memindai jaringan ini (RFC1918)",
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
                    Toast.makeText(context, "Centang izin terlebih dahulu", Toast.LENGTH_SHORT).show()
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
                        status = "Gagal: ${e.message}"
                        Toast.makeText(context, e.message ?: "Scan gagal", Toast.LENGTH_LONG).show()
                    } finally {
                        scanning = false
                    }
                }
            },
            enabled = allowed && !scanning,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (scanning) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                Spacer(Modifier.width(8.dp))
            }
            Text(if (scanning) "SCANNING..." else "MULAI SCAN")
        }

        if (results.isEmpty() && !scanning) {
            DeviceRow("MikroTik CCR", "192.168.1.1", true)
            DeviceRow("Access Point Office", "192.168.1.10", true)
            DeviceRow("Switch Lantai 2", "192.168.1.20", false)
            Text("Contoh di atas adalah data demo. Hasil scan aktual muncul setelah tombol ditekan.", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    var downloadMbps by remember { mutableStateOf(0.0) }
    var uploadMbps by remember { mutableStateOf(0.0) }
    var pingMs by remember { mutableStateOf(0.0) }
    var jitterMs by remember { mutableStateOf(0.0) }
    var lossPct by remember { mutableStateOf(0.0) }
    var progress by remember { mutableStateOf(0f) }
    var selected by remember { mutableStateOf(ServerSelector.selected) }
    var status by remember { mutableStateOf("Server: ${selected.name} • ${selected.location}") }
    var showServerPicker by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf(listOf("— — Belum ada tes")) }
    val catalog = remember { TestServer.catalog() }

    // Apply selected server to engine config
    LaunchedEffect(selected) {
        ServerSelector.select(selected)
        status = "${selected.displayName} • ${selected.displaySubtitle}"
    }

    Page("Speed Test", Icons.Default.Speed, nav) {
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
                        Text("Latency ~ ${String.format(Locale.US, "%.0f", it)} ms", fontSize = 10.sp, color = Color(0xFF35E381))
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
                            Text(if (probing) "PROBING..." else "AUTO NEAREST", fontSize = 10.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                selected = TestServer.haansiro()
                                showServerPicker = false
                            },
                            modifier = Modifier.weight(1f)
                        ) { Text("DEFAULT HaaNSirO", fontSize = 10.sp) }
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
                                    Text("${s.location} • ${s.distanceKm ?: "-"} km", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (active) Text("●", color = Color(0xFF35E381))
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
                Text("SERVER  •  ${selected.name}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        strokeWidth = 12.dp
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val main = when {
                            running && phase.contains("Upload", true) -> uploadMbps
                            running && phase.contains("Download", true) -> downloadMbps
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
            MetricBox("Loss", if (pingMs > 0) String.format(Locale.US, "%.0f%%", lossPct) else "—", Color(0xFF35E381), Modifier.weight(1f))
        }
        Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = {
                    if (running) return@Button
                    running = true
                    showServerPicker = false
                    phase = "Ping..."
                    progress = 0.05f
                    ServerSelector.select(selected)
                    status = "Tes ke ${selected.displayName}"
                    downloadMbps = 0.0; uploadMbps = 0.0; pingMs = 0.0; jitterMs = 0.0; lossPct = 0.0
                    scope.launch {
                        try {
                            val result = withContext(Dispatchers.IO) {
                                engine.run(multiConnection = true) { p ->
                                    when (p.phase) {
                                        Phase.PING -> {
                                            phase = "Ping..."
                                            progress = 0.1f
                                            if (p.pingMs > 0) pingMs = p.pingMs
                                        }
                                        Phase.DOWNLOAD -> {
                                            phase = "Download..."
                                            progress = (0.15f + (p.mbps / 300.0).toFloat() * 0.4f).coerceIn(0.15f, 0.55f)
                                            if (p.mbps > 0) downloadMbps = p.mbps
                                        }
                                        Phase.UPLOAD -> {
                                            phase = "Upload..."
                                            progress = (0.55f + (p.mbps / 150.0).toFloat() * 0.4f).coerceIn(0.55f, 0.95f)
                                            if (p.mbps > 0) uploadMbps = p.mbps
                                        }
                                        Phase.COMPLETED -> { phase = "Selesai"; progress = 1f }
                                        Phase.ERROR -> { phase = "Error" }
                                    }
                                }
                            }
                            downloadMbps = result.downloadMbps
                            uploadMbps = result.uploadMbps
                            pingMs = result.pingMs
                            jitterMs = result.jitterMs
                            lossPct = result.packetLossPercent
                            phase = "Selesai"
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
                            phase = "Dibatalkan"; status = "Tes dibatalkan"
                        } catch (e: Exception) {
                            phase = "Gagal"; status = "Gagal: ${e.message}"
                            Toast.makeText(context, e.message ?: "Speed test gagal", Toast.LENGTH_LONG).show()
                        } finally {
                            running = false
                        }
                    }
                },
                enabled = !running && !probing,
                modifier = Modifier.weight(1f)
            ) {
                if (running) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (running) "TESTING..." else "MULAI TEST")
            }
            if (running) {
                OutlinedButton(
                    onClick = { engine.cancel(); phase = "Membatalkan..."; status = "Membatalkan tes..." },
                    modifier = Modifier.weight(0.45f)
                ) { Text("STOP") }
            }
        }
        Text("History Test Speed", fontWeight = FontWeight.Bold)
        history.forEach { line ->
            val parts = line.split(" — ")
            HistoryRow(parts.getOrElse(0) { line }, parts.getOrElse(1) { "" })
        }
        OutlinedButton(
            onClick = { nav?.navigate("speedResults") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Lihat semua hasil Speed Test") }
        CardBlock("History Test Speed") {
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
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) }
    var devices by remember { mutableStateOf<List<LiveDevice>>(emptyList()) }
    var scanning by remember { mutableStateOf(false) }
    var authorized by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("Aktifkan otorisasi lalu scan segmen privat") }

    val routers = devices.filter { it.kind == "gateway" || it.kind == "router" }
    val aps = devices.filter { it.kind == "ap" }
    val switches = devices.filter { it.kind == "switch" }
    val tabs = listOf(
        "All (${devices.size})",
        "GW/Router (${routers.size})",
        "AP (${aps.size})",
        "Switch (${switches.size})"
    )
    val filtered = when (selectedTab) {
        1 -> routers
        2 -> aps
        3 -> switches
        else -> devices
    }

    val labelAuthorize = t("net.authorize_scan")
    val msgAuthorizeFirst = t("net.authorize_first")
    val labelScanLan = t("net.scan_lan")
    val labelLoading = t("common.loading")
    Page(t("screen.device_manager"), Icons.Default.Storage, nav) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = authorized, onCheckedChange = { authorized = it })
            Text(
                labelAuthorize,
                fontSize = 12.sp,
                modifier = Modifier.clickable { authorized = !authorized }
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                enabled = !scanning,
                onClick = {
                    if (!authorized) {
                        Toast.makeText(context, msgAuthorizeFirst, Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scanning = true
                    status = "Scanning…"
                    scope.launch {
                        try {
                            devices = LiveNetworkInfo.discoverDevices(context, authorized = true)
                            status = "${devices.size} devices · ${devices.count { it.online }} online"
                        } catch (e: Exception) {
                            status = e.message ?: "Scan failed"
                            Toast.makeText(context, status, Toast.LENGTH_LONG).show()
                        } finally {
                            scanning = false
                        }
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (scanning) labelLoading else labelScanLan) }
            OutlinedButton(
                onClick = {
                    val snap = LiveNetworkInfo.snapshot(context)
                    val gw = snap.gateway
                    if (gw != null) {
                        SelectedDeviceStore.ip = gw
                        SelectedDeviceStore.name = "Gateway"
                        SelectedDeviceStore.kind = "gateway"
                        nav.navigate("deviceDetailRich")
                    } else {
                        Toast.makeText(context, "Gateway tidak terdeteksi", Toast.LENGTH_SHORT).show()
                    }
                }
            ) { Text("GATEWAY") }
        }
        Text(status, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        InteractiveTabStrip(tabs, selectedTab) { selectedTab = it }
        if (filtered.isEmpty()) {
            CardBlock("Kosong") {
                Text(
                    "Belum ada hasil scan. Gunakan SCAN LAN pada jaringan yang Anda kelola.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        filtered.forEach { d ->
            DeviceCard(
                name = "${d.name}${d.latencyMs?.let { "  ·  ${it}ms" } ?: ""}",
                ip = d.ip,
                online = d.online
            ) {
                SelectedDeviceStore.ip = d.ip
                SelectedDeviceStore.name = d.name
                SelectedDeviceStore.kind = d.kind
                nav.navigate("deviceDetailRich")
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
                            color = Color(0xFF35E381)
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
            Text(it, fontSize = 12.sp, color = Color(0xFF35E381))
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
                        appendLog(pingResult!!)
                        busy = false
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text(if (busy) "…" else "PING") }
            Button(
                onClick = {
                    nav?.navigate("diagnostic")
                },
                modifier = Modifier.weight(1f)
            ) { Text("DIAGNOSTIC") }
        }
    }
}

@Composable
fun AlertsScreen(nav: NavController) {
    val context = LocalContext.current
    var link by remember { mutableStateOf(LiveNetworkInfo.snapshot(context)) }
    var gatewayOk by remember { mutableStateOf<Boolean?>(null) }
    var gatewayMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        link = LiveNetworkInfo.snapshot(context)
        val gw = link.gateway
        if (gw != null) {
            val (ok, ms) = LiveNetworkInfo.probe(gw, 1000)
            gatewayOk = ok
            gatewayMs = ms
        } else {
            gatewayOk = false
        }
    }

    val alerts = buildList {
        if (!link.online) {
            add(AlertItem("Tidak ada koneksi data", "Tidak ada transport aktif (Wi‑Fi/seluler/ethernet)", "now", Color(0xFFFF5E67), "critical"))
        }
        if (link.online && gatewayOk == false) {
            add(AlertItem("Gateway tidak merespons", "Host ${link.gateway ?: "?"} tidak reachable", "now", Color(0xFFFF5E67), "critical"))
        }
        if (link.online && gatewayOk == true && (gatewayMs ?: 0) > 100) {
            add(AlertItem("Latency gateway tinggi", "${link.gateway} · ${gatewayMs} ms", "now", Color(0xFFFFB020), "warning"))
        }
        if (link.online && link.transport == "Cellular") {
            add(AlertItem("Menggunakan seluler", "Bukan Wi‑Fi — cek SSID lapangan jika diharapkan", "now", Color(0xFF4EDCFF), "info"))
        }
        if (link.online && link.ssid == null && link.transport == "Wi‑Fi") {
            add(AlertItem("SSID tidak terbaca", "Butuh izin lokasi / Nearby Wi‑Fi di Android 13+", "now", Color(0xFFFFB020), "warning"))
        }
        if (isEmpty()) {
            add(AlertItem("Semua nominal", "Link ${link.transport} OK${gatewayMs?.let { " · GW ${it}ms" } ?: ""}", "now", Color(0xFF35E381), "info"))
        }
    }

    Page(t("screen.alerts"), Icons.Default.Notifications, nav) {
        Text(
            t("alert.live_source"),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        alerts.forEach { a ->
            AlertCard(a.title, a.detail, a.time, a.color) {
                SelectedDeviceStore.name = a.title
                SelectedDeviceStore.ip = link.gateway ?: link.ip ?: "—"
                nav.navigate("alertDetail")
            }
        }
        OutlinedButton(
            onClick = {
                link = LiveNetworkInfo.snapshot(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(t("alert.refresh")) }
    }
}

private data class AlertItem(val title: String, val detail: String, val time: String, val color: Color, val kind: String)

@Composable
fun AlertDetailScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val link = remember { LiveNetworkInfo.snapshot(context) }
    Page(t("screen.alert_detail"), Icons.Default.Notifications, nav) {
        CardBlock(SelectedDeviceStore.name) {
            Text(
                "Sumber  status lokal perangkat\n" +
                    "Transport  ${link.transport}\n" +
                    "IP lokal  ${link.ip ?: "—"}\n" +
                    "Gateway  ${link.gateway ?: "—"}\n" +
                    "SSID  ${link.ssid ?: "—"}",
                fontSize = 12.sp,
                lineHeight = 20.sp
            )
        }
        CardBlock("Tindakan disarankan") {
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
            modifier = Modifier.fillMaxWidth()
        ) { Text("BUKA DIAGNOSTIC") }
    }
}

@Composable
fun JobsScreen(nav: NavController) {
    Page(t("screen.jobs"), Icons.Default.Build, nav) {
        Text(
            t("jobs.intro"),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        val jobs = listOf(
            JobItem("WO", "Buka & kelola tiket", "Buat, ubah status, catat keluhan pelanggan", Color(0xFF4EDCFF), "tickets"),
            JobItem("SCAN", "Survey perangkat LAN", "Device Manager · scan RFC1918 berizin", Color(0xFF35E381), "devices"),
            JobItem("WIFI", "Analisis spektrum", "WiFi Analyzer · channel & RSSI", Color(0xFFB680FF), "wifi"),
            JobItem("SPEED", "Uji throughput", "Speed Test HTTPS operator", Color(0xFFFFB020), "speedtest"),
            JobItem("DIAG", "Ping / reachability", "Diagnostic ke host yang diizinkan", Color(0xFFFF5E67), "diagnostic")
        )
        jobs.forEach { j ->
            JobCard(j.tag, j.title, j.detail, j.color) {
                when (j.kind) {
                    "tickets" -> nav.navigate("tickets")
                    "devices" -> nav.navigate("devices")
                    "wifi" -> nav.navigate("wifi")
                    "speedtest" -> nav.navigate("speedtest")
                    "diagnostic" -> nav.navigate("diagnostic")
                }
            }
        }
        CardBlock(t("jobs.flow")) {
            Text(
                "1. Ambil tiket di Tickets\n" +
                    "2. Verifikasi link di Network Monitor\n" +
                    "3. Scan perangkat / WiFi sesuai lokasi\n" +
                    "4. Uji speed & catat hasil\n" +
                    "5. Tutup tiket dengan status selesai",
                fontSize = 12.sp,
                lineHeight = 20.sp
            )
        }
    }
}

private data class JobItem(val tag: String, val title: String, val detail: String, val color: Color, val kind: String)

@Composable
fun ReportsScreen(nav: NavController? = null) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var selectedReport by remember { mutableStateOf<String?>(null) }
    var period by remember { mutableStateOf("7 Hari Terakhir") }
    var generating by remember { mutableStateOf(false) }
    var generated by remember { mutableStateOf(false) }
    var reportFile by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    val networkReports = listOf(
        Triple("Network Performance", "Grafik & statistik jaringan", Icons.Default.BarChart),
        Triple("Bandwidth Usage", "Monitoring penggunaan bandwidth", Icons.Default.NetworkCheck),
        Triple("Speed Test History", "Riwayat hasil test", Icons.Default.Speed)
    )
    val deviceReports = listOf(
        Triple("Device Uptime", "Laporan uptime perangkat", Icons.Default.Router),
        Triple("Device Inventory", "Inventaris perangkat aktif", Icons.Default.Storage),
        Triple("Alert Summary", "Ringkasan alert perangkat", Icons.Default.Notifications)
    )
    val activityReports = listOf(
        Triple("Technician Activity", "Aktivitas teknisi lapangan", Icons.Default.Person),
        Triple("Incident Report", "Laporan gangguan", Icons.Default.Warning),
        Triple("Work Order Summary", "Ringkasan ticket & job", Icons.Default.ConfirmationNumber)
    )
    val currentList = when (selectedTab) {
        1 -> deviceReports
        2 -> activityReports
        else -> networkReports
    }

    Page("Reports", Icons.Default.BarChart, nav) {
        InteractiveTabStrip(listOf("Network", "Device", "Activity"), selectedTab) {
            selectedTab = it
            selectedReport = null
            generated = false
        }

        currentList.forEach { (title, subtitle, icon) ->
            ReportRow(title, subtitle, icon, selected = selectedReport == title) {
                selectedReport = title
                generated = false
                reportFile = null
            }
        }

        Text("PERIODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("24 Jam", "7 Hari Terakhir", "30 Hari", "Custom").forEach { p ->
                FilterChip(
                    selected = period == p,
                    onClick = { period = p },
                    label = { Text(p, fontSize = 10.sp) }
                )
            }
        }

        selectedReport?.let { report ->
            CardBlock("Detail: $report") {
                when {
                    report.contains("Performance", true) -> {
                        Text("Avg latency 14 ms  •  Peak RX 145 Mbps", fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { 0.72f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                        Spacer(Modifier.height(4.dp))
                        Text("Utilization 72%  •  Packet loss 0.2%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    report.contains("Uptime", true) -> {
                        Text("CCR1009  99.98%  •  AP-Office  99.2%  •  Switch-L2  100%", fontSize = 12.sp, lineHeight = 20.sp)
                    }
                    report.contains("Bandwidth", true) -> {
                        Text("Total RX  2.4 TB  •  Total TX  890 GB", fontSize = 12.sp)
                        Spacer(Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { 0.64f }, modifier = Modifier.fillMaxWidth().height(8.dp))
                    }
                    report.contains("Speed", true) -> {
                        Text("Last test  92.4 / 48.7 Mbps  •  Avg  78 / 35 Mbps", fontSize = 12.sp)
                    }
                    report.contains("Technician", true) -> {
                        Text("Jobs closed 18  •  Avg resolution 2.4h  •  Field visits 12", fontSize = 12.sp)
                    }
                    report.contains("Incident", true) -> {
                        Text("Open incidents 3  •  Critical 1  •  Resolved this period 9", fontSize = 12.sp)
                    }
                    report.contains("Work Order", true) -> {
                        Text("Created 24  •  Closed 19  •  In progress 5", fontSize = 12.sp)
                    }
                    report.contains("Inventory", true) -> {
                        Text("Routers 4  •  AP 11  •  Switches 6  •  CPE 128", fontSize = 12.sp)
                    }
                    report.contains("Alert", true) -> {
                        Text("Critical 2  •  Warning 7  •  Info 15  (periode $period)", fontSize = 12.sp)
                    }
                    else -> Text("Metrik untuk $report — periode $period", fontSize = 12.sp)
                }
            }
        }

        var reportPath by remember { mutableStateOf<String?>(null) }
        var reportBody by remember { mutableStateOf<String?>(null) }

        Button(
            onClick = {
                if (selectedReport == null) {
                    Toast.makeText(context, "Pilih jenis report dulu", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                generating = true
                generated = false
                reportPath = null
                reportBody = null
                scope.launch {
                    val report = selectedReport!!
                    val ts = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val slug = report.lowercase(Locale.US).replace(Regex("[^a-z0-9]+"), "_")
                    val fileName = "mport_${slug}_$ts.csv"
                    val content = buildString {
                        appendLine("MPorT Tech Pro — Report")
                        appendLine("Type,$report")
                        appendLine("Period,$period")
                        appendLine("Generated,${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}")
                        appendLine()
                        appendLine("Metric,Value")
                        when {
                            report.contains("Performance", true) -> {
                                appendLine("Avg Latency (ms),14")
                                appendLine("Peak RX (Mbps),145")
                                appendLine("Utilization (%),72")
                                appendLine("Packet Loss (%),0.2")
                            }
                            report.contains("Uptime", true) -> {
                                appendLine("CCR1009 (%),99.98")
                                appendLine("AP-Office (%),99.2")
                                appendLine("Switch-L2 (%),100")
                            }
                            report.contains("Bandwidth", true) -> {
                                appendLine("Total RX (TB),2.4")
                                appendLine("Total TX (GB),890")
                            }
                            report.contains("Speed", true) -> {
                                appendLine("Last Download (Mbps),92.4")
                                appendLine("Last Upload (Mbps),48.7")
                                appendLine("Avg Download (Mbps),78")
                                appendLine("Avg Upload (Mbps),35")
                            }
                            report.contains("Technician", true) -> {
                                appendLine("Jobs Closed,18")
                                appendLine("Avg Resolution (h),2.4")
                                appendLine("Field Visits,12")
                            }
                            report.contains("Incident", true) -> {
                                appendLine("Open Incidents,3")
                                appendLine("Critical,1")
                                appendLine("Resolved,9")
                            }
                            report.contains("Work Order", true) -> {
                                appendLine("Created,24")
                                appendLine("Closed,19")
                                appendLine("In Progress,5")
                            }
                            report.contains("Inventory", true) -> {
                                appendLine("Routers,4")
                                appendLine("Access Points,11")
                                appendLine("Switches,6")
                                appendLine("CPE,128")
                            }
                            report.contains("Alert", true) -> {
                                appendLine("Critical,2")
                                appendLine("Warning,7")
                                appendLine("Info,15")
                            }
                            else -> appendLine("Status,OK")
                        }
                        appendLine()
                        appendLine("Generated by MPorT Tech Pro")
                    }
                    val file = withContext(Dispatchers.IO) {
                        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
                        File(dir, fileName).also { it.writeText(content) }
                    }
                    reportFile = fileName
                    reportPath = file.absolutePath
                    reportBody = content
                    generating = false
                    generated = true
                    Toast.makeText(context, "Report disimpan: ${file.name}", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !generating,
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            if (generating) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                when {
                    generating -> "GENERATING..."
                    generated -> "GENERATE LAGI"
                    else -> "GENERATE REPORT"
                },
                fontWeight = FontWeight.Bold
            )
        }

        if (generated && reportFile != null && reportPath != null) {
            val sizeKb = remember(reportPath) {
                val f = File(reportPath!!)
                if (f.exists()) String.format(Locale.US, "%.1f KB", f.length() / 1024.0) else "—"
            }
            CardBlock("Report siap") {
                Text("File: $reportFile", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text("Path: $reportPath", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Periode: $period  •  Ukuran: $sizeKb  •  Format: CSV", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                reportBody?.let { body ->
                    Text(body.take(400) + if (body.length > 400) "…" else "", fontSize = 10.sp, lineHeight = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        val body = reportBody ?: return@OutlinedButton
                        Toast.makeText(context, "Preview (${body.lines().size} baris)", Toast.LENGTH_SHORT).show()
                    }) { Text("Preview") }
                    Button(onClick = {
                        val body = reportBody ?: return@Button
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_SUBJECT, reportFile)
                            putExtra(Intent.EXTRA_TEXT, body)
                        }
                        context.startActivity(Intent.createChooser(share, "Bag report"))
                    }) { Text("Share") }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(nav: NavController) {
    val context = LocalContext.current
    val themeModeState = LocalThemeMode.current
    // Keep switch in sync with app-wide theme without stale local state
    var darkMode by remember {
        mutableStateOf(themeModeState.value != ThemeMode.LIGHT)
    }
    // Re-sync if theme changed elsewhere
    LaunchedEffect(themeModeState.value) {
        darkMode = themeModeState.value != ThemeMode.LIGHT
    }
    var notifications by remember { mutableStateOf(true) }
    var biometrics by remember { mutableStateOf(false) }
    var displayName by remember { mutableStateOf("Budi Santoso") }
    var role by remember { mutableStateOf("Teknisi Lapangan") }
    var phone by remember { mutableStateOf("0812-3456-7890") }
    var email by remember { mutableStateOf("budi@mport.tech") }
    var showEditProfile by remember { mutableStateOf(false) }
    var showSecurity by remember { mutableStateOf(false) }
    var showServer by remember { mutableStateOf(false) }
    var serverHost by remember { mutableStateOf("api.mport.tech") }
    var serverPort by remember { mutableStateOf("443") }
    var pinEnabled by remember { mutableStateOf(true) }

    Page(t("screen.profile"), Icons.Default.Person, nav) {
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
                    Text(role, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(email, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("● Online", color = Color(0xFF35E381), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Field Unit", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        SettingsRow(t("profile.edit"), Icons.Default.Person) { showEditProfile = true }
        SwitchRow(t("common.dark_mode"), Icons.Default.DarkMode, darkMode) {
            darkMode = it
            themeModeState.value = if (it) ThemeMode.DARK else ThemeMode.LIGHT
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
        SettingsRow("MikroTik Connection", Icons.Default.Router) { nav.navigate("mikrotik") }
        SettingsRow("Server Settings", Icons.Default.Cloud) { showServer = true }
        SettingsRow(t("screen.customers"), Icons.Default.People) { nav.navigate("customers") }
        SettingsRow("Work Orders", Icons.Default.ConfirmationNumber) { nav.navigate("tickets") }
        SettingsRow(t("screen.reports"), Icons.Default.BarChart) { nav.navigate("reports") }
        SettingsRow("Backup & Restore", Icons.Default.Backup) {
            Toast.makeText(context, "Backup lokal: mport_backup_${System.currentTimeMillis()}.db (simulasi)", Toast.LENGTH_LONG).show()
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
                    OutlinedTextField(role, { role = it }, label = { Text("Jabatan") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(phone, { phone = it }, label = { Text("Telepon") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(email, { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    showEditProfile = false
                    Toast.makeText(context, "Profil diperbarui", Toast.LENGTH_SHORT).show()
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfile = false }) { Text(t("common.cancel")) }
            }
        )
    }

    if (showSecurity) {
        AlertDialog(
            onDismissRequest = { showSecurity = false },
            title = { Text("Security", fontWeight = FontWeight.Bold) },
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
                }) { Text("Simpan") }
            },
            dismissButton = {
                TextButton(onClick = { showSecurity = false }) { Text("Tutup") }
            }
        )
    }

    if (showServer) {
        AlertDialog(
            onDismissRequest = { showServer = false },
            title = { Text("Server Settings", fontWeight = FontWeight.Bold) },
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
                }) { Text("Simpan") }
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
fun MikroTikScreen(nav: NavController? = null) {
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
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (checking) t("common.loading") else t("mt.check")) }

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
            Text("MPorT Tech Pro", fontWeight = FontWeight.Bold)
            Text("Toolkit teknisi jaringan · data lokal on-device", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        CardBlock("Jaringan perangkat") {
            Text(
                "${link.transport}  ·  ${link.ip ?: "—"}\nGW ${link.gateway ?: "—"}",
                fontSize = 12.sp
            )
        }
        SettingsRow(t("screen.profile"), Icons.Default.Person) { nav?.navigate("profile") }
        SettingsRow("Pelanggan (Room)", Icons.Default.People) { nav?.navigate("customers") }
        SettingsRow("Tiket lapangan", Icons.Default.ConfirmationNumber) { nav?.navigate("tickets") }
        SettingsRow("Diagnostic", Icons.Default.NetworkCheck) { nav?.navigate("diagnostic") }
        SettingsRow("WiFi Analyzer", Icons.Default.Wifi) { nav?.navigate("wifi") }
        SettingsRow("About", Icons.Default.Info) { nav?.navigate("about") }
        CardBlock(t("settings.privacy")) {
            Text(
t("settings.privacy_body"),
                fontSize = 12.sp,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun Page(
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
                            text = { Text("Refresh") },
                            onClick = {
                                menuOpen = false
                                // no-op visual refresh
                            },
                            leadingIcon = { Icon(Icons.Default.Refresh, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Share page") },
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
private fun InteractiveTabStrip(items: List<String>, selected: Int, onSelect: (Int) -> Unit) {
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
private fun RouterHero() {
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
private fun CardBlock(title: String, content: @Composable ColumnScope.() -> Unit) {
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
private fun MetricBox(label: String, value: String, color: Color, modifier: Modifier) {
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
private fun ToolTile(tile: Tile, onClick: () -> Unit) {
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
private fun WifiRow(
    name: String,
    detail: String,
    signal: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Wifi, null, tint = Color(0xFF35E381))
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(name.removePrefix("★ ").trim(), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Text(detail, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(signal, fontSize = 10.sp)
        }
    }
}

@Composable
private fun DeviceRow(name: String, ip: String, online: Boolean) {
    val color = if (online) Color(0xFF35E381) else Color(0xFFFF5E67)
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
private fun DeviceCard(name: String, ip: String, online: Boolean, onClick: () -> Unit) {
    val color = if (online) Color(0xFF35E381) else Color(0xFFFF5E67)
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
private fun InterfaceList() {
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
private fun AlertCard(title: String, detail: String, time: String, color: Color, onClick: () -> Unit) {
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
private fun JobCard(tag: String, title: String, detail: String, color: Color, onClick: () -> Unit = {}) {
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
private fun ReportRow(
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
private fun HistoryRow(title: String, detail: String) {
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
private fun SettingsRow(title: String, icon: ImageVector, onClick: () -> Unit) {
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
private fun SwitchRow(title: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit = {}) {
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

private fun formatIp(ip: Int): String {
    return "${ip and 0xff}.${ip shr 8 and 0xff}.${ip shr 16 and 0xff}.${ip shr 24 and 0xff}"
}

private fun openWifiConnectDialog(context: android.content.Context, ssid: String, security: String = "") {
    val isOpen = security.equals("Open", ignoreCase = true)
    // Native system sheet: Password + Advanced options + CANCEL / CONNECT
    com.mporttech.pro.features.wifi.WifiConnector.connect(context, ssid, isOpen = isOpen)
}


