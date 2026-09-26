package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkz.affa.ui.screens.Episode
import com.lkz.affa.ui.theme.TextGray

import androidx.compose.foundation.clickable

// Mapeo compartido dominio → tarjeta (título genérico + subtítulo real si existe).
internal fun com.lkz.affa.domain.model.Episode.toUiEpisode(): Episode {
    val n = if (number % 1 == 0f) number.toInt().toString() else number.toString()
    val generic = "Episodio $n"
    return Episode(
        number = generic,
        title = title.takeIf { it.isNotBlank() && it != generic } ?: "",
        duration = "",
        description = ""
    )
}

@Composable
fun EpisodeItem(episode: Episode, imageUrl: String = "", onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Portada del anime
        Box(
            modifier = Modifier
                .width(130.dp)
                .height(75.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Gray)
        ) {
            if (imageUrl.isNotEmpty()) {
                coil.compose.AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            // Duración en la esquina inferior derecha (solo si se conoce)
            if (episode.duration.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = episode.duration,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Información del episodio
        Column(
            modifier = Modifier
                .weight(1f)
                .align(Alignment.Top)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = episode.number,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Subtítulo real solo si los metadatos lo traen
                    if (episode.title.isNotBlank()) {
                        Text(
                            text = episode.title,
                            color = TextGray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // ponytail: sin funcionamiento — se conecta con Mi Lista / historial
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Visto",
                    tint = TextGray,
                    modifier = Modifier.size(22.dp)
                )
            }

            if (episode.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = episode.description,
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}