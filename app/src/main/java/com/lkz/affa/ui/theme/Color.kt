package com.lkz.affa.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val AppPrimary = Color(0xFFA829FF)
val AppSecondary = Color(0xFFA829FF) // Sin acento secundario, reutilizamos el principal
val AppScaffoldBackground = Color(0xFF120E15)
val AppSurface = Color(0xFF2A242E)
val AppSurfaceLight = Color(0xFF3B3340) // Un poco más claro que Surface

val AppTextPrimary = Color(0xFFFFFFFF)
val AppTextSecondary = Color(0xFFE0E0E0)
val AppTextMuted = Color(0xFFAAAAAA)

val AppError = Color(0xFFCF6679)
val AppSuccess = Color(0xFF4CAF50)
val AppOnline = Color(0xFF00E676)
val AppRating = Color(0xFFFFCA28)

val PrimaryGradient = Brush.linearGradient(colors = listOf(AppPrimary, AppSecondary))

fun Color.withAlpha(alpha: Float): Color {
    return this.copy(alpha = alpha)
}

val AppBlackOverlay = Color.Black.withAlpha(0.4f) // Para fondos oscuros semi-transparentes
val AppGrayLight = Color(0xFFBDBDBD) // Gris claro para separadores o bordes sutiles
val AppGrayMedium = Color(0xFFAAAA) // Gris medio, más visible que el secundario de texto
val AppBackgroundDarkGray =
        Color(0xFF181825) // Un gris muy oscuro para fondos, más agradable que negro puro
