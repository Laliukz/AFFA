package com.lkz.affa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = AppPrimary,
    secondary = AppSecondary,
    background = AppScaffoldBackground,
    surface = AppSurface,
    surfaceVariant = AppSurfaceLight,
    onPrimary = AppTextPrimary,
    onSecondary = AppTextPrimary,
    onBackground = AppTextPrimary,
    onSurface = AppTextPrimary,
    error = AppError
)

@Composable
fun AFFATheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = Typography,
        content = content
    )
}