package com.example.muritin

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun MuriTinTheme(
    darkTheme: Boolean = false, // No dark mode for now, add in Week 5
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF6650a4), // Purple for buttons, icons
            secondary = Color(0xFF625b71), // Purplegrey for accents
            error = Color(0xFFB71C1C), // Red for errors
            background = Color(0xFFF5F5F5), // Light gray background
            surface = Color.White // Card and surface backgrounds
        ),
        typography = Typography(
            headlineMedium = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            ),
            bodyLarge = TextStyle(
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            ),
            labelLarge = TextStyle(
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp
            )
        ),
        content = content
    )
}