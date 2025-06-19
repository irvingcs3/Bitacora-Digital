package com.example.bitacoradigital.ui.screens.registrovisita

import android.content.Context
import android.net.Uri
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import com.example.bitacoradigital.util.Constants
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import coil.compose.rememberAsyncImagePainter
import java.io.File
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
@Composable
fun PasoDocumento(viewModel: RegistroVisitaViewModel) {
    val context = LocalContext.current
    val uri = viewModel.documentoUri.value
    val cargando by viewModel.cargandoReconocimiento.collectAsState()
    val error by viewModel.errorReconocimiento.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

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

    var tempUri by remember { mutableStateOf<Uri?>(null) }

    fun createImageUri(context: Context): Uri {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val image = File.createTempFile("document_", ".jpg", imagesDir)
        return FileProvider.getUriForFile(
            context,
            Constants.FILE_PROVIDER_AUTHORITY,
            image
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempUri?.let { viewModel.documentoUri.value = it }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Paso 2: Escanea tu documento",
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Toca para escanear tu identificación",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (hasCameraPermission) {
                        val uriTemp = createImageUri(context)
                        tempUri = uriTemp
                        launcher.launch(uriTemp)
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFD65930),
                    contentColor = Color.White
                )
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Tomar Foto", style = MaterialTheme.typography.bodyLarge)
            }

            if (!hasCameraPermission) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Se requiere permiso de cámara",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            AnimatedVisibility(visible = uri != null, enter = fadeIn()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(24.dp))
                    uri?.let { selected ->
                        val painter = rememberAsyncImagePainter(model = selected)
                        Image(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.reconocerDocumento(context)
                                viewModel.avanzarPaso()
                            }
                        },
                        enabled = uri != null && !cargando
                    ) {
                        Text(
                            if (cargando) "Procesando..." else "Continuar",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }

                    if (cargando) {
                        Spacer(modifier = Modifier.height(16.dp))
                        CircularProgressIndicator()
                    }
                }
            }

            error?.let { msg ->
                LaunchedEffect(msg) {
                    snackbarHostState.showSnackbar(msg)
                    viewModel.clearReconocimientoError()
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
