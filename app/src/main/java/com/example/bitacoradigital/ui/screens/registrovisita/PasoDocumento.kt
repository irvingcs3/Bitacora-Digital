package com.example.bitacoradigital.ui.screens.registrovisita

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import java.io.File
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
@Composable
fun PasoDocumento(viewModel: RegistroVisitaViewModel) {
    val context = LocalContext.current
    val uri = viewModel.documentoUri.value
    val cargando by viewModel.cargandoReconocimiento.collectAsState()
    val error by viewModel.errorReconocimiento.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var tempUri by remember { mutableStateOf<Uri?>(null) }

    fun createImageUri(context: Context): Uri {
        val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
        val image = File.createTempFile("document_", ".jpg", imagesDir)
        return FileProvider.getUriForFile(context, "${'$'}{context.packageName}.fileprovider", image)
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            tempUri?.let { viewModel.documentoUri.value = it }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Paso 2: Escanea tu documento", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            tempUri = createImageUri(context)
            launcher.launch(tempUri)
        }) {
            Text("Tomar Foto")
        }

        Spacer(modifier = Modifier.height(16.dp))

        uri?.let {
            val painter = rememberAsyncImagePainter(model = it)
            Image(
                painter = painter,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                coroutineScope.launch {
                    viewModel.reconocerDocumento(context)
                    viewModel.avanzarPaso()
                }
            },
            enabled = uri != null && !cargando
        ) {
            Text(if (cargando) "Procesando..." else "Continuar")
        }

        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        if (cargando) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}
