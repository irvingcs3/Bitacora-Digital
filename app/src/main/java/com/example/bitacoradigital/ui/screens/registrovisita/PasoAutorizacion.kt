package com.example.bitacoradigital.ui.screens.registrovisita

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasoAutorizacion(viewModel: RegistroVisitaViewModel) {
    val destino by viewModel.destinoSeleccionado.collectAsState()
    val residentes by viewModel.residentesDestino.collectAsState()
    val invitanteId by viewModel.invitanteId.collectAsState()
    val cargando by viewModel.cargandoResidentes.collectAsState()
    val error by viewModel.errorResidentes.collectAsState()
    val cargandoReg by viewModel.cargandoRegistro.collectAsState()
    val registroCompleto by viewModel.registroCompleto.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(destino) {
        destino?.let { viewModel.cargarResidentesDestino(it.perimetro_id) }
    }

    LaunchedEffect(registroCompleto) {
        if (registroCompleto) viewModel.avanzarPaso()
    }

    var expanded by remember { mutableStateOf(false) }
    val seleccionado = residentes.find { it.idPersona == invitanteId }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text("Paso 7: ¿Quién autorizó?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            if (cargando) {
                CircularProgressIndicator()
            } else if (error != null) {
                Text(error ?: "", color = MaterialTheme.colorScheme.error)
            } else {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = seleccionado?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Residente") },
                        trailingIcon = {
                            if (expanded) Icon(Icons.Default.ArrowDropUp, contentDescription = null)
                            else Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        residentes.forEach { res ->
                            DropdownMenuItem(text = { Text(res.name) }, onClick = {
                                viewModel.invitanteId.value = res.idPersona
                                expanded = false
                            })
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = { viewModel.registrarVisita(context) },
                enabled = invitanteId != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Finalizar Registro")
            }
        }

        if (cargandoReg) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }
        }
    }
}
