package com.example.bitacoradigital.ui.screens.registrovisita

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
// LazyRow y items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items

// AsyncImage (de Coil)
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
import kotlinx.coroutines.launch
@Composable
fun PasoFotos(viewModel: RegistroVisitaViewModel) {
    val fotos by viewModel.fotosAdicionales.collectAsState()
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri -> uri?.let { viewModel.agregarFoto(it) } }
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Paso 5: Fotos adicionales", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(fotos) { foto ->
                Box {
                    AsyncImage(
                        model = foto,
                        contentDescription = null,
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    IconButton(
                        onClick = { viewModel.eliminarFoto(foto) },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red)
                    }
                }
            }
        }

        if (fotos.size < 3) {
            Spacer(Modifier.height(16.dp))
            Button(onClick = { launcher.launch("image/*") }) {
                Text("Agregar Foto (${3 - fotos.size} restantes)")
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(onClick = { viewModel.avanzarPaso() }, modifier = Modifier.fillMaxWidth()) {
            Text("Continuar")
        }
    }
}
