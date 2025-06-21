package com.example.bitacoradigital.ui.screens
// 📁 ui/screens/ConfiguracionScreen.kt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Password

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.rememberScrollState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.example.bitacoradigital.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
import com.example.bitacoradigital.ui.components.HomeConfigNavBar

@Composable
fun ConfiguracionScreen(
    sessionViewModel: SessionViewModel,
    onCerrarSesion: () -> Unit,
    onHomeClick: () -> Unit,
    onResetPassword: () -> Unit
) {
    val usuario by sessionViewModel.usuario.collectAsState()
    val scrollState = rememberScrollState()
    var nombre by remember(usuario) { mutableStateOf(usuario?.nombre ?: "") }
    var apellidoPat by remember(usuario) { mutableStateOf(usuario?.apellido_paterno ?: "") }
    var apellidoMat by remember(usuario) { mutableStateOf(usuario?.apellido_materno ?: "") }
    var telefono by remember(usuario) { mutableStateOf(usuario?.telefono ?: "") }
    var showPermisos by remember { mutableStateOf(false) }
    var logoutConfirm by remember { mutableStateOf(false) }
    var updateMsg by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val hayCambios = usuario != null && (
        nombre != (usuario?.nombre ?: "") ||
        apellidoPat != (usuario?.apellido_paterno ?: "") ||
        apellidoMat != (usuario?.apellido_materno ?: "") ||
        telefono != (usuario?.telefono ?: "")
    )

    Scaffold(
        bottomBar = {
            HomeConfigNavBar(
                current = "config",
                onHomeClick = onHomeClick,
                onConfigClick = {}
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(16.dp)
                .animateContentSize(),
            verticalArrangement = Arrangement.Top
        ) {
            Text("Configuración", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            usuario?.let { user ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(user.display.take(1), color = MaterialTheme.colorScheme.onPrimary)
                            }
                            Spacer(Modifier.width(16.dp))
                            Text(user.email)
                        }
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = nombre,
                            onValueChange = { nombre = it },
                            label = { Text("Nombre") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = apellidoPat,
                            onValueChange = { apellidoPat = it },
                            label = { Text("Apellido paterno") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = apellidoMat,
                            onValueChange = { apellidoMat = it },
                            label = { Text("Apellido materno") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = telefono,
                            onValueChange = { telefono = it },
                            label = { Text("Teléfono") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = {
                                sessionViewModel.actualizarPerfil(
                                    nombre,
                                    apellidoPat,
                                    apellidoMat,
                                    telefono
                                ) { ok ->
                                    updateMsg = if (ok) "Datos actualizados correctamente" else "Error al actualizar datos"
                                }
                            },
                            enabled = hayCambios,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD65930)),
                            modifier = Modifier.align(Alignment.End)
                        ) { Text("Actualizar Datos") }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onResetPassword,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Password, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Restablecer contraseña")
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showPermisos = !showPermisos },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ver permisos")
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        if (showPermisos) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
                AnimatedVisibility(showPermisos) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        user.empresas.forEach { empresa ->
                            Text(empresa.nombre)
                            empresa.perimetros.forEach {
                                Text(
                                    text = "- ${it.nombre}",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { logoutConfirm = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cerrar sesión", color = MaterialTheme.colorScheme.onError) }
            } ?: Text("No hay datos del usuario.")

            if (logoutConfirm) {
                AlertDialog(
                    onDismissRequest = { logoutConfirm = false },
                    confirmButton = {
                        TextButton(onClick = {
                            logoutConfirm = false
                            coroutineScope.launch {
                                val token = sessionViewModel.token.value
                                if (token != null) {
                                    val request = Request.Builder()
                                        .url("https://bit.cs3.mx/_allauth/app/v1/auth/session")
                                        .delete()
                                        .addHeader("x-session-token", token)
                                        .build()
                                    withContext(Dispatchers.IO) { OkHttpClient().newCall(request).execute().close() }
                                }
                                sessionViewModel.cerrarSesion()
                                onCerrarSesion()
                            }
                        }) { Text("Sí") }
                    },
                    dismissButton = {
                        TextButton(onClick = { logoutConfirm = false }) { Text("No") }
                    },
                    text = { Text("¿Desea cerrar sesión?") }
                )
            }

            updateMsg?.let { msg ->
                AlertDialog(
                    onDismissRequest = { updateMsg = null },
                    confirmButton = { TextButton(onClick = { updateMsg = null }) { Text("OK") } },
                    text = { Text(msg) }
                )
            }
        }
    }
}

