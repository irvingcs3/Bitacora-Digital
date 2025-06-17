package com.example.bitacoradigital.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun HomeConfigNavBar(
    current: String,
    onHomeClick: () -> Unit,
    onConfigClick: () -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = current == "home",
            onClick = onHomeClick,
            icon = { androidx.compose.material3.Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = current == "config",
            onClick = onConfigClick,
            icon = { androidx.compose.material3.Icon(Icons.Default.Settings, contentDescription = "Configuración") },
            label = { Text("Configuración") }
        )
    }
}
