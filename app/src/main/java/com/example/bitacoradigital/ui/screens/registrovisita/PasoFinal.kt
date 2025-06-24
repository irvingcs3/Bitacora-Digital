// 📁 ui/screens/registrovisita/PasoFinal.kt
package com.example.bitacoradigital.ui.screens.registrovisita

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File
import com.example.bitacoradigital.util.Constants
import com.example.bitacoradigital.viewmodel.RegistroVisitaViewModel
import androidx.navigation.NavHostController

@Composable
fun PasoFinal(viewModel: RegistroVisitaViewModel, navController: NavHostController) {
    val respuesta by viewModel.respuestaRegistro.collectAsState()
    val qrBitmap by viewModel.qrBitmap.collectAsState()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("✅ ¡Registro Completado!", style = MaterialTheme.typography.headlineSmall)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Tu registro ha sido procesado exitosamente.\nEl residente será notificado.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        respuesta?.let {
            Text(it, fontSize = 20.sp)
        }

        qrBitmap?.let { bmp ->
            Spacer(modifier = Modifier.height(16.dp))
            Image(bitmap = bmp.asImageBitmap(), contentDescription = null, modifier = Modifier.size(200.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                val file = File(context.cacheDir, "qr_share.jpg")
                file.outputStream().use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 100, it) }
                val uri = FileProvider.getUriForFile(context, Constants.FILE_PROVIDER_AUTHORITY, file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_STREAM, uri)
                }
                context.startActivity(Intent.createChooser(intent, "Compartir manualmente"))
            }) {
                Text("Compartir manualmente")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(onClick = {
            viewModel.reiniciar()
            navController.navigate("visitas") {
                popUpTo("visitas") { inclusive = true }
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Registrar otra visita")
        }
    }
}
