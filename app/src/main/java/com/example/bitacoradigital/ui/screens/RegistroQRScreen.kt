package com.example.bitacoradigital.ui.screens

import android.Manifest
import android.graphics.Bitmap
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.bitacoradigital.data.SessionPreferences
import com.example.bitacoradigital.viewmodel.RegistroQRViewModel
import com.example.bitacoradigital.viewmodel.RegistroQRViewModelFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

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
    val cargando by viewModel.cargando.collectAsState()
    val error by viewModel.error.collectAsState()

    val scope = rememberCoroutineScope()
    val anim = remember { Animatable(0f) }
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) { viewModel.cargarCheckpoints() }
    LaunchedEffect(checkpoints) {
        if (checkpoints.size == 1) viewModel.seleccionado.value = checkpoints.first()
    }

    if (mostrandoImagen && crop != null) {
        Box(Modifier.fillMaxSize()) {
            Image(bitmap = crop!!.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize())
            Button(onClick = { viewModel.reiniciar() }, modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)) {
                Text("Continuar")
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
                CameraPreview(onCodeScanned = { codigo -> viewModel.procesarCodigo(codigo) })
            }
        }

        resultado?.let { res ->
            LaunchedEffect(res) {
                anim.snapTo(0f)
                anim.animateTo(1f, tween(500))
                delay(3000)
                if (res == "valido") viewModel.mostrandoImagen.value = true
                else viewModel.reiniciar()
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

    LaunchedEffect(Unit) {
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
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
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
