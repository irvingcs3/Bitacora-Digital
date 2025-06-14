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
import com.example.bitacoradigital.ui.screens.perimetro.PerimetrosScreen
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

            RegistroVisitaWizardScreen(perimetroId = perimetroId) // ← ya no pases nada aquí
        }

        composable("visitas/qr") {
            RegistroQRScreen()
        }

        composable("perimetros") {
            val perimetroId = homeViewModel.perimetroSeleccionado.value?.perimetroId ?: return@composable
            PerimetrosScreen(
                perimetroId = perimetroId,
                permisos = homeViewModel.perimetroSeleccionado.value?.modulos?.get("Perímetro") ?: emptyList()
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
                }
            )
        }

        composable("denegado") {
            AccesoDenegadoScreen()
        }
    }
}
