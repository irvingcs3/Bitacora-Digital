package com.example.bitacoradigital.ui.screens.qr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.viewmodel.GenerarCodigoQRViewModel
import com.example.bitacoradigital.viewmodel.GenerarCodigoQRViewModelFactory

@Composable
fun GenerarCodigoQRScreen() {
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: GenerarCodigoQRViewModel = viewModel(factory = GenerarCodigoQRViewModelFactory(prefs))

    val telefono by viewModel.telefono.collectAsState()
    val caducidad by viewModel.caducidad.collectAsState()
    val mensaje by viewModel.mensaje.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = telefono,
            onValueChange = { viewModel.telefono.value = it },
            label = { Text("Teléfono") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Box {
            OutlinedTextField(
                value = caducidad.toString(),
                onValueChange = {},
                label = { Text("Caducidad") },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            )
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                (1..3).forEach {
                    DropdownMenuItem(text = { Text(it.toString()) }, onClick = {
                        viewModel.caducidad.value = it
                        expanded = false
                    })
                }
            }
        }

        Button(onClick = { viewModel.enviarInvitacion() }, enabled = !cargando, modifier = Modifier.fillMaxWidth()) {
            Text(if (cargando) "Enviando..." else "Generar")
        }

        mensaje?.let { Text(it) }
    }
}
