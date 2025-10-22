// 📁 ui/screens/RegistroVisitaWizardScreen.kt
package com.example.bitacoradigital.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.network.ApiService
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.ui.components.Stepper
import com.example.bitacoradigital.ui.screens.lomascountry.LomasCountryRegistroScreen
import com.example.bitacoradigital.ui.screens.registrovisita.PasoAutorizacion
import com.example.bitacoradigital.ui.screens.registrovisita.PasoConfirmacion
import com.example.bitacoradigital.ui.screens.registrovisita.PasoDestino
import com.example.bitacoradigital.ui.screens.registrovisita.PasoDocumento
import com.example.bitacoradigital.ui.screens.registrovisita.PasoFinal
import com.example.bitacoradigital.ui.screens.registrovisita.PasoFotos
import com.example.bitacoradigital.ui.screens.registrovisita.PasoTelefono
import com.example.bitacoradigital.ui.screens.registrovisita.PasoVerificacion
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModelFactory

@Composable
fun RegistroVisitaWizardScreen(
    perimetroId: Int,
    navController: NavHostController,
    isLomasCountry: Boolean = false
) {
    val context = LocalContext.current
    val apiService = remember { ApiService.create() }
    val sessionPrefs = remember { SessionPreferences(context) }

    if (isLomasCountry) {
        LomasCountryRegistroScreen(
            perimetroId = perimetroId,
            navController = navController,
            apiService = apiService,
            sessionPrefs = sessionPrefs
        )
    } else {
        val viewModel: RegistroVisitaViewModel = viewModel(
            factory = RegistroVisitaViewModelFactory(apiService, sessionPrefs, perimetroId, false)
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
                Stepper(pasoActual, totalPasos = 8)
                Spacer(Modifier.height(24.dp))

                when (pasoActual) {
                    1 -> PasoTelefono(viewModel, navController, false)
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
}
