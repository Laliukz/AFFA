package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lkz.affa.ui.theme.*

@Composable
fun SectionHeader(
    title: String,
    actionText: String? = null,
    onActionClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    badgeCount: Int? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(contentPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = AppTextStyles.title
            )
            if (badgeCount != null) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .background(AppTextPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badgeCount.toString(),
                        style = AppTextStyles.badge.copy(color = AppSecondary)
                    )
                }
            }
        }

        if (trailing != null) {
            trailing()
        } else if (actionText != null) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onActionClick?.invoke() }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    style = AppTextStyles.buttonSmall.copy(color = AppSecondary)
                )
            }
        }
    }
}

