package com.example.bitacoradigital.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.animation.animateContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavHostController
import com.example.bitacoradigital.ui.components.HomeConfigNavBar

@Composable
fun LomasCountryScreen(
    permisos: List<String>,
    navController: NavHostController
) {
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
            text = "Lomas Country",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        if ("Registrar Visitas" in permisos) {
            PermisoCardLC(
                icon = Icons.Default.PersonAdd,
                titulo = "Registrar Visitas",
                descripcion = "Captura datos completos de visitantes, proveedores o delivery.",
                color = MaterialTheme.colorScheme.primary,
                onClick = { navController.navigate("lomas/manual") }
            )
        }

        if ("Registro por QR" in permisos) {
            PermisoCardLC(
                icon = Icons.Default.QrCodeScanner,
                titulo = "Registro por QR",
                descripcion = "Escanea códigos QR de acceso rápido con verificación automática.",
                color = MaterialTheme.colorScheme.primary,
                onClick = { navController.navigate("lomas/qr") }
            )
        }

    }
}

}

@Composable
fun PermisoCardLC(
    icon: ImageVector,
    titulo: String,
    descripcion: String,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(170.dp)
            .animateContentSize()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f), shape = RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "ACTIVO",
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Column {
                Text(
                    titulo,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    descripcion,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f),
                    fontSize = 13.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    "\u2022  Toca para acceder",
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
fun RegistrarVisitaManualScreenLC() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Pantalla: Registrar Visita Manual")
    }
}
