package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.lkz.affa.R
import com.lkz.affa.data.models.Content
import com.lkz.affa.navigation.Screen
import com.lkz.affa.ui.components.*
import com.lkz.affa.ui.viewmodels.ContentViewModel
import com.lkz.affa.ui.viewmodels.MyListViewModel
import com.lkz.affa.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnimeHomeScreen(
        navController: NavController,
        viewModel: ContentViewModel = viewModel(),
        myListViewModel: MyListViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var contentToStatusDialog by remember { mutableStateOf<Content?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshContinueWatching()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize().background(AppScaffoldBackground)
    ) {
        LazyColumn(
                modifier = Modifier.fillMaxSize(),
        ) {
            item { TopBar(navController) }

            if (state.trends.isNotEmpty()) {
                item {
                    HeroCarousel(
                            contentList = state.trends.take(5), // Top 5 trending for the carousel
                            onPlayClick = { content ->
                                content.id?.let {
                                    navController.navigate(Screen.Details.createRoute(it))
                                }
                            },
                            onAddClick = { content -> contentToStatusDialog = content }
                    )
                }
            }

            if (state.continueWatching.isNotEmpty()) {
                item {
                    ContentSection(
                            title = stringResource(R.string.home_continue_watching),
                            contents = state.continueWatching,
                            isLarge = true,
                            hasTextOverlay = true,
                            cardWidth = 160.dp,
                            cardHeight = 100.dp,
                            onCardClick = { content ->
                                content.id?.let {
                                    navController.navigate(Screen.Details.createRoute(it))
                                }
                            },
                            onViewAll = {
                                navController.navigate(Screen.MyList.createRoute("Viendo"))
                            }
                    )
                }
            }

            if (state.todayEpisodes.isNotEmpty()) {
                item {
                    ContentSection(
                            title = stringResource(R.string.home_today_episodes),
                            contents = state.todayEpisodes.take(5),
                            isLarge = false,
                            hasTextOverlay = false,
                            cardWidth = 150.dp,
                            cardHeight = 200.dp,
                            onCardClick = { content ->
                                content.id?.let {
                                    navController.navigate(Screen.Details.createRoute(it))
                                }
                            },
                            onViewAll = {
                                navController.navigate(
                                        Screen.SearchResults.createRoute("Episodios de Hoy")
                                )
                            }
                    )
                }
            }

            if (state.popular.isNotEmpty()) {
                item {
                    ContentSection(
                            title = stringResource(R.string.home_popular),
                            contents = state.popular.take(5),
                            isLarge = false,
                            hasTextOverlay = false,
                            cardWidth = 150.dp,
                            cardHeight = 200.dp,
                            onCardClick = { content ->
                                content.id?.let {
                                    navController.navigate(Screen.Details.createRoute(it))
                                }
                            },
                            onViewAll = {
                                navController.navigate(
                                        Screen.SearchResults.createRoute("Populares")
                                )
                            }
                    )
                }
            }

            if (state.recentlyAdded.isNotEmpty()) {
                item {
                    ContentSection(
                            title = stringResource(R.string.home_recently_added),
                            contents = state.recentlyAdded.take(5),
                            isLarge = false,
                            hasTextOverlay = false,
                            cardWidth = 150.dp,
                            cardHeight = 200.dp,
                            onCardClick = { content ->
                                content.id?.let {
                                    navController.navigate(Screen.Details.createRoute(it))
                                }
                            },
                            onViewAll = {
                                navController.navigate(
                                        Screen.SearchResults.createRoute("Recién Añadidos")
                                )
                            }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }

    if (contentToStatusDialog != null) {
        val content = contentToStatusDialog!!
        AlertDialog(
                onDismissRequest = { contentToStatusDialog = null },
                title = { Text(stringResource(R.string.home_add_to_list), color = AppTextPrimary) },
                text = {
                    Text(stringResource(R.string.home_add_to_list_prompt), color = AppTextSecondary)
                },
                containerColor = AppSurface,
                confirmButton = {},
                dismissButton = {
                    Column {
                        listOf("Viendo", "Planeado", "Completado").forEach { status ->
                            val translatedStatus = when(status) {
                                "Viendo" -> stringResource(R.string.status_watching)
                                "Planeado" -> stringResource(R.string.status_planned)
                                "Completado" -> stringResource(R.string.status_completed)
                                else -> status
                            }
                            TextButton(
                                    onClick = {
                                        myListViewModel.addFavorite(content, status)
                                        contentToStatusDialog = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                            ) { Text(translatedStatus, color = AppPrimary, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
        )
    }
}

@Composable
private fun TopBar(navController: NavController) {
    Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
    ) {
        AssetImage(
                path = "images/app_icon.png",
                contentDescription = "Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(R.string.app_name), style = AppTextStyles.title, modifier = Modifier.weight(1f))
        IconButton(onClick = {}, modifier = Modifier.size(30.dp)) {
            Icon(Icons.Default.Cast, contentDescription = "Cast", tint = AppTextPrimary)
        }
        Spacer(modifier = Modifier.width(8.dp))
        IconButton(
                onClick = { navController.navigate(Screen.Search.route) },
                modifier = Modifier.size(30.dp)
        ) { Icon(Icons.Default.Search, contentDescription = "Search", tint = AppTextPrimary) }
        Spacer(modifier = Modifier.width(16.dp))
        AssetImage(
                path = "images/Avatar1.png",
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(32.dp).clip(CircleShape)
        )
    }
}
