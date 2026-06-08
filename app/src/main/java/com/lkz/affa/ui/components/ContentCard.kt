package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkz.affa.data.models.Content
import com.lkz.affa.ui.theme.AppPrimary
import com.lkz.affa.ui.theme.AppSecondary
import com.lkz.affa.ui.theme.*

@Composable
fun ContentCard(
        content: Content,
        isLarge: Boolean = false,
        hasTextOverlay: Boolean = false,
        isFlexible: Boolean = false,
        customWidth: Dp? = null,
        customHeight: Dp? = null,
        badgeText: String? = null,
        onClick: () -> Unit = {},
        modifier: Modifier = Modifier
) {
    val width =
            customWidth
                    ?: if (hasTextOverlay || isFlexible) null else if (isLarge) 200.dp else 140.dp
    val height =
            customHeight
                    ?: if (isFlexible) null
                    else if (hasTextOverlay) 240.dp else if (isLarge) 280.dp else 200.dp

    val baseModifier =
            modifier
                    .clickable { onClick() }
                    .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())

    Column(
            modifier =
                    baseModifier.then(
                            if (hasTextOverlay) Modifier else Modifier.padding(end = 16.dp)
                    ),
            horizontalAlignment = Alignment.Start
    ) {
        val imageModifier =
                Modifier.then(if (height != null) Modifier.height(height) else Modifier)
                        .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())

        val imageStack =
                @Composable
                {
                    Box(modifier = Modifier.clip(RoundedCornerShape(12.dp)).then(imageModifier)) {
                        CommonImage(
                                imageUrl = content.imageUrl,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                        )

                        if (hasTextOverlay) {
                            Box(
                                    modifier =
                                            Modifier.fillMaxSize()
                                                    .background(
                                                            Brush.verticalGradient(
                                                                    colors =
                                                                            listOf(
                                                                                    Color.Transparent,
                                                                                    AppScaffoldBackground
                                                                                            .copy(
                                                                                                    alpha =
                                                                                                            0.87f
                                                                                            )
                                                                            ),
                                                                    startY = 50f
                                                            )
                                                    )
                            )
                        }

                        if (content.label != null) {
                            Box(
                                    modifier =
                                            Modifier.padding(top = 5.dp, start = 2.dp)
                                                    .background(
                                                        AppScaffoldBackground.copy(alpha = 0.5f),
                                                            RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                        text = content.label,
                                        color = AppTextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontStyle =
                                                if (content.label.startsWith("#")) FontStyle.Italic
                                                else FontStyle.Normal
                                )
                            }
                        }

                        if (content.progress != null) {
                            Box(
                                    modifier =
                                            Modifier.align(Alignment.BottomCenter)
                                                    .fillMaxWidth()
                                                    .clip(
                                                            RoundedCornerShape(
                                                                    bottomStart = 12.dp,
                                                                    bottomEnd = 12.dp
                                                            )
                                                    )
                            ) {
                                AppProgressBar(
                                        value = content.progress,
                                        backgroundColor = AppTextPrimary.copy(alpha = 0.24f)
                                )
                            }
                        }

                        if (badgeText != null) {
                            Box(
                                    modifier =
                                            Modifier.align(Alignment.BottomStart)
                                                    .padding(start = 3.dp, bottom = 3.dp)
                                                    .background(
                                                            AppScaffoldBackground.copy(alpha = 0.5f),
                                                            RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 5.dp, vertical = 2.dp)
                            ) {
                                Text(
                                        text = badgeText,
                                        color = AppTextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        if (hasTextOverlay) {
                            Column(
                                    modifier =
                                            Modifier.align(Alignment.BottomStart)
                                                    .padding(
                                                            start = 12.dp,
                                                            end = 12.dp,
                                                            bottom = 12.dp
                                                    )
                            ) {
                                Text(
                                        text = content.title,
                                        color = AppTextPrimary,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        lineHeight = 17.6.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (content.rating != null) {
                                        Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = AppRating,
                                                modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                                text = content.rating.toString(),
                                                color = AppTextPrimary,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                    }
                                    if (content.subtitle != null) {
                                        Text(
                                                text = content.subtitle,
                                                color = AppTextSecondary,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

        if (isFlexible) {
            Box(modifier = Modifier.weight(1f)) { imageStack() }
        } else {
            imageStack()
        }

        if (!hasTextOverlay) {
            Spacer(modifier = Modifier.height(8.dp))
            if (content.subtitle != null && content.subtitle.startsWith("T")) {
                Text(
                        text = content.subtitle.split("•").first().trim(),
                        color = AppSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                    text = content.title,
                    color = AppTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
            )
            if (content.subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                val subtitleText =
                        if (content.subtitle.startsWith("T")) {
                            content.subtitle.split("•").last().trim()
                        } else {
                            content.subtitle
                        }
                Text(
                        text = subtitleText,
                        color = AppTextSecondary,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun getLabelColor(label: String): Color {
    if (label == "NEW EP") return AppSecondary
    if (label == "DUB") return AppPrimary
    if (label == "Nuevo" || label.lowercase().contains("top")) return AppSecondary
    return AppScaffoldBackground.copy(alpha = 0.7f)
}
