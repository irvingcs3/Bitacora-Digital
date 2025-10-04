package com.example.bitacoradigital.ui.screens.guardia

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
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

@Composable
fun DronGuardScreen(viewModel: DronGuardViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }
    val locationPermissions = remember {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }
    var hasLocationPermission by remember {
        mutableStateOf(
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var pendingPermissionRequest by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
        val granted = results.filterKeys { key ->
            key == Manifest.permission.ACCESS_FINE_LOCATION || key == Manifest.permission.ACCESS_COARSE_LOCATION
        }.values.any { it }
        Log.d("DronGuardScreen", "Resultado solicitud permisos ubicacion: $results, granted=$granted")
        hasLocationPermission = granted
        if (granted) {
            if (pendingPermissionRequest) {
                Log.d("DronGuardScreen", "Permisos otorgados tras solicitud, reintentando envio de alerta")
            }
        } else {
            Log.w("DronGuardScreen", "Permisos de ubicacion denegados por el usuario")
        }
    }
    var envioIniciado by remember { mutableStateOf(false) }
    val direccionEvento by viewModel.direccionEvento.collectAsState()
    val showMessage = envioIniciado && direccionEvento != null
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val sizeAnim = remember { Animatable(200.dp, Dp.VectorConverter) }

    fun obtenerUbicacionYEnviar() {
        Log.d("DronGuardScreen", "Iniciando obtencion de ubicacion para enviar alerta")
        envioIniciado = true
        fused.lastLocation.addOnSuccessListener { loc ->
            Log.d("DronGuardScreen", "Ubicacion obtenida con fused client: $loc")
            if (loc != null) {
                viewModel.enviarAlerta(loc.latitude, loc.longitude)
            } else {
                Log.w("DronGuardScreen", "Ubicacion fused nula, intentando obtener ultima ubicacion conocida")
                val fallback = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                if (fallback != null) {
                    Log.d("DronGuardScreen", "Ubicacion conocida previa encontrada: $fallback")
                    viewModel.enviarAlerta(fallback.latitude, fallback.longitude)
                } else {
                    Log.w("DronGuardScreen", "No hay ubicacion previa, solicitando una actual")
                    val listener = object : LocationListener {
                        override fun onLocationChanged(p0: Location) {
                            Log.d("DronGuardScreen", "Ubicacion por listener: $p0")
                            viewModel.enviarAlerta(p0.latitude, p0.longitude)
                            locationManager.removeUpdates(this)
                        }

                        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                        override fun onProviderEnabled(provider: String) {}
                        override fun onProviderDisabled(provider: String) {
                            Log.w("DronGuardScreen", "Proveedor de ubicacion deshabilitado: $provider")
                        }
                    }
                    val provider = if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                        LocationManager.GPS_PROVIDER
                    } else {
                        LocationManager.NETWORK_PROVIDER
                    }
                    Log.d("DronGuardScreen", "Solicitando actualizacion de ubicacion al proveedor: $provider")
                    try {
                        locationManager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                    } catch (security: SecurityException) {
                        Log.e("DronGuardScreen", "Error solicitando actualizacion de ubicacion", security)
                    }
                }
            }
        }.addOnFailureListener { error ->
            Log.e("DronGuardScreen", "Fallo al obtener ubicacion con fused client", error)
            val fallback = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (fallback != null) {
                Log.d("DronGuardScreen", "Ubicacion fallback tras error fused: $fallback")
                viewModel.enviarAlerta(fallback.latitude, fallback.longitude)
            } else {
                Log.e("DronGuardScreen", "No se pudo obtener ubicacion tras error fused")
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.clearDireccionEvento()
    }

    LaunchedEffect(direccionEvento) {
        direccionEvento?.let {
            Log.d("DronGuardScreen", "Direccion del evento recibida: $it")
        }
    }

    LaunchedEffect(pressed) {
        if (pressed && !envioIniciado) {
            Log.d("DronGuardScreen", "Botón presionado")
            sizeAnim.snapTo(200.dp)
            sizeAnim.animateTo(600.dp, tween(3000))
            if (pressed) {
                Log.d("DronGuardScreen", "Presionado 3s, enviando alerta")
                if (!hasLocationPermission) {
                    Log.w("DronGuardScreen", "Intento de envio sin permisos, solicitandolos al usuario")
                    pendingPermissionRequest = true
                    permissionLauncher.launch(locationPermissions)
                    return@LaunchedEffect
                }
                pendingPermissionRequest = false
                obtenerUbicacionYEnviar()
            }
        } else if (!pressed && !envioIniciado) {
            sizeAnim.snapTo(200.dp)
        }
    }

    LaunchedEffect(hasLocationPermission, pendingPermissionRequest, pressed) {
        if (hasLocationPermission && pendingPermissionRequest) {
            Log.d("DronGuardScreen", "Permisos ya disponibles tras solicitud previa, lanzando envio")
            pendingPermissionRequest = false
            if (!envioIniciado) {
                obtenerUbicacionYEnviar()
            }
        } else if (!hasLocationPermission && !pressed) {
            pendingPermissionRequest = false
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
