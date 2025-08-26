package com.example.bitacoradigital.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.util.FeatureGate

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

            if ("bitacora general" in permisos) {
                PermisoCard(
                    icon = Icons.Default.PersonAdd,
                    titulo = "Registrar Visitas",
                    descripcion = "Captura datos completos de visitantes, proveedores o delivery.",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate("lomascountry/manual") }
                )

                PermisoCard(
                    icon = Icons.Default.QrCodeScanner,
                    titulo = "Registro por QR",
                    descripcion = "Escanea códigos QR de acceso rápido con verificación automática.",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate("lomascountry/qr") }
                )


            }else if ("Escaneo para Handhelds" in permisos){
                PermisoCard(
                    icon = Icons.Default.QrCodeScanner,
                    titulo = "Escaneo con Handheld",
                    descripcion = "Lectura de códigos con lector físico Zebra.",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { navController.navigate("lomascountry/handheld") }
                )
            }


            PermisoCard(
                icon = Icons.Default.QrCode,
                titulo = "Generar Código QR",
                descripcion = "Crea una invitación para acceso por QR",
                color = MaterialTheme.colorScheme.primary,
                onClick = { navController.navigate("lomascountry/qr/generar") }
            )

        }
    }
}
