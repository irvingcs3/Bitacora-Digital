package com.example.bitacoradigital.ui.screens.perimetro

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bitacoradigital.ui.screens.registrovisita.NavegacionJerarquia
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.network.ApiService
import com.example.bitacoradigital.viewmodel.PerimetroViewModel
import com.example.bitacoradigital.viewmodel.PerimetroViewModelFactory

@Composable
fun PerimetrosScreen(perimetroId: Int, permisos: List<String>) {
    val context = LocalContext.current
    val apiService = remember { ApiService.create() }
    val prefs = remember { SessionPreferences(context) }

    val viewModel: PerimetroViewModel = viewModel(
        factory = PerimetroViewModelFactory(apiService, prefs, perimetroId)
    )

    val ruta by viewModel.ruta.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    var nuevoNombre by remember { mutableStateOf("") }
    val puedeCrear = "Crear Perímetro" in permisos

    LaunchedEffect(Unit) { viewModel.cargarJerarquia() }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (cargando) {
            CircularProgressIndicator()
        } else if (error != null) {
            Text("Error: ${'$'}error", color = MaterialTheme.colorScheme.error)
        } else {
            NavegacionJerarquia(
                ruta = ruta,
                onSeleccion = { viewModel.navegarHacia(it) },
                onConfirmar = {},
                onRetroceder = { viewModel.retroceder() }
            )
        }

        if (puedeCrear) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = nuevoNombre,
                onValueChange = { nuevoNombre = it },
                label = { Text("Nombre del perímetro") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    viewModel.crearHijo(nuevoNombre)
                    nuevoNombre = ""
                }) {
                    Text("Agregar Hijo")
                }
                Button(onClick = {
                    viewModel.crearHermano(nuevoNombre)
                    nuevoNombre = ""
                }) {
                    Text("Agregar Hermano")
                }
            }
        }
    }
}
