package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.lkz.affa.data.models.Content
import com.lkz.affa.ui.viewmodels.ContentViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.lkz.affa.R
import com.lkz.affa.ui.components.CategoryChip
import com.lkz.affa.ui.components.ContentCard
import com.lkz.affa.ui.components.CustomSearchBar
import com.lkz.affa.ui.components.SectionHeader
import com.lkz.affa.ui.theme.*

private val TextWhite70 = AppTextPrimary.copy(alpha = 0.7f)

@Composable
fun SearchResultsScreen(
    initialSearchQuery: String,
    onNavigateBack: () -> Unit,
    onNavigateToDetails: (String) -> Unit,
    viewModel: ContentViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf(initialSearchQuery) }
    var searchResults by remember { mutableStateOf<List<Content>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val filters = listOf("Sort By", "Genre", "Year", "Status")
    var selectedSort by remember { mutableStateOf<String?>(null) }
    var selectedGenre by remember { mutableStateOf<String?>(null) }
    var selectedYear by remember { mutableStateOf<Int?>(null) }
    var selectedStatus by remember { mutableStateOf<String?>(null) }
    
    var expandedFilterIndex by remember { mutableStateOf(-1) }
    val filterOptions = mapOf(
        "Sort By" to listOf("Popularity", "Trending", "Newest", "Score"),
        "Genre" to listOf("Action", "Adventure", "Comedy", "Drama", "Fantasy", "Romance", "Sci-Fi"),
        "Year" to listOf("2024", "2023", "2022", "2021", "2020", "Older"),
        "Status" to listOf("Releasing", "Finished")
    )
    val context = LocalContext.current
    
    val state by viewModel.state.collectAsState()

    LaunchedEffect(searchQuery, selectedSort, selectedGenre, selectedYear, selectedStatus) {
        val isSpecialCategory = searchQuery in listOf("Populares", "Recién Añadidos", "Continuar Viendo", "Episodios de Hoy")
        if (!isSpecialCategory && (searchQuery.isNotEmpty() || selectedSort != null || selectedGenre != null || selectedYear != null || selectedStatus != null)) {
            isLoading = true
            delay(500)
            try {
                searchResults = viewModel.search(
                    query = searchQuery,
                    sort = selectedSort,
                    genre = selectedGenre,
                    year = selectedYear,
                    status = selectedStatus
                )
            } catch (e: Exception) {
                e.printStackTrace()
                searchResults = emptyList()
            }
            isLoading = false
        }
    }

    val displayResults = when (searchQuery) {
        "Populares" -> state.popular
        "Recién Añadidos" -> state.recentlyAdded
        "Continuar Viendo" -> state.continueWatching
        "Episodios de Hoy" -> state.todayEpisodes
        else -> if (searchQuery.isEmpty()) state.popular else searchResults
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppScaffoldBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.search_back),
                    tint = TextWhite70,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            CustomSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClearClick = { searchQuery = "" },
                modifier = Modifier.weight(1f)
            )
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(filters) { index, filterCategory ->
                val isSort = filterCategory == "Sort By"

                val chipText = when(filterCategory) {
                    "Sort By" -> selectedSort?.let { getLocalizedFilterOption(it) } ?: getLocalizedFilterCategory("Sort By")
                    "Genre" -> selectedGenre?.let { getLocalizedFilterOption(it) } ?: getLocalizedFilterCategory("Genre")
                    "Year" -> selectedYear?.let { if (it == 2010) getLocalizedFilterOption("Older") else it.toString() } ?: getLocalizedFilterCategory("Year")
                    "Status" -> selectedStatus?.let { getLocalizedFilterOption(it) } ?: getLocalizedFilterCategory("Status")
                    else -> getLocalizedFilterCategory(filterCategory)
                }
                
                val isSelected = when(filterCategory) {
                    "Sort By" -> selectedSort != null
                    "Genre" -> selectedGenre != null
                    "Year" -> selectedYear != null
                    "Status" -> selectedStatus != null
                    else -> false
                }

                Box {
                    CategoryChip(
                        text = chipText,
                        isSelected = isSelected,
                        icon = if (isSort) Icons.AutoMirrored.Filled.Sort else Icons.Default.KeyboardArrowDown,
                        onClick = {
                            expandedFilterIndex = if (expandedFilterIndex == index) -1 else index
                        }
                    )

                    DropdownMenu(
                        expanded = expandedFilterIndex == index,
                        onDismissRequest = { expandedFilterIndex = -1 },
                        modifier = Modifier.background(AppSurface)
                    ) {
                        if (isSelected) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.search_clear_filter), color = AppPrimary, fontWeight = FontWeight.Bold) },
                                onClick = {
                                    expandedFilterIndex = -1
                                    when(filterCategory) {
                                        "Sort By" -> selectedSort = null
                                        "Genre" -> selectedGenre = null
                                        "Year" -> selectedYear = null
                                        "Status" -> selectedStatus = null
                                    }
                                }
                            )
                        }

                        filterOptions[filterCategory]?.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(getLocalizedFilterOption(option), color = AppTextPrimary) },
                                onClick = {
                                    expandedFilterIndex = -1
                                    when(filterCategory) {
                                        "Sort By" -> selectedSort = option
                                        "Genre" -> selectedGenre = option
                                        "Year" -> selectedYear = if (option == "Older") 2010 else option.toIntOrNull()
                                        "Status" -> selectedStatus = option
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        SectionHeader(
            title = stringResource(R.string.search_top_results),
            actionText = null,
            onActionClick = null,
            trailing = {
                Box(
                    modifier = Modifier
                        .background(AppPrimary.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayResults.size.toString(),
                        color = AppPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            badgeCount = null,
            modifier = Modifier,
            contentPadding = PaddingValues(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppPrimary)
            }
        } else if (displayResults.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    color = AppTextPrimary.copy(alpha = 0.54f),
                    fontSize = 16.sp
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayResults) { content ->
                    ContentCard(
                        content = content,
                        hasTextOverlay = true,
                        onClick = { content.id?.let { onNavigateToDetails(it) } }
                    )
                }
            }
        }
    }
}
