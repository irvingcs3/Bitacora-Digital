package com.example.bitacoradigital.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.platform.LocalContext
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModelFactory
import com.example.bitacoradigital.ui.components.Stepper
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.network.ApiService
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.ui.screens.registrovisita.*
import androidx.navigation.NavHostController

@Composable
fun RegistroBitacoraGeneralWizardScreen(perimetroId: Int, navController: NavHostController) {
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
                4 -> PasoDestinoGeneral(viewModel)
                5 -> PasoFotos(viewModel)
                6 -> PasoConfirmacionGeneral(viewModel)
                7 -> PasoFinal(viewModel, navController)
            }
        }
    }
}
