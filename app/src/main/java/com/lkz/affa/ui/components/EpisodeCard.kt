package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkz.affa.R
import com.lkz.affa.ui.theme.*
import com.lkz.affa.ui.screens.Episode

@Composable
fun EpisodeCard(episode: Episode, isWatched: Boolean = false, onMarkWatched: () -> Unit = {}, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            CommonImage(
                imageUrl = episode.imageUrl,
                width = 140.dp,
                height = 90.dp,
                borderRadius = 8.dp
            )
            
            if (episode.duration.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .background(AppScaffoldBackground.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = episode.duration,
                        color = AppTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (isWatched) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .background(AppPrimary.copy(alpha = 0.9f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.history_watched).uppercase(),
                        color = AppTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "E${episode.number}",
                    style = AppTextStyles.label.copy(color = AppSecondary)
                )
                IconButton(
                    onClick = onMarkWatched,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (isWatched) Icons.Default.CheckCircle else Icons.Default.CheckCircleOutline, 
                        contentDescription = stringResource(R.string.episode_mark_watched), 
                        tint = if (isWatched) AppPrimary else AppTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Text(
                text = episode.title,
                style = AppTextStyles.cardTitle,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.episode_tap_to_watch),
                style = AppTextStyles.body.copy(color = AppTextSecondary, fontSize = 11.sp)
            )
            if (episode.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.description,
                    style = AppTextStyles.body.copy(color = AppTextSecondary, fontSize = 12.sp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
