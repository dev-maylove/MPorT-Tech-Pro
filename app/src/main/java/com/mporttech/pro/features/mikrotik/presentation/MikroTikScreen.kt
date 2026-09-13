package com.mporttech.pro.features.mikrotik.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Router
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController

@Composable
fun MikroTikScreen(
    nav: NavController? = null,
    vm: MikroTikViewModel = hiltViewModel()
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row {
            if (nav != null) {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, null)
                }
            }
            Text("MikroTik", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        }
        Text(
            "Credentials disimpan terenkripsi (SecureStorage). API RouterOS (8728) — fondasi Phase D.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = ui.host,
            onValueChange = vm::updateHost,
            label = { Text("Host / IP") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Router, null) }
        )
        OutlinedTextField(
            value = ui.username,
            onValueChange = vm::updateUser,
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = ui.password,
            onValueChange = vm::updatePass,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = ui.port,
            onValueChange = vm::updatePort,
            label = { Text("API Port") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { vm.connect() },
                enabled = !ui.connecting,
                modifier = Modifier.weight(1f),
        colors = ButtonDefaults.buttonColors(
            contentColor = Color(0xFF000000),
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color(0xFF000000),
                    disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
                    disabledContentColor = Color(0xFF000000)
                )) {
                Text(if (ui.connecting) "Connecting…" else "Connect")
            }
            OutlinedButton(
                onClick = { vm.disconnect() },
                modifier = Modifier.weight(1f)
            ) { Text("Disconnect") }
        }
        TextButton(onClick = { vm.clearSaved() }) {
            Text("Hapus credentials tersimpan")
        }
        ui.message?.let { Text(it, color = MaterialTheme.colorScheme.primary, fontSize = 13.sp) }
        ui.error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
        if (ui.info.connected) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Session", fontWeight = FontWeight.Bold)
                    Text("Identity: ${ui.info.identity}")
                    Text("Board: ${ui.info.boardName}")
                    Text("Version: ${ui.info.version}")
                }
            }
        }
    }
}
