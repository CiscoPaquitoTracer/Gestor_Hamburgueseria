package com.example.hamgurgueseria_monchosos.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

// "tnum": todos los dígitos del mismo ancho, para que los montos queden alineados en columna.
private fun TextStyle.cifras() = copy(fontFeatureSettings = "tnum")

val Typography = Typography(
    displaySmall = base.displaySmall.copy(fontWeight = FontWeight.Bold, fontSize = 34.sp).cifras(),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold).cifras(),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold).cifras(),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold).cifras(),
    titleSmall = base.titleSmall.cifras(),
    bodyLarge = base.bodyLarge.cifras(),
    bodyMedium = base.bodyMedium.cifras(),
    // Encabezados de sección en mayúsculas (ver EncabezadoSeccion).
    labelMedium = base.labelMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp),
)
