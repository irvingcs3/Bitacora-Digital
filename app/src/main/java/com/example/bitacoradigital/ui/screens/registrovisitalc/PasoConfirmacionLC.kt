// 📁 ui/screens/registrovisitalc/PasoConfirmacionLC.kt
package com.example.bitacoradigital.ui.screens.registrovisitalc

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.bitacoradigital.viewmodel.RegistroVisitaLCViewModel

@Composable
fun PasoConfirmacionLC(viewModel: RegistroVisitaLCViewModel) {
    val telefono by viewModel.telefono.collectAsState()
    val nombre by viewModel.nombre.collectAsState()
    val paterno by viewModel.apellidoPaterno.collectAsState()
    val materno by viewModel.apellidoMaterno.collectAsState()
    val documento = viewModel.documentoUri.value
    val destino by viewModel.destinoSeleccionado.collectAsState()
    val fotos by viewModel.fotosAdicionales.collectAsState()
    val scrollState = rememberScrollState()



    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {

        Text("Paso 6: Confirma tu Registro", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("📱 Teléfono: $telefono")
                Text("👤 Nombre: $nombre $paterno $materno")
                destino?.let {
                    Spacer(Modifier.height(4.dp))
                    Text("📍 Destino: ${it.nombre} (ID ${it.perimetro_id})")
                }
                Spacer(Modifier.height(4.dp))
                if (viewModel.destinoGeneral.value.isNotBlank()) {
                    Text("🏢 Destino General: ${viewModel.destinoGeneral.value}")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        documento?.let {
            Text("Documento Oficial:", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            AsyncImage(
                model = it,
                contentDescription = "Documento",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
            )
        }

        if (fotos.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Fotos Adicionales:")
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fotos.forEach {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { viewModel.avanzarPaso() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Siguiente")
        }
        }
    }
}
