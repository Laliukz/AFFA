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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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

// ponytail: scrapers devuelven géneros en ES, metadata en EN — mapa mínimo para cruzarlos
private val GENRE_ES_EN = mapOf(
    "accion" to "action", "aventura" to "adventure", "comedia" to "comedy",
    "fantasia" to "fantasy", "terror" to "horror", "misterio" to "mystery",
    "ciencia ficcion" to "sci-fi", "recuentos de la vida" to "slice of life",
    "deportes" to "sports", "sobrenatural" to "supernatural", "suspenso" to "suspense",
    "psicologico" to "psychological", "musica" to "music", "escolares" to "school",
    "artes marciales" to "martial arts", "demencia" to "dementia"
)

private fun normGenre(s: String): String {
    val plain = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    val lower = plain.lowercase().trim()
    return GENRE_ES_EN[lower] ?: lower
}

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun AnimeHomeScreen(
    viewModel: HomeViewModel? = null,
    detailsViewModel: DetailsViewModel? = null,
    playerViewModel: PlayerViewModel? = null,
    exploreViewModel: ExploreViewModel? = null,
    myListViewModel: MyListViewModel? = null
) {
    var currentScreen by remember { mutableStateOf("home") }
    var selected by remember { mutableStateOf<HomeAnimeItem?>(null) }
    var playEp by remember { mutableStateOf<com.lkz.affa.domain.model.Episode?>(null) }
    var seeAll by remember { mutableStateOf<Pair<String, SeeAllSection>?>(null) }
    // ponytail: volver a la pantalla de origen (home o explorar)
    var detailsReturn by remember { mutableStateOf("home") }
    val uiState by viewModel?.uiState?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(HomeUiState.Loading) }
    val savedItems by myListViewModel?.items?.collectAsStateWithLifecycle()
        ?: remember { mutableStateOf(emptyList()) }

    when (currentScreen) {
        "details" -> {
            val isSaved = remember(savedItems, selected?.title) {
                val t = selected?.title?.lowercase()?.trim() ?: ""
                savedItems.any { it.title.lowercase().trim() == t }
            }
            AnimeDetailScreen(
                item = selected,
                viewModel = detailsViewModel,
                isSavedInMyList = isSaved,
                onToggleMyList = { item -> myListViewModel?.toggle(item) },
                onBackClick = { currentScreen = detailsReturn },
                onEpisodeClick = { ep ->
                    playEp = ep
                    currentScreen = "player"
                }
            )
        }
        "player" -> {
            val details =
                detailsViewModel?.uiState?.collectAsStateWithLifecycle()?.value as? DetailsUiState.Success
            val homeData =
                (viewModel?.uiState?.collectAsStateWithLifecycle()?.value as? HomeUiState.Success)?.data
            val selGenres = remember(details) {
                (details?.details?.genres ?: details?.item?.genres.orEmpty()).map(::normGenre).toSet()
            }
            val pool = remember(homeData) {
                if (homeData == null) emptyList()
                else (homeData.heroItems + homeData.popularItems + homeData.recentlyAddedItems + homeData.recentEpisodes)
                    .filter { it.title != selected?.title }
                    .distinctBy { it.title.lowercase().trim() }
            }
            val genreMatch = remember(pool, selGenres) {
                if (selGenres.isEmpty()) emptyList()
                else pool.filter { it.genres.any { g -> normGenre(g) in selGenres } }.take(10)
            }
            // ponytail: sin cruce de géneros (ES vs EN) se muestran populares, no hueco vacío
            val similars = genreMatch.ifEmpty { pool.take(10) }
            val similarTitle = if (genreMatch.isNotEmpty()) "Similares" else "También te puede interesar"
            NowPlayingScreen(
                animeTitle = selected?.title ?: "",
                animeImageUrl = selected?.imageUrl ?: "",
                aliases = ((selected?.let { listOf(it.title) }.orEmpty()) + (selected?.synonyms.orEmpty())),
                episode = playEp,
                episodes = details?.episodes.orEmpty(),
                viewModel = playerViewModel,
                details = details?.details,
                animeScore = details?.item?.score,
                similarItems = similars,
                similarTitle = similarTitle,
                episodeUrl = selected?.episodeUrl,
                onBackClick = { currentScreen = "details" },
                onSimilarClick = {
                    selected = it
                    detailsReturn = "home"
                    currentScreen = "details"
                }
            )
        }
        "explore" -> {
            AnimeSearchScreen(
                viewModel = exploreViewModel,
                currentScreen = currentScreen,
                onScreenSelected = { currentScreen = it },
                onAnimeClick = {
                    selected = it
                    detailsReturn = "explore"
                    currentScreen = "details"
                },
                onBackClick = { currentScreen = "home" }
            )
        }
        "mylist" -> {
            MyListScreen(
                currentScreen = currentScreen,
                viewModel = myListViewModel,
                onScreenSelected = { currentScreen = it },
                onAnimeClick = {
                    selected = it
                    detailsReturn = "mylist"
                    currentScreen = "details"
                }
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
                                onAnimeClick = { item ->
                                    selected = item
                                    detailsReturn = "home"
                                    currentScreen = "details"
                                }
                            )
                        } else {
                            val refreshing by viewModel?.isRefreshing?.collectAsStateWithLifecycle()
                                ?: remember { mutableStateOf(false) }
                            HomeContent(
                                data = state.data,
                                paddingValues = paddingValues,
                                isRefreshing = refreshing,
                                onRefresh = { viewModel?.loadHome() },
                                onAnimeClick = { item ->
                                    selected = item
                                    detailsReturn = "home"
                                    currentScreen = "details"
                                },
                                onSeeAll = { title, section -> seeAll = title to section },
                                onSearchClick = { currentScreen = "explore" },
                                isInList = { title ->
                                    val t = title.lowercase().trim()
                                    savedItems.any { it.title.lowercase().trim() == t }
                                },
                                onAddClick = { item -> myListViewModel?.toggle(item) }
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
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onAnimeClick: (HomeAnimeItem?) -> Unit,
    onSeeAll: (String, SeeAllSection) -> Unit,
    onSearchClick: () -> Unit = {},
    isInList: (String) -> Boolean = { false },
    onAddClick: (HomeAnimeItem) -> Unit = {}
) {
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
        TopBar(onSearchClick = onSearchClick)
        HeroCarousel(
            items = data.heroItems,
            isInList = isInList,
            onAnimeClick = { item -> onAnimeClick(item) },
            onAddClick = onAddClick
        )
        ContinueWatchingSection(onAnimeClick = { onAnimeClick(null) })
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
    onAnimeClick: (HomeAnimeItem?) -> Unit,
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
                    .clickable { onAnimeClick(item) }
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
    onAnimeClick: (HomeAnimeItem?) -> Unit,
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
                    .clickable { onAnimeClick(item) }
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
    onAnimeClick: (HomeAnimeItem?) -> Unit,
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
                    .clickable { onAnimeClick(item) }
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

