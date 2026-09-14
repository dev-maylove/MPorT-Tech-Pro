package com.mporttech.pro.features.networktools

import com.mporttech.pro.ui.i18n.t

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

// ─────────────────────────────────────────────────────────────
// Shared premium chrome
// ─────────────────────────────────────────────────────────────

private val AccentBlue = Color(0xFF00F0FF)
private val AccentCyan = Color(0xFF00F0FF)
private val SuccessGreen = Color(0xFF39FF14)
private val ErrorRed = Color(0xFFFF2E63)
private val WarningAmber = Color(0xFFFFD60A)
private val CardBg = Color(0xFF0B1220)
private val SurfaceElevated = Color(0xFF111C2E)
private val BorderSubtle = Color(0xFF1A2A42)

@Composable
private fun ToolScaffold(
    title: String,
    subtitle: String,
    icon: ImageVector,
    nav: NavController?,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Premium header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0A1628), Color(0xFF050914))
                    )
                )
                .padding(top = 8.dp, bottom = 16.dp)
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (nav != null) {
                        IconButton(
                            onClick = {
                                if (nav.previousBackStackEntry != null) nav.popBackStack()
                                else nav.navigate("tools") { launchSingleTop = true }
                            }
                        ) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFFB8DFFF))
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0D2A45),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(icon, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE8FBFF)
                        )
                        Text(
                            subtitle,
                            fontSize = 11.sp,
                            color = Color(0xFF9EE8FF)
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
private fun PremiumCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            Modifier
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun StatPill(label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        Text(label, color = Color(0xFF5EC8E8), fontSize = 10.sp)
    }
}

@Composable
private fun PresetChips(presets: List<String>, onSelect: (String) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        presets.forEach { p ->
            AssistChip(
                onClick = { onSelect(p) },
                label = { Text(p, fontSize = 10.sp) },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = SurfaceElevated,
                    labelColor = Color(0xFFB8DFFF)
                )
            )
        }
    }
}

@Composable
private fun ActionButton(
    text: String,
    loading: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color(0xFF000000)
            )
            Spacer(Modifier.width(10.dp))
            Text(t("nt.running"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF000000))
        } else {
            Text(text, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF000000))
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PING TOOL
// ─────────────────────────────────────────────────────────────

data class PingSample(
    val seq: Int,
    val success: Boolean,
    val latencyMs: Long?,
    val message: String
)

data class PingStats(
    val sent: Int = 0,
    val received: Int = 0,
    val lost: Int = 0,
    val minMs: Long? = null,
    val avgMs: Double? = null,
    val maxMs: Long? = null,
    val lossPct: Double = 0.0
)

@Composable
fun PingToolScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var host by rememberSaveable { mutableStateOf("8.8.8.8") }
    var count by rememberSaveable { mutableStateOf(4) }
    var running by remember { mutableStateOf(false) }
    val runFlag = remember { AtomicBoolean(false) }
    var samples by remember { mutableStateOf<List<PingSample>>(emptyList()) }
    var stats by remember { mutableStateOf(PingStats()) }
    var lastRunAt by remember { mutableStateOf<String?>(null) }

    val presets = listOf("8.8.8.8", "1.1.1.1", "google.com", "192.168.1.1")

    fun computeStats(list: List<PingSample>): PingStats {
        // Single-pass aggregation — O(n)
        var sent = 0
        var received = 0
        var minMs: Long? = null
        var maxMs: Long? = null
        var sum = 0.0
        for (s in list) {
            sent++
            val lat = s.latencyMs
            if (s.success && lat != null) {
                received++
                sum += lat
                if (minMs == null || lat < minMs) minMs = lat
                if (maxMs == null || lat > maxMs) maxMs = lat
            }
        }
        val lost = sent - received
        return PingStats(
            sent = sent,
            received = received,
            lost = lost,
            minMs = minMs,
            avgMs = if (received > 0) sum / received else null,
            maxMs = maxMs,
            lossPct = if (sent > 0) (lost * 100.0 / sent) else 0.0
        )
    }

    ToolScaffold(
        title = "Ping Tool",
        subtitle = "Latency & packet loss diagnostics",
        icon = Icons.Default.NetworkCheck,
        nav = nav
    ) {
        PremiumCard {
            Text(t("nt.target_host"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
            OutlinedTextField(
                value = host,
                onValueChange = { host = it.trim() },
                label = { Text(t("nt.ip_hostname")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedLabelColor = AccentBlue,
                    cursorColor = AccentBlue
                )
            )
            PresetChips(presets) { host = it }

            Text(t("nt.packet_count"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(4, 8, 16).forEach { n ->
                    FilterChip(
                        selected = count == n,
                        onClick = { count = n },
                        label = { Text("$n", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = Color(0xFF001A2B)
                        )
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (running) return@Button
                        if (host.isBlank()) {
                            Toast.makeText(context, com.mporttech.pro.ui.i18n.Str.get("nt.enter_host", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)), Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        running = true
                        runFlag.set(true)
                        samples = emptyList()
                        stats = PingStats()
                        scope.launch {
                            var seq = 0
                            val streamed = mutableListOf<PingSample>()
                            // Continuous ping until STOP
                            while (runFlag.get()) {
                                seq++
                                val summary = try {
                                    NetworkOutputParser.ping(host, count = 1, timeoutSec = 2)
                                } catch (_: Exception) {
                                    null
                                }
                                val s = summary?.samples?.firstOrNull()
                                val sample = if (s != null) {
                                    PingSample(
                                        seq = seq,
                                        success = s.success,
                                        latencyMs = s.timeMs?.toLong(),
                                        message = s.raw.ifBlank {
                                            if (s.success) "seq=$seq time=${s.timeMs} ms" else "seq=$seq timeout"
                                        }
                                    )
                                } else {
                                    PingSample(seq, false, null, "seq=$seq no response")
                                }
                                streamed.add(sample)
                                // keep last 40 lines visible
                                if (streamed.size > 40) streamed.removeAt(0)
                                samples = streamed.toList()
                                stats = computeStats(streamed)
                                lastRunAt = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) +
                                    " • continuous"
                                delay(800)
                            }
                        }
                    },
                    enabled = host.isNotBlank() && !running,
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF00E5FF),
            contentColor = Color(0xFF000000),
            disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
            disabledContentColor = Color(0xFF000000)
        )
                ) {
                    Text(if (running) t("nt.running") else t("nt.start_ping"), fontWeight = FontWeight.Bold, color = Color(0xFF000000), fontSize = 15.sp)
                }
                OutlinedButton(
                    onClick = { runFlag.set(false); running = false },
                    enabled = running,
                    modifier = Modifier.height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) { Text(t("nt.stop")) }
            }
        }

        // Live stats — show immediately while running or when samples exist
        AnimatedVisibility(visible = running || samples.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
            PremiumCard {
                Text(t("nt.statistics"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill("Sent", "${stats.sent}", AccentCyan)
                    StatPill("Recv", "${stats.received}", SuccessGreen)
                    StatPill("Lost", "${stats.lost}", if (stats.lost > 0) ErrorRed else SuccessGreen)
                    StatPill("Loss", String.format(Locale.US, "%.0f%%", stats.lossPct), if (stats.lossPct > 0) WarningAmber else SuccessGreen)
                }
                if (stats.avgMs != null) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatPill("Min", "${stats.minMs} ms", AccentBlue)
                        StatPill("Avg", String.format(Locale.US, "%.1f ms", stats.avgMs), AccentCyan)
                        StatPill("Max", "${stats.maxMs} ms", WarningAmber)
                    }
                }
                lastRunAt?.let {
                    Text("${t("nt.last_run")} • $it", fontSize = 10.sp, color = Color(0xFF6A829E))
                }
            }
        }

        // Packet log
        if (samples.isNotEmpty()) {
            PremiumCard {
                Text(t("nt.packet_log"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
                samples.forEach { s ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceElevated)
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (s.success) SuccessGreen else ErrorRed)
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "seq=${s.seq}  ${s.message}",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = if (s.success) Color(0xFFD0E6FF) else Color(0xFFFFB0B6),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        PremiumCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Parser ICMP native (jika tersedia) → fallback TCP probe. Statistik dihitung single-pass O(n).",
                    fontSize = 11.sp,
                    color = Color(0xFF5EC8E8)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// TRACEROUTE
// ─────────────────────────────────────────────────────────────

data class TraceHop(
    val hop: Int,
    val host: String?,
    val ip: String?,
    val latencyMs: Long?,
    val status: String // "ok" | "timeout" | "error"
)

@Composable
fun TracerouteScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var host by rememberSaveable { mutableStateOf("google.com") }
    var maxHops by rememberSaveable { mutableStateOf(12) }
    var running by remember { mutableStateOf(false) }
    var hops by remember { mutableStateOf<List<TraceHop>>(emptyList()) }
    var resolvedIp by remember { mutableStateOf<String?>(null) }
    var summary by remember { mutableStateOf<String?>(null) }

    val presets = listOf("google.com", "cloudflare.com", "1.1.1.1", "8.8.8.8")

    // Path discovery delegated to NetworkOutputParser.traceroute

    ToolScaffold(
        title = t("nt.trace_title"),
        subtitle = t("nt.trace_sub"),
        icon = Icons.Default.Timeline,
        nav = nav
    ) {
        PremiumCard {
            Text(t("nt.destination"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
            OutlinedTextField(
                value = host,
                onValueChange = { host = it.trim() },
                label = { Text(t("nt.hostname_ip")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedLabelColor = AccentBlue,
                    cursorColor = AccentBlue
                )
            )
            PresetChips(presets) { host = it }

            Text(t("nt.max_hops"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(8, 12, 20).forEach { n ->
                    FilterChip(
                        selected = maxHops == n,
                        onClick = { maxHops = n },
                        label = { Text("$n", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = Color(0xFF001A2B)
                        )
                    )
                }
            }

            ActionButton(
                text = "TRACE ROUTE",
                loading = running,
                enabled = host.isNotBlank()
            ) {
                if (host.isBlank()) {
                    Toast.makeText(context, com.mporttech.pro.ui.i18n.Str.get("nt.enter_dest", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)), Toast.LENGTH_SHORT).show()
                    return@ActionButton
                }
                running = true
                hops = emptyList()
                resolvedIp = null
                summary = null
                scope.launch {
                    val result = NetworkOutputParser.traceroute(host, maxHops)
                    resolvedIp = result.resolvedIp
                    if (result.error != null) {
                        summary = result.error
                    }
                    val list = result.hops.map { h ->
                        TraceHop(
                            hop = h.hop,
                            host = h.host,
                            ip = h.ip,
                            latencyMs = h.latencyMs?.toLong(),
                            status = when {
                                h.timedOut -> "timeout"
                                h.latencyMs != null -> "ok"
                                else -> "error"
                            }
                        )
                    }
                    val streamed = mutableListOf<TraceHop>()
                    for (h in list) {
                        streamed.add(h)
                        hops = streamed.toList()
                        delay(80)
                    }
                    val okHops = list.count { it.status == "ok" }
                    summary = "Completed • $okHops/${list.size} hops reachable" +
                        (resolvedIp?.let { " • dest $it" } ?: "")
                    running = false
                }
            }
        }

        resolvedIp?.let { ip ->
            PremiumCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, null, tint = AccentCyan, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(t("nt.resolved"), fontSize = 10.sp, color = Color(0xFF9EE8FF))
                        Text(ip, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE8FBFF), fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        if (hops.isNotEmpty()) {
            PremiumCard {
                Text(t("nt.hop_table"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
                hops.forEach { hop ->
                    val statusColor = when (hop.status) {
                        "ok" -> SuccessGreen
                        "timeout" -> WarningAmber
                        else -> ErrorRed
                    }
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceElevated)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = statusColor.copy(alpha = 0.2f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "${hop.hop}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = statusColor
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                hop.host ?: "*",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = Color(0xFFE8FBFF)
                            )
                            Text(
                                hop.ip ?: t("nt.no_reply"),
                                fontSize = 10.sp,
                                color = Color(0xFF5EC8E8),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Text(
                            hop.latencyMs?.let { "${it} ms" } ?: "—",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = statusColor
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                summary?.let {
                    Spacer(Modifier.height(4.dp))
                    Text(it, fontSize = 11.sp, color = AccentCyan)
                }
            }
        }

        PremiumCard {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Traceroute penuh membutuhkan raw ICMP (root). Versi ini menampilkan path discovery profesional dengan hop akhir diukur secara nyata ke destinasi.",
                    fontSize = 11.sp,
                    color = Color(0xFF5EC8E8)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// DNS LOOKUP
// ─────────────────────────────────────────────────────────────

data class DnsRecord(
    val type: String,
    val value: String,
    val note: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsLookupScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("google.com") }
    var running by remember { mutableStateOf(false) }
    var records by remember { mutableStateOf<List<DnsRecord>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var resolveTimeMs by remember { mutableStateOf<Long?>(null) }
    var canonical by remember { mutableStateOf<String?>(null) }

    val presets = listOf("google.com", "cloudflare.com", "github.com", "1.1.1.1")

    // Uses NetworkOutputParser.dnsLookup — single resolve + typed records

    ToolScaffold(
        title = t("nt.dns_title"),
        subtitle = t("nt.dns_sub"),
        icon = Icons.Default.Search,
        nav = nav
    ) {
        PremiumCard {
            Text(t("nt.query"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
            OutlinedTextField(
                value = query,
                onValueChange = { query = it.trim() },
                label = { Text(t("nt.domain_or_ip")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedLabelColor = AccentBlue,
                    cursorColor = AccentBlue
                )
            )
            var presetExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(
                expanded = presetExpanded,
                onExpandedChange = { presetExpanded = it }
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(t("nt.preset_domain")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = presetExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedLabelColor = AccentBlue
                    )
                )
                ExposedDropdownMenu(
                    expanded = presetExpanded,
                    onDismissRequest = { presetExpanded = false }
                ) {
                    presets.forEach { p ->
                        DropdownMenuItem(
                            text = { Text(p) },
                            onClick = {
                                query = p
                                presetExpanded = false
                            }
                        )
                    }
                }
            }

            ActionButton(
                text = "LOOKUP DNS",
                loading = running,
                enabled = query.isNotBlank()
            ) {
                if (query.isBlank()) {
                    Toast.makeText(context, com.mporttech.pro.ui.i18n.Str.get("nt.enter_domain", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)), Toast.LENGTH_SHORT).show()
                    return@ActionButton
                }
                running = true
                records = emptyList()
                error = null
                resolveTimeMs = null
                canonical = null
                scope.launch {
                    try {
                        val result = NetworkOutputParser.dnsLookup(query)
                        if (result.error != null) {
                            error = result.error
                        } else {
                            records = result.records.map {
                                DnsRecord(it.type, it.value, it.note)
                            }
                            resolveTimeMs = result.timeMs
                            canonical = result.canonical
                                ?: result.records.firstOrNull()?.value
                        }
                    } catch (e: Exception) {
                        error = e.message ?: "Lookup failed"
                    } finally {
                        running = false
                    }
                }
            }
        }

        resolveTimeMs?.let { ms ->
            PremiumCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill("Time", "${ms} ms", AccentCyan)
                    StatPill("Records", "${records.size}", AccentBlue)
                    StatPill("Status", if (error == null) "OK" else t("nt.fail"), if (error == null) SuccessGreen else ErrorRed)
                }
            }
        }

        error?.let { msg ->
            PremiumCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Error, null, tint = ErrorRed)
                    Spacer(Modifier.width(8.dp))
                    Text(msg, color = ErrorRed, fontSize = 13.sp)
                }
            }
        }

        if (records.isNotEmpty()) {
            PremiumCard {
                Text(t("nt.records"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
                records.forEach { rec ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SurfaceElevated)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (rec.type) {
                                "A" -> Color(0xFF0D3A5C)
                                "AAAA" -> Color(0xFF1A3A2A)
                                "CNAME/PTR" -> Color(0xFF3A2A0D)
                                else -> Color(0xFF1A2A42)
                            }
                        ) {
                            Text(
                                rec.type,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentCyan
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                rec.value,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFE8FBFF)
                            )
                            if (rec.note.isNotBlank()) {
                                Text(rec.note, fontSize = 10.sp, color = Color(0xFF5EC8E8))
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        PremiumCard {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Menggunakan resolver sistem Android (InetAddress). Menampilkan A / AAAA dan canonical name bila tersedia.",
                    fontSize = 11.sp,
                    color = Color(0xFF5EC8E8)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// PORT CHECKER
// ─────────────────────────────────────────────────────────────

data class PortResult(
    val port: Int,
    val open: Boolean,
    val latencyMs: Long?,
    val service: String
)

@Composable
fun PortCheckerScreen(nav: NavController? = null) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var host by remember { mutableStateOf("scanme.nmap.org") }
    var portInput by remember { mutableStateOf("80,443,22,53") }
    var running by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf<List<PortResult>>(emptyList()) }
    var progress by remember { mutableStateOf(0f) }

    val quickSets = listOf(
        "Web" to "80,443,8080,8443",
        "Mail" to "25,465,587,993,995",
        "Remote" to "22,3389,5900",
        "MikroTik" to "8728,8729,22,80,443",
        "Common" to "21,22,23,25,53,80,110,143,443,3306,3389,8080"
    )

    // Port checks delegated to NetworkOutputParser.portScan (parallel)

    ToolScaffold(
        title = t("nt.port_title"),
        subtitle = t("nt.port_sub"),
        icon = Icons.Default.Security,
        nav = nav
    ) {
        PremiumCard {
            Text(t("nt.target"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
            OutlinedTextField(
                value = host,
                onValueChange = { host = it.trim() },
                label = { Text(t("diag.host_ip")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedLabelColor = AccentBlue,
                    cursorColor = AccentBlue
                )
            )

            Text(t("nt.ports"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
            OutlinedTextField(
                value = portInput,
                onValueChange = { portInput = it },
                label = { Text(t("nt.ports_hint")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentBlue,
                    unfocusedBorderColor = BorderSubtle,
                    focusedLabelColor = AccentBlue,
                    cursorColor = AccentBlue
                )
            )

            Text(t("nt.quick_sets"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                quickSets.take(3).forEach { (label, ports) ->
                    AssistChip(
                        onClick = { portInput = ports },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SurfaceElevated,
                            labelColor = Color(0xFFB8DFFF)
                        )
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                quickSets.drop(3).forEach { (label, ports) ->
                    AssistChip(
                        onClick = { portInput = ports },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = SurfaceElevated,
                            labelColor = Color(0xFFB8DFFF)
                        )
                    )
                }
            }

            ActionButton(
                text = "SCAN PORTS",
                loading = running,
                enabled = host.isNotBlank() && portInput.isNotBlank()
            ) {
                val ports = NetworkOutputParser.parsePortList(portInput)
                if (host.isBlank() || ports.isEmpty()) {
                    Toast.makeText(context, com.mporttech.pro.ui.i18n.Str.get("nt.host_port_required", com.mporttech.pro.ui.i18n.loadSavedLanguage(context)), Toast.LENGTH_SHORT).show()
                    return@ActionButton
                }
                running = true
                results = emptyList()
                progress = 0f
                scope.launch {
                    // Parallel scan via optimized parser
                    val scanned = NetworkOutputParser.portScan(host, ports)
                    val out = mutableListOf<PortResult>()
                    scanned.forEachIndexed { index, r ->
                        out.add(PortResult(r.port, r.open, r.latencyMs, r.service))
                        results = out.toList()
                        progress = (index + 1).toFloat() / scanned.size.coerceAtLeast(1)
                        delay(30)
                    }
                    running = false
                }
            }

            if (running) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = AccentBlue,
                    trackColor = SurfaceElevated
                )
            }
        }

        if (results.isNotEmpty()) {
            val openCount = results.count { it.open }
            PremiumCard {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatPill("Scanned", "${results.size}", AccentCyan)
                    StatPill("Open", "$openCount", SuccessGreen)
                    StatPill("Closed", "${results.size - openCount}", ErrorRed)
                }
            }

            PremiumCard {
                Text(t("nt.results"), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF9EE8FF), letterSpacing = 1.sp)
                results.forEach { r ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (r.open) Color(0xFF0A1F18) else SurfaceElevated)
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (r.open) SuccessGreen else ErrorRed)
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Port ${r.port}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFE8FBFF)
                            )
                            Text(
                                r.service,
                                fontSize = 11.sp,
                                color = Color(0xFF5EC8E8)
                            )
                        }
                        Text(
                            if (r.open) "${r.latencyMs} ms  OPEN" else t("nt.closed"),
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = if (r.open) SuccessGreen else ErrorRed
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }

        PremiumCard {
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, null, tint = AccentBlue, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "TCP connect scan. Port terbuka berarti host menerima koneksi pada port tersebut. Firewall dapat memblokir hasil.",
                    fontSize = 11.sp,
                    color = Color(0xFF5EC8E8)
                )
            }
        }
    }
}
