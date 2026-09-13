package com.mporttech.pro.features.auth

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mporttech.pro.R
import com.mporttech.pro.core.auth.UserRole
import com.mporttech.pro.core.common.Constants
import com.mporttech.pro.ui.i18n.t

@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    vm: LoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }

    val ui by vm.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(ui.successUser) {
        val user = ui.successUser ?: return@LaunchedEffect
        val role = if (user.role == UserRole.ADMIN) "Admin" else "Teknisi"
        Toast.makeText(context, "Selamat datang, ${user.name} ($role)", Toast.LENGTH_SHORT).show()
        vm.consumeSuccess()
        onLoggedIn()
    }

    LaunchedEffect(ui.error) {
        val err = ui.error ?: return@LaunchedEffect
        Toast.makeText(context, err, Toast.LENGTH_LONG).show()
        vm.consumeError()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF050A14), Color(0xFF0A1628), Color(0xFF050A14)))
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Image(
                painter = painterResource(R.drawable.mport_tech_logo),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(120.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("MPorT Tech", color = Color.White, fontWeight = FontWeight.Black, fontSize = 22.sp)
            Text(
                "Masuk untuk melanjutkan",
                color = Color(0xFF8EC8F0),
                fontSize = 13.sp
            )
            Spacer(Modifier.height(28.dp))
            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(t("login.username")) },
                leadingIcon = { Icon(Icons.Default.Person, null) },
                singleLine = true,
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text(t("login.password")) },
                leadingIcon = { Icon(Icons.Default.Lock, null) },
                trailingIcon = {
                    IconButton(onClick = { showPass = !showPass }) {
                        Icon(
                            if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null
                        )
                    }
                },
                visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                singleLine = true,
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { vm.login(username, password) },
                enabled = !ui.loading && username.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (ui.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(t("login.button"), fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (Constants.ALLOW_OFFLINE_DEMO_LOGIN) {
                    t("login.hint")
                } else {
                    "Gunakan akun teknisi / admin dari server MPorT"
                },
                color = Color(0xFF6A829E),
                fontSize = 11.sp,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Server: ${Constants.API_BASE_URL}",
                color = Color(0xFF4A6070),
                fontSize = 10.sp
            )
        }
    }
}
