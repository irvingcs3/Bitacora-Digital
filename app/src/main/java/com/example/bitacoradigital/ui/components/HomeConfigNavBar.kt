package com.example.bitacoradigital.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow

@Composable
fun HomeConfigNavBar(
    current: String,
    onHomeClick: () -> Unit,
    onConfigClick: () -> Unit
) {
    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        NavigationBarItem(
            selected = current == "home",
            onClick = onHomeClick,
            icon = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Home"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Home",
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            alwaysShowLabel = false,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = current == "config",
            onClick = onConfigClick,
            icon = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Configuración"
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Configuración",
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            },
            alwaysShowLabel = false,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                unselectedTextColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                indicatorColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
