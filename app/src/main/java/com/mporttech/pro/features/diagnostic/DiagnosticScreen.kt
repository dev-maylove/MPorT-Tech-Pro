package com.mporttech.pro.features.diagnostic

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.mporttech.pro.core.database.DiagnosticEntity
import com.mporttech.pro.data.repository.DiagnosticRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosticViewModel @Inject constructor(
    private val repo: DiagnosticRepository
) : ViewModel() {
    val items = repo.observe()
    suspend fun ping(target: String) = repo.ping(target)
}

@Composable
fun DiagnosticScreen(
    nav: NavController? = null,
    vm: DiagnosticViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val items by vm.items.collectAsStateWithLifecycle(initialValue = emptyList())
    var target by remember { mutableStateOf("8.8.8.8") }
    var running by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val presets = listOf("8.8.8.8", "1.1.1.1", "192.168.1.1", "google.com")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (nav != null) {
                    IconButton(onClick = {
                        if (nav.previousBackStackEntry != null) nav.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
                Icon(Icons.Default.NetworkCheck, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Network Diagnostic", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Reachability Check", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    OutlinedTextField(
                        value = target,
                        onValueChange = { target = it.trim() },
                        label = { Text("Host / IP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presets.forEach { p ->
                            AssistChip(
                                onClick = { target = p },
                                label = { Text(p, fontSize = 10.sp) }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            if (target.isBlank()) {
                                Toast.makeText(context, "Isi host/IP dulu", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            running = true
                            scope.launch {
                                try {
                                    vm.ping(target)
                                } finally {
                                    running = false
                                }
                            }
                        },
                        enabled = !running,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (running) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(if (running) "CHECKING..." else "RUN REACHABILITY CHECK")
                    }
                }
            }
        }

        item {
            Text("History", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        if (items.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Text(
                        "Belum ada hasil. Jalankan ping ke host yang diizinkan.",
                        modifier = Modifier.padding(14.dp),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(items) { item: DiagnosticEntity ->
                DiagnosticRow(item)
            }
        }
    }
}

@Composable
private fun DiagnosticRow(item: DiagnosticEntity) {
    val color = if (item.success) Color(0xFF35E381) else Color(0xFFFF5E67)
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("●", color = color, fontSize = 14.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(item.target, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    "${item.message}${item.latencyMs?.let { " • ${it} ms" } ?: ""}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
