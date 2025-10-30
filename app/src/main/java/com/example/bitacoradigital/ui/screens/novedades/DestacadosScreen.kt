package com.example.bitacoradigital.ui.screens.novedades

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.Novedad
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.viewmodel.HomeViewModel
import com.example.bitacoradigital.viewmodel.NovedadesViewModel
import com.example.bitacoradigital.viewmodel.NovedadesViewModelFactory
import androidx.navigation.NavHostController

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestacadosScreen(
    homeViewModel: HomeViewModel,
    permisos: List<String>,
    navController: NavHostController
) {
    val perimetro = homeViewModel.perimetroSeleccionado.collectAsState().value?.perimetroId ?: return
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: NovedadesViewModel = viewModel(factory = NovedadesViewModelFactory(prefs, perimetro))

    val comentarios by viewModel.comentarios.collectAsState()
    val destacados by viewModel.destacados.collectAsState()

    val puedeResponder = "Responder Comentario" in permisos
    val puedeEditar = "Editar Comentario" in permisos
    val puedeEliminar = "Borrar Comentario" in permisos
    val puedeVer = "Ver Novedades" in permisos

    LaunchedEffect(Unit) {
        if (puedeVer) viewModel.cargarComentarios()
    }

    val planos = remember(comentarios) {
        val list = mutableListOf<Novedad>()
        fun flat(n: Novedad) {
            list.add(n)
            n.respuestas.forEach { flat(it) }
        }
        comentarios.forEach { flat(it) }
        list
    }.filter { destacados.contains(it.id) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Destacados") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        },
        bottomBar = {
            HomeConfigNavBar(
                current = "",
                onHomeClick = { navController.navigate("home") },
                onConfigClick = { navController.navigate("configuracion") }
            )
        }
    ) { innerPadding ->
        if (!puedeVer) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Sin permisos para ver novedades")
            }
        } else if (planos.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Text("No hay mensajes destacados")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(planos, key = { it.id }) { c ->
                    ComentarioItem(
                        comentario = c,
                        nivel = 0,
                        destacados = destacados,
                        onToggleDestacado = { viewModel.toggleDestacado(it) },
                        onResponder = { id, texto, uri ->
                            viewModel.publicarComentario(context, texto, uri, id)
                        },
                        onEditar = { id, txt -> viewModel.editarComentario(id, txt) },
                        onEliminar = { viewModel.eliminarComentario(it) },
                        puedeResponder = puedeResponder,
                        puedeEditar = puedeEditar,
                        puedeEliminar = puedeEliminar
                    )
                }
            }
        }
    }
}
