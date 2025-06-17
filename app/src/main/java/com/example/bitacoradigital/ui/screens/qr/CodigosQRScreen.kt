package com.example.bitacoradigital.ui.screens.qr

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.bitacoradigital.ui.components.HomeConfigNavBar

@Composable
fun CodigosQRScreen(permisos: List<String>, navController: NavHostController) {
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Códigos QR",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if ("Generar Código QR" in permisos) {
            PermisoCard(
                titulo = "Generar Código QR",
                descripcion = "Crea una invitación para acceso por QR",
                onClick = { navController.navigate("qr/generar") }
            )
        }
    }
    }
}

@Composable
private fun PermisoCard(titulo: String, descripcion: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2979FF)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(titulo, color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(descripcion, color = Color.White.copy(alpha = 0.9f))
        }
    }
}
