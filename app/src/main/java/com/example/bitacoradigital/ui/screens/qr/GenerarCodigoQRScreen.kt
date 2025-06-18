package com.example.bitacoradigital.ui.screens.qr

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.viewmodel.GenerarCodigoQRViewModel
import com.example.bitacoradigital.viewmodel.GenerarCodigoQRViewModelFactory
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import androidx.navigation.NavHostController

@Composable
fun GenerarCodigoQRScreen(navController: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: GenerarCodigoQRViewModel = viewModel(factory = GenerarCodigoQRViewModelFactory(prefs))

    val telefono by viewModel.telefono.collectAsState()
    val caducidad by viewModel.caducidad.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    Scaffold(
        bottomBar = {
            HomeConfigNavBar(
                current = "",
                onHomeClick = { navController.navigate("home") },
                onConfigClick = { navController.navigate("configuracion") }
            )
        }
    ) { innerPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = telefono,
            onValueChange = { viewModel.telefono.value = it },
            label = { Text("Teléfono") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = caducidad,
            onValueChange = { viewModel.caducidad.value = it },
            label = { Text("Caducidad (días)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Text("Número de días que será válido el código")

        Button(onClick = { viewModel.enviarInvitacion() }, enabled = !cargando, modifier = Modifier.fillMaxWidth()) {
            Text(if (cargando) "Enviando..." else "Generar")
        }

        mensaje?.let { Text(it) }
    }
    }
}
