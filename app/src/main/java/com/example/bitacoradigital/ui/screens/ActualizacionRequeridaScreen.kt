package com.example.bitacoradigital.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.viewmodel.SessionViewModel
import kotlinx.coroutines.launch

@Composable
fun ActualizacionRequeridaScreen(
    sessionViewModel: SessionViewModel,
    onCerrarSesion: () -> Unit
) {
    val scope = rememberCoroutineScope()
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Debes ir a la pagina para descargar el apk de version mas reciente en https://bit.cs3.mx/bitacora/app.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Lamentamos los inconvenientes",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = {
                scope.launch {
                    sessionViewModel.cerrarSesion()
                    onCerrarSesion()
                }
            }) {
                Text("Cerrar sesión")
            }
        }
    }
}
