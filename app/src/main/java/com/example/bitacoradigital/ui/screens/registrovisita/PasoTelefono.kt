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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
import kotlinx.coroutines.launch

@Composable
fun PasoTelefono(viewModel: RegistroVisitaViewModel, isLomasCountry: Boolean = false) {
    val telefono by viewModel.telefono.collectAsState()
    val verificado by viewModel.numeroVerificado.collectAsState()

    var cargando by remember { mutableStateOf(false) }
    var mensajeError by remember { mutableStateOf<String?>(null) }
    var verificacionFallida by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(isLomasCountry) {
        if (isLomasCountry) {
            focusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .then(
                    if (isLomasCountry) Modifier.fillMaxSize().padding(32.dp)
                    else Modifier.fillMaxWidth().padding(16.dp)
                )
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Text(
                "Paso 1: Verificación de teléfono",
                style = if (isLomasCountry)
                    MaterialTheme.typography.headlineMedium
                else
                    MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(if (isLomasCountry) 24.dp else 16.dp))

        OutlinedTextField(
            value = telefono,
            onValueChange = { viewModel.telefono.value = it },
            label = { Text("Número de WhatsApp") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            textStyle = if (isLomasCountry) MaterialTheme.typography.headlineMedium else LocalTextStyle.current,
            singleLine = isLomasCountry,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isLomasCountry) Modifier.height(80.dp).focusRequester(focusRequester)
                    else Modifier
                )
        )
            Spacer(modifier = Modifier.height(if (isLomasCountry) 24.dp else 16.dp))

        if (!verificado) {
            Button(
                onClick = {
                    cargando = true
                    mensajeError = null

                    coroutineScope.launch {
                        verificacionFallida = false
                        val existe = viewModel.verificarNumeroWhatsApp(telefono)
                        if (existe) {
                            viewModel.numeroVerificado.value = true
                            if (isLomasCountry) {
                                viewModel.cargarDatosCredencial()
                            }
                            viewModel.avanzarPaso()
                        } else {
                            mensajeError = "Número inválido o no verificado en WhatsApp"
                            verificacionFallida = true
                        }
                        cargando = false
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
                    onClick = {
                        coroutineScope.launch {
                            if (isLomasCountry) {
                                cargando = true
                                viewModel.cargarDatosCredencial()
                                cargando = false
                            }
                            viewModel.avanzarPaso()
                        }
                    },
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
