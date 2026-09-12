package com.mporttech.pro.features.network.scanner.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.mporttech.pro.features.tools.SelectedDeviceStore

/**
 * V2 Network Scanner — driven by ScanNetworkUseCase + ViewModel.
 */
@Composable
fun NetworkScannerScreen(
    nav: NavController,
    vm: NetworkScannerViewModel = hiltViewModel()
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.quickScan()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Network Scanner", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { vm.scan() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Scan")
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = ui.authorized,
                onCheckedChange = { vm.setAuthorized(it) }
            )
            Text("Authorize private LAN scan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (ui.loading || ui.status.isNotBlank()) {
            Text(ui.status, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            if (ui.loading) {
                LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical = 8.dp))
            }
        }
        ui.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
        }
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(ui.devices, key = { it.ip }) { d ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            SelectedDeviceStore.ip = d.ip
                            SelectedDeviceStore.name = d.name
                            SelectedDeviceStore.kind = d.kind
                            nav.navigate("deviceDetailRich")
                        }
                ) {
                    Row(
                        Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Router, null, tint = if (d.online) Color(0xFF35E381) else Color.Gray)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(d.name, fontWeight = FontWeight.Bold)
                            Text(d.ip, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            d.latencyMs?.let { "$it ms" } ?: if (d.online) "online" else "offline",
                            fontSize = 12.sp,
                            color = if (d.online) Color(0xFF35E381) else Color.Gray
                        )
                    }
                }
            }
        }
    }
}
