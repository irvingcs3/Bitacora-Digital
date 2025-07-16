package com.example.bitacoradigital.ui.screens.novedades

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.Novedad
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.viewmodel.HomeViewModel
import com.example.bitacoradigital.viewmodel.NovedadesViewModel
import com.example.bitacoradigital.viewmodel.NovedadesViewModelFactory

@Composable
fun NovedadesScreen(
    homeViewModel: HomeViewModel,
    navController: NavHostController
) {
    val perimetro = homeViewModel.perimetroSeleccionado.collectAsState().value?.perimetroId ?: return
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: NovedadesViewModel = viewModel(factory = NovedadesViewModelFactory(prefs, perimetro))

    val comentarios by viewModel.comentarios.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(Unit) { viewModel.cargarComentarios() }

    var nuevo by remember { mutableStateOf("") }
    var imagenNueva by remember { mutableStateOf<android.net.Uri?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imagenNueva = uri }
    val snackbarHostState = remember { SnackbarHostState() }
    error?.let { msg ->
        LaunchedEffect(msg) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    Scaffold(
        bottomBar = {
            HomeConfigNavBar(
                current = "",
                onHomeClick = { navController.navigate("home") },
                onConfigClick = { navController.navigate("configuracion") }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Text("Novedades", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            if (cargando) {
                CircularProgressIndicator()
            } else if (comentarios.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text("Aun no hay novedades")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(comentarios, key = { it.id }) { c ->
                        ComentarioItem(
                            comentario = c,
                            nivel = 0,
                            onResponder = { id, texto, uri ->
                                viewModel.publicarComentario(context, texto, uri, id)
                            }
                        )
                    }
                }
            }
            OutlinedTextField(
                value = nuevo,
                onValueChange = { nuevo = it },
                label = { Text("Nuevo comentario") },
                modifier = Modifier.fillMaxWidth()
            )
            imagenNueva?.let { uri ->
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = uri,
                        contentDescription = null,
                        modifier = Modifier
                            .size(64.dp)
                            .padding(end = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(onClick = { imagenNueva = null }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { imageLauncher.launch("image/*") }) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Agregar imagen")
                }
                Button(
                    onClick = {
                    viewModel.publicarComentario(context, nuevo, imagenNueva, null)
                    nuevo = ""
                    imagenNueva = null
                },
                enabled = nuevo.isNotBlank()
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Publicar")
            }
            }
        }
    }
}

@Composable
fun ComentarioItem(
    comentario: Novedad,
    nivel: Int,
    onResponder: (Int, String, android.net.Uri?) -> Unit
) {
    var responder by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    var imagenRespuesta by remember { mutableStateOf<android.net.Uri?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imagenRespuesta = uri }
    var expandido by remember { mutableStateOf(true) }
    val rotacion by animateFloatAsState(if (expandido) 90f else 0f, label = "rotacion")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = (nivel * 16).dp)
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .animateContentSize(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Column(Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (comentario.respuestas.isNotEmpty()) {
                        IconButton(onClick = { expandido = !expandido }) {
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.rotate(rotacion)
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text("Autor: ${'$'}{comentario.autor}", style = MaterialTheme.typography.labelMedium)
                        Text(comentario.contenido, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                comentario.imagen?.let { url ->
                    Spacer(Modifier.height(4.dp))
                    val request = ImageRequest.Builder(LocalContext.current)
                        .data(url)
                        .crossfade(true)
                        .build()
                    AsyncImage(
                        model = request,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        contentScale = ContentScale.Crop
                    )
                }
                Text(
                    comentario.fecha_creacion,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End)
                )
                TextButton(onClick = { responder = !responder }) {
                    Text("Responder")
                }
                AnimatedVisibility(visible = responder, enter = fadeIn(), exit = fadeOut()) {
                    Column {
                        OutlinedTextField(
                            value = texto,
                            onValueChange = { texto = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Escribe una respuesta") }
                        )
                        imagenRespuesta?.let { uri ->
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(end = 8.dp),
                                    contentScale = ContentScale.Crop
                                )
                                IconButton(onClick = { imagenRespuesta = null }) {
                                    Icon(Icons.Default.Close, contentDescription = null)
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { imageLauncher.launch("image/*") }) {
                                Icon(Icons.Default.Image, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Agregar imagen")
                            }
                        Button(
                            onClick = {
                                onResponder(comentario.id, texto, imagenRespuesta)
                                texto = ""
                                imagenRespuesta = null
                                responder = false
                            },
                            enabled = texto.isNotBlank(),
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                        ) {
                            Text("Enviar")
                        }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(visible = expandido) {
            Column {
                Spacer(Modifier.height(8.dp))
                comentario.respuestas.forEach { child ->
                    ComentarioItem(comentario = child, nivel = nivel + 1, onResponder = onResponder)
                }
            }
        }
    }
}
