package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
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
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.lkz.affa.R
import com.lkz.affa.ui.components.SectionLabel
import com.lkz.affa.ui.theme.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.lkz.affa.ui.viewmodels.HistoryViewModel
import com.lkz.affa.ui.viewmodels.HistoryState
import androidx.compose.material3.CircularProgressIndicator
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class HistoryItem(
    val animeId: String,
    val showTitle: String,
    val episodeNumber: String,
    val episodeTitle: String?,
    val viewedAtMillis: Long, // Changed to Long for grouping
    val viewedAtStr: String,
    val timeLeft: String?,
    val progress: Float,
    val isCompleted: Boolean,
    val duration: String?,
    val imageUrl: String?
)



@Composable
fun HistoryScreen(navController: NavController, viewModel: HistoryViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppScaffoldBackground),
    ) {
        TopBarHistory { navController.popBackStack() }
        
        when (state) {
            is HistoryState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppPrimary)
                }
            }
            is HistoryState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = (state as HistoryState.Error).message, color = AppError)
                }
            }
            is HistoryState.Success -> {
                val maps = (state as HistoryState.Success).history
                val historyItems = maps.map { map ->
                    val animeId = map["anime_id"] as? String ?: ""
                    val title = map["anime_title"] as? String ?: "Desconocido"
                    val epNum = map["last_episode_number"] as? String ?: "1"
                    val updatedAt = map["updated_at"] as? Long ?: 0L
                    val pos = map["position_seconds"] as? Int ?: 0
                    val dur = map["duration_seconds"] as? Int ?: 0
                    val img = map["anime_image"] as? String
                    
                    val progress = if (dur > 0) pos.toFloat() / dur.toFloat() else 0f
                    val isCompleted = pos > 0 && dur > 0 && (dur - pos) < 30
                    val timeLeft = if (!isCompleted && dur > pos) "${(dur - pos) / 60}m restantes" else null
                    val durationStr = if (dur > 0) String.format("%02d:%02d", dur / 60, dur % 60) else null

                    val now = System.currentTimeMillis()
                    val diff = now - updatedAt
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    val mins = TimeUnit.MILLISECONDS.toMinutes(diff)
                    
                    val viewedAtStr = when {
                        days > 0 -> "Hace ${days}d"
                        hours > 0 -> "Hace ${hours}h"
                        mins > 0 -> "Hace ${mins}m"
                        else -> "Hace un momento"
                    }

                    HistoryItem(
                        animeId = animeId,
                        showTitle = title,
                        episodeNumber = if (epNum.startsWith("Ep")) epNum else "Episodio $epNum",
                        episodeTitle = null, // Can't easily get from history DB right now
                        viewedAtMillis = updatedAt,
                        viewedAtStr = viewedAtStr,
                        timeLeft = timeLeft,
                        progress = progress.coerceIn(0f, 1f),
                        isCompleted = isCompleted,
                        duration = durationStr,
                        imageUrl = img
                    )
                }
                HistoryList(historyItems = historyItems, navController = navController)
            }
        }
    }
}

@Composable
fun TopBarHistory(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.history_back),
            tint = AppTextPrimary,
            modifier = Modifier
                .size(28.dp)
                .clickable { onBackClick() }
        )
        Spacer(modifier = Modifier.width(24.dp))
        Text(
            text = stringResource(R.string.history_title),
            color = AppTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stringResource(R.string.history_clear_all),
            color = AppError,
            fontSize = 16.sp,
            modifier = Modifier.clickable { }
        )
    }
}

fun getGroupFor(millis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val calNow = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val calThen = Calendar.getInstance().apply { timeInMillis = millis }
    
    val currentYear = calNow.get(Calendar.YEAR)
    val currentDayOfYear = calNow.get(Calendar.DAY_OF_YEAR)
    
    val thenYear = calThen.get(Calendar.YEAR)
    val thenDayOfYear = calThen.get(Calendar.DAY_OF_YEAR)
    
    if (currentYear == thenYear) {
        if (currentDayOfYear == thenDayOfYear) return "Hoy"
        if (currentDayOfYear - thenDayOfYear == 1) return "Ayer"
        if (currentDayOfYear - thenDayOfYear <= 7) return "Esta semana"
        return "Anteriormente"
    }
    return "Anteriormente"
}

@Composable
fun HistoryList(historyItems: List<HistoryItem>, navController: NavController) {
    if (historyItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(64.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = stringResource(R.string.history_empty), color = AppTextSecondary, fontSize = 16.sp)
            }
        }
    } else {
        val groupedItems = historyItems.groupBy { getGroupFor(it.viewedAtMillis) }
        val order = listOf("Hoy", "Ayer", "Esta semana", "Anteriormente")
        
        LazyColumn(
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            order.forEach { groupName ->
                val itemsInGroup = groupedItems[groupName]
                if (!itemsInGroup.isNullOrEmpty()) {
                    item { SectionLabel(groupName) }
                    itemsInGroup.forEach { item ->
                        item { HistoryListItem(item = item, navController = navController) }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}


@Composable
fun HistoryListItem(item: HistoryItem, navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { navController.navigate(com.lkz.affa.navigation.Screen.Details.createRoute(item.animeId)) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(140.dp)
                .height(90.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppTextMuted)
        ) {
            com.lkz.affa.ui.components.CommonImage(
                imageUrl = item.imageUrl ?: "",
                modifier = Modifier.fillMaxSize(),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop
            )
            if (!item.isCompleted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(AppBackgroundDarkGray.copy(alpha = 0.8f))
                ) {
                    if (item.progress > 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(item.progress)
                                .background(AppPrimary)
                        )
                    }
                }
            }

            if (item.duration != null && !item.isCompleted) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(AppScaffoldBackground.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.duration,
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
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = item.showTitle,
                    color = AppTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.history_more_options),
                    tint = AppTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.episodeNumber,
                color = AppSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )

            if (item.episodeTitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.episodeTitle,
                    color = AppTextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.viewedAtStr,
                    color = AppTextSecondary,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.weight(1f))

                if (item.isCompleted) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = stringResource(R.string.history_watched),
                            tint = AppSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.history_watched),
                            color = AppSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else if (item.timeLeft != null) {
                    Text(
                        text = item.timeLeft,
                        color = AppTextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
