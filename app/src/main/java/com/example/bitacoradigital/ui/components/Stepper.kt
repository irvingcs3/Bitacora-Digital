package com.example.bitacoradigital.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment

@Composable
fun Stepper(pasoActual: Int, totalPasos: Int = 6) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalPasos) { index ->
            val paso = index + 1
            val color = when {
                paso < pasoActual -> MaterialTheme.colorScheme.primary
                paso == pasoActual -> MaterialTheme.colorScheme.secondary
                else -> Color.Gray.copy(alpha = 0.3f)
            }

            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(color)
            )
        }
    }
}
