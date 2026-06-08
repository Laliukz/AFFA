package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.lkz.affa.R
import com.lkz.affa.data.models.Content
import com.lkz.affa.ui.theme.*
import com.lkz.affa.navigation.Screen
import com.lkz.affa.ui.components.CategoryChips
import com.lkz.affa.ui.components.CommonImage
import com.lkz.affa.ui.components.ContentCard
import com.lkz.affa.ui.viewmodels.MyListViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyListScreen(
    navController: NavController,
    viewModel: MyListViewModel = viewModel(),
    initialCategory: String = "Viendo"
) {
    val savedList by viewModel.savedList.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var isGridView by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }
    val categories = listOf("Viendo", "Planeado", "Completado", "Todos")
    val displayCategories = listOf(
        stringResource(R.string.list_watching),
        stringResource(R.string.list_planned),
        stringResource(R.string.list_completed),
        stringResource(R.string.list_all)
    )
    
    val initialIndex = categories.indexOf(initialCategory).takeIf { it >= 0 } ?: 0
    var selectedCategoryIndex by remember { mutableIntStateOf(initialIndex) }
    var selectedOrder by remember { mutableStateOf("RECIENTES") }

    LaunchedEffect(initialCategory) {
        val index = categories.indexOf(initialCategory)
        if (index >= 0) {
            selectedCategoryIndex = index
            viewModel.loadFavorites(categories[index])
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, selectedCategoryIndex) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadFavorites(categories[selectedCategoryIndex])
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val sortedList = remember(savedList, selectedOrder, selectedCategoryIndex) {
        when (selectedOrder) {
            "ALFABÉTICO" -> savedList.sortedBy { it.title }
            "RECIENTES" -> {
                if (categories[selectedCategoryIndex] == "Viendo") {
                    savedList.sortedWith(Comparator { a, b ->
                        val remA = a.label?.removePrefix("+")?.toIntOrNull() ?: 0
                        val remB = b.label?.removePrefix("+")?.toIntOrNull() ?: 0
                        if (remA == 0 && remB != 0) return@Comparator 1
                        if (remB == 0 && remA != 0) return@Comparator -1
                        remA.compareTo(remB)
                    })
                } else {
                    savedList
                }
            }
            else -> savedList
        }
    }

    Scaffold(
        containerColor = AppScaffoldBackground,
        topBar = {
            TopBarList(
                isGridView = isGridView,
                isEditing = isEditing,
                onToggleGrid = { isGridView = !isGridView },
                onToggleEdit = { isEditing = !isEditing }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CategoryChips(
                categories = displayCategories,
                selectedIndex = selectedCategoryIndex,
                onSelected = { index ->
                    selectedCategoryIndex = index
                    viewModel.loadFavorites(categories[index])
                },
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 8.dp)
            )
            
            ListFiltersRow(
                count = sortedList.size,
                selectedOrder = selectedOrder,
                onOrderChanged = { selectedOrder = it }
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = { viewModel.forceRefresh() },
                modifier = Modifier.weight(1f).fillMaxWidth()
            ) {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 16.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sortedList.size) { index ->
                            val item = sortedList[index]
                            ListItemWidget(
                                content = item,
                                isGrid = true,
                                isEditing = isEditing,
                                navController = navController,
                                onRemove = { item.id?.let { viewModel.removeFavorite(it) } }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sortedList.size) { index ->
                            val item = sortedList[index]
                            ListItemWidget(
                                content = item,
                                isGrid = false,
                                isEditing = isEditing,
                                navController = navController,
                                onRemove = { item.id?.let { viewModel.removeFavorite(it) } }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TopBarList(
    isGridView: Boolean,
    isEditing: Boolean,
    onToggleGrid: () -> Unit,
    onToggleEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.mylist_title),
            style = MaterialTheme.typography.headlineMedium,
            color = AppTextPrimary,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggleGrid) {
                Icon(
                    if (isGridView) Icons.AutoMirrored.Filled.List else Icons.Default.GridView,
                    contentDescription = "Toggle View",
                    tint = AppTextPrimary
                )
            }
            TextButton(onClick = onToggleEdit) {
                Text(
                    text = if (isEditing) stringResource(R.string.mylist_done) else stringResource(R.string.mylist_edit),
                    style = MaterialTheme.typography.bodyLarge,
                    color = AppPrimary
                )
            }
        }
    }
}

@Composable
fun ListFiltersRow(count: Int, selectedOrder: String, onOrderChanged: (String) -> Unit) {
    val options = listOf("RECIENTES", "ALFABETICO")
    val displayOptions = listOf(stringResource(R.string.mylist_order_recent), stringResource(R.string.mylist_order_alpha))
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable { expanded = true }
                    .padding(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.mylist_order, if (selectedOrder == "RECIENTES") stringResource(R.string.mylist_order_recent) else stringResource(R.string.mylist_order_alpha)),
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = AppTextSecondary
                )
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = AppTextSecondary, modifier = Modifier.size(16.dp))
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(AppSurfaceLight)
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(displayOptions[index], color = AppTextPrimary) },
                        onClick = {
                            onOrderChanged(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        Text(
            text = stringResource(R.string.mylist_count_animes, count),
            style = MaterialTheme.typography.bodySmall,
            color = AppTextSecondary
        )
    }
}

@Composable
fun ListItemWidget(
    content: Content,
    isGrid: Boolean,
    isEditing: Boolean,
    navController: NavController,
    onRemove: () -> Unit
) {
    Box {
        if (isGrid) {
            ContentCard(
                content = content,
                customHeight = 180.dp,
                badgeText = null,
                onClick = { content.id?.let { navController.navigate(Screen.Details.createRoute(it)) } }
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AppSurfaceLight, RoundedCornerShape(12.dp))
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { content.id?.let { navController.navigate(Screen.Details.createRoute(it)) } }
                    .padding(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .width(85.dp)
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppBackgroundDarkGray)
                ) {
                    CommonImage(
                        imageUrl = content.imageUrl,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(
                    modifier = Modifier.weight(1f).heightIn(min = 120.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = content.title, 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), 
                            color = AppTextPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val statusText = when(content.releaseStatus?.uppercase()) {
                            "RELEASING" -> stringResource(R.string.mylist_status_releasing)
                            "FINISHED" -> stringResource(R.string.mylist_status_finished)
                            "NOT_YET_RELEASED" -> stringResource(R.string.mylist_status_upcoming)
                            "CANCELLED" -> stringResource(R.string.mylist_status_cancelled)
                            else -> content.releaseStatus ?: stringResource(R.string.mylist_status_unknown)
                        }
                        val statusColor = if (content.releaseStatus?.uppercase() == "RELEASING") AppSuccess else AppTextSecondary
                        
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = statusColor
                        )
                    }
                    
                    Column {
                        val remaining = content.label?.removePrefix("+")?.toIntOrNull() ?: 0
                        val remainingText = if (remaining > 0) {
                            stringResource(R.string.mylist_episodes_remaining, remaining)
                        } else {
                            if (content.releaseStatus?.uppercase() == "RELEASING") stringResource(R.string.mylist_up_to_date) else stringResource(R.string.mylist_completed_status)
                        }
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (remaining > 0) Icons.Default.PlayArrow else Icons.Default.CheckCircle, 
                                contentDescription = null, 
                                tint = if (remaining > 0) AppSecondary else AppSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = remainingText, 
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium), 
                                color = AppTextPrimary
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val maxEpisodesText = if (content.releaseStatus?.uppercase() == "RELEASING") {
                            if (content.nextAiringEpisode != null && content.nextAiringEpisode > 1) {
                                stringResource(R.string.mylist_episodes_aired, content.nextAiringEpisode - 1)
                            } else {
                                null
                            }
                        } else {
                            if (content.episodesCount != null && content.episodesCount > 0) {
                                stringResource(R.string.mylist_episodes_total, content.episodesCount)
                            } else null
                        }
                        
                        if (maxEpisodesText != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.List, 
                                    contentDescription = null, 
                                    tint = AppTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = maxEpisodesText, 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = AppTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
        
        if (isEditing) {
            Box(
                modifier = Modifier
                    .align(if (isGrid) Alignment.TopEnd else Alignment.TopStart)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(AppError, CircleShape)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Remove", tint = AppTextPrimary, modifier = Modifier.size(16.dp))
            }
        }
    }
}
