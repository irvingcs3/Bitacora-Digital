package com.example.bitacoradigital.ui.screens.guardia

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.bitacoradigital.ui.components.HomeConfigNavBar

@Composable
fun GuardiaScreen(permisos: List<String>, navController: NavHostController) {
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Guardia",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            val opciones = listOf(
                Opcion("boton de pánico", "Botón de pánico", "Envía alerta inmediata", Icons.Default.Warning, "dronguard"),
                Opcion("Asistencia", "Asistencia", "Registra entradas y salidas", Icons.Default.AccessTime, null),
                Opcion("Definir Turnos", "Definir Turnos", "Configura los horarios", Icons.Default.CalendarToday, null),
                Opcion("Inventario De Guardias", "Inventario de Guardias", "Gestiona personal disponible", Icons.Default.Inventory, null),
                Opcion("Pase de lista", "Pase de lista", "Confirma la asistencia", Icons.Default.ListAlt, null),
                Opcion("Estado De Fuerza", "Estado de Fuerza", "Resumen de elementos", Icons.Default.People, null)
            )

            opciones.filter { it.permiso in permisos }.forEach { op ->
                GuardiaCard(
                    icon = op.icon,
                    title = op.title,
                    description = op.descripcion,
                    onClick = {
                        op.ruta?.let { navController.navigate(it) }
                    }
                )
            }
        }
    }
}

@Composable
private fun GuardiaCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed = interactionSource.collectIsPressedAsState().value
    val bgColor by animateColorAsState(
        if (pressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        else MaterialTheme.colorScheme.surface
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private data class Opcion(
    val permiso: String,
    val title: String,
    val descripcion: String,
    val icon: ImageVector,
    val ruta: String?
)
