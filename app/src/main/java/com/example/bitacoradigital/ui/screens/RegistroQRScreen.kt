package com.example.bitacoradigital.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.Canvas

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bitacoradigital.ui.components.HomeConfigNavBar
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.viewmodel.RegistroQRViewModel
import com.example.bitacoradigital.viewmodel.RegistroQRViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroQRScreen(perimetroId: Int, navController: NavHostController) {
    val context = LocalContext.current
    val prefs = remember { SessionPreferences(context) }
    val viewModel: RegistroQRViewModel = viewModel(factory = RegistroQRViewModelFactory(prefs, perimetroId))

    val checkpoints by viewModel.checkpoints.collectAsState()
    val seleccionado by viewModel.seleccionado.collectAsState()
    val resultado by viewModel.resultado.collectAsState()
    val crop by viewModel.imagenCrop.collectAsState()
    val mostrandoImagen by viewModel.mostrandoImagen.collectAsState()
    val siguientePerimetro by viewModel.nombreSiguientePerimetro.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    val scope = rememberCoroutineScope()
    val anim = remember { Animatable(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current

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
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) { viewModel.cargarCheckpoints() }
    LaunchedEffect(checkpoints) {
        if (checkpoints.size == 1) viewModel.seleccionado.value = checkpoints.first()
    }

    var cameraActive by remember { mutableStateOf(true) }

    if (mostrandoImagen && crop != null) {
        val time by produceState(initialValue = "") {
            while (true) {
                value = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                kotlinx.coroutines.delay(1000)
            }
        }

        val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        val scale by androidx.compose.animation.core.animateFloatAsState(if (pressed) 0.97f else 1f)

        Scaffold(
            containerColor = com.example.bitacoradigital.ui.theme.DashboardBackground,
            topBar = {
                SmallTopAppBar(
                    title = {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(time, style = MaterialTheme.typography.bodyLarge)
                            Image(
                                painter = painterResource(id = com.example.bitacoradigital.R.drawable.logo_topbar),
                                contentDescription = null,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
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
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + scaleIn()
                    ) {
                        Card(elevation = CardDefaults.elevatedCardElevation(8.dp)) {
                            Image(
                                bitmap = crop!!.asImageBitmap(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 300.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    siguientePerimetro?.let { nombre ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn()
                        ) {
                            Text(
                                "Por favor, brinda instrucciones para ir a $nombre",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.reiniciar()
                            navController.navigate("visitas") {
                                popUpTo("visitas") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .scale(scale)
                            .padding(horizontal = 32.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        interactionSource = interactionSource
                    ) {
                        Text("Continuar", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        return
    }

    Box(Modifier.fillMaxSize()) {
        when {
            cargando -> { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
            error != null -> { Text(error ?: "", modifier = Modifier.align(Alignment.Center)) }
            seleccionado == null -> {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Selecciona un checkpoint")
                    checkpoints.forEach { cp ->
                        Button(onClick = { viewModel.seleccionado.value = cp }, modifier = Modifier.padding(8.dp)) { Text(cp.nombre) }
                    }
                }
            }
            else -> {
                if (hasCameraPermission) {
                    if (cameraActive) {
                        CameraPreview(onCodeScanned = { codigo ->
                            cameraActive = false
                            viewModel.procesarCodigo(codigo)
                        })
                    }
                } else {
                    Column(
                        Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Se requiere permiso de cámara para escanear")
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Conceder permiso")
                        }
                    }
                }
            }
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
                    navController.navigate("visitas") {
                        popUpTo("visitas") { inclusive = true }
                    }
                }
            }
            CanvasOverlay(color = if (res == "valido") Color(0x8800FF00) else Color(0x88FF0000), progress = anim.value, text = res)
        }
    }
}

@Composable
private fun CanvasOverlay(color: Color, progress: Float, text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color, radius = size.maxDimension / 2f * progress, center = center)
        }
        Text(text.uppercase(), color = Color.White, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
    }
}

@Composable
private fun CameraPreview(onCodeScanned: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val scanner = remember { BarcodeScanning.getClient() }
    var scanning by remember { mutableStateOf(false) }

    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

    DisposableEffect(Unit) {
        val provider = ProcessCameraProvider.getInstance(context).get()
        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder().build().apply {
            setAnalyzer(ContextCompat.getMainExecutor(context)) { image ->
                if (!scanning) {
                    scanning = true
                    processImage(image, scanner, onCodeScanned) { scanning = false }
                } else {
                    image.close()
                }
            }
        }
        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis
        )

        onDispose {
            provider.unbindAll()
        }
    }
}

private fun processImage(image: ImageProxy, scanner: com.google.mlkit.vision.barcode.BarcodeScanner, onScanned: (String) -> Unit, onComplete: () -> Unit) {
    val mediaImage = image.image
    if (mediaImage != null) {
        val input = InputImage.fromMediaImage(mediaImage, image.imageInfo.rotationDegrees)
        scanner.process(input)
            .addOnSuccessListener { list ->
                val raw = list.firstOrNull()?.rawValue
                if (raw != null) onScanned(raw)
            }
            .addOnCompleteListener {
                image.close()
                onComplete()
            }
    } else {
        image.close()
        onComplete()
    }
}
