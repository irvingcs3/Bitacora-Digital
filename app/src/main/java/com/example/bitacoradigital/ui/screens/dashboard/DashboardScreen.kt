package com.example.bitacoradigital.ui.screens.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.InvitacionEstado
import com.example.bitacoradigital.model.VisitasDia
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.ui.screens.dashboard.DashboardViewModel
import com.example.bitacoradigital.viewmodel.DashboardViewModelFactory
import com.example.bitacoradigital.viewmodel.HomeViewModel

@Composable
fun DashboardScreen(
    homeViewModel: HomeViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val perimetroId = homeViewModel.perimetroSeleccionado.collectAsState().value?.perimetroId ?: return
    val viewModel: DashboardViewModel = viewModel(factory = DashboardViewModelFactory(prefs, perimetroId))

    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargarDatos() }

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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard(
                    title = "Residentes",
                    value = state.counts?.total_residentes ?: 0,
                    icon = Icons.Default.People
                )
                DashboardCard(
                    title = "Visitas hoy",
                    value = state.counts?.visitas_hoy ?: 0,
                    icon = Icons.Default.CalendarToday
                )
                DashboardCard(
                    title = "QRs",
                    value = state.counts?.total_qrs ?: 0,
                    icon = Icons.Default.QrCode
                )
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Estado de Invitaciones por QR", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    InvitacionesChart(state.invitaciones)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Registros de Accesos Semanal", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    VisitasBarChart(state.visitasSemana)
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tendencia Semanal de Accesos", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LineChart(state.visitasLinea, Color(0xFF1976D2))
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Tendencia Accesos por QR", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    LineChart(state.visitasLineaQr, Color(0xFFE65100))
                }
            }
        }
    }
}

@Composable
private fun RowScope.DashboardCard(title: String, value: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(title, color = Color.Gray)
                Text(value.toString(), style = MaterialTheme.typography.titleLarge)
            }
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}

@Composable
private fun InvitacionesChart(data: List<InvitacionEstado>) {
    val activa = data.firstOrNull { it.estado == "Activa" }?.cantidad ?: 0
    val expirada = data.firstOrNull { it.estado == "Expirada" }?.cantidad ?: 0
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Bar(color = Color(0xFF2196F3), value = activa)
        Bar(color = Color(0xFFF44336), value = expirada)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Activa: $activa", color = Color(0xFF2196F3))
        Text("Expirada: $expirada", color = Color(0xFFF44336))
    }
}

@Composable
private fun RowScope.Bar(color: Color, value: Int) {
    Canvas(modifier = Modifier
        .weight(1f)
        .height(80.dp)) {
        val barHeight = size.height * (value / 10f)
        drawRect(color, topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - barHeight), size = androidx.compose.ui.geometry.Size(size.width, barHeight))
    }
}

@Composable
private fun VisitasBarChart(data: List<VisitasDia>) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        data.forEach { dia ->
            Canvas(modifier = Modifier
                .weight(1f)
                .height(80.dp)) {
                val barHeight = size.height * (dia.visitas / 10f)
                drawRect(Color(0xFF1976D2), topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - barHeight), size = androidx.compose.ui.geometry.Size(size.width, barHeight))
            }
        }
    }
}

@Composable
private fun LineChart(data: List<VisitasDia>, color: Color) {
    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(120.dp)) {
        if (data.isEmpty()) return@Canvas
        val max = data.maxOf { it.visitas }.coerceAtLeast(1)
        val stepX = size.width / (data.size - 1)
        val points = data.mapIndexed { index, d ->
            androidx.compose.ui.geometry.Offset(stepX * index, size.height - (d.visitas / max.toFloat()) * size.height)
        }
        for (i in 0 until points.size - 1) {
            drawLine(color, points[i], points[i + 1], strokeWidth = 4f, cap = StrokeCap.Round)
        }
        points.forEach { p ->
            drawCircle(color, 6f, p)
        }
    }
}
