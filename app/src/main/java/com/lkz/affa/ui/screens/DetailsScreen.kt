package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import com.lkz.affa.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lkz.affa.data.repository.FavoritesRepository
import com.lkz.affa.navigation.Screen
import com.lkz.affa.ui.components.*
import com.lkz.affa.ui.viewmodels.DetailsState
import com.lkz.affa.ui.viewmodels.DetailsViewModel
import com.lkz.affa.ui.viewmodels.EpisodesState
import com.lkz.affa.ui.viewmodels.EpisodesViewModel
import com.lkz.affa.ui.viewmodels.MyListViewModel
import com.lkz.affa.ui.theme.*

data class Episode(
        val number: Int,
        val title: String,
        val duration: String,
        val imageUrl: String,
        val description: String
)

@Composable
fun DetailsScreen(
        navController: NavController,
        animeId: String,
        myListViewModel: MyListViewModel = viewModel(),
        detailsViewModel: DetailsViewModel = viewModel(),
        episodesViewModel: EpisodesViewModel = viewModel()
) {
    var showStatusDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("") }

    val uiState by detailsViewModel.uiState.collectAsState()
    val episodesState by episodesViewModel.state.collectAsState()
    val favoriteIds by FavoritesRepository.favoriteIds.collectAsState()
    val isFavorite = animeId in favoriteIds

    when (val state = uiState) {
        is DetailsState.Loading -> {
            Box(
                    modifier = Modifier.fillMaxSize().background(AppScaffoldBackground),
                    contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = AppPrimary) }
        }
        is DetailsState.Error -> {
            Box(
                    modifier = Modifier.fillMaxSize().background(AppScaffoldBackground),
                    contentAlignment = Alignment.Center
            ) { Text(text = state.message, color = AppError) }
        }
        is DetailsState.Success -> {
            val content = state.content
            
            val episodesStr = stringResource(R.string.details_tab_episodes)
            val seasonsStr = stringResource(R.string.details_tab_seasons)
            val similarStr = stringResource(R.string.details_tab_similar)
            
            if (selectedTab.isEmpty()) {
                selectedTab = episodesStr
            }

            LaunchedEffect(content.title) { episodesViewModel.loadEpisodes(content) }

            LazyColumn(modifier = Modifier.fillMaxSize().background(AppScaffoldBackground)) {
                item {
                    DetailsHeader(
                        imageUrl = content.imageUrl,
                        bannerUrl = content.bannerUrl,
                        onBackClick = { navController.popBackStack() },
                        onCastClick = { }
                    )
                }

                item {
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val statusText =
                                    when {
                                        content.releaseStatus?.equals("FINISHED", true) == true ->
                                                stringResource(R.string.mylist_status_finished)
                                        content.releaseStatus?.equals("RELEASING", true) == true ->
                                                stringResource(R.string.mylist_status_releasing)
                                        content.releaseStatus?.equals("NOT_YET_RELEASED", true) ==
                                                true -> stringResource(R.string.mylist_status_upcoming)
                                        else -> stringResource(R.string.mylist_status_unknown)
                                    }
                            ContainerLabel(text = statusText)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = content.title,
                                color = AppTextPrimary,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                                text = stringResource(R.string.details_type_tags, content.tags?.take(2)?.joinToString(", ") ?: stringResource(R.string.details_default_tags)),
                                color = AppTextSecondary,
                                fontSize = 12.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val rawRating = content.rating ?: 0.0
                            val ratingScale10 =
                                    if (rawRating > 10.0) rawRating / 10.0 else rawRating

                            Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Rating",
                                    tint = AppPrimary,
                                    modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                    text = String.format("%.1f", ratingScale10),
                                    color = AppTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Row {
                                val score = (ratingScale10 / 2.0).toInt()
                                repeat(score) {
                                    Icon(
                                            Icons.Default.Star,
                                            contentDescription = null,
                                            tint = AppPrimary,
                                            modifier = Modifier.size(16.dp)
                                    )
                                }
                                repeat(5 - score) {
                                    Icon(
                                            Icons.Default.StarBorder,
                                            contentDescription = null,
                                            tint = AppPrimary,
                                            modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            val maxEp = if (episodesState is EpisodesState.Success) {
                                (episodesState as EpisodesState.Success).episodes.maxOfOrNull { it.number } ?: content.episodesCount ?: 1
                            } else {
                                content.episodesCount ?: Int.MAX_VALUE
                            }
                            
                            val nextEpToWatch = (1..maxEp).firstOrNull { !state.watchedEpisodes.contains(it) } ?: 1
                            val isFinished = state.watchedEpisodes.size >= maxEp && maxEp > 0
                            val buttonLabel = if (isFinished) stringResource(R.string.details_watch_again) else stringResource(R.string.details_play_episode, nextEpToWatch)

                            PrimaryButton(
                                    label = buttonLabel,
                                    onClick = { navController.navigate(Screen.VideoPlayer.createRoute(content.id ?: "", nextEpToWatch)) },
                                    icon = Icons.Default.PlayArrow,
                                    modifier = Modifier.weight(1f)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            IconButton(
                                    onClick = { 
                                        if (!isFavorite) {
                                            showStatusDialog = true 
                                        } else {
                                            myListViewModel.removeFavorite(animeId)
                                        }
                                    },
                                    modifier =
                                            Modifier.size(48.dp)
                                                    .background(
                                                            AppSurfaceLight,
                                                            RoundedCornerShape(12.dp)
                                                    )
                            ) {
                                Icon(
                                        if (isFavorite) Icons.Default.Check else Icons.Default.Add,
                                        contentDescription = stringResource(R.string.details_add_to_list),
                                        tint = AppTextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        DetailsSynopsis(description = content.description)

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tabs = mutableListOf(episodesStr, similarStr)
                            if (!content.relations.isNullOrEmpty()) {
                                tabs.add(1, seasonsStr)
                            }
                            tabs.forEachIndexed { index, tab ->
                                val isSelected = selectedTab == tab
                                Column(
                                        modifier = Modifier.clickable { selectedTab = tab },
                                        horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                            text = tab,
                                            color =
                                                    if (isSelected) AppTextPrimary
                                                    else AppTextSecondary,
                                            fontWeight =
                                                    if (isSelected) FontWeight.Bold
                                                    else FontWeight.Normal,
                                            fontSize = 16.sp
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Box(
                                            modifier =
                                                    Modifier.height(2.dp)
                                                            .width(20.dp)
                                                            .background(
                                                                    if (isSelected) AppPrimary
                                                                    else Color.Transparent
                                                            )
                                    )
                                }
                                if (index < tabs.size - 1) {
                                    Spacer(modifier = Modifier.width(24.dp))
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }

                when (selectedTab) {
                    episodesStr -> {
                        item {
                            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                                Row(
                                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                                text = stringResource(R.string.details_all_episodes),
                                                color = AppTextPrimary,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                                Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = AppTextPrimary
                                        )
                                    }
                                    if (episodesState is EpisodesState.Success) {
                                        val episodesList =
                                                (episodesState as EpisodesState.Success).episodes
                                        TextButton(
                                                onClick = {
                                                    val maxEp =
                                                            episodesList.maxOfOrNull { it.number }
                                                                    ?: 1
                                                    detailsViewModel.toggleEpisodeWatched(
                                                            content.id ?: "",
                                                            maxEp,
                                                            true
                                                    )
                                                }
                                        ) {
                                            Text(
                                                    stringResource(R.string.details_mark_all),
                                                    color = AppPrimary,
                                                    fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        when (val currentEpisodesState = episodesState) {
                            is EpisodesState.Loading -> {
                                item {
                                    Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                    ) { CircularProgressIndicator(color = AppPrimary) }
                                }
                            }
                            is EpisodesState.Error -> {
                                item {
                                    Box(
                                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                                            contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                                text = stringResource(R.string.details_error_episodes),
                                                color = AppError
                                        )
                                    }
                                }
                            }
                            is EpisodesState.Success -> {
                                val episodesList = currentEpisodesState.episodes
                                if (episodesList.isEmpty()) {
                                    item {
                                        Box(
                                                modifier = Modifier.fillMaxWidth().padding(32.dp),
                                                contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                    text = stringResource(R.string.details_no_episodes_published),
                                                    color = AppTextSecondary
                                            )
                                        }
                                    }
                                } else {
                                    items(episodesList) { animeEp ->
                                        val cleanTitle =
                                                if (animeEp.title.contains("Episode", true) ||
                                                                animeEp.title.contains(
                                                                        "Episodio",
                                                                        true
                                                                )
                                                )
                                                        ""
                                                else animeEp.title
                                        val mappedEpisode =
                                                Episode(
                                                        number = animeEp.number,
                                                        title =
                                                                cleanTitle.ifEmpty {
                                                                    stringResource(R.string.details_episode_number, animeEp.number)
                                                                },
                                                        duration = "",
                                                        imageUrl =
                                                                if (!animeEp.image.isNullOrEmpty())
                                                                        animeEp.image
                                                                else
                                                                        (content.bannerUrl
                                                                                ?: content.imageUrl),
                                                        description = ""
                                                )
                                        val isWatched = state.watchedEpisodes.contains(animeEp.number)
                                        EpisodeCard(
                                                episode = mappedEpisode,
                                                isWatched = isWatched,
                                                onMarkWatched = {
                                                    detailsViewModel.toggleEpisodeWatched(
                                                            content.id ?: "",
                                                            animeEp.number,
                                                            !isWatched
                                                    )
                                                },
                                                onClick = {
                                                    navController.navigate(Screen.VideoPlayer.createRoute(content.id ?: "", animeEp.number))
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    seasonsStr -> {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            content.relations?.chunked(3)?.forEach { rowItems ->
                                Row(
                                        modifier =
                                                Modifier.fillMaxWidth()
                                                        .height(180.dp)
                                                        .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    rowItems.forEach { related ->
                                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                            ContentCard(
                                                    content = related,
                                                    isFlexible = true,
                                                    onClick = {
                                                        related.id?.let { id ->
                                                            navController.navigate(
                                                                    Screen.Details.createRoute(id)
                                                            )
                                                        }
                                                    }
                                            )
                                        }
                                    }
                                    repeat(3 - rowItems.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                    similarStr -> {
                        val similarAnime = state.similarAnime
                        if (similarAnime.isEmpty()) {
                            item {
                                Box(
                                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                                        contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                            text = stringResource(R.string.details_no_related_content),
                                            color = AppTextSecondary,
                                            fontSize = 14.sp
                                    )
                                }
                            }
                        } else {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                                similarAnime.chunked(3).forEach { rowItems ->
                                    Row(
                                            modifier =
                                                    Modifier.fillMaxWidth()
                                                            .height(180.dp)
                                                            .padding(horizontal = 16.dp),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        rowItems.forEach { similar ->
                                            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                                                ContentCard(
                                                        content = similar,
                                                        isFlexible = true,
                                                        onClick = {
                                                            similar.id?.let { id ->
                                                                navController.navigate(
                                                                        Screen.Details.createRoute(
                                                                                id
                                                                        )
                                                                )
                                                            }
                                                        }
                                                )
                                            }
                                        }
                                        repeat(3 - rowItems.size) {
                                            Spacer(modifier = Modifier.weight(1f))
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                }
            }

            DetailsStatusDialog(
                showDialog = showStatusDialog,
                onDismiss = { showStatusDialog = false },
                onStatusSelected = { internalStatus ->
                    myListViewModel.addFavorite(content, internalStatus)
                    showStatusDialog = false
                }
            )
        }
    }
}

@Composable
fun ContainerLabel(text: String) {
    Box(
            modifier =
                    Modifier.background(AppPrimary, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) { Text(text = text, color = AppTextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
}

