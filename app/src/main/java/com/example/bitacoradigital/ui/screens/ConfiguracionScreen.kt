package com.example.bitacoradigital.ui.screens
// 📁 ui/screens/ConfiguracionScreen.kt

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings

import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.model.User
import com.example.bitacoradigital.viewmodel.SessionViewModel
import kotlinx.coroutines.launch
import com.example.bitacoradigital.ui.components.HomeConfigNavBar

@Composable
fun ConfiguracionScreen(
    sessionViewModel: SessionViewModel,
    onCerrarSesion: () -> Unit,
    onHomeClick: () -> Unit
) {
    val usuario by sessionViewModel.usuario.collectAsState()

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
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Top
        ) {
            Text("Configuración", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(16.dp))

            usuario?.let { user ->
                Text("Usuario: ${user.display} (${user.email})")
                Spacer(modifier = Modifier.height(8.dp))
                Text("ID: ${user.id}")
                Spacer(modifier = Modifier.height(8.dp))

                user.empresas.forEach { empresa ->
                    Text("Empresa: ${empresa.nombre}")
                    empresa.perimetros.forEach {
                        Text(
                            text = "- ${it.nombre}",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                val coroutineScope = rememberCoroutineScope()

                Button(
                    onClick = {
                        coroutineScope.launch {
                            sessionViewModel.cerrarSesion()
                            onCerrarSesion()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Cerrar sesión", color = MaterialTheme.colorScheme.onError)
                }
            } ?: Text("No hay datos del usuario.")
        }
    }
}

