package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lkz.affa.ui.theme.PrimaryGradient
import com.lkz.affa.ui.theme.AppSecondary
import com.lkz.affa.ui.theme.*

/**
 * A reusable progress bar widget.
 *
 * Use this for:
 * - Download progress
 * - Watch progress on content cards
 * - Storage usage indicators
 */
@Composable
fun AppProgressBar(
    value: Float,
    color: Color? = null,
    backgroundColor: Color? = null,
    height: Dp = 4.dp,
    borderRadius: Dp = 4.dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(borderRadius))
            .background(backgroundColor ?: AppTextPrimary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(value.coerceIn(0.0f, 1.0f))
                .height(height)
                .background(color ?: AppSecondary)
        )
    }
}

/**
 * A gradient progress bar for special use cases (e.g., splash screen).
 */
@Composable
fun GradientProgressBar(
    value: Float,
    height: Dp = 4.dp,
    borderRadius: Dp = 2.dp,
    gradient: Brush? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(borderRadius))
            .background(AppTextPrimary.copy(alpha = 0.1f)),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(value.coerceIn(0.0f, 1.0f))
                .height(height)
                .background(gradient ?: PrimaryGradient)
        )
    }
}
