package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
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
import com.lkz.affa.data.models.Content
import com.lkz.affa.ui.theme.AppRating
import com.lkz.affa.ui.theme.AppTextSecondary
import com.lkz.affa.ui.theme.*

/**
 * Equivalent of Flutter's ContentListTile.
 * A horizontal row item with a poster thumbnail, title, subtitle and rating.
 *
 * @param content  The anime/content to display.
 * @param onClick  Callback when the row is tapped (caller handles navigation).
 */
@Composable
fun ContentListTile(
    content: Content,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        CommonImage(
            imageUrl = content.imageUrl,
            width = 100.dp,
            height = 145.dp,
            borderRadius = 8.dp,
            modifier = Modifier.clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = content.title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = AppTextPrimary
            )

            if (!content.subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = content.subtitle!!,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    color = AppTextSecondary
                )
            }

            if (content.rating != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = null,
                        tint = AppRating,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%.1f".format(content.rating),
                        fontSize = 12.sp,
                        color = AppTextPrimary.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
