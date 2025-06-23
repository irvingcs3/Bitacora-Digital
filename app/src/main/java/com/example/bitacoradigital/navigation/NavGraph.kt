// 📁 navigation/AppNavGraph.kt

package com.example.bitacoradigital.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.example.bitacoradigital.ui.screens.auth.LoginScreen
import com.example.bitacoradigital.ui.screens.auth.SignupScreen
import com.example.bitacoradigital.ui.screens.auth.VerificationScreen
import com.example.bitacoradigital.ui.screens.auth.PasswordRequestScreen
import com.example.bitacoradigital.ui.screens.auth.PasswordResetScreen
import androidx.navigation.compose.composable
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.network.ApiService
import com.example.bitacoradigital.ui.screens.*
import com.example.bitacoradigital.ui.screens.RegistroQRScreen
import com.example.bitacoradigital.ui.screens.perimetro.PerimetrosScreen
import com.example.bitacoradigital.ui.screens.qr.CodigosQRScreen
import com.example.bitacoradigital.ui.screens.qr.GenerarCodigoQRScreen
import com.example.bitacoradigital.ui.screens.qr.SeguimientoQRScreen
import com.example.bitacoradigital.ui.screens.dashboard.DashboardScreen
import com.example.bitacoradigital.ui.screens.accesos.AccesosScreen
import com.example.bitacoradigital.ui.screens.residentes.ResidentesScreen
import com.example.bitacoradigital.viewmodel.HomeViewModel
import com.example.bitacoradigital.viewmodel.LoginViewModel
import com.example.bitacoradigital.viewmodel.SessionViewModel
import com.example.bitacoradigital.viewmodel.SignupViewModel
import com.example.bitacoradigital.viewmodel.ForgotPasswordViewModel


@Composable
fun AppNavGraph(
    navController: NavHostController,
    loginViewModel: LoginViewModel,
    sessionViewModel: SessionViewModel,
    homeViewModel: HomeViewModel,
    forgotPasswordViewModel: ForgotPasswordViewModel
) {
    val usuario by sessionViewModel.usuario.collectAsState()
    LaunchedEffect(usuario) {
        if (usuario != null) {
            homeViewModel.cargarDesdeLogin(usuario!!, sessionViewModel)
        }
    }

    val tieneAcceso by sessionViewModel.tieneAccesoABitacora.collectAsState()

    val startDestination = when (tieneAcceso) {
        true -> "home"
        false -> "denegado"
        null -> "login" // ← esto asegura inicio limpio
    }


    val signupViewModel: SignupViewModel = viewModel()
    val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel()

    NavHost(navController = navController, startDestination = startDestination) {

        composable("login") {
            LoginScreen(
                loginViewModel = loginViewModel,
                sessionViewModel = sessionViewModel,
                homeViewModel = homeViewModel, // ⬅️ IMPORTANTE
                onLoginSuccess = { navController.navigate("home") { popUpTo("login") { inclusive = true } } },
                onLoginDenied = { navController.navigate("denegado") { popUpTo("login") { inclusive = true } } },
                onAwaitCode = { navController.navigate("verify") },
                onRegisterClick = { navController.navigate("register") },
                onForgotPasswordClick = { navController.navigate("forgot/email") }
            )
        }

        composable("register") {
            SignupScreen(
                signupViewModel = signupViewModel,
                sessionViewModel = sessionViewModel,
                onAwaitCode = { navController.navigate("verify") },
                onLoginClick = { navController.popBackStack() }
            )
        }

        composable("verify") {
            VerificationScreen(
                signupViewModel = signupViewModel,
                sessionViewModel = sessionViewModel,
                homeViewModel = homeViewModel,
                onVerified = {
                    navController.navigate("home") { popUpTo("login") { inclusive = true } }
                }
            )
        }

        composable("forgot/email") {
            PasswordRequestScreen(
                viewModel = forgotPasswordViewModel,
                sessionViewModel = sessionViewModel,
                onAwaitCode = { navController.navigate("forgot/reset") }
            )
        }

        composable("forgot/reset") {
            PasswordResetScreen(
                viewModel = forgotPasswordViewModel,
                sessionViewModel = sessionViewModel,
                onSuccess = {
                    navController.navigate("login") { popUpTo("login") { inclusive = true } }

                }
            )
        }
        composable("visitas") {
            VisitasScreen(
                permisos = homeViewModel.perimetroSeleccionado.value?.modulos?.get("Registros de Visitas") ?: emptyList(),
                navController = navController // ✅ aquí lo pasas
            )
        }

        composable("visitas/manual") {
            val perimetroId = homeViewModel.perimetroSeleccionado.value?.perimetroId ?: return@composable

            RegistroVisitaWizardScreen(perimetroId = perimetroId, navController = navController)
        }

        composable("visitas/qr") {
            val perimetroId = homeViewModel.perimetroSeleccionado.value?.perimetroId ?: return@composable
            RegistroQRScreen(perimetroId = perimetroId, navController = navController)
        }

        composable("qr") {
            CodigosQRScreen(
                homeViewModel = homeViewModel,
                permisos = homeViewModel.perimetroSeleccionado.value?.modulos?.get("Códigos QR") ?: emptyList(),
                navController = navController
            )
        }

        composable("qr/seguimiento/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toIntOrNull() ?: return@composable
            SeguimientoQRScreen(
                idInvitacion = id,
                permisos = homeViewModel.perimetroSeleccionado.value?.modulos?.get("Códigos QR") ?: emptyList(),
                navController = navController
            )
        }

        composable("qr/generar") {
            GenerarCodigoQRScreen(navController)
        }

        composable("perimetros") {
            val perimetroId = homeViewModel.perimetroSeleccionado.value?.perimetroId ?: return@composable
            PerimetrosScreen(
                perimetroId = perimetroId,
                permisos = homeViewModel.perimetroSeleccionado.value?.modulos?.get("Perímetro") ?: emptyList(),
                navController = navController
            )
        }

        composable("accesos") {
            AccesosScreen(
                homeViewModel = homeViewModel,
                permisos = homeViewModel.perimetroSeleccionado.value?.modulos?.get("Accesos") ?: emptyList(),
                navController = navController
            )
        }

        composable("residentes") {
            ResidentesScreen(
                homeViewModel = homeViewModel,
                permisos = homeViewModel.perimetroSeleccionado.value?.modulos?.get("Residentes") ?: emptyList(),
                navController = navController
            )
        }

        composable("home") {
            HomeScreen(
                homeViewModel = homeViewModel,
                sessionViewModel = sessionViewModel,
                onConfiguracionClick = { navController.navigate("configuracion") },
                navController = navController
            )
        }

        composable("dashboard") {
            DashboardScreen(navController = navController)
        }


        composable("configuracion") {
            ConfiguracionScreen(
                sessionViewModel = sessionViewModel,
                onCerrarSesion = {
                    navController.navigate("login") {
                        popUpTo("configuracion") { inclusive = true }
                    }
                },
                onHomeClick = {
                    navController.navigate("home") {
                        popUpTo("configuracion") { inclusive = true }
                    }
                },
                onResetPassword = { navController.navigate("forgot/email") }
            )
        }

        composable("denegado") {
            AccesoDenegadoScreen()
        }
    }
}
