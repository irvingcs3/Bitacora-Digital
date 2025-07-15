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
import android.location.LocationManager
import android.location.LocationListener
import android.location.Location
import android.os.Looper
import android.os.Bundle
import android.content.Context
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
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val scope = rememberCoroutineScope()

    var envioIniciado by remember { mutableStateOf(false) }
    val direccionEvento by viewModel.direccionEvento.collectAsState()
    val showMessage = envioIniciado && direccionEvento != null
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sizeAnim = remember { Animatable(200.dp, Dp.VectorConverter) }

    LaunchedEffect(Unit) {
        viewModel.clearDireccionEvento()
        viewModel.registrarBotonPanico()
    }

    LaunchedEffect(pressed) {
        if (pressed && !envioIniciado) {
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
                        if (loc != null) {
                            viewModel.enviarAlerta(loc.latitude, loc.longitude)
                        } else {
                            val fallback = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                            if (fallback != null) {
                                viewModel.enviarAlerta(fallback.latitude, fallback.longitude)
                            } else {
                                val listener = object : LocationListener {
                                    override fun onLocationChanged(p0: Location) {
                                        Log.d("DronGuardScreen", "Fallback ubicaci\u00f3n: ${'$'}p0")
                                        viewModel.enviarAlerta(p0.latitude, p0.longitude)
                                        locationManager.removeUpdates(this)
                                    }

                                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                                    override fun onProviderEnabled(provider: String) {}
                                    override fun onProviderDisabled(provider: String) {}
                                }
                                val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                                    LocationManager.GPS_PROVIDER
                                } else {
                                    LocationManager.NETWORK_PROVIDER
                                }
                                locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                            }
                        }
                    }.addOnFailureListener {
                        val fallback = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        fallback?.let { viewModel.enviarAlerta(it.latitude, it.longitude) }
                    }
                }
                envioIniciado = true
            }
        } else if (!pressed && !envioIniciado) {
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
                    "Dron desplegado a tu ubicacion en: ${direccionEvento ?: ""}",
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
