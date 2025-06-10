// 📁 ui/screens/RegistroVisitaWizardScreen.kt
package com.example.bitacoradigital.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bitacoradigital.ui.screens.registrovisita.PasoConfirmacion
import com.example.bitacoradigital.ui.screens.registrovisita.PasoDestino
import com.example.bitacoradigital.ui.screens.registrovisita.PasoDocumento
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.network.ApiService

import com.example.bitacoradigital.ui.screens.registrovisita.PasoFinal
import com.example.bitacoradigital.ui.screens.registrovisita.PasoFotos
import com.example.bitacoradigital.ui.screens.registrovisita.PasoTelefono
import com.example.bitacoradigital.ui.screens.registrovisita.PasoVerificacion
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModelFactory


@Composable
fun RegistroVisitaWizardScreen(perimetroId: Int) {
    val context = LocalContext.current
    val apiService = remember { ApiService.create() }
    val sessionPrefs = remember { SessionPreferences(context) }

    val viewModel: RegistroVisitaViewModel = viewModel(
        factory = RegistroVisitaViewModelFactory(apiService, sessionPrefs, perimetroId)
    )

    val pasoActual by viewModel.pasoActual.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Stepper(pasoActual)
        Spacer(Modifier.height(24.dp))

        when (pasoActual) {
            1 -> PasoTelefono(viewModel)
            2 -> PasoDocumento(viewModel)
            3 -> PasoVerificacion(viewModel)
            4 -> PasoDestino(viewModel)
            5 -> PasoFotos(viewModel)
            6 -> PasoConfirmacion(viewModel)
            7 -> PasoFinal(viewModel)
        }
    }
}


@Composable
fun Stepper(paso: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(5) {
            val color = if (it + 1 == paso) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .background(color)
            )
        }
    }
}
