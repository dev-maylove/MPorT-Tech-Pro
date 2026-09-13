package com.mporttech.pro.features.speedtest

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SpeedTestResultsScreen(nav: NavController? = null) {
    val context = LocalContext.current
    var records by remember { mutableStateOf(SpeedTestHistoryStore.load(context)) }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault()) }

    Scaffold(
        topBar = {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { nav?.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null)
                }
                Text("Hasil Speed Test", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
                IconButton(onClick = {
                    SpeedTestHistoryStore.clear(context)
                    records = emptyList()
                    Toast.makeText(context, "Riwayat dihapus", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(Icons.Default.Delete, null)
                }
            }
        }
    ) { pad ->
        if (records.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Speed, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Text("Belum ada hasil tes", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Jalankan Speed Test lalu kembali ke sini", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(pad).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(records, key = { it.timestamp }) { r ->
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Text(fmt.format(Date(r.timestamp)), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(r.serverName, fontWeight = FontWeight.Bold)
                            if (r.location.isNotBlank()) {
                                Text(r.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text("Download", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(String.format(Locale.US, "%.1f Mbps", r.downloadMbps), fontWeight = FontWeight.Bold, color = Color(0xFF21B6FF))
                                }
                                Column {
                                    Text("Upload", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(String.format(Locale.US, "%.1f Mbps", r.uploadMbps), fontWeight = FontWeight.Bold, color = Color(0xFFB680FF))
                                }
                                Column {
                                    Text("Ping", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(String.format(Locale.US, "%.0f ms", r.pingMs), fontWeight = FontWeight.Bold, color = Color(0xFF35E381))
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                String.format(Locale.US, "Jitter %.0f ms · Loss %.1f%%", r.jitterMs, r.lossPct),
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
