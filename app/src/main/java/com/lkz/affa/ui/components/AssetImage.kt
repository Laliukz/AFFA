package com.lkz.affa.ui.components

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext

/**
 * Loads and displays an image from the app's assets folder.
 *
 * @param path Path relative to the assets folder (e.g. "images/app_icon.png")
 */
@Composable
fun AssetImage(
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
