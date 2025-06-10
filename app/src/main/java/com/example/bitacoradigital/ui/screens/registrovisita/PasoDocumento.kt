package com.example.bitacoradigital.ui.screens.registrovisita

import android.net.Uri
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
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
@Composable
fun PasoDocumento(viewModel: RegistroVisitaViewModel) {
    val uri = viewModel.documentoUri.value

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { result: Uri? ->
        result?.let {
            viewModel.documentoUri.value = it
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Paso 2: Escanea tu documento", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { launcher.launch("image/*") }) {
            Text("Tomar Foto o Seleccionar Imagen")
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

        Button(onClick = { viewModel.avanzarPaso() }, enabled = uri != null) {
            Text("Continuar")
        }
    }
}
