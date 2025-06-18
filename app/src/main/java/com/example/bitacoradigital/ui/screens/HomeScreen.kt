package com.example.bitacoradigital.ui.screens
// 📁 ui/screens/HomeScreen.kt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.bitacoradigital.model.PerimetroVisual
import com.example.bitacoradigital.viewmodel.HomeViewModel
import com.example.bitacoradigital.viewmodel.SessionViewModel
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.ui.components.menu.ModuleButton
import com.example.bitacoradigital.ui.components.menu.moduloIcon

@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel,
    sessionViewModel: SessionViewModel,
    onConfiguracionClick: () -> Unit,
    navController: NavHostController

) {
    val perimetros by homeViewModel.perimetrosVisuales.collectAsState()
    val perimetroSeleccionado by homeViewModel.perimetroSeleccionado.collectAsState()
    val empresaSeleccionada by homeViewModel.empresaSeleccionada.collectAsState()

    Scaffold(
        topBar = {
            TopBar(
                perimetros = perimetros,
                seleccionado = perimetroSeleccionado,
                empresaSeleccionada = empresaSeleccionada,
                onSelectEmpresa = { homeViewModel.seleccionarEmpresa(it) },
                onSelectPerimetro = { homeViewModel.seleccionarPerimetro(it) },
                onFavorito = { homeViewModel.marcarFavorito(it.perimetroId, it.empresaId, sessionViewModel) }
            )
        },
        bottomBar = {
            HomeConfigNavBar(
                current = "home",
                onHomeClick = {},
                onConfigClick = onConfiguracionClick
            )
        }
    ) { innerPadding ->
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            perimetroSeleccionado?.let { activo ->
                Text(
                    text = "Bienvenidx a, ${activo.perimetroNombre}",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    text = "${activo.rol} de ${activo.perimetroNombre}",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    activo.modulos.keys.forEach { modulo ->
                        val icon = moduloIcon(modulo)
                        ModuleButton(title = modulo, icon = icon) {
                            when (modulo) {
                                "Registros de Visitas" -> navController.navigate("visitas")
                                "Perímetro" -> navController.navigate("perimetros")
                                "Códigos QR" -> navController.navigate("qr")
                                "Dashboard" -> navController.navigate("dashboard")
                                "Accesos" -> navController.navigate("accesos")
                                else -> { }
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    perimetros: List<PerimetroVisual>,
    seleccionado: PerimetroVisual?,
    empresaSeleccionada: Int?,
    onSelectEmpresa: (Int) -> Unit,
    onSelectPerimetro: (PerimetroVisual) -> Unit,
    onFavorito: (PerimetroVisual) -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                var expandedPerimetro by remember { mutableStateOf(false) }
                var expandedEmpresa by remember { mutableStateOf(false) }

                // Perímetro
                Box {
                    TextButton(onClick = { expandedPerimetro = true }) {
                        Text(seleccionado?.perimetroNombre ?: "")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(expanded = expandedPerimetro, onDismissRequest = { expandedPerimetro = false }) {
                        perimetros.filter { empresaSeleccionada == null || it.empresaId == empresaSeleccionada }.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.perimetroNombre) },
                                onClick = {
                                    onSelectPerimetro(item)
                                    expandedPerimetro = false
                                },
                                leadingIcon = {
                                    IconButton(onClick = { onFavorito(item) }) {
                                        Icon(
                                            imageVector = if (item.esFavorito) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = null
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Text("Bitácora", style = MaterialTheme.typography.titleMedium)
                Text("Digital", color = MaterialTheme.colorScheme.primary)

                Spacer(modifier = Modifier.width(16.dp))

                // Empresa
                Box {
                    TextButton(onClick = { expandedEmpresa = true }) {
                        val empresaNombre = perimetros.firstOrNull { it.empresaId == empresaSeleccionada }?.empresaNombre ?: ""
                        Text(empresaNombre)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(expanded = expandedEmpresa, onDismissRequest = { expandedEmpresa = false }) {
                        perimetros.map { it.empresaId to it.empresaNombre }.distinct().forEach { (id, nombre) ->
                            DropdownMenuItem(
                                text = { Text(nombre) },
                                onClick = {
                                    onSelectEmpresa(id)
                                    expandedEmpresa = false
                                }
                            )
                        }
                    }
                }
            }
        }
    )
}
