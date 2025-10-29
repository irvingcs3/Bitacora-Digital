package com.example.bitacoradigital.ui.screens.novedades

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.navigationBarsPadding
import com.example.bitacoradigital.ui.theme.BrandOrange
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import kotlin.math.abs
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.launch
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import java.io.File
import androidx.compose.material3.*
import androidx.compose.material3.DatePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.bitacoradigital.util.Constants
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
    permisos: List<String>,
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

    val puedeResponder = "Responder Comentario" in permisos
    val puedeEditar = "Editar Comentario" in permisos
    val puedeEliminar = "Borrar Comentario" in permisos
    val puedeVer = "Ver Novedades" in permisos
    val puedePublicar = "Publicar Novedad" in permisos
    val puedeReporteIA = "reporte_con_ia" in permisos

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
        if (puedeVer) viewModel.cargarComentarios()
    }


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (puedeVer) viewModel.cargarComentarios()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    var nuevo by remember { mutableStateOf("") }
    var imagenNueva by remember { mutableStateOf<Uri?>(null) }
    var showCtpatDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val showScrollToTop by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 ||
                    listState.firstVisibleItemScrollOffset > 0
        }
    }
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> imagenNueva = uri }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    var tempUri by remember { mutableStateOf<Uri?>(null) }

    fun createImageUri(): Uri {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val image = File.createTempFile("comment_", ".jpg", imagesDir)
        return FileProvider.getUriForFile(
            context,
            Constants.FILE_PROVIDER_AUTHORITY,
            image
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success -> if (success) tempUri?.let { imagenNueva = it } }
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            AnimatedVisibility(showScrollToTop) {
                FloatingActionButton(
                    onClick = {
                        coroutineScope.launch { listState.animateScrollToItem(0) }
                    },
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp, end = 16.dp)
                        .zIndex(1f)
                ) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Ir al inicio")
                }
            }
        }
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
            if (!puedeVer) {
                Text("Sin permisos para ver novedades")
            } else if (cargando && comentarios.isEmpty()) {
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
                        state = listState,
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
            if (puedePublicar) {
                var mentionExpanded by remember { mutableStateOf(false) }
                var mentionQuery by remember { mutableStateOf("") }
                var mentionRange by remember { mutableStateOf<Pair<Int, Int>?>(null) }
                val mentionOptions = remember { listOf("ia", "asistencia") }
                val filteredMentionOptions = remember(mentionQuery) {
                    if (mentionQuery.isBlank()) mentionOptions else mentionOptions.filter {
                        it.startsWith(mentionQuery, ignoreCase = true)
                    }
                }
                LaunchedEffect(filteredMentionOptions) {
                    if (filteredMentionOptions.isEmpty()) {
                        mentionExpanded = false
                    }
                }
                LaunchedEffect(puedeReporteIA) {
                    if (!puedeReporteIA) {
                        mentionExpanded = false
                        mentionQuery = ""
                        mentionRange = null
                    }
                }
                ExposedDropdownMenuBox(
                    expanded = mentionExpanded && puedeReporteIA && filteredMentionOptions.isNotEmpty(),
                    onExpandedChange = { expanded ->
                        if (puedeReporteIA) {
                            mentionExpanded = expanded && filteredMentionOptions.isNotEmpty()
                        }
                    }
                ) {
                    OutlinedTextField(
                        value = nuevo,
                        onValueChange = { value ->
                            nuevo = value
                            if (puedeReporteIA) {
                                val caretIndex = value.length
                                val atIndex = value.lastIndexOf('@', caretIndex - 1)
                                if (atIndex >= 0) {
                                    val preceding = value.getOrNull(atIndex - 1)
                                    val mentionText = value.substring(atIndex + 1, caretIndex)
                                    val hasSpace = mentionText.any { it.isWhitespace() }
                                    if ((preceding == null || preceding.isWhitespace()) && !hasSpace) {
                                        mentionRange = atIndex to caretIndex
                                        mentionQuery = mentionText
                                        mentionExpanded = true
                                    } else {
                                        mentionRange = null
                                        mentionExpanded = false
                                        mentionQuery = ""
                                    }
                                } else {
                                    mentionRange = null
                                    mentionExpanded = false
                                    mentionQuery = ""
                                }
                            }
                        },
                        label = { Text("Nuevo comentario") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        trailingIcon = {
                            if (puedeReporteIA && mentionRange != null && filteredMentionOptions.isNotEmpty()) {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = mentionExpanded)
                            }
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = mentionExpanded && puedeReporteIA && filteredMentionOptions.isNotEmpty(),
                        onDismissRequest = { mentionExpanded = false }
                    ) {
                        filteredMentionOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text("@$option") },
                                onClick = {
                                    val range = mentionRange ?: (nuevo.length to nuevo.length)
                                    val start = range.first
                                    val end = range.second
                                    val before = nuevo.substring(0, start)
                                    val after = if (end <= nuevo.length) nuevo.substring(end) else ""
                                    nuevo = buildString {
                                        append(before)
                                        append("@${option} ")
                                        append(after)
                                    }
                                    mentionExpanded = false
                                    mentionQuery = ""
                                    mentionRange = null
                                }
                            )
                        }
                    }
                }
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
                    var attachMenu by remember { mutableStateOf(false) }
                    Box {
                        TextButton(onClick = { attachMenu = true }) {
                            Icon(Icons.Default.AttachFile, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Adjuntar")
                        }
                        DropdownMenu(expanded = attachMenu, onDismissRequest = { attachMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Imagen") },
                                onClick = {
                                    attachMenu = false
                                    imageLauncher.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Tomar foto") },
                                onClick = {
                                    attachMenu = false
                                    if (hasCameraPermission) {
                                        val uriTemp = createImageUri()
                                        tempUri = uriTemp
                                        cameraLauncher.launch(uriTemp)
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("CTPAT") },
                                onClick = {
                                    attachMenu = false
                                    showCtpatDialog = true
                                }
                            )
                        }
                    }
                    Button(
                        onClick = {
                            val trimmed = nuevo.trimStart()
                            when {
                                puedeReporteIA && trimmed.startsWith("@ia") -> {
                                    val prompt = trimmed.removePrefix("@ia").trimStart()
                                    if (prompt.isBlank()) {
                                        viewModel.mostrarError("Escribe una solicitud para @ia")
                                    } else {
                                        viewModel.publicarComentarioIA(nuevo, null)
                                        nuevo = ""
                                        imagenNueva = null
                                        mentionExpanded = false
                                        mentionQuery = ""
                                        mentionRange = null
                                    }
                                }
                                nuevo.contains("@asistencia") -> {
                                    if (imagenNueva != null) {
                                        viewModel.publicarComentario(context, nuevo, imagenNueva, null)
                                        nuevo = ""
                                        imagenNueva = null
                                        mentionExpanded = false
                                        mentionQuery = ""
                                        mentionRange = null
                                    }
                                }
                                else -> {
                                    viewModel.publicarComentario(context, nuevo, imagenNueva, null)
                                    nuevo = ""
                                    imagenNueva = null
                                    mentionExpanded = false
                                    mentionQuery = ""
                                    mentionRange = null
                                }
                            }
                        },
                        enabled = nuevo.isNotBlank()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Publicar")
                    }
                }
                if (showCtpatDialog) {
                    CtpatDialog { showCtpatDialog = false }
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun FullScreenImageDialog(url: String, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        val scale = remember { mutableStateOf(1f) }
        val transformState = rememberTransformableState { zoomChange, _, _ ->
            scale.value *= zoomChange
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale.value,
                        scaleY = scale.value
                    )
                    .transformable(transformState)
            )
            Icon(
                Icons.Default.ZoomIn,
                contentDescription = "Zoom",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
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
    onResponder: (Int, String, Uri?) -> Unit,
    onEditar: (Int, String) -> Unit,
    onEliminar: (Int) -> Unit,
    puedeResponder: Boolean,
    puedeEditar: Boolean,
    puedeEliminar: Boolean
) {
    var responder by remember { mutableStateOf(false) }
    var texto by remember { mutableStateOf("") }
    var imagenRespuesta by remember { mutableStateOf<Uri?>(null) }
    var showImage by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var editDialog by remember { mutableStateOf(false) }
    var editText by remember { mutableStateOf(comentario.contenido) }
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
        )
        {
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
                        comentario.tipo?.let { tipo ->
                            Spacer(Modifier.height(4.dp))
                            TipoBadge(tipo)
                        }
                    }
                    IconButton(onClick = { onToggleDestacado(comentario.id) }) {
                        val marcado = destacados.contains(comentario.id)
                        Icon(
                            imageVector = if (marcado) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null
                        )                    }
                    if (puedeEditar) {
                        IconButton(onClick = {
                            editText = comentario.contenido
                            editDialog = true
                        }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar")
                        }
                    }
                    if (puedeEliminar) {
                        IconButton(onClick = { confirmDelete = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
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
                if (puedeResponder) {
                    TextButton(onClick = { responder = !responder }) {
                        Text("Responder")
                    }
                }
                AnimatedVisibility(visible = responder && puedeResponder, enter = fadeIn(), exit = fadeOut()) {
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
                        onResponder = onResponder,
                        onEditar = onEditar,
                        onEliminar = onEliminar,
                        puedeResponder = puedeResponder,
                        puedeEditar = puedeEditar,
                        puedeEliminar = puedeEliminar
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        showImage?.let { img ->
            FullScreenImageDialog(url = img) { showImage = null }
        }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                confirmButton = {
                    TextButton(onClick = {
                        onEliminar(comentario.id)
                        confirmDelete = false
                    }) { Text("Eliminar") }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
                title = { Text("Eliminar comentario") },
                text = { Text("¿Seguro que deseas eliminar este comentario?") }
            )
        }
        if (editDialog) {
            AlertDialog(
                onDismissRequest = { editDialog = false },
                confirmButton = {
                    TextButton(onClick = {
                        onEditar(comentario.id, editText)
                        editDialog = false
                    }) { Text("Guardar") }
                },
                dismissButton = { TextButton(onClick = { editDialog = false }) { Text("Cancelar") } },
                title = { Text("Editar comentario") },
                text = {
                    OutlinedTextField(
                        value = editText,
                        onValueChange = { editText = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            )
        }
    }
}
@Composable
private fun TipoBadge(tipo: String) {
    val normalized = tipo
        .lowercase()
        .replace('_', ' ')
        .split(" ")
        .filter { it.isNotBlank() }
        .joinToString(" ") { token -> token.replaceFirstChar { c -> c.uppercase() } }
        .ifEmpty { tipo }
    val uppercase = tipo.uppercase()
    val (containerColor, labelColor) = when (uppercase) {
        "INCIDENCIA" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        "CONSIGNA" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        "RESPUESTA" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) to MaterialTheme.colorScheme.primary
    }
    AssistChip(
        onClick = {},
        enabled = false,
        border = null,
        label = { Text(normalized) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = labelColor,
            disabledContainerColor = containerColor,
            disabledLabelColor = labelColor
        )
    )
}

enum class CtpatEstado(val label: String) { CONFORME("Conforme"), NO_CONFORME("No conforme"), NA("N/A") }

private val ctpatItems = listOf(
    "Parachoques (revisión con linterna y espejo internos)",
    "Motor (especialmente rincones, con linterna/espejo)",
    "Llantas (incluye repuesto; vibración y revisión de huecos)",
    "Piso del camión (sin personas ocultas; tapetes levantados)",
    "Tanque de gasolina (golpeo para comprobar huecos, interior con linterna)",
    "Compartimentos de almacenamiento (interior/exterior)",
    "Tanques de aire (golpeo y revisión de soldaduras/marcas)",
    "Ejes de accionamiento (sin reparaciones recientes, golpeo)",
    "Quinta rueda (espacios limpios; área de batería segura)",
    "Exteriores y chasis (uso de espejo en labio interno y luces traseras)",
    "Puertas interior/exterior (funcionamiento de pernos y remaches)",
    "Piso del tráiler (tablas planas y atornilladas)",
    "Muros laterales (paneles sin daños, revisados con linterna)",
    "Pared frontal (sin reparaciones recientes, sin paredes falsas)",
    "Techo (altura y remaches en buena condición)",
    "Unidad de refrigeración (contenido y estado interior con linterna)",
    "Escape (cuerda firme, empaques bien grabados)"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CtpatDialog(onDismiss: () -> Unit) {
    val estados = remember { ctpatItems.map { mutableStateOf<CtpatEstado?>(null) } }
    val observaciones = remember { ctpatItems.map { mutableStateOf("") } }
    var comentarioGeneral by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Scaffold(
                topBar = { TopAppBar(title = { Text("Checklist de inspección") }) },
                floatingActionButton = {
                    FloatingActionButton(onClick = onDismiss) {
                        Icon(Icons.Default.Send, contentDescription = "Enviar")
                    }
                }
            ) { innerPadding ->
                LazyColumn(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(ctpatItems) { index, item ->
                        Card(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(4.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BrandOrange
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = item,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = BrandOrange
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CtpatEstado.values().forEach { estado ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            RadioButton(
                                                selected = estados[index].value == estado,
                                                onClick = { estados[index].value = estado }
                                            )
                                            val icon = when (estado) {
                                                CtpatEstado.CONFORME -> Icons.Default.Check
                                                CtpatEstado.NO_CONFORME -> Icons.Default.Close
                                                CtpatEstado.NA -> Icons.Default.Remove
                                            }
                                            Icon(icon, contentDescription = estado.label)
                                            Spacer(Modifier.width(4.dp))
                                            Text(estado.label)
                                        }
                                    }
                                }
                                if (estados[index].value == CtpatEstado.NO_CONFORME) {
                                    Spacer(Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = observaciones[index].value,
                                        onValueChange = { observaciones[index].value = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Observaciones") }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = comentarioGeneral,
                            onValueChange = { comentarioGeneral = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Comentario general") }
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) { Text("Cerrar") }
                        }
                    }
                }
            }
        }
    }
}