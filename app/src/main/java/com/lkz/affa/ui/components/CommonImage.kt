package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.lkz.affa.ui.theme.*

/**
 * Equivalent of Flutter's CommonImage (CachedNetworkImage wrapper).
 *
 * Handles:
 * - Empty URL → placeholder box with ImageNotSupported icon
 * - Loading  → dark box with CircularProgressIndicator
 * - Error    → dark box with ErrorOutline icon
 * - Crossfade 300ms (same as fadeInDuration in Flutter)
 * - Optional clipping via [shape] (equivalent to Flutter's ClipRRect + BorderRadius)
 *
 * Usage:
 * ```
 * CommonImage(imageUrl = url, width = 100.dp, height = 145.dp, borderRadius = 8.dp)
 * // Or with asymmetric corners:
 * CommonImage(imageUrl = url, shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
 * ```
 */
@Composable
fun CommonImage(
    imageUrl: String,
    modifier: Modifier = Modifier,
    width: Dp? = null,
    height: Dp? = null,
    contentScale: ContentScale = ContentScale.Crop,
    contentDescription: String? = null,
    borderRadius: Dp? = null,
    shape: Shape? = null,
) {
    val resolvedShape: Shape? = shape ?: borderRadius?.let { RoundedCornerShape(it) }

    val sizeModifier = Modifier
        .then(if (width != null) Modifier.width(width) else Modifier)
        .then(if (height != null) Modifier.height(height) else Modifier)

    val finalModifier = modifier
        .then(sizeModifier)
        .then(if (resolvedShape != null) Modifier.clip(resolvedShape) else Modifier)

    if (imageUrl.isEmpty()) {
        Box(
            modifier = finalModifier.background(AppSurfaceLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ImageNotSupported,
                contentDescription = null,
                tint = AppTextPrimary.copy(alpha = 0.54f)
            )
        }
        return
    }

    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(300)
            .build(),
        contentDescription = contentDescription,
        modifier = finalModifier,
        contentScale = contentScale
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppSurface),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = AppTextPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            is AsyncImagePainter.State.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(AppSurfaceLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = AppTextPrimary.copy(alpha = 0.54f)
                    )
                }
            }
            else -> SubcomposeAsyncImageContent()
        }
    }
}
