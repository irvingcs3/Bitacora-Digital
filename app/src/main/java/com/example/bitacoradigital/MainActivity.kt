package com.example.bitacoradigital

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.*
import androidx.navigation.compose.rememberNavController
import com.example.bitacoradigital.navigation.AppNavGraph
import com.example.bitacoradigital.ui.theme.BitacoraDigitalTheme
import com.example.bitacoradigital.viewmodel.HomeViewModel
import com.example.bitacoradigital.viewmodel.LoginViewModel
import com.example.bitacoradigital.viewmodel.SessionViewModel
import com.example.bitacoradigital.viewmodel.ForgotPasswordViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BitacoraDigitalTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    val navController = rememberNavController()

                    // ViewModels
                    val loginViewModel: LoginViewModel = viewModel()
                    val sessionViewModel: SessionViewModel = viewModel(factory = SessionViewModel.Factory(applicationContext))
                    val homeViewModel: HomeViewModel = viewModel()
                    val forgotPasswordViewModel: ForgotPasswordViewModel = viewModel()

                    AppNavGraph(
                        navController = navController,
                        loginViewModel = loginViewModel,
                        sessionViewModel = sessionViewModel,
                        homeViewModel = homeViewModel,
                        forgotPasswordViewModel = forgotPasswordViewModel
                    )
                }
            }
        }
    }
}
