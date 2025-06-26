// 📁 ui/screens/registrovisita/PasoVerificacion.kt

package com.example.bitacoradigital.ui.screens.registrovisita

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel

@Composable
fun PasoVerificacion(viewModel: RegistroVisitaViewModel) {
    val nombre by viewModel.nombre.collectAsState()
    val apellidoPaterno by viewModel.apellidoPaterno.collectAsState()
    val apellidoMaterno by viewModel.apellidoMaterno.collectAsState()

    var editando by remember { mutableStateOf(false) }
    var nuevoNombre by remember { mutableStateOf(nombre) }
    var nuevoApellidoPat by remember { mutableStateOf(apellidoPaterno) }
    var nuevoApellidoMat by remember { mutableStateOf(apellidoMaterno) }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Paso 3: Verifica tus Datos", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            if (editando) {
                OutlinedTextField(
                    value = nuevoNombre,
                    onValueChange = { nuevoNombre = it },
                    label = { Text("Nombre(s)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = nuevoApellidoPat,
                    onValueChange = { nuevoApellidoPat = it },
                    label = { Text("Apellido Paterno") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = nuevoApellidoMat,
                    onValueChange = { nuevoApellidoMat = it },
                    label = { Text("Apellido Materno") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                )
                Spacer(Modifier.height(16.dp))

                ElevatedButton(
                    onClick = {
                        viewModel.actualizarDatos(
                            nombre = nuevoNombre,
                            paterno = nuevoApellidoPat,
                            materno = nuevoApellidoMat
                        )
                        editando = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Guardar y Continuar")
                }
            } else {
                Card {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Nombre(s): $nombre")
                        Text("Apellido Paterno: $apellidoPaterno")
                        Text("Apellido Materno: $apellidoMaterno")
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ElevatedButton(onClick = { editando = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Editar")
                    }
                    ElevatedButton(
                        onClick = { viewModel.avanzarPaso() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Siguiente")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = { viewModel.retrocederPaso() }) {
                Text("← Regresar")
            }
        }
    }
}

