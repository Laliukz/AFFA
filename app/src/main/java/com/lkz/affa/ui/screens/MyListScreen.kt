package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.lkz.affa.domain.model.HomeAnimeItem
import com.lkz.affa.domain.model.MyListItem
import com.lkz.affa.domain.model.MyListStatus
import com.lkz.affa.ui.components.BottomNavigationBar
import com.lkz.affa.ui.theme.*

@Composable
fun MyListScreen(
    currentScreen: String = "mylist",
    viewModel: MyListViewModel? = null,
    onScreenSelected: (String) -> Unit = {},
    onAnimeClick: (HomeAnimeItem) -> Unit = {}
) {
    val state = viewModel?.uiState?.collectAsStateWithLifecycle()?.value ?: MyListUiState.Empty

    Scaffold(
        bottomBar = { BottomNavigationBar(currentScreen, onScreenSelected) },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (state) {
                is MyListUiState.Empty -> {
                    MyListTopBar(isEditMode = false, onToggleEdit = {})
                    EmptyListPlaceholder()
                }
                is MyListUiState.Success -> {
                    MyListTopBar(
                        isEditMode = state.isEditMode,
                        onToggleEdit = { viewModel?.toggleEditMode() }
                    )
                    FilterChipsRow(
                        selectedFilter = state.filter,
                        onFilterSelected = { viewModel?.setFilter(it) }
                    )
                    ListHeaderRow(
                        totalCount = state.items.size,
                        filter = state.filter
                    )
                    MyListGrid(
                        items = state.items,
                        isEditMode = state.isEditMode,
                        onAnimeClick = onAnimeClick,
                        onRemove = { viewModel?.remove(it.title) },
                        onStatusChange = { item, newStatus -> viewModel?.updateStatus(item.title, newStatus) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MyListTopBar(
    isEditMode: Boolean,
    onToggleEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Mi Lista",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = if (isEditMode) "Listo" else "Editar",
            color = PrimaryPurple,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .clickable { onToggleEdit() }
                .padding(4.dp)
        )
    }
}

@Composable
private fun FilterChipsRow(
    selectedFilter: MyListFilter,
    onFilterSelected: (MyListFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val options = listOf(
            "Todos" to MyListFilter.All,
            "Viendo" to MyListFilter.ByStatus(MyListStatus.WATCHING),
            "Planeado" to MyListFilter.ByStatus(MyListStatus.PLAN_TO_WATCH),
            "Completado" to MyListFilter.ByStatus(MyListStatus.COMPLETED)
        )
        options.forEach { (label, filter) ->
            val isSelected = selectedFilter == filter
            Button(
                onClick = { onFilterSelected(filter) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) PrimaryPurple else CardBackground
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else TextGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ListHeaderRow(totalCount: Int, filter: MyListFilter) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "ORDEN: RECIENTES",
            color = TextGray,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        Text(
            text = "$totalCount Animes",
            color = TextGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MyListGrid(
    items: List<MyListItem>,
    isEditMode: Boolean,
    onAnimeClick: (HomeAnimeItem) -> Unit,
    onRemove: (MyListItem) -> Unit,
    onStatusChange: (MyListItem, MyListStatus) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(items, key = { it.title }) { item ->
            MyListCard(
                item = item,
                isEditMode = isEditMode,
                onClick = {
                    onAnimeClick(
                        HomeAnimeItem(
                            title = item.title,
                            slug = item.slug,
                            imageUrl = item.imageUrl,
                            bannerUrl = item.bannerUrl,
                            score = item.score,
                            genres = item.genres
                        )
                    )
                },
                onRemove = { onRemove(item) },
                onStatusChange = { newStatus -> onStatusChange(item, newStatus) }
            )
        }
    }
}

@Composable
private fun MyListCard(
    item: MyListItem,
    isEditMode: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onStatusChange: (MyListStatus) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(CardBackground)
        ) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Status Badge
            Text(
                text = item.status.label.uppercase(),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(PrimaryPurple, RoundedCornerShape(4.dp))
                    .clickable { showMenu = true }
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )

            StatusDropdown(
                expanded = showMenu,
                currentStatus = item.status,
                onDismiss = { showMenu = false },
                onSelect = {
                    showMenu = false
                    onStatusChange(it)
                }
            )

            // Rating
            if (item.score != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = RatingYellow,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "%.1f".format(item.score),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Edit Delete Button
            if (isEditMode) {
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .background(Color.Red.copy(alpha = 0.85f), CircleShape)
                        .size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Eliminar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
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
            text = item.genres.firstOrNull() ?: item.status.label,
            color = TextGray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun StatusDropdown(
    expanded: Boolean,
    currentStatus: MyListStatus,
    onDismiss: () -> Unit,
    onSelect: (MyListStatus) -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier.background(CardBackground)
    ) {
        MyListStatus.values().forEach { status ->
            DropdownMenuItem(
                text = {
                    Text(
                        text = status.label,
                        color = if (status == currentStatus) PrimaryPurple else Color.White,
                        fontWeight = if (status == currentStatus) FontWeight.Bold else FontWeight.Normal
                    )
                },
                onClick = { onSelect(status) }
            )
        }
    }
}

@Composable
private fun EmptyListPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Tu lista está vacía",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Agrega animes usando el botón (+) en el inicio o en la pantalla de detalles.",
                color = TextGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
