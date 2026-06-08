package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lkz.affa.data.models.Content
import com.lkz.affa.navigation.Screen
import com.lkz.affa.ui.components.CategoryChip
import com.lkz.affa.ui.components.ContentCard
import com.lkz.affa.ui.components.CustomSearchBar
import com.lkz.affa.ui.components.SectionHeader
import com.lkz.affa.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.lkz.affa.R
import com.lkz.affa.ui.viewmodels.ContentViewModel
import kotlinx.coroutines.delay

@Composable
fun getLocalizedFilterCategory(category: String): String {
    return when(category) {
        "Sort By" -> stringResource(R.string.search_sort_by)
        "Genre" -> stringResource(R.string.search_genre)
        "Year" -> stringResource(R.string.search_year)
        "Status" -> stringResource(R.string.search_status)
        else -> category
    }
}

@Composable
fun getLocalizedFilterOption(option: String): String {
    return when(option) {
        "Popularity" -> stringResource(R.string.search_sort_popularity)
        "Trending" -> stringResource(R.string.search_sort_trending)
        "Newest" -> stringResource(R.string.search_sort_newest)
        "Score" -> stringResource(R.string.search_sort_score)
        "Action" -> stringResource(R.string.search_genre_action)
        "Adventure" -> stringResource(R.string.search_genre_adventure)
        "Comedy" -> stringResource(R.string.search_genre_comedy)
        "Drama" -> stringResource(R.string.search_genre_drama)
        "Fantasy" -> stringResource(R.string.search_genre_fantasy)
        "Romance" -> stringResource(R.string.search_genre_romance)
        "Sci-Fi" -> stringResource(R.string.search_genre_scifi)
        "Older" -> stringResource(R.string.search_year_older)
        "Releasing" -> stringResource(R.string.search_status_releasing)
        "Finished" -> stringResource(R.string.search_status_finished)
        else -> option
    }
}

@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: ContentViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<Content>>(emptyList()) }
    
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
        if (searchQuery.isEmpty() && selectedSort == null && selectedGenre == null && selectedYear == null && selectedStatus == null) {
            isLoading = false
            return@LaunchedEffect
        }
        
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

    val displayResults = if (searchQuery.isEmpty()) emptyList() else searchResults

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
                onClick = { navController.popBackStack() },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.search_back),
                    tint = AppTextPrimary.copy(alpha = 0.7f),
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
            title = stringResource(R.string.search_results),
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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = AppPrimary)
            } else if (displayResults.isEmpty()) {
                Text(
                    text = stringResource(R.string.search_no_results),
                    color = AppTextPrimary.copy(alpha = 0.54f),
                    fontSize = 16.sp
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayResults) { content ->
                        ContentCard(
                            content = content,
                            hasTextOverlay = true,
                            onClick = { content.id?.let { navController.navigate(Screen.Details.createRoute(it)) } }
                        )
                    }
                }
            }
        }
    }
}

