package com.mporttech.pro.features.auth

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
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
import com.mporttech.pro.core.auth.AppUser
import com.mporttech.pro.core.auth.SessionManager

@Composable
fun TechnicianAdminScreen(nav: NavController? = null) {
    val context = LocalContext.current
    var techs by remember { mutableStateOf(SessionManager.listTechnicians(context)) }
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    if (!SessionManager.isAdmin(context)) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Hanya admin yang dapat mengelola teknisi")
        }
        return
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { nav?.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, null)
            }
            Text("Kelola Teknisi", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
        Spacer(Modifier.height(12.dp))
        Card(shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tambah teknisi baru", fontWeight = FontWeight.Bold)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nama") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Password") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Button(
                    onClick = {
                        val ok = SessionManager.addTechnician(context, name, username, password)
                        if (ok) {
                            techs = SessionManager.listTechnicians(context)
                            name = ""; username = ""; password = ""
                            Toast.makeText(context, "Teknisi ditambahkan", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Gagal (username sudah ada / tidak valid)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            contentColor = Color(0xFF000000),
                    containerColor = Color(0xFF00E5FF),
                    contentColor = Color(0xFF000000),
                    disabledContainerColor = Color(0xFF00E5FF).copy(alpha = 0.45f),
                    disabledContentColor = Color(0xFF000000)
                )) {
                    Icon(Icons.Default.PersonAdd, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Tambah Teknisi")
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Daftar teknisi (${techs.size})", fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(techs, key = { it.id }) { u: AppUser ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Row(
                        Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(u.name, fontWeight = FontWeight.Bold)
                            Text("@${u.username}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = {
                            SessionManager.removeTechnician(context, u.id)
                            techs = SessionManager.listTechnicians(context)
                        }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}
