package com.example.bitacoradigital.ui.screens.handheld

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.model.Checkpoint
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.viewmodel.EscaneoHandheldViewModel
import com.example.bitacoradigital.viewmodel.EscaneoHandheldViewModelFactory
import kotlinx.coroutines.delay

@Composable
fun EscaneoHandheldScreen(perimetroId: Int, navController: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: EscaneoHandheldViewModel = viewModel(factory = EscaneoHandheldViewModelFactory(prefs, perimetroId))

    val checkpoints by viewModel.checkpoints.collectAsState()
    val seleccionado by viewModel.seleccionado.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val resultado by viewModel.resultado.collectAsState()
    val scannedText by viewModel.scannedText.collectAsState()
    val networkError by viewModel.networkError.collectAsState()
    val imagenCrop by viewModel.imagenCrop.collectAsState()
    val mostrandoImagen by viewModel.mostrandoImagen.collectAsState()

    val anim = remember { Animatable(0f) }

    LaunchedEffect(Unit) { viewModel.cargarCheckpoints() }
    LaunchedEffect(checkpoints) {
        if (checkpoints.size == 1) viewModel.seleccionado.value = checkpoints.first()
    }

    LaunchedEffect(scannedText) {
        scannedText?.let {
            viewModel.procesarCodigo(it)
            viewModel.scannedText.value = null
        }
    }

    Scaffold(
        bottomBar = {
            HomeConfigNavBar(
                current = "",
                onHomeClick = { navController.navigate("home") },
                onConfigClick = { navController.navigate("configuracion") }
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                seleccionado == null -> CheckpointList(checkpoints) { viewModel.seleccionado.value = it }
                cargando -> LoadingView()
                mostrandoImagen && imagenCrop != null -> ImagenResultadoView(imagenCrop!!) {
                    viewModel.reiniciar()
                    navController.navigate("lomascountry") { popUpTo("lomascountry") { inclusive = true } }
                }
                else -> ScanView(viewModel)
            }

            resultado?.let { res ->
                LaunchedEffect(res) {
                    anim.snapTo(0f)
                    anim.animateTo(1f, tween(500))
                    delay(3000)
                    if (res == "valido") {
                        viewModel.mostrandoImagen.value = true
                    } else {
                        viewModel.reiniciar()
                        navController.navigate("lomascountry") { popUpTo("lomascountry") { inclusive = true } }
                    }
                }
                CanvasOverlay(
                    color = if (res == "valido") Color(0x8800FF00) else Color(0x88FF0000),
                    progress = anim.value,
                    text = res
                )
            }
            networkError?.let { err ->
                Text(
                    text = err,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun CheckpointList(checkpoints: List<Checkpoint>, onSelect: (Checkpoint) -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        items(checkpoints) { cp ->
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onSelect(cp) }) {
                Text(cp.nombre, modifier = Modifier.padding(24.dp), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ScanView(viewModel: EscaneoHandheldViewModel) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.startScanner(context)
                Lifecycle.Event.ON_PAUSE -> viewModel.stopScanner(context)
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            viewModel.stopScanner(context)
        }
    }
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Usa los botones físicos para escanear", textAlign = TextAlign.Center)
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ImagenResultadoView(bitmap: android.graphics.Bitmap, onFinish: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn()) {
            Card(elevation = CardDefaults.elevatedCardElevation(8.dp)) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onFinish) { Text("Finalizar") }
    }
}

@Composable
private fun CanvasOverlay(color: Color, progress: Float, text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color, radius = size.maxDimension / 2f * progress, center = center)
        }
        Text(
            text.uppercase(),
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
    }
}

