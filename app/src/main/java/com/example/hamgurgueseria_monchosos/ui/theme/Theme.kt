package com.example.hamgurgueseria_monchosos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = Cafe,
    onPrimary = Blanco,
    primaryContainer = CafeFondo,
    onPrimaryContainer = Cafe,
    secondary = Mostaza,
    onSecondary = Tinta,
    secondaryContainer = MostazaFondo,
    onSecondaryContainer = Tinta,
    tertiary = Ketchup,
    error = Ketchup,
    background = Papel,
    onBackground = Tinta,
    surface = Papel,
    onSurface = Tinta,
    onSurfaceVariant = TextoSuave,
    // Paneles y barras blancos sobre el fondo papel.
    surfaceContainerLowest = Blanco,
    surfaceContainerLow = Blanco,
    surfaceContainer = Blanco,
    surfaceContainerHigh = Blanco,
    surfaceContainerHighest = Blanco,
    outline = Linea,
    outlineVariant = Linea,
)

private val DarkColorScheme = darkColorScheme(
    primary = CafeClaro,
    onPrimary = Cafe,
    primaryContainer = Color(0xFF3D1205),
    onPrimaryContainer = CafeClaro,
    secondary = Mostaza,
    onSecondary = Tinta,
    // Chips elegidos y botones − / + (sin esto Material usa un gris morado).
    secondaryContainer = Color(0xFF4A3426),
    onSecondaryContainer = Color(0xFFFFDCC4),
    tertiary = Color(0xFFFFB4AB),
    background = Carbon,
    surface = Carbon,
    surfaceContainerLowest = CarbonPanel,
    surfaceContainerLow = CarbonPanel,
    surfaceContainer = CarbonPanel,
    surfaceContainerHigh = CarbonPanel,
    surfaceContainerHighest = CarbonPanel,
    outline = CarbonLinea,
    outlineVariant = CarbonLinea,
)

// Esquinas cortas: se ve más a punto de venta que a burbuja de chat.
private val Formas = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

@Composable
fun HamgurgueseriamonchososTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = Formas,
        content = content
    )
}
