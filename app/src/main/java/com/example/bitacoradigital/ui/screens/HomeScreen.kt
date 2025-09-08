package com.example.bitacoradigital.ui.screens
// 📁 ui/screens/HomeScreen.kt

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import com.example.bitacoradigital.R
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

                // Agrupaciones de módulos visibles
                val ocultos = listOf(
                    "Tags Vehiculares",
                    "Eventos",
                    "Administración de Usuarios",
                    "Ajustes"
                )

                val perimetroMods = listOf("Dashboard", "Perímetro", "Residentes", "Accesos")
                val accesoMods = listOf("Códigos QR", "Registros de Visitas", "Lomas Country")
                val guardiaMods = listOf("Guardia")
                val novedadesMods = listOf("Novedades")
                val dronGuardMods = listOf("DronGuard")
                val amenidadesMods = activo.modulos["Amenidades"] ?: emptyList()


                val modulosVisibles = activo.modulos.keys.filterNot { it in ocultos }

                val modsPerimetro = modulosVisibles.filter { it in perimetroMods }
                val modsAcceso = modulosVisibles.filter { it in accesoMods }
                val modsGuardia = modulosVisibles.filter {it in guardiaMods}
                val modsNovedades = modulosVisibles.filter {it in novedadesMods}
                val modsDronGuard = modulosVisibles.filter { it in dronGuardMods }

                Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
                    if (modsPerimetro.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Perímetros", style = MaterialTheme.typography.titleMedium)
                                modsPerimetro.forEach { modulo ->
                                    val icon = moduloIcon(modulo)
                                    ModuleButton(title = modulo, icon = icon) {
                                        when (modulo) {
                                            "Registros de Visitas" -> navController.navigate("visitas")
                                            "Lomas Country" -> navController.navigate("lomascountry")
                                            "Perímetro" -> navController.navigate("perimetros")
                                            "Códigos QR" -> navController.navigate("qr")
                                            "Dashboard" -> navController.navigate("dashboard")
                                            "Accesos" -> navController.navigate("accesos")
                                            "Residentes" -> navController.navigate("residentes")
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (modsAcceso.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Control de Acceso", style = MaterialTheme.typography.titleMedium)
                                modsAcceso.forEach { modulo ->
                                    val icon = moduloIcon(modulo)
                                    ModuleButton(title = modulo, icon = icon) {
                                        when (modulo) {
                                            "Registros de Visitas" -> navController.navigate("visitas")
                                            "Lomas Country" -> navController.navigate("lomascountry")
                                            "Perímetro" -> navController.navigate("perimetros")
                                            "Códigos QR" -> navController.navigate("qr")
                                            "Dashboard" -> navController.navigate("dashboard")
                                            "Accesos" -> navController.navigate("accesos")
                                            "Residentes" -> navController.navigate("residentes")
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (modsGuardia.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Guardia", style = MaterialTheme.typography.titleMedium)
                                modsGuardia.forEach { modulo ->
                                    val icon = moduloIcon(modulo)
                                    ModuleButton(title = modulo, icon = icon) {
                                        when (modulo) {
                                            "Guardia" -> navController.navigate("guardia")
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (modsNovedades.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Novedades", style = MaterialTheme.typography.titleMedium)
                                modsNovedades.forEach { modulo ->
                                    val icon = moduloIcon(modulo)
                                    ModuleButton(title = modulo, icon = icon) {
                                        when (modulo) {
                                            "Novedades" -> navController.navigate("novedades")
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (modsDronGuard.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("DronGuard", style = MaterialTheme.typography.titleMedium)
                                modsDronGuard.forEach { modulo ->
                                    val icon = moduloIcon(modulo)
                                    ModuleButton(title = modulo, icon = icon) {
                                        when (modulo) {
                                            "DronGuard" -> navController.navigate("dronGuard")
                                            else -> {}
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (amenidadesMods.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Amenidades", style = MaterialTheme.typography.titleMedium)
                                amenidadesMods.forEach { modulo ->
                                    val icon = moduloIcon(modulo)
                                    ModuleButton(title = modulo, icon = icon) {
                                        when (modulo) {
                                            "Calendario de Actividades" -> navController.navigate("amenidades/calendario")
                                            else -> {}
                                        }
                                    }
                                }
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
            Box(modifier = Modifier.fillMaxWidth()) {
                var expandedPerimetro by remember { mutableStateOf(false) }
                var expandedEmpresa by remember { mutableStateOf(false) }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
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
                }

                Image(
                    painter = painterResource(id = R.drawable.logo_topbar),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )

                Box(modifier = Modifier.align(Alignment.CenterEnd)) {
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
