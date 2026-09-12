package com.mporttech.pro.features.tickets

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mporttech.pro.ui.i18n.t
import com.mporttech.pro.core.database.TicketEntity
import com.mporttech.pro.data.repository.TicketRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class TicketViewModel @Inject constructor(
    private val repo: TicketRepository
) : ViewModel() {
    val items = repo.observe()
    fun add(title: String, description: String = "") =
        viewModelScope.launch { repo.add(title, description) }
}

private val AccentBlue = Color(0xFF21B6FF)
private val SuccessGreen = Color(0xFF35E381)
private val WarningAmber = Color(0xFFFFB020)
private val ErrorRed = Color(0xFFFF5E67)
private val CardBg = Color(0xFF0B1220)
private val SurfaceElev = Color(0xFF111C2E)
private val BorderSubtle = Color(0xFF1A2A42)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketScreen(
    nav: NavController? = null,
    vm: TicketViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val items by vm.items.collectAsStateWithLifecycle(initialValue = emptyList())
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("Normal") }
    var category by remember { mutableStateOf("Gangguan") }
    var showForm by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf("ALL") }
    val scope = rememberCoroutineScope()

    val filtered = when (filter) {
        "OPEN" -> items.filter { it.status.equals("OPEN", true) }
        "DONE" -> items.filter {
            it.status.equals("DONE", true) || it.status.equals("CLOSED", true)
        }
        else -> items
    }
    val openCount = items.count { it.status.equals("OPEN", true) }
    val doneCount = items.count {
        it.status.equals("DONE", true) || it.status.equals("CLOSED", true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            // Premium header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF0A1628), Color(0xFF0D2A45))
                        )
                    )
                    .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (nav != null) {
                        IconButton(
                            onClick = {
                                if (nav.previousBackStackEntry != null) nav.popBackStack()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color(0xFFB8DFFF))
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0D3A5C),
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ConfirmationNumber,
                                null,
                                tint = AccentBlue,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Work Orders",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            "Ticket & job tracking",
                            fontSize = 11.sp,
                            color = Color(0xFF7BA3C9)
                        )
                    }
                    TextButton(onClick = { showForm = !showForm }) {
                        Text(
                            if (showForm) "Tutup" else "+ Baru",
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Stats strip
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatChip("Total", "${items.size}", AccentBlue, Modifier.weight(1f))
                StatChip("Open", "$openCount", WarningAmber, Modifier.weight(1f))
                StatChip("Done", "$doneCount", SuccessGreen, Modifier.weight(1f))
            }
        }

        // Create form
        item {
            AnimatedVisibility(visible = showForm, enter = fadeIn(), exit = fadeOut()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(
                        Modifier
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AddCircle,
                                null,
                                tint = AccentBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "TICKET BARU",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFB8DFFF),
                                letterSpacing = 1.sp
                            )
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("Masalah / Judul") },
                            placeholder = { Text("Contoh: Link down RT-05") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors()
                        )
                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Deskripsi") },
                            placeholder = { Text("Detail masalah, lokasi, kontak…") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 96.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors()
                        )

                        Text(
                            "PRIORITAS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7BA3C9),
                            letterSpacing = 1.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Low", "Normal", "High", "Critical").forEach { p ->
                                val selected = priority == p
                                val color = when (p) {
                                    "Critical" -> ErrorRed
                                    "High" -> WarningAmber
                                    "Normal" -> AccentBlue
                                    else -> Color(0xFF8AA0B8)
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = { priority = p },
                                    label = { Text(p, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = color.copy(alpha = 0.25f),
                                        selectedLabelColor = color
                                    )
                                )
                            }
                        }

                        Text(
                            "KATEGORI",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7BA3C9),
                            letterSpacing = 1.sp
                        )
                        val categories = listOf("Gangguan", "Instalasi", "Maintenance", "Upgrade")
                        var catExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = catExpanded,
                            onExpandedChange = { catExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Pilih kategori") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = catExpanded,
                                onDismissRequest = { catExpanded = false }
                            ) {
                                categories.forEach { c ->
                                    DropdownMenuItem(
                                        text = { Text(c) },
                                        onClick = {
                                            category = c
                                            catExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                if (title.isBlank()) {
                                    Toast.makeText(context, "Judul wajib diisi", Toast.LENGTH_SHORT)
                                        .show()
                                    return@Button
                                }
                                scope.launch {
                                    val desc = buildString {
                                        if (description.isNotBlank()) append(description.trim())
                                        if (isNotEmpty()) append("\n")
                                        append("[$priority • $category]")
                                    }
                                    vm.add(title.trim(), desc)
                                    title = ""
                                    description = ""
                                    priority = "Normal"
                                    category = "Gangguan"
                                    showForm = false
                                    Toast.makeText(
                                        context,
                                        "Ticket #$priority dibuat",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue,
                                contentColor = Color(0xFF001A2B)
                            )
                        ) {
                            Icon(Icons.Default.Send, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("CREATE TICKET", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // Filters
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "DAFTAR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF7BA3C9),
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.weight(1f))
                listOf("ALL" to "Semua", "OPEN" to "Open", "DONE" to "Done").forEach { (key, label) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AccentBlue,
                            selectedLabelColor = Color(0xFF001A2B)
                        )
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Default.ConfirmationNumber,
                    title = "Belum ada work order",
                    subtitle = "Buat ticket baru untuk menyimpan job ke database lokal."
                )
            }
        } else {
            items(filtered, key = { it.id }) { t ->
                TicketCard(t)
            }
        }
    }
}

@Composable
private fun TicketCard(t: TicketEntity) {
    val statusColor = when (t.status.uppercase()) {
        "OPEN" -> WarningAmber
        "IN_PROGRESS" -> AccentBlue
        "DONE", "CLOSED" -> SuccessGreen
        else -> Color(0xFF9DB0C7)
    }
    val date = remember(t.createdAt) {
        SimpleDateFormat("dd MMM yyyy • HH:mm", Locale("id")).format(Date(t.createdAt))
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            Modifier
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        t.status.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "#${t.id}",
                    fontSize = 11.sp,
                    color = Color(0xFF7BA3C9),
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.weight(1f))
                Text(date, fontSize = 10.sp, color = Color(0xFF6A829E))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                t.title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
            if (t.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    t.description,
                    fontSize = 12.sp,
                    color = Color(0xFF8AA0B8),
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun StatChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElev)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color(0xFF8AA0B8), fontSize = 10.sp)
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0D2A45),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = AccentBlue, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                subtitle,
                fontSize = 12.sp,
                color = Color(0xFF8AA0B8),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue,
    unfocusedBorderColor = BorderSubtle,
    focusedLabelColor = AccentBlue,
    cursorColor = AccentBlue,
    focusedContainerColor = SurfaceElev,
    unfocusedContainerColor = SurfaceElev
)
