package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lkz.affa.domain.model.HomeAnimeItem
import com.lkz.affa.ui.components.BottomNavigationBar
import com.lkz.affa.ui.theme.AppPrimary
import com.lkz.affa.ui.theme.CardBackground
import com.lkz.affa.ui.theme.DarkBackground
import com.lkz.affa.ui.theme.FilterActiveRed
import com.lkz.affa.ui.theme.RatingYellow
import com.lkz.affa.ui.theme.TextGray

@Composable
fun AnimeSearchScreen(
    viewModel: ExploreViewModel?,
    currentScreen: String = "explore",
    onScreenSelected: (String) -> Unit = {},
    onAnimeClick: (HomeAnimeItem) -> Unit = {},
    onBackClick: () -> Unit = {}
) {
    val query by viewModel?.query?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("") }
    val results by viewModel?.results?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(emptyList()) }
    val loading by viewModel?.loading?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val genre by viewModel?.genre?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(null) }
    val year by viewModel?.year?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(null) }
    val status by viewModel?.status?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(null) }
    val sort by viewModel?.sort?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("POPULARITY_DESC") }
    val loadingMore by viewModel?.loadingMore?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val hasMore by viewModel?.hasMore?.collectAsStateWithLifecycle()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel?.observe() }

    Scaffold(
        bottomBar = { BottomNavigationBar(currentScreen, onScreenSelected) },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            SearchTopBar(
                query = query,
                onQuery = { viewModel?.query?.value = it },
                onBack = onBackClick
            )
            FilterRow(
                sort = sort,
                onSort = { viewModel?.sort?.value = it },
                genre = genre,
                onGenre = { viewModel?.genre?.value = it },
                year = year,
                onYear = { viewModel?.year?.value = it },
                status = status,
                onStatus = { viewModel?.status?.value = it }
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (query.isBlank()) "Tendencias" else "Resultados",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(CardBackground, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${results.size}",
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            when {
                loading && results.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppPrimary)
                }
                results.isEmpty() -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (query.isBlank() && genre == null && year == null && status == null) "Cargando tendencias…" else "Sin resultados",
                        color = TextGray,
                        fontSize = 14.sp
                    )
                }
                else -> {
                    val gridState = rememberLazyGridState()
                    LaunchedEffect(gridState, results.size, loadingMore, hasMore) {
                        androidx.compose.runtime.snapshotFlow {
                            val layoutInfo = gridState.layoutInfo
                            val totalItems = layoutInfo.totalItemsCount
                            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisible to totalItems
                        }.collect { (lastVisible, totalItems) ->
                            if (hasMore && !loadingMore && totalItems > 0 && lastVisible >= totalItems - 4) {
                                viewModel?.loadMore()
                            }
                        }
                    }

                    LazyVerticalGrid(
                        state = gridState,
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(results, key = { it.title }) { item ->
                            ExploreCard(item = item, onClick = { onAnimeClick(item) })
                        }
                        if (loadingMore) {
                            item(span = { GridItemSpan(2) }) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = AppPrimary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchTopBar(query: String, onQuery: (String) -> Unit, onBack: () -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Atrás",
                tint = Color.White
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        TextField(
            value = query,
            onValueChange = onQuery,
            placeholder = { Text(text = "Buscar anime…", color = TextGray, fontSize = 14.sp) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Limpiar",
                            tint = TextGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
        )
    }
}

@Composable
private fun ExploreCard(item: HomeAnimeItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground),
            contentAlignment = Alignment.Center
        ) {
            if (item.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
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
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 15.sp,
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

private val SORT_OPTIONS = listOf(
    "POPULARITY_DESC" to "Popularidad",
    "SCORE_DESC" to "Puntuación",
    "TRENDING_DESC" to "Tendencia",
    "TITLE_ROMAJI" to "A-Z"
)

private val GENRE_OPTIONS: List<Pair<String?, String>> = listOf(
    null to "Genre",
    "Action" to "Action",
    "Adventure" to "Adventure",
    "Comedy" to "Comedy",
    "Drama" to "Drama",
    "Fantasy" to "Fantasy",
    "Horror" to "Horror",
    "Mystery" to "Mystery",
    "Music" to "Music",
    "Mecha" to "Mecha",
    "Psychological" to "Psychological",
    "Romance" to "Romance",
    "Sci-Fi" to "Sci-Fi",
    "Slice of Life" to "Slice of Life",
    "Sports" to "Sports",
    "Supernatural" to "Supernatural",
    "Suspense" to "Suspense"
)

private val STATUS_OPTIONS: List<Pair<String?, String>> = listOf(
    null to "Status",
    "RELEASING" to "En emisión",
    "FINISHED" to "Finalizado"
)

@Composable
private fun FilterRow(
    sort: String,
    onSort: (String) -> Unit,
    genre: String?,
    onGenre: (String?) -> Unit,
    year: Int?,
    onYear: (Int?) -> Unit,
    status: String?,
    onStatus: (String?) -> Unit
) {
    val thisYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val yearOptions = remember(thisYear) {
        listOf<Pair<Int?, String>>(null to "Year") + (0..8).map { (thisYear - it) to (thisYear - it).toString() }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var sortOpen by remember { mutableStateOf(false) }
        Box {
            Button(
                onClick = { sortOpen = true },
                colors = ButtonDefaults.buttonColors(containerColor = FilterActiveRed),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Sort",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Sort By",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            DropdownMenu(
                expanded = sortOpen,
                onDismissRequest = { sortOpen = false },
                modifier = Modifier.background(CardBackground)
            ) {
                SORT_OPTIONS.forEach { (v, l) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = (if (v == sort) "✓ " else "") + l,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        },
                        onClick = { onSort(v); sortOpen = false }
                    )
                }
            }
        }
        FilterPill(
            selected = genre,
            options = GENRE_OPTIONS,
            onPick = onGenre
        )
        FilterPill(
            selected = year,
            options = yearOptions,
            onPick = onYear
        )
        FilterPill(
            selected = status,
            options = STATUS_OPTIONS,
            onPick = onStatus
        )
    }
}

@Composable
private fun <T> FilterPill(
    selected: T,
    options: List<Pair<T, String>>,
    onPick: (T) -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { open = true },
            colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBackground),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Text(
                text = options.firstOrNull { it.first == selected }?.second ?: "",
                color = Color.White,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = TextGray,
                modifier = Modifier.size(16.dp)
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.background(CardBackground)
        ) {
            options.forEach { (v, l) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = (if (v == selected) "✓ " else "") + l,
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    },
                    onClick = { onPick(v); open = false }
                )
            }
        }
    }
}
