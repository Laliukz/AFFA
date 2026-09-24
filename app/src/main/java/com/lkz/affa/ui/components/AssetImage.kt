package com.lkz.affa.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

@Composable
fun AssetImage(
    // Ruta relativa dentro del directorio assets (ej. "images/app_icon.png")
    path: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    contentScale: ContentScale = ContentScale.Fit,
) {
    val context = LocalContext.current
    val bitmap = remember(path) {
        context.assets.open(path).use { stream ->
            BitmapFactory.decodeStream(stream).asImageBitmap()
        }
    }

    Image(
        bitmap = bitmap,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
    )
}