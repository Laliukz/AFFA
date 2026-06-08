package com.lkz.affa.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lkz.affa.ui.theme.AppTextSecondary
import com.lkz.affa.ui.theme.AppTextStyles

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = AppTextStyles.label.copy(color = AppTextSecondary),
        modifier = modifier.padding(horizontal = 24.dp, vertical = 8.dp)
    )
}
