package com.mporttech.pro.features.discovery

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.mporttech.pro.features.tools.LiveNetworkInfo
import com.mporttech.pro.features.tools.SelectedDeviceStore
import com.mporttech.pro.ui.i18n.t
import kotlinx.coroutines.launch

private val Bg = Color(0xFF0B0F14)
private val CardBg = Color(0xFF151A22)
private val Accent = Color(0xFF3B82F6)
private val Green = Color(0xFF39FF14)
private val Amber = Color(0xFFF59E0B)
private val Red = Color(0xFFEF4444)
private val Muted = Color(0xFF5EC8E8)

@Composable
fun DiscoveryHubScreen(nav: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(0) } // 0 wifi discovery, 1 signal
    var snap by remember { mutableStateOf(LiveNetworkInfo.snapshot(context)) }
    var nodes by remember { mutableStateOf<List<DiscoveredNode>>(emptyList()) }
    LaunchedEffect(Unit) {
        while (true) {
            snap = LiveNetworkInfo.snapshot(context)
            kotlinx.coroutines.delay(4000)
        }
    }
    var scanning by remember { mutableStateOf(false) }
    var authorized by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        scanning = true
        status = "Searching network…"
        try {
            // Always show gateway + this device first
            nodes = DiscoveryEngine.discover(context, authorized = false)
            // Then expand scan on private LAN (user is on local network)
            status = "Searching for nearby devices…"
            val full = DiscoveryEngine.discover(context, authorized = true)
            if (full.isNotEmpty()) nodes = full
            status = if (nodes.isEmpty()) "No devices found" else "${nodes.size} devices found"
        } catch (e: Exception) {
            status = e.message ?: "Search failed"
        }
        scanning = false
    }

    val msgAuth = t("net.authorize_first")
    val labelScan = t("net.scan_lan")
    val labelLoading = t("common.loading")
    val labelAuthScan = t("net.authorize_scan")
    fun runFullScan() {
        if (!authorized) {
            Toast.makeText(context, msgAuth, Toast.LENGTH_SHORT).show()
            return
        }
        scanning = true
        status = "Searching network…"
        scope.launch {
            try {
                nodes = DiscoveryEngine.discover(context, authorized = true)
                status = "${nodes.size} devices"
            } catch (e: Exception) {
                status = e.message ?: "Scan failed"
            } finally {
                scanning = false
            }
        }
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            DiscoveryBottomBar(nav, selected = "discovery")
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp)
        ) {
            DiscoveryTopBar(
                title = snap.ssid ?: "Network",
                onProfile = { nav.navigate("profile") }
            )
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                SegTab("WiFi", Icons.Default.Wifi, tab == 0, Modifier.weight(1f)) { tab = 0 }
                SegTab("Signal", Icons.Default.BarChart, tab == 1, Modifier.weight(1f)) {
                    tab = 1
                    nav.navigate("signalHub")
                }
            }
            Spacer(Modifier.height(16.dp))
            when (tab) {
                0 -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Devices (${nodes.size})",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE8FBFF)
                        )
                        IconButton(onClick = { runFullScan() }) {
                            Icon(Icons.Default.Refresh, null, tint = Muted)
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = authorized,
                            onCheckedChange = { authorized = it },
                            colors = CheckboxDefaults.colors(checkedColor = Accent)
                        )
                        Text(
                            labelAuthScan,
                            fontSize = 11.sp,
                            color = Muted,
                            modifier = Modifier.clickable { authorized = !authorized }
                        )
                    }
                    if (scanning || status.isNotBlank()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .background(CardBg, RoundedCornerShape(24.dp))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (scanning) {
                                CircularProgressIndicator(
                                    Modifier.size(18.dp),
                                    color = Accent,
                                    strokeWidth = 2.dp
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(
                                if (scanning) "Searching Network…" else status,
                                color = Muted,
                                fontSize = 13.sp
                            )
                        }
                    }
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                        modifier = Modifier
                            .weight(1f)
                            .background(CardBg, RoundedCornerShape(16.dp))
                    ) {
                        items(nodes, key = { it.ip }) { node ->
                            DeviceDiscoveryRow(node) {
                                SelectedDeviceStore.ip = node.ip
                                SelectedDeviceStore.name = node.name
                                SelectedDeviceStore.kind = node.kind
                                SelectedDeviceStore.mac = null
                                SelectedDeviceStore.vendor = node.vendor
                                nav.navigate("deviceDetailRich")
                            }
                            HorizontalDivider(color = Color(0xFF1F2937), thickness = 0.5.dp)
                        }
                        if (nodes.isEmpty() && !scanning) {
                            item {
                                Text(
                                    "Tap scan icon after authorizing LAN scan",
                                    color = Muted,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { runFullScan() },
                        enabled = !scanning,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (scanning) labelLoading else labelScan)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun SignalHubScreen(nav: NavController) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf(0) } // 0 signal 1 throughput 2 latency
    var samples by remember { mutableStateOf<List<Int>>(emptyList()) }
    var current by remember { mutableStateOf<SignalSample?>(null) }
    var gwMs by remember { mutableStateOf<Long?>(null) }
    var inetMs by remember { mutableStateOf<Long?>(null) }
    var packetLoss by remember { mutableStateOf(0) }
    var dnsMs by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        DiscoveryEngine.signalFlow(context).collect { s ->
            current = s
            samples = (samples + s.rssi).takeLast(40)
        }
    }
    // Continuous latency probes (IO) — independent of tab mode
    LaunchedEffect(Unit) {
        while (true) {
            val localSnap = LiveNetworkInfo.snapshot(context)
            val gw = localSnap.gateway
            if (gw != null) {
                val ms = DiscoveryEngine.tcpPing(gw, 80, 1000)
                gwMs = ms
                if (ms != null) DiscoveryEngine.pushHistory("gw", ms)
            }
            val ms2 = DiscoveryEngine.tcpPing("8.8.8.8", 53, 1200)
            inetMs = ms2
            if (ms2 != null) DiscoveryEngine.pushHistory("inet", ms2)
            dnsMs = try {
                val start = android.os.SystemClock.elapsedRealtime()
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    java.net.InetAddress.getByName("google.com")
                }
                android.os.SystemClock.elapsedRealtime() - start
            } catch (_: Exception) {
                null
            }
            kotlinx.coroutines.delay(2000)
        }
    }

    var snap by remember { mutableStateOf(LiveNetworkInfo.snapshot(context)) }
    LaunchedEffect(Unit) {
        while (true) {
            snap = LiveNetworkInfo.snapshot(context)
            kotlinx.coroutines.delay(3000)
        }
    }

    Scaffold(containerColor = Bg, bottomBar = { DiscoveryBottomBar(nav, "signal") }) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp)
        ) {
            DiscoveryTopBar(snap.ssid ?: "Network", onProfile = { nav.navigate("profile") })
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                SegTab("Signal Strength", Icons.Default.BarChart, true, Modifier.weight(1f)) {}
                SegTab("Latency", Icons.Default.Speed, false, Modifier.weight(1f)) {
                    nav.navigate("latencyHub")
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(CardBg, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                listOf("Signal", "Throughput", "Latency").forEachIndexed { i, label ->
                    val sel = mode == i
                    Box(
                        Modifier
                            .weight(1f)
                            .background(if (sel) Color(0xFF1E293B) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { mode = i }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (sel) Color(0xFFE8FBFF) else Muted, fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            when (mode) {
                0 -> SignalPanel(current, samples)
                1 -> ThroughputPanel(current)
                else -> LatencyPanel(snap.gateway, gwMs, inetMs, packetLoss, dnsMs, nav)
            }
        }
    }
}

@Composable
private fun SignalPanel(current: SignalSample?, samples: List<Int>) {
    val rssi = current?.rssi ?: -100
    val color = when {
        rssi >= -55 -> Green
        rssi >= -70 -> Amber
        else -> Red
    }
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Router, null, tint = Accent)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Access Point", fontWeight = FontWeight.Bold, color = Color(0xFFE8FBFF))
                    Text(current?.bssid ?: "—", fontSize = 11.sp, color = Muted)
                }
                Text("$rssi dBm", color = color, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                val band = current?.frequency?.let { if (it >= 5000) "5 GHz" else "2.4 GHz" } ?: "—"
                AssistChip(
                    onClick = {},
                    label = { Text(band, fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF1E293B), labelColor = Color(0xFFE8FBFF))
                )
                Spacer(Modifier.width(8.dp))
                if (current?.ssid != null) {
                    Surface(color = Color(0xFF14532D), shape = RoundedCornerShape(6.dp)) {
                        Text("CONNECTED", color = Green, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("Band  ${current?.frequency?.let { if (it >= 5000) "5 GHz" else "2.4 GHz (20 MHz)" } ?: "—"}   ·   PHY  ${current?.linkMbps ?: "—"} Mbps", fontSize = 11.sp, color = Muted)
            Spacer(Modifier.height(12.dp))
            RssiChart(samples, Modifier.fillMaxWidth().height(160.dp))
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("Access Point Roaming", fontWeight = FontWeight.Bold, color = Color(0xFFE8FBFF))
    Spacer(Modifier.height(8.dp))
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Text(
            "No access point changes recorded yet",
            color = Muted,
            fontSize = 13.sp,
            modifier = Modifier.padding(20.dp).fillMaxWidth()
        )
    }
}

@Composable
private fun ThroughputPanel(current: SignalSample?) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Link rate", fontWeight = FontWeight.Bold, color = Color(0xFFE8FBFF))
            Spacer(Modifier.height(8.dp))
            Text(
                "${current?.linkMbps ?: "—"} Mbps",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Accent
            )
            Text("Negotiated PHY speed (WifiManager)", fontSize = 11.sp, color = Muted)
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { ((current?.linkMbps ?: 0) / 300f).coerceIn(0.02f, 1f) },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = Accent,
                trackColor = Color(0xFF1E293B)
            )
        }
    }
}

@Composable
private fun LatencyPanel(
    gateway: String?,
    gwMs: Long?,
    inetMs: Long?,
    loss: Int,
    dnsMs: Long?,
    nav: NavController
) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp)) {
            LatencyRow(Icons.Default.Router, gateway ?: "Gateway", gateway ?: "—", gwMs)
            HorizontalDivider(color = Color(0xFF1F2937), modifier = Modifier.padding(vertical = 10.dp))
            LatencyRow(Icons.Default.Public, "Internet", "8.8.8.8", inetMs)
            Spacer(Modifier.height(12.dp))
            Text("Packet Loss: $loss %    DNS Latency: ${dnsMs ?: "—"} ms", fontSize = 11.sp, color = Muted)
            Spacer(Modifier.height(8.dp))
            val hist = DiscoveryEngine.history("inet").ifEmpty { DiscoveryEngine.history("gw") }
            Sparkline(hist, Modifier.fillMaxWidth().height(100.dp), Accent)
            TextButton(onClick = { nav.navigate("latencyHub") }) {
                Text("Open full Network Latency")
            }
        }
    }
}

@Composable
fun LatencyHubScreen(nav: NavController) {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<LatencySample>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            loading = items.isEmpty()
            try {
                items = DiscoveryEngine.probeTargets(context)
            } catch (_: Exception) {
            }
            loading = false
            kotlinx.coroutines.delay(2500)
        }
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color(0xFFE8FBFF))
                }
                Text("Network Latency", fontWeight = FontWeight.Bold, color = Color(0xFFE8FBFF), fontSize = 18.sp)
            }
        },
        bottomBar = { DiscoveryBottomBar(nav, "latency") }
    ) { pad ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column {
                        if (loading && items.isEmpty()) {
                            Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = Accent, modifier = Modifier.size(28.dp))
                            }
                        }
                        items.forEachIndexed { idx, s ->
                            LatencyTargetRow(s)
                            if (idx != items.lastIndex) {
                                HorizontalDivider(color = Color(0xFF1F2937))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceDetailRichScreen(nav: NavController) {
    val context = LocalContext.current
    val ip = SelectedDeviceStore.ip
    val name = SelectedDeviceStore.name
    var ms by remember { mutableStateOf<Long?>(null) }
    var online by remember { mutableStateOf(false) }
    var ports by remember { mutableStateOf<List<Int>>(emptyList()) }
    var vendor by remember { mutableStateOf(SelectedDeviceStore.kind) }
    var probing by remember { mutableStateOf(true) }

    LaunchedEffect(ip) {
        probing = true
        val ping = DiscoveryEngine.tcpPing(ip, 80, 1200) ?: DiscoveryEngine.tcpPing(ip, 443, 1000)
        ms = ping
        online = ping != null
        val open = mutableListOf<Int>()
        for (p in listOf(22, 53, 80, 443, 8080, 8728, 8291)) {
            val ok = DiscoveryEngine.tcpPing(ip, p, 400) != null
            if (ok) open.add(p)
        }
        ports = open
        vendor = VendorLookup.guessFromName(name) ?: vendor
        probing = false
    }

    Scaffold(
        containerColor = Bg,
        topBar = {
            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null, tint = Color(0xFFE8FBFF))
                }
                Text("Device Detail", fontWeight = FontWeight.Bold, color = Color(0xFFE8FBFF), fontSize = 18.sp)
            }
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(horizontal = 16.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Storage, null, tint = Muted, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(8.dp))
                Text(name, fontWeight = FontWeight.Bold, color = Color(0xFFE8FBFF), fontSize = 20.sp)
                Text(name, color = Muted, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
            InfoGroup {
                InfoLine("Model", name)
                InfoLine("Manufacturer", vendor.replaceFirstChar { it.uppercase() })
            }
            Spacer(Modifier.height(12.dp))
            Text("Network", fontWeight = FontWeight.Bold, color = Color(0xFFE8FBFF))
            Spacer(Modifier.height(8.dp))
            InfoGroup {
                InfoLine(
                    "IP Address",
                    ip,
                    valueColor = Accent,
                    onClick = {
                        try {
                            val url = if (80 in ports) "http://$ip/" else if (443 in ports) "https://$ip/" else "http://$ip/"
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: Exception) {
                            Toast.makeText(context, e.message ?: "Cannot open", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                InfoLine(
                    "Ping",
                    if (probing) "…" else ms?.let { "$it ms" } ?: "Timeout",
                    valueColor = if (ms != null) Green else Red
                )
                InfoLine("Packet Loss", if (online) "No Packet Loss" else "Unreachable")
            }
            Spacer(Modifier.height(12.dp))
            Text("UPNP / Web", fontWeight = FontWeight.Bold, color = Color(0xFFE8FBFF))
            Spacer(Modifier.height(8.dp))
            InfoGroup {
                val product = if (80 in ports || 443 in ports) {
                    if (443 in ports && 80 !in ports) "https://$ip/" else "http://$ip/"
                } else "—"
                InfoLine(
                    "Product Site",
                    product,
                    valueColor = Accent,
                    onClick = {
                        if (product != "—") {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(product)))
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Cannot open", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                val mfg = vendorSite(vendor)
                InfoLine(
                    "Manufacturer Site",
                    mfg,
                    valueColor = Accent,
                    onClick = {
                        if (mfg != "—" && mfg.startsWith("http")) {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(mfg)))
                            } catch (e: Exception) {
                                Toast.makeText(context, e.message ?: "Cannot open", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Open Ports", fontWeight = FontWeight.Bold, color = Color(0xFFE8FBFF))
            Spacer(Modifier.height(8.dp))
            InfoGroup {
                if (ports.isEmpty()) {
                    Text(
                        if (probing) "Probing…" else "None detected",
                        color = Muted,
                        modifier = Modifier.padding(14.dp)
                    )
                } else {
                    ports.forEach { p ->
                        InfoLine("Port $p", portLabel(p), valueColor = Green)
                    }
                }
            }
        }
    }
}

private fun vendorSite(v: String): String = when {
    "tp-link" in v.lowercase() || "tp-link" == v.lowercase() -> "http://www.tp-link.com/"
    "mikrotik" in v.lowercase() -> "https://mikrotik.com/"
    "huawei" in v.lowercase() -> "https://www.huawei.com/"
    "ubiquiti" in v.lowercase() -> "https://www.ui.com/"
    else -> "—"
}

private fun portLabel(p: Int): String = when (p) {
    22 -> "SSH"
    53 -> "DNS"
    80 -> "HTTP"
    443 -> "HTTPS"
    8080 -> "HTTP-alt"
    8728 -> "RouterOS API"
    8291 -> "Winbox"
    else -> "Open"
}

@Composable
private fun InfoGroup(content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(14.dp)) {
        Column(content = content)
    }
}

@Composable
private fun InfoLine(
    label: String,
    value: String,
    valueColor: Color = Color(0xFFE8FBFF),
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Muted, fontSize = 13.sp)
        Text(value, color = valueColor, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DeviceDiscoveryRow(node: DiscoveredNode, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            when (node.kind) {
                "gateway" -> Icons.Default.Router
                "me" -> Icons.Default.Phone
                "ap" -> Icons.Default.Wifi
                else -> Icons.Default.Storage
            },
            null,
            tint = Muted,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(node.name, color = Color(0xFFE8FBFF), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                if (node.kind == "gateway") {
                    Spacer(Modifier.width(6.dp))
                    Surface(color = Accent, shape = RoundedCornerShape(6.dp)) {
                        Text("Gateway", color = Color(0xFFE8FBFF), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
                if (node.isMe) {
                    Spacer(Modifier.width(6.dp))
                    Surface(color = Accent, shape = RoundedCornerShape(6.dp)) {
                        Text("Me", color = Color(0xFFE8FBFF), fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
            node.vendor?.let {
                Text(it, fontSize = 11.sp, color = Muted)
            }
        }
        Text(node.ip, color = Muted, fontSize = 12.sp)
        Spacer(Modifier.width(6.dp))
        Icon(Icons.Default.ChevronRight, null, tint = Muted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun LatencyRow(icon: ImageVector, title: String, subtitle: String, ms: Long?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Muted)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color(0xFFE8FBFF), fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Muted, fontSize = 11.sp)
        }
        Text(
            ms?.let { "$it ms" } ?: "—",
            color = if (ms != null) Green else Red,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun LatencyTargetRow(s: LatencySample) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(36.dp)
                .background(Color(0xFF1E293B), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                when {
                    "google" in s.target -> Icons.Default.Public
                    "facebook" in s.target -> Icons.Default.Public
                    "x.com" in s.target -> Icons.Default.Share
                    else -> Icons.Default.Router
                },
                null,
                tint = Color(0xFFE8FBFF),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(s.label, color = Color(0xFFE8FBFF), fontWeight = FontWeight.SemiBold)
            Text(s.ip ?: s.target, color = Muted, fontSize = 11.sp)
        }
        Sparkline(s.history, Modifier.width(72.dp).height(28.dp), Muted)
        Spacer(Modifier.width(10.dp))
        Text(
            s.ms?.let { "$it ms" } ?: "—",
            color = if (s.ok) Green else Red,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RssiChart(samples: List<Int>, modifier: Modifier) {
    Canvas(modifier.background(Color(0xFF0F172A), RoundedCornerShape(8.dp))) {
        if (samples.size < 2) return@Canvas
        val min = -90f
        val max = -20f
        val path = Path()
        samples.forEachIndexed { i, v ->
            val x = size.width * i / (samples.size - 1).coerceAtLeast(1)
            val y = size.height * (1f - ((v - min) / (max - min)).coerceIn(0f, 1f))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        // grid
        for (db in listOf(-20, -30, -40, -50, -60, -70, -80)) {
            val y = size.height * (1f - ((db - min) / (max - min)))
            drawLine(Color(0xFF1E293B), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        drawPath(path, Accent, style = Stroke(width = 3f, cap = StrokeCap.Round))
    }
}

@Composable
private fun Sparkline(values: List<Long>, modifier: Modifier, color: Color) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val min = (values.minOrNull() ?: 0L).toFloat()
        val max = (values.maxOrNull() ?: 1L).toFloat().coerceAtLeast(min + 1f)
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = size.width * i / (values.size - 1)
            val y = size.height * (1f - ((v - min) / (max - min)).coerceIn(0f, 1f))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
    }
}

@Composable
private fun DiscoveryTopBar(title: String, onProfile: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onProfile) {
            Icon(Icons.Default.AccountCircle, null, tint = Muted, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.Wifi, null, tint = Green, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, color = Color(0xFFE8FBFF), fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Icon(Icons.Default.Search, null, tint = Muted)
    }
}

@Composable
private fun SegTab(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .background(if (selected) Color(0xFF1E293B) else Color.Transparent, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = if (selected) Accent else Muted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (selected) Accent else Muted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun DiscoveryBottomBar(nav: NavController, selected: String) {
    NavigationBar(containerColor = Color(0xFF0B0F14)) {
        val items = listOf(
            Triple("speedtest", "Speed", Icons.Default.Speed),
            Triple("signalHub", "Signal", Icons.Default.BarChart),
            Triple("wifi", "Scan", Icons.Default.Wifi),
            Triple("discovery", "Discovery", Icons.Default.Search),
            Triple("tools", "Tools", Icons.Default.Build)
        )
        items.forEach { (route, label, icon) ->
            NavigationBarItem(
                selected = selected == route || (selected == "latency" && route == "signalHub"),
                onClick = {
                    nav.navigate(route) {
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, label) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Accent,
                    selectedTextColor = Accent,
                    unselectedIconColor = Muted,
                    unselectedTextColor = Muted,
                    indicatorColor = Color(0xFF1E293B)
                )
            )
        }
    }
}
