package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkz.affa.R
import com.lkz.affa.data.models.Content
import com.lkz.affa.ui.components.CommonImage
import com.lkz.affa.ui.components.PrimaryButton
import com.lkz.affa.ui.theme.*

@Composable
fun DetailsHeader(
    imageUrl: String?,
    bannerUrl: String?,
    onBackClick: () -> Unit,
    onCastClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth().height(260.dp)) {
        CommonImage(
            imageUrl = bannerUrl ?: imageUrl ?: "",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, AppSurface),
                        startY = 300f
                    )
                )
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.background(AppBlackOverlay, RoundedCornerShape(50))
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.details_back),
                    tint = AppTextPrimary
                )
            }
            Row {
                IconButton(
                    onClick = onCastClick,
                    modifier = Modifier.background(
                        AppScaffoldBackground.copy(alpha = 0.4f),
                        RoundedCornerShape(50)
                    )
                ) {
                    Icon(
                        Icons.Default.Cast,
                        contentDescription = stringResource(R.string.details_cast),
                        tint = AppTextPrimary
                    )
                }
            }
        }
    }
}

@Composable
fun DetailsSynopsis(description: String?) {
    var expandedDescription by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.clickable { expandedDescription = !expandedDescription }
    ) {
        Text(
            text = description?.replace(Regex("<[^>]*>"), "")
                ?: stringResource(R.string.details_no_description),
            color = AppTextSecondary,
            fontSize = 14.sp,
            maxLines = if (expandedDescription) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis
        )
        if (!expandedDescription) {
            Text(
                text = stringResource(R.string.details_see_more),
                color = AppPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun DetailsStatusDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onStatusSelected: (String) -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(stringResource(R.string.details_dialog_add_title), color = AppTextPrimary) },
            text = {
                Text(
                    stringResource(R.string.details_dialog_add_message),
                    color = AppTextSecondary
                )
            },
            containerColor = AppSurface,
            confirmButton = {},
            dismissButton = {
                Column {
                    val statusesMap = mapOf(
                        "Viendo" to stringResource(R.string.list_watching),
                        "Planeado" to stringResource(R.string.list_planned),
                        "Completado" to stringResource(R.string.list_completed)
                    )
                    statusesMap.forEach { (internalStatus, displayStatus) ->
                        TextButton(
                            onClick = { onStatusSelected(internalStatus) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                displayStatus,
                                color = AppPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        )
    }
}
