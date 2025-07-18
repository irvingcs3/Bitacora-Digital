package com.example.bitacoradigital.ui.screens.novedades

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.Novedad
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.viewmodel.HomeViewModel
import com.example.bitacoradigital.viewmodel.NovedadesViewModel
import com.example.bitacoradigital.viewmodel.NovedadesViewModelFactory
import com.example.bitacoradigital.util.toReadableDateTime


@RequiresApi(Build.VERSION_CODES.O)
enum class FilterType { HORA, FECHA, AUTOR, CONTENIDO }

@Composable
fun autorColor(name: String): Color {
    val palette = listOf(

        Color(0xFFFFFFFF), // orange
        Color(0xFF000000),
        Color(0xFFD65930)
    )
    val index = abs(name.hashCode()) % palette.size
    return palette[index]
}

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
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
    val destacados by viewModel.destacados.collectAsState()

    var filterText by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf(FilterType.CONTENIDO) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState()

    val filtrados = remember(comentarios, filterText, filterType) {
        fun filtrar(n: Novedad): Novedad? {
            val match = when (filterType) {
                FilterType.AUTOR -> n.autor.contains(filterText, true)
                FilterType.FECHA, FilterType.HORA -> n.fecha_creacion.contains(filterText, true)
                FilterType.CONTENIDO -> n.contenido.contains(filterText, true)
            }
            val children = n.respuestas.mapNotNull { filtrar(it) }
            return if (match || children.isNotEmpty()) n.copy(respuestas = children) else null
        }
        if (filterText.isBlank()) comentarios else comentarios.mapNotNull { filtrar(it) }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        viewModel.cargarComentarios()
    }


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.cargarComentarios()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
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
        topBar = {
            SmallTopAppBar(
                title = { Text("Novedades") },
                actions = {
                    IconButton(onClick = { navController.navigate("destacados") }) {
                        Icon(Icons.Default.Star, contentDescription = null)
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            if (showDatePicker) {
                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let { millis ->
                                val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
                                filterText = date.toString()
                            }
                            showDatePicker = false
                        }) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }
            if (showTimePicker) {
                val context = LocalContext.current
                LaunchedEffect(Unit) {
                    val now = LocalTime.now()
                    val dialog = TimePickerDialog(
                        context,
                        { _, hour, minute ->
                            filterText = LocalTime.of(hour, minute).toString()
                            showTimePicker = false
                        },
                        now.hour,
                        now.minute,
                        true
                    )
                    dialog.setOnDismissListener { showTimePicker = false }
                    dialog.show()
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    label = { Text("Filtrar") },
                    modifier = Modifier.weight(1f),
                    readOnly = filterType == FilterType.FECHA || filterType == FilterType.HORA,
                    trailingIcon = {
                        when (filterType) {
                            FilterType.FECHA -> IconButton(onClick = { showDatePicker = true }) {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            }
                            FilterType.HORA -> IconButton(onClick = { showTimePicker = true }) {
                                Icon(Icons.Default.AccessTime, contentDescription = null)
                            }
                            else -> {}
                        }
                    }
                )
                Spacer(Modifier.width(8.dp))
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    TextButton(onClick = { menuOpen = true }) {
                        Text(filterType.name.lowercase().replaceFirstChar { it.uppercase() })
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        FilterType.values().forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt.name.lowercase().replaceFirstChar { c -> c.uppercase() }) },
                                onClick = {
                                    filterType = opt
                                    menuOpen = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            if (cargando) {
                CircularProgressIndicator()
            } else if (filtrados.isEmpty()) {
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
                val swipeState = rememberSwipeRefreshState(cargando)
                SwipeRefresh(
                    state = swipeState,
                    onRefresh = { viewModel.cargarComentarios() },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtrados, key = { it.id }) { c ->
                            ComentarioItem(
                                comentario = c,
                                nivel = 0,
                                destacados = destacados,
                                onToggleDestacado = { viewModel.toggleDestacado(it) },
                                onResponder = { id, texto, uri ->
                                    viewModel.publicarComentario(context, texto, uri, id)
                                }
                            )
                        }
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

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FullScreenImageDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ComentarioItem(
    comentario: Novedad,
    nivel: Int,
    destacados: Set<Int>,
    onToggleDestacado: (Int) -> Unit,
    onResponder: (Int, String, android.net.Uri?) -> Unit
) {
    var responder by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    var imagenRespuesta by remember { mutableStateOf<android.net.Uri?>(null) }
    var showImage by remember { mutableStateOf<String?>(null) }
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imagenRespuesta = uri }
    var expandido by remember { mutableStateOf(false) }
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
                        Text(
                            comentario.autor,
                            style = MaterialTheme.typography.titleLarge,
                            color = autorColor(comentario.autor)
                        )
                        Text(
                            comentario.fecha_creacion.toReadableDateTime(),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    IconButton(onClick = { onToggleDestacado(comentario.id) }) {
                        val marcado = destacados.contains(comentario.id)
                        Icon(
                            imageVector = if (marcado) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null
                        )                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    comentario.contenido,
                    style = MaterialTheme.typography.bodyLarge
                )
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
                            .height(150.dp)
                            .clickable { showImage = url },
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.height(4.dp))
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
                comentario.respuestas.forEachIndexed { index, child ->
                    ComentarioItem(
                        comentario = child,
                        nivel = nivel + 1,
                        destacados = destacados,
                        onToggleDestacado = onToggleDestacado,
                        onResponder = onResponder
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        showImage?.let { img ->
            FullScreenImageDialog(url = img) { showImage = null }
        }
    }
}
