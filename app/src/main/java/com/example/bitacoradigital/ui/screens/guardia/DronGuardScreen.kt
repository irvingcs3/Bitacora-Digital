package com.example.bitacoradigital.ui.screens.guardia

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import com.example.bitacoradigital.viewmodel.DronGuardViewModel
import com.google.android.gms.location.LocationServices
import android.util.Log
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DronGuardScreen(viewModel: DronGuardViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val scope = rememberCoroutineScope()

    var showMessage by remember { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sizeAnim = remember { Animatable(200.dp, Dp.VectorConverter) }

    LaunchedEffect(Unit) {
        viewModel.registrarBotonPanico()
    }

    LaunchedEffect(pressed) {
        if (pressed && !showMessage) {
            Log.d("DronGuardScreen", "Bot\u00f3n presionado")
            sizeAnim.snapTo(200.dp)
            sizeAnim.animateTo(600.dp, tween(3000))
            if (pressed) {
                Log.d("DronGuardScreen", "Presionado 3s, enviando alerta")
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    fused.lastLocation.addOnSuccessListener { loc ->
                        Log.d("DronGuardScreen", "Ubicaci\u00f3n obtenida: ${loc}")
                        loc?.let { viewModel.enviarAlerta(it.latitude, it.longitude) }
                    }
                }
                showMessage = true
            }
        } else if (!pressed && !showMessage) {
            sizeAnim.snapTo(200.dp)
        }
    }

    if (showMessage) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Red),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "Dron desplegado a tu ubicacion",
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { navController.navigate("home") }) {
                    Text("Volver a pantalla principal")
                }
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(
                onClick = {},
                modifier = Modifier.size(sizeAnim.value),
                interactionSource = interaction,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("SOS", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
