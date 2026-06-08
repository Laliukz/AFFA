package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkz.affa.ui.theme.AppPrimary
import com.lkz.affa.ui.theme.AppSurface
import com.lkz.affa.ui.theme.*

@Composable
fun CategoryChips(
    categories: List<String> = listOf("Todos", "Acción", "Aventura", "Romance", "Fantasía"),
    selectedIndex: Int = 0,
    onSelected: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.height(40.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 0.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        itemsIndexed(categories) { index, category ->
            CategoryChip(
                text = category,
                isSelected = index == selectedIndex,
                onClick = { onSelected?.invoke(index) }
            )
        }
    }
}

@Composable
fun CategoryChip(
    text: String,
    isSelected: Boolean = false,
    icon: ImageVector? = null,
    onClick: () -> Unit = {}
) {
    val shape = RoundedCornerShape(20.dp)
    
    Box(
        modifier = Modifier
            .height(40.dp)
            .then(
                if (isSelected) {
                    Modifier.shadow(
                        elevation = 8.dp,
                        shape = shape,
                        spotColor = AppPrimary,
                        ambientColor = AppPrimary
                    )
                } else {
                    Modifier.border(
                        width = 1.dp,
                        color = AppTextPrimary.copy(alpha = 0.1f),
                        shape = shape
                    )
                }
            )
            .clip(shape)
            .background(if (isSelected) AppPrimary else AppSurface)
            .clickable { onClick() }
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon, 
                    contentDescription = null, 
                    tint = if (isSelected) AppTextPrimary else AppTextSecondary, 
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = text,
                color = if (isSelected) AppTextPrimary else AppTextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
