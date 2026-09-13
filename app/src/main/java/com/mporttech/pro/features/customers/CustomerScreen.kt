package com.mporttech.pro.features.customers

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.mporttech.pro.ui.i18n.t
import com.mporttech.pro.core.database.CustomerEntity
import com.mporttech.pro.data.repository.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CustomerViewModel @Inject constructor(
    private val repo: CustomerRepository
) : ViewModel() {
    val items = repo.observe()
    private val _syncing = MutableStateFlow(false)
    val syncing = _syncing.asStateFlow()
    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    fun add(name: String, phone: String, address: String, plan: String) =
        viewModelScope.launch { repo.add(name, phone, address, plan) }

    fun sync() = viewModelScope.launch {
        if (_syncing.value) return@launch
        _syncing.value = true
        when (val r = repo.syncFromRemote()) {
            is com.mporttech.pro.core.common.Result.Success ->
                _syncMessage.value = "Sinkron ${r.data} pelanggan"
            is com.mporttech.pro.core.common.Result.Error ->
                _syncMessage.value = r.message
            else -> {}
        }
        _syncing.value = false
    }

    fun consumeSyncMessage() { _syncMessage.value = null }
}

private val AccentBlue = Color(0xFF00F0FF)
private val AccentCyan = Color(0xFF00F0FF)
private val SuccessGreen = Color(0xFF39FF14)
private val CardBg = Color(0xFF0B1220)
private val SurfaceElev = Color(0xFF111C2E)
private val BorderSubtle = Color(0xFF1A2A42)

@Composable
fun CustomerScreen(
    nav: NavController? = null,
    vm: CustomerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val items by vm.items.collectAsStateWithLifecycle(initialValue = emptyList())
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var plan by remember { mutableStateOf("Standard") }
    var showForm by remember { mutableStateOf(true) }
    var query by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val syncing by vm.syncing.collectAsStateWithLifecycle(initialValue = false)
    val syncMessage by vm.syncMessage.collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(Unit) { vm.sync() }
    LaunchedEffect(syncMessage) {
        val msg = syncMessage ?: return@LaunchedEffect
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        vm.consumeSyncMessage()
    }

    val plans = listOf("Basic", "Standard", "Business", "Enterprise")
    val filtered = if (query.isBlank()) items else items.filter {
        it.name.contains(query, true) ||
            it.phone.contains(query, true) ||
            it.address.contains(query, true) ||
            it.packageName.contains(query, true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
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
                            Icon(Icons.Default.People, null, tint = AccentBlue, modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Customers", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE8FBFF))
                        Text("Data pelanggan lokal", fontSize = 11.sp, color = Color(0xFF9EE8FF))
                    }
                    TextButton(onClick = { showForm = !showForm }) {
                        Text(
                            if (showForm) "Tutup" else "+ Tambah",
                            color = AccentBlue,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricMini("Total", "${items.size}", AccentCyan, Modifier.weight(1f))
                MetricMini(
                    "Business+",
                    "${items.count { it.packageName.contains("Business", true) || it.packageName.contains("Enterprise", true) }}",
                    SuccessGreen,
                    Modifier.weight(1f)
                )
                MetricMini(
                    "Standard",
                    "${items.count { it.packageName.contains("Standard", true) || it.packageName.contains("Basic", true) }}",
                    AccentBlue,
                    Modifier.weight(1f)
                )
            }
        }

        item {
            AnimatedVisibility(visible = showForm, enter = fadeIn(), exit = fadeOut()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardBg)
                ) {
                    Column(
                        Modifier
                            .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PersonAdd, null, tint = AccentBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "PELANGGAN BARU",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFB8DFFF),
                                letterSpacing = 1.sp
                            )
                        }

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Nama lengkap") },
                            leadingIcon = { Icon(Icons.Default.Person, null, tint = Color(0xFF9EE8FF)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors()
                        )
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = { Text("Telepon / WhatsApp") },
                            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color(0xFF9EE8FF)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors()
                        )
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("Alamat instalasi") },
                            leadingIcon = { Icon(Icons.Default.Home, null, tint = Color(0xFF9EE8FF)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors()
                        )

                        Text(
                            "PAKET LAYANAN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9EE8FF),
                            letterSpacing = 1.sp
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            plans.forEach { p ->
                                FilterChip(
                                    selected = plan == p,
                                    onClick = { plan = p },
                                    label = { Text(p, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = AccentBlue,
                                        selectedLabelColor = Color(0xFF001A2B)
                                    )
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    Toast.makeText(context, "Nama wajib diisi", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                scope.launch {
                                    vm.add(
                                        name.trim(),
                                        phone.trim(),
                                        address.trim(),
                                        plan.trim().ifBlank { "Standard" }
                                    )
                                    name = ""
                                    phone = ""
                                    address = ""
                                    plan = "Standard"
                                    showForm = false
                                    Toast.makeText(context, "Pelanggan disimpan", Toast.LENGTH_SHORT).show()
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
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("SIMPAN PELANGGAN", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(com.mporttech.pro.ui.i18n.t("customers.search")) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color(0xFF9EE8FF)) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Clear, null, tint = Color(0xFF9EE8FF))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors()
            )
        }

        item {
            Text(
                "DAFTAR (${filtered.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                color = Color(0xFF9EE8FF),
                letterSpacing = 1.sp
            )
        }

        if (filtered.isEmpty()) {
            item {
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
                                Icon(Icons.Default.People, null, tint = AccentBlue, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(com.mporttech.pro.ui.i18n.t("customers.empty"), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE8FBFF))
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Tekan + Tambah untuk menyimpan data pelanggan ke database lokal.",
                            fontSize = 12.sp,
                            color = Color(0xFF5EC8E8)
                        )
                    }
                }
            }
        } else {
            items(filtered, key = { it.id }) { c ->
                CustomerCard(c)
            }
        }
    }
}

@Composable
private fun CustomerCard(c: CustomerEntity) {
    val planColor = when {
        c.packageName.contains("Enterprise", true) -> Color(0xFFB680FF)
        c.packageName.contains("Business", true) -> SuccessGreen
        c.packageName.contains("Standard", true) -> AccentBlue
        else -> Color(0xFF5EC8E8)
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg)
    ) {
        Row(
            Modifier
                .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0D2A45),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        c.name.take(1).uppercase(),
                        fontWeight = FontWeight.Bold,
                        color = AccentCyan,
                        fontSize = 16.sp
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE8FBFF))
                if (c.phone.isNotBlank()) {
                    Text(c.phone, fontSize = 12.sp, color = Color(0xFF5EC8E8))
                }
                if (c.address.isNotBlank()) {
                    Text(c.address, fontSize = 11.sp, color = Color(0xFF6A829E), maxLines = 1)
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = planColor.copy(alpha = 0.15f)
            ) {
                Text(
                    c.packageName,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = planColor
                )
            }
        }
    }
}

@Composable
private fun MetricMini(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(SurfaceElev)
            .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(label, color = Color(0xFF5EC8E8), fontSize = 10.sp)
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
