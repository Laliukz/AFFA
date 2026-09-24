package com.lkz.affa.ui.screens
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkz.affa.ui.components.BottomNavigationBar
import com.lkz.affa.ui.components.HeroCarousel
import com.lkz.affa.ui.components.TopBar
import com.lkz.affa.ui.theme.*


import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.lkz.affa.core.metadata.MetadataModule
import com.lkz.affa.core.network.scraper.AnimeAv1Scraper
import com.lkz.affa.data.repository.HomeData
import com.lkz.affa.data.repository.HomeRepository
import com.lkz.affa.data.repository.SeeAllSection
import com.lkz.affa.domain.model.HomeAnimeItem

@Composable
fun AnimeHomeScreen(viewModel: HomeViewModel? = null) {
    var currentScreen by remember { mutableStateOf("home") }
    var seeAll by remember { mutableStateOf<Pair<String, SeeAllSection>?>(null) }
    val uiState by viewModel?.uiState?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(HomeUiState.Loading) }

    when (currentScreen) {
        "details" -> {
            AnimeDetailScreen(
                onBackClick = { currentScreen = "home" },
                onEpisodeClick = { currentScreen = "player" }
            )
        }
        "player" -> {
            NowPlayingScreen(
                onBackClick = { currentScreen = "details" }
            )
        }
        "explore" -> {
            AnimeSearchScreen(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
        "mylist" -> {
            MyListScreen(
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it }
            )
        }
        else -> {
            Scaffold(
                bottomBar = {
                    BottomNavigationBar(
                        currentScreen = currentScreen,
                        onScreenSelected = { currentScreen = it }
                    )
                },
                containerColor = DarkBackground
            ) { paddingValues ->
                when (val state = uiState) {
                    is HomeUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryPurple)
                        }
                    }
                    is HomeUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(paddingValues),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = state.message, color = Color.White)
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { viewModel?.loadHome() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                                ) {
                                    Text("Reintentar")
                                }
                            }
                        }
                    }
                    is HomeUiState.Success -> {
                        val target = seeAll
                        if (target != null) {
                            SeeAllScreen(
                                title = target.first,
                                section = target.second,
                                viewModel = viewModel,
                                paddingValues = paddingValues,
                                onBackClick = { seeAll = null },
                                onAnimeClick = { currentScreen = "details" }
                            )
                        } else {
                            HomeContent(
                                data = state.data,
                                paddingValues = paddingValues,
                                onAnimeClick = { currentScreen = "details" },
                                onSeeAll = { title, section -> seeAll = title to section }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeContent(
    data: HomeData,
    paddingValues: PaddingValues,
    onAnimeClick: () -> Unit,
    onSeeAll: (String, SeeAllSection) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        TopBar()
        HeroCarousel(items = data.heroItems, onAnimeClick = { onAnimeClick() })
        ContinueWatchingSection(onAnimeClick = onAnimeClick)
        TodayEpisodes(
            items = data.recentEpisodes,
            onAnimeClick = onAnimeClick,
            onSeeAll = { onSeeAll("Episodios Recientes", SeeAllSection.RECENT) }
        )
        PopularSection(
            items = data.popularItems,
            onAnimeClick = onAnimeClick,
            onSeeAll = { onSeeAll("Populares", SeeAllSection.POPULAR) }
        )
        RecentlyAddedSection(
            items = data.recentlyAddedItems,
            onAnimeClick = onAnimeClick,
            onSeeAll = { onSeeAll("Recién Añadidos", SeeAllSection.ADDED) }
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun ContinueWatchingSection(onAnimeClick: () -> Unit) {
    SectionHeader(title = "Continuar Viendo", showSeeAll = true)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(2) {
            Column(
                modifier = Modifier
                    .width(280.dp)
                    .clickable { onAnimeClick() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray) // Placeholder for Episode Image
                ) {
                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f) // 60% progress
                            .height(4.dp)
                            .background(PrimaryPurple)
                            .align(Alignment.BottomStart)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = "T1 : E12", color = PrimaryPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(
                    text = "Batalla Final en Neo-Tokyo",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(text = "24m restantes", color = TextGray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TodayEpisodes(
    items: List<HomeAnimeItem>,
    onAnimeClick: () -> Unit,
    onSeeAll: () -> Unit = {}
) {
    SectionHeader(title = "Episodios Recientes", showSeeAll = true, onSeeAll = onSeeAll)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .clickable { onAnimeClick() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = item.latestEpisode?.let { "EP ${if (it % 1 == 0f) it.toInt() else it}" } ?: "#${index + 1}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.genres.firstOrNull() ?: "Anime",
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PopularSection(
    items: List<HomeAnimeItem>,
    onAnimeClick: () -> Unit,
    onSeeAll: () -> Unit = {}
) {
    SectionHeader(title = "Populares", showSeeAll = true, onSeeAll = onSeeAll)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .clickable { onAnimeClick() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Text(
                        text = "#${index + 1}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    if (item.score != null) {
                        Text(
                            text = "★ ${item.score}",
                            color = RatingYellow,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.genres.take(2).joinToString(", ").ifEmpty { "Anime" },
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun RecentlyAddedSection(
    items: List<HomeAnimeItem>,
    onAnimeClick: () -> Unit,
    onSeeAll: () -> Unit = {}
) {
    SectionHeader(title = "Recién Añadidos", showSeeAll = true, onSeeAll = onSeeAll)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items.size) { index ->
            val item = items[index]
            Column(
                modifier = Modifier
                    .width(150.dp)
                    .clickable { onAnimeClick() }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CardBackground)
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    if (index < 2) {
                        Text(
                            text = "Nuevo",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(PrimaryPurple, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.genres.firstOrNull() ?: "Anime",
                    color = TextGray,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, showSeeAll: Boolean = false, onSeeAll: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        if (showSeeAll) {
            Text(
                text = "Ver Todo",
                color = PrimaryPurple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(enabled = onSeeAll != null) { onSeeAll?.invoke() }
            )
        }
    }
}

