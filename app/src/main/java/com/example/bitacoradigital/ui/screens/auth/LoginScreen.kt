package com.example.bitacoradigital.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bitacoradigital.viewmodel.HomeViewModel
import com.example.bitacoradigital.viewmodel.LoginViewModel
import com.example.bitacoradigital.viewmodel.SessionViewModel

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel,
    sessionViewModel: SessionViewModel,
    homeViewModel: HomeViewModel, // ← NUEVO
    onLoginSuccess: () -> Unit,
    onLoginDenied: () -> Unit,
    onAwaitCode: () -> Unit = {},
    onRegisterClick: () -> Unit,
    onForgotPasswordClick: () -> Unit = {}
)
 {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    val loginState = loginViewModel.loginState
    val loading = loginViewModel.loading

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        // Logo
        Text("Bitácora Digital", style = MaterialTheme.typography.headlineSmall)
        Text("Gestión de comunidades residenciales", fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))

        Spacer(modifier = Modifier.height(24.dp))

        Text("Iniciar Sesión", style = MaterialTheme.typography.titleMedium)
        Text("Ingresa tus credenciales", fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = "Ver contraseña"
                    )
                }
            },
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                loginViewModel.login(
                    email = email,
                    password = password,
                    sessionViewModel = sessionViewModel,
                    onLoginSuccess = {
                        val user = sessionViewModel.usuario.value
                        if (user != null) {
                            homeViewModel.cargarDesdeLogin(user, sessionViewModel)
                        }
                        onLoginSuccess()
                    },
                    onLoginDenied = onLoginDenied,
                    onAwaitCode = onAwaitCode
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Login, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Iniciar Sesión")
        }

        Spacer(modifier = Modifier.height(8.dp))

        loginState?.let {
            Text(it, color = if (it.contains("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }

        Text(
            text = "¿Olvidaste tu contraseña?",
            modifier = Modifier
                .clickable { onForgotPasswordClick() }
                .padding(4.dp),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
        )

        Text(
            text = "¿No tienes cuenta? Regístrate aquí",
            modifier = Modifier
                .clickable { onRegisterClick() }
                .padding(4.dp),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp
        )
        }

        if (loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}
