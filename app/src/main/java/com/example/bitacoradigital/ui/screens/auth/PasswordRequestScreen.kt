package com.example.bitacoradigital.ui.screens.auth


import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.viewmodel.ForgotPasswordViewModel
import com.example.bitacoradigital.viewmodel.SessionViewModel

@Composable
fun PasswordRequestScreen(
    viewModel: ForgotPasswordViewModel,
    sessionViewModel: SessionViewModel,
    onAwaitCode: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    val state = viewModel.state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Recuperar contraseña", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Correo electrónico") },
            singleLine = true,
            keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.requestCode(email, sessionViewModel, onAwaitCode) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enviar código")
        }

        state?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

    }
}
