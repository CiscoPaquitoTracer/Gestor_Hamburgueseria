package com.example.hamgurgueseria_monchosos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Ketchup,
    onPrimary = Color.White,
    secondary = MostazaOscura,
    secondaryContainer = Mostaza.copy(alpha = 0.25f),
    tertiary = Pan,
    background = Crema,
    surface = Crema,
)

private val DarkColorScheme = darkColorScheme(
    primary = KetchupClaro,
    secondary = Mostaza,
    tertiary = PanClaro,
    background = Carbon,
    surface = Carbon,
)

@Composable
fun HamgurgueseriamonchososTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
