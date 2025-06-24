package com.example.bitacoradigital.ui.screens.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.InvitacionEstado
import com.example.bitacoradigital.model.VisitasDia
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.ui.theme.DashboardBackground
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
        containerColor = DashboardBackground,
        bottomBar = {
            HomeConfigNavBar(
                current = "",
                onHomeClick = { navController.navigate("home") },
                onConfigClick = { navController.navigate("configuracion") }
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val largeScreen = maxWidth > 600.dp
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    if (largeScreen) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardCard(
                                title = "Residentes",
                                value = state.counts?.total_residentes ?: 0,
                                icon = Icons.Default.People,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardCard(
                                title = "Visitas hoy",
                                value = state.counts?.visitas_hoy ?: 0,
                                icon = Icons.Default.CalendarToday,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardCard(
                                title = "QRs",
                                value = state.counts?.total_qrs ?: 0,
                                icon = Icons.Default.QrCode,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DashboardCard(
                                title = "Residentes",
                                value = state.counts?.total_residentes ?: 0,
                                icon = Icons.Default.People,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardCard(
                                title = "Visitas hoy",
                                value = state.counts?.visitas_hoy ?: 0,
                                icon = Icons.Default.CalendarToday,
                                modifier = Modifier.weight(1f)
                            )
                            DashboardCard(
                                title = "QRs",
                                value = state.counts?.total_qrs ?: 0,
                                icon = Icons.Default.QrCode,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.elevatedCardElevation()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Estado de Invitaciones por QR",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            InvitacionesChart(state.invitaciones)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.elevatedCardElevation()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Registros de Accesos Semanal",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            VisitasBarChart(state.visitasSemana)
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.elevatedCardElevation()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Tendencia Semanal de Accesos",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            LineChart(state.visitasLinea, Color(0xFF1976D2))
                        }
                    }
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.elevatedCardElevation()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Tendencia Accesos por QR",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(8.dp))
                            LineChart(state.visitasLineaQr, Color(0xFFE65100))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DashboardCard(
    title: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier
            .animateContentSize()
            .clickable { expanded = !expanded },
        colors = CardDefaults.elevatedCardColors(),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelLarge
                )
                Crossfade(targetState = value) { v ->
                    Text(v.toString(), style = MaterialTheme.typography.titleLarge)
                }
                AnimatedVisibility(visible = expanded) {
                    Text(
                        "Último acceso hoy",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitacionesChart(data: List<InvitacionEstado>) {
    val activa = data.firstOrNull { it.estado == "Activa" }?.cantidad ?: 0
    val expirada = data.firstOrNull { it.estado == "Expirada" }?.cantidad ?: 0
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        BarWithLabel("Activa", activa, Color(0xFF2196F3))
        BarWithLabel("Expirada", expirada, Color(0xFFF44336))
    }
}

@Composable
private fun RowScope.BarWithLabel(label: String, value: Int, color: Color) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Bar(color = color, value = value)
        Spacer(Modifier.height(4.dp))
        Text("$value", color = color, style = MaterialTheme.typography.labelLarge)
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun Bar(color: Color, value: Int) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        val barHeight = size.height * (value / 10f)
        drawRect(
            color,
            topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - barHeight),
            size = androidx.compose.ui.geometry.Size(size.width, barHeight)
        )
    }
}

@Composable
private fun VisitasBarChart(data: List<VisitasDia>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        data.forEach { dia ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Bar(color = Color(0xFF1976D2), value = dia.visitas)
                Spacer(Modifier.height(4.dp))
                Text(dia.name, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun LineChart(data: List<VisitasDia>, color: Color) {
    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .pointerInput(Unit) {}
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            data.forEach { dia ->
                Text(dia.name, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
