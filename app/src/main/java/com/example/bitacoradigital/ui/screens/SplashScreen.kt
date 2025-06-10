package com.example.bitacoradigital.ui.screens

// 📁 ui/screens/SplashScreen.kt

import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay
import com.example.bitacoradigital.viewmodel.SessionViewModel

@Composable
fun SplashScreen(
    navController: NavHostController,
    sessionViewModel: SessionViewModel
) {
    val token by sessionViewModel.sessionToken.collectAsState(initial = null)
    val usuario by sessionViewModel.usuario.collectAsState()

    LaunchedEffect(token, usuario) {
        if (token != null && usuario != null) {
            navController.navigate("home") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("login") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }
}

