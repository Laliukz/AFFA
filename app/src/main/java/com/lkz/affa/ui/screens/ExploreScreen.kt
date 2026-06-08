package com.lkz.affa.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.res.stringResource
import com.lkz.affa.R
import com.lkz.affa.data.models.Content
import com.lkz.affa.navigation.Screen
import com.lkz.affa.ui.components.CategoryChips
import com.lkz.affa.ui.components.ContentCard
import com.lkz.affa.ui.components.ContentSection
import com.lkz.affa.ui.components.CustomSearchBar
import com.lkz.affa.ui.components.SectionHeader
import com.lkz.affa.ui.viewmodels.ContentViewModel
import com.lkz.affa.ui.theme.*

@Composable
fun ExploreScreen(
    navController: NavController,
    viewModel: ContentViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    val scrollState = rememberScrollState()

    val categories = listOf("Todos", "Action", "Adventure", "Romance", "Fantasy")
    val displayCategories = listOf(
        stringResource(R.string.explore_category_all),
        stringResource(R.string.explore_category_action),
        stringResource(R.string.explore_category_adventure),
        stringResource(R.string.explore_category_romance),
        stringResource(R.string.explore_category_fantasy)
    )

    LaunchedEffect(scrollState.value) {
        if (scrollState.value > 0 && scrollState.value >= scrollState.maxValue - 800) {
            viewModel.loadMoreExplore()
        }
    }

    Scaffold(
        containerColor = AppScaffoldBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = stringResource(R.string.explore_title),
                style = MaterialTheme.typography.headlineMedium,
                color = AppTextPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
            )

            CustomSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onClearClick = { searchQuery = "" },
                onSearch = { 
                    if (searchQuery.isNotEmpty()) {
                        navController.navigate(Screen.SearchResults.createRoute(searchQuery))
                    }
                },
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            CategoryChips(
                categories = displayCategories,
                selectedIndex = selectedCategoryIndex,
                onSelected = { index -> 
                    selectedCategoryIndex = index
                    val genre = if (index == 0) null else categories[index]
                    viewModel.setExploreGenre(genre)
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            if (selectedCategoryIndex == 0 && state.trends.isNotEmpty()) {
                ContentSection(
                    title = stringResource(R.string.explore_trends),
                    contents = state.trends,
                    isLarge = false,
                    hasTextOverlay = false,
                    cardWidth = null,
                    cardHeight = null,
                    onCardClick = { content -> content.id?.let { navController.navigate(Screen.Details.createRoute(it)) } },
                    onViewAll = null
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            SectionHeader(
                title = stringResource(R.string.explore_all),
                actionText = null,
                onActionClick = null,
                trailing = null,
                badgeCount = null,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))
            if (state.exploreGrid.isNotEmpty()) {
                AnimeGridContent(navController, state.exploreGrid)
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AnimeGridContent(navController: NavController, contents: List<Content>) {
    val rows = contents.chunked(2)
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        rows.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { content ->
                    ContentCard(
                        content = content,
                        hasTextOverlay = true,
                        customHeight = 200.dp,
                        onClick = { content.id?.let { navController.navigate(Screen.Details.createRoute(it)) } },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
