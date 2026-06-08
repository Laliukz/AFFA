package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkz.affa.data.models.Content
import com.lkz.affa.data.repository.FavoritesRepository
import com.lkz.affa.ui.theme.*

@Composable
fun HeroSection(content: Content, onPlayClick: () -> Unit, onAddClick: () -> Unit = {}) {
    val favoriteIds by FavoritesRepository.favoriteIds.collectAsState()
    val isFavorite = content.id in favoriteIds

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f)
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to AppScaffoldBackground,
                            0.5f to Color.Transparent,
                            1.0f to AppScaffoldBackground
                        ),
                        blendMode = BlendMode.DstIn
                    )
                }
        ) {
            CommonImage(
                imageUrl = content.imageUrl,
                width = null,
                height = 500.dp,
                borderRadius = 20.dp
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Transparent,
                        0.9f to AppScaffoldBackground
                    )
                )
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            if (content.label != null) {
                Box(
                    modifier = Modifier
                        .background(AppPrimary.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = content.label,
                        color = AppTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Text(
                text = content.title,
                color = AppTextPrimary,
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 30.sp
            )

            if (!content.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = content.description ?: "",
                    color = AppTextPrimary.copy(alpha = 0.7f),
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayClick,
                    modifier = Modifier
                        .weight(200f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppSecondary,
                        contentColor = AppTextPrimary
                    ),
                    shape = RoundedCornerShape(30.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver Ahora", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .background(AppTextPrimary.copy(alpha = 0.1f), CircleShape)
                ) {
                    IconButton(
                        onClick = { if (!isFavorite) onAddClick() },
                    ) {
                        Icon(if (isFavorite) Icons.Default.Check else Icons.Default.Add, contentDescription = "Add", tint = AppTextPrimary)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
