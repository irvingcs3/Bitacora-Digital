// 📁 ui/screens/registrovisita/PasoTelefono.kt
package com.example.bitacoradigital.ui.screens.registrovisita

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
import kotlinx.coroutines.launch

@Composable
fun PasoTelefono(viewModel: RegistroVisitaViewModel) {
    val telefono by viewModel.telefono.collectAsState()
    val verificado by viewModel.numeroVerificado.collectAsState()

    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var verificacionFallida by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

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
                        verificacionFallida = false
                        val existe = viewModel.verificarNumeroWhatsApp(telefono)
                        cargando = false
                        if (existe) {
                            viewModel.numeroVerificado.value = true
                            viewModel.avanzarPaso()
                        } else {
                            mensajeError = "Número inválido o no verificado en WhatsApp"
                            verificacionFallida = true
                        }
                    }
                },
                enabled = !cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, shape = MaterialTheme.shapes.medium)
            ) { 
                Text(if (cargando) "Verificando..." else "Verificar número")
            }
            if (verificacionFallida) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.avanzarPaso() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, shape = MaterialTheme.shapes.medium)
                ) { Text("Continuar de todos modos") }
            }
        } else {
            Text("✅ Número verificado", color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { viewModel.avanzarPaso() },
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, shape = MaterialTheme.shapes.medium)
            ) {
                Text("Siguiente")
            }
        }

        mensajeError?.let { msg ->
            LaunchedEffect(msg) {
                snackbarHostState.showSnackbar(msg)
                mensajeError = null
            }
        }
    }
    SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
}
