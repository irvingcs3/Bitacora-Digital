// 📁 ui/screens/registrovisita/PasoTelefono.kt
package com.example.bitacoradigital.ui.screens.registrovisita

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
import kotlinx.coroutines.launch

@Composable
fun PasoTelefono(viewModel: RegistroVisitaViewModel) {
    val telefono by viewModel.telefono.collectAsState()
    val verificado by viewModel.numeroVerificado.collectAsState()

    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)) {

        Text("Paso 1: Verificación de teléfono", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { viewModel.telefono.value = it },
            label = { Text("Número de WhatsApp") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!verificado) {
            Button(
                onClick = {
                    cargando = true
                    mensajeError = null

                    coroutineScope.launch {
                        val existe = viewModel.verificarNumeroWhatsApp(telefono)
                        cargando = false
                        if (existe) {
                            viewModel.numeroVerificado.value = true
                            viewModel.avanzarPaso()
                        } else {
                            mensajeError = "Número inválido o no verificado en WhatsApp"
                        }
                    }
                },
                enabled = !cargando,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (cargando) "Verificando..." else "Verificar número")
            }
        } else {
            Text("✅ Número verificado", color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.avanzarPaso() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Siguiente")
            }
        }

        mensajeError?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
