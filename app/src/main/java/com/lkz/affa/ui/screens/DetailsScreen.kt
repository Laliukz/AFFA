package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lkz.affa.domain.model.HomeAnimeItem
import com.lkz.affa.ui.components.EpisodeItem
import com.lkz.affa.ui.components.toUiEpisode
import com.lkz.affa.ui.theme.AppPrimary
import com.lkz.affa.ui.theme.AppTextPrimary
import com.lkz.affa.ui.theme.DarkBackground
import com.lkz.affa.ui.theme.RatingYellow
import com.lkz.affa.ui.theme.TextGray

data class Episode(
    val number: String,
    val title: String,
    val duration: String,
    val description: String,
    val progress: Float? = null
)

@Composable
fun AnimeDetailScreen(
    item: HomeAnimeItem?,
    viewModel: DetailsViewModel? = null,
    isSavedInMyList: Boolean = false,
    onToggleMyList: (HomeAnimeItem) -> Unit = {},
    onBackClick: () -> Unit = {},
    onEpisodeClick: (com.lkz.affa.domain.model.Episode) -> Unit = {}
) {
    LaunchedEffect(item?.title) { viewModel?.load(item) }
    val state = viewModel?.uiState?.collectAsStateWithLifecycle()?.value

    Scaffold(containerColor = DarkBackground) { paddingValues ->
        when (state) {
            null, is DetailsUiState.Loading -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = AppPrimary) }

            is DetailsUiState.Error -> Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = state.message, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel?.retry() },
                        colors = ButtonDefaults.buttonColors(containerColor = AppPrimary)
                    ) { Text("Reintentar") }
                }
            }

            is DetailsUiState.Success -> {
                val pairs = state.episodes.map { dom -> dom to dom.toUiEpisode() }
                // Respaldo de imagen: metadata → póster del scraper (algunos items llegan sin cover)
                val poster = item?.imageUrl?.takeIf { it.isNotBlank() }
                    ?: state.details?.imageUrl.orEmpty()
                DetailsContent(
                    paddingValues = paddingValues,
                    item = state.item,
                    posterUrl = poster,
                    synopsisEs = state.details?.synopsis?.takeIf { it.isNotBlank() },
                    pairs = pairs,
                    isSavedInMyList = isSavedInMyList,
                    onToggleMyList = onToggleMyList,
                    onBackClick = onBackClick,
                    onEpisodeClick = onEpisodeClick,
                    watched = viewModel?.watched?.collectAsStateWithLifecycle()?.value.orEmpty(),
                    onToggleWatched = { viewModel?.toggleWatched(it) }
                )
            }
        }
    }
}

@Composable
private fun DetailsContent(
    paddingValues: PaddingValues,
    item: HomeAnimeItem,
    posterUrl: String,
    synopsisEs: String?,
    pairs: List<Pair<com.lkz.affa.domain.model.Episode, Episode>>,
    isSavedInMyList: Boolean = false,
    onToggleMyList: (HomeAnimeItem) -> Unit = {},
    onBackClick: () -> Unit,
    onEpisodeClick: (com.lkz.affa.domain.model.Episode) -> Unit,
    watched: Set<String> = emptySet(),
    onToggleWatched: (String) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(paddingValues),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
                AsyncImage(
                    model = item.bannerUrl ?: posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.5f),
                                Color.Transparent,
                                DarkBackground.copy(alpha = 0.7f),
                                DarkBackground
                            )
                        )
                    )
                )
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        .size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = Color.White
                    )
                }
                // Ficha montada sobre el degradado: recupera ~100dp
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    AsyncImage(
                        model = posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(110.dp)
                            .height(155.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.DarkGray)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.score != null) {
                                Text(
                                    text = "★ ${item.score}",
                                    color = RatingYellow,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                        if (pairs.isNotEmpty()) {
                            Text(
                                text = "${pairs.size} episodios",
                                    color = TextGray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                        if (item.genres.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.genres.take(3).joinToString(" • "),
                                color = TextGray,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        item {
            // Sinopsis del scraper (español); respaldo la de metadata (inglés)
            val synopsis = synopsisEs ?: item.synopsis
            if (!synopsis.isNullOrBlank()) {
                var expanded by androidx.compose.runtime.remember(item.title) {
                    androidx.compose.runtime.mutableStateOf(false)
                }
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = synopsis,
                        color = TextGray,
                        fontSize = 13.sp,
                        maxLines = if (expanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (expanded) "Ver menos" else "Ver más",
                        color = AppPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clickable { expanded = !expanded }
                    )
                }
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Button(
                    onClick = { pairs.firstOrNull()?.first?.let(onEpisodeClick) },
                    enabled = pairs.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = AppPrimary),
                    shape = RoundedCornerShape(24.dp),
                    contentPadding = PaddingValues(horizontal = 60.dp, vertical = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = AppTextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Ver Ahora",
                        color = AppTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = { onToggleMyList(item) },
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (isSavedInMyList) AppPrimary else AppTextPrimary.copy(alpha = 0.5f)
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSavedInMyList) AppPrimary.copy(alpha = 0.2f) else DarkBackground.copy(alpha = 0.55f)
                        ),
                        contentPadding = PaddingValues(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isSavedInMyList) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = if (isSavedInMyList) "En Mi Lista" else "Agregar a Mi Lista",
                            tint = if (isSavedInMyList) AppPrimary else AppTextPrimary
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
        }
        item {
            Text(
                text = "Episodios",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
        if (pairs.isEmpty()) {
            item {
                Text(
                    text = "Sin episodios disponibles",
                    color = TextGray,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        } else {
            items(pairs) { (dom, ui) ->
                EpisodeItem(
                    episode = ui,
                    imageUrl = posterUrl,
                    isWatched = dom.url in watched,
                    onClick = { onEpisodeClick(dom) },
                    onWatchedClick = { onToggleWatched(dom.url) }
                )
            }
        }
    }
}
