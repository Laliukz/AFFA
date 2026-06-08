package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkz.affa.ui.theme.*

@Composable
fun CustomSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit = {}
) {
    Row(
        modifier = modifier
            .height(48.dp)
            .background(AppSurface, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Buscar",
            tint = AppTextSecondary
        )

        Spacer(modifier = Modifier.width(8.dp))

        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            textStyle = AppTextStyles.body.copy(color = AppTextPrimary),
            cursorBrush = SolidColor(AppTextPrimary),
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            decorationBox = { innerTextField ->
                if (query.isEmpty()) {
                    Text(
                        "Buscar anime, series o películas", 
                        style = AppTextStyles.body.copy(color = AppTextSecondary, fontSize = 14.sp)
                    )
                }
                innerTextField()
            }
        )

        if (query.isNotEmpty()) {
            IconButton(
                onClick = onClearClick,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Limpiar",
                    tint = AppTextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
