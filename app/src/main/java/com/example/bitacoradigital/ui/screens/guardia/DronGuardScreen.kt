package com.example.bitacoradigital.ui.screens.guardia

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.navigation.NavHostController
import com.example.bitacoradigital.viewmodel.DronGuardViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun DronGuardScreen(viewModel: DronGuardViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val fused = remember { LocationServices.getFusedLocationProviderClient(context) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val scope = rememberCoroutineScope()

    var job by remember { mutableStateOf<Job?>(null) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()

    LaunchedEffect(pressed) {
        if (pressed) {
            job = scope.launch {
                delay(3000)
                if (pressed) {
                    if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                        return@launch
                    }
                    fused.lastLocation.addOnSuccessListener { loc ->
                        loc?.let { viewModel.enviarAlerta(it.latitude, it.longitude) }
                    }
                }
            }
        } else {
            job?.cancel(); job = null
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(
            onClick = {},
            modifier = Modifier.size(200.dp),
            interactionSource = interaction,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
        ) {
            Text("SOS", color = MaterialTheme.colorScheme.onPrimary)
        }
    }
}
