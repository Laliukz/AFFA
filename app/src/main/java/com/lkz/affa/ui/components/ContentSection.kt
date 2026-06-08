package com.lkz.affa.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lkz.affa.ui.theme.*

@Composable
fun ContentSection(
    title: String,
    contents: List<com.lkz.affa.data.models.Content>,
    isLarge: Boolean = false,
    hasTextOverlay: Boolean = false,
    onCardClick: (com.lkz.affa.data.models.Content) -> Unit = {},
    onViewAll: (() -> Unit)? = null,
    cardWidth: Dp? = null,
    cardHeight: Dp? = null,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        SectionHeader(
            title = title,
            actionText = if (onViewAll != null) "Ver Todo" else null,
            onActionClick = onViewAll,
            trailing = null,
            badgeCount = null,
            modifier = Modifier.padding(horizontal = 0.dp, vertical = 0.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(contents.size) { index ->
                ContentCard(
                    content = contents[index],
                    isLarge = isLarge,
                    hasTextOverlay = hasTextOverlay,
                    customWidth = cardWidth,
                    customHeight = cardHeight,
                    onClick = { onCardClick(contents[index]) }
                )
            }
        }
    }
}
