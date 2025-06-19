// 📁 ui/screens/registrovisita/PasoFinal.kt
package com.example.bitacoradigital.ui.screens.registrovisita

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel

@Composable
fun PasoFinal(viewModel: RegistroVisitaViewModel) {
    val respuesta by viewModel.respuestaRegistro.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✅ ¡Registro Completado!", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Tu registro ha sido procesado exitosamente.\nEl residente será notificado.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        respuesta?.let {
            Text(it, fontSize = 20.sp)
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = { viewModel.reiniciar() }, modifier = Modifier.fillMaxWidth()) {
            Text("Registrar otra visita")
        }
    }
}
