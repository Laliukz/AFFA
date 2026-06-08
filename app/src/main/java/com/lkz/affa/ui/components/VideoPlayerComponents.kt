package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.navigation.NavController
import com.lkz.affa.R
import com.lkz.affa.data.models.StreamLink
import com.lkz.affa.data.models.Content
import com.lkz.affa.ui.theme.*

@Composable
fun VideoPlayerArea() {}

@Composable
fun VideoEpisodeInfo(
    episodeNum: Int,
    title: String?,
    releaseStatus: String?
) {
    Text(
        text = stringResource(R.string.video_episode_upper, episodeNum.toString()),
        color = AppPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = title ?: stringResource(R.string.video_loading),
        color = AppTextPrimary,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        lineHeight = 22.sp
    )
    Spacer(modifier = Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = stringResource(R.string.video_released),
            tint = AppTextSecondary,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        val statusText = when (releaseStatus?.uppercase()) {
            "RELEASING" -> stringResource(R.string.mylist_status_releasing)
            "FINISHED" -> stringResource(R.string.mylist_status_finished)
            "NOT_YET_RELEASED" -> stringResource(R.string.mylist_status_upcoming)
            "CANCELLED" -> stringResource(R.string.mylist_status_cancelled)
            else -> releaseStatus ?: stringResource(R.string.mylist_status_unknown)
        }
        Text(statusText, fontSize = 12.sp, color = AppTextSecondary)
    }
}

@Composable
fun VideoServerSelector(
    streams: List<StreamLink>,
    currentStream: StreamLink?,
    onStreamSelected: (StreamLink) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = stringResource(R.string.video_select_server_title), color = AppTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = stringResource(R.string.video_report_problem), color = AppError, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
    Spacer(modifier = Modifier.height(12.dp))
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        streams.forEach { stream ->
            val isSelected = currentStream?.url == stream.url
            Button(
                onClick = { onStreamSelected(stream) },
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) AppPrimary.copy(alpha = 0.2f) else AppSurface,
                    contentColor = if (isSelected) AppPrimary else AppTextPrimary
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Dns, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stream.server, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(12.dp))
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(AppSuccess)) // Green dot
                }
            }
        }
    }
}

@Composable
fun VideoNavigationButtons(
    episodeNum: Int,
    maxEp: Int,
    onPreviousClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = {
                if (episodeNum > 1) {
                    onPreviousClick()
                }
            },
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppSurface, contentColor = AppTextMuted)
        ) {
            Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.video_ep_short, episodeNum - 1), fontSize = 16.sp)
        }
        Button(
            onClick = {
                if (episodeNum < maxEp) {
                    onNextClick()
                }
            },
            modifier = Modifier.weight(1f).height(50.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AppTextPrimary, contentColor = AppScaffoldBackground)
        ) {
            Text(stringResource(R.string.video_ep_short, episodeNum + 1), fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
