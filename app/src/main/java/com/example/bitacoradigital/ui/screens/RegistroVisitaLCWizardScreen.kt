// 📁 ui/screens/RegistroVisitaLCWizardScreen.kt
package com.example.bitacoradigital.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.viewmodel.RegistroVisitaLCViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bitacoradigital.ui.screens.registrovisitalc.PasoConfirmacionLC
import com.example.bitacoradigital.ui.screens.registrovisitalc.PasoDestinoLC
import com.example.bitacoradigital.ui.screens.registrovisita.PasoDocumento
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalContext
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.network.ApiService

import com.example.bitacoradigital.ui.screens.registrovisitalc.PasoFinalLC
import com.example.bitacoradigital.ui.screens.registrovisita.PasoFotos
import com.example.bitacoradigital.ui.screens.registrovisita.PasoTelefono
import com.example.bitacoradigital.ui.screens.registrovisita.PasoVerificacion
import com.example.bitacoradigital.viewmodel.RegistroVisitaLCViewModelFactory
import com.example.bitacoradigital.ui.components.Stepper
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import androidx.navigation.NavHostController


@Composable
fun RegistroVisitaLCWizardScreen(perimetroId: Int, navController: NavHostController) {
    val context = LocalContext.current
    val apiService = remember { ApiService.create() }
    val sessionPrefs = remember { SessionPreferences(context) }

    val viewModel: RegistroVisitaLCViewModel = viewModel(
        factory = RegistroVisitaLCViewModelFactory(apiService, sessionPrefs, perimetroId)
    )

    val pasoActual by viewModel.pasoActual.collectAsState()
    val scrollState = rememberScrollState()

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
            .padding(16.dp)
    ) {
        Stepper(pasoActual, totalPasos = 7)
        Spacer(Modifier.height(24.dp))

        when (pasoActual) {
            1 -> PasoTelefono(viewModel)
            2 -> PasoDocumento(viewModel)
            3 -> PasoVerificacion(viewModel)
            4 -> PasoDestinoLC(viewModel)
            5 -> PasoFotos(viewModel)
            6 -> PasoConfirmacionLC(viewModel)
            7 -> PasoFinalLC(viewModel, navController)
        }
    }
    }
}
