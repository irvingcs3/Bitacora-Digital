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
import com.example.bitacoradigital.ui.screens.registrovisita.PasoAutorizacion
import com.example.bitacoradigital.ui.screens.registrovisita.PasoFotos
import com.example.bitacoradigital.ui.screens.registrovisita.PasoTelefono
import com.example.bitacoradigital.ui.screens.registrovisita.PasoVerificacion
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModelFactory
import com.example.bitacoradigital.ui.components.Stepper
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import androidx.navigation.NavHostController


@Composable
fun RegistroVisitaWizardScreen(perimetroId: Int, navController: NavHostController) {
    val context = LocalContext.current
    val apiService = remember { ApiService.create() }
    val sessionPrefs = remember { SessionPreferences(context) }

    val viewModel: RegistroVisitaViewModel = viewModel(
        factory = RegistroVisitaViewModelFactory(apiService, sessionPrefs, perimetroId)
    )

    val pasoActual by viewModel.pasoActual.collectAsState()

    Scaffold(
        bottomBar = {
            HomeConfigNavBar(
                current = "",
                onHomeClick = { navController.navigate("home") },
                onConfigClick = { navController.navigate("configuracion") }
            )
        }
    ) { innerPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp)) {
        Stepper(pasoActual, totalPasos = 8)
        Spacer(Modifier.height(24.dp))

        when (pasoActual) {
            1 -> PasoTelefono(viewModel)
            2 -> PasoDocumento(viewModel)
            3 -> PasoVerificacion(viewModel)
            4 -> PasoDestino(viewModel)
            5 -> PasoFotos(viewModel)
            6 -> PasoConfirmacion(viewModel)
            7 -> PasoAutorizacion(viewModel)
            8 -> PasoFinal(viewModel, navController)
        }
    }
    }
}
