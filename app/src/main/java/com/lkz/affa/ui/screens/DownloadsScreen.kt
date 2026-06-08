package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.res.stringResource
import com.lkz.affa.R
import com.lkz.affa.ui.theme.*

@Composable
fun DownloadsScreen(navController: NavController) {
    Scaffold(
        containerColor = AppScaffoldBackground,
        topBar = {
            TopBarDownloads(onBack = { navController.popBackStack() })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            StorageInfo()
            
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(3) { index ->
                    DownloadItem(
                        title = stringResource(R.string.downloads_example_title, index + 1),
                        episode = stringResource(R.string.downloads_example_episode, index + 1),
                        size = stringResource(R.string.downloads_size)
                    )
                }
            }
        }
    }
}

@Composable
fun TopBarDownloads(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.downloads_back),
                tint = AppTextPrimary
            )
        }
        Text(
            text = stringResource(R.string.downloads_title),
            style = AppTextStyles.headline,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun StorageInfo() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.downloads_storage),
                color = AppTextSecondary,
                fontSize = 14.sp
            )
            Text(
                text = stringResource(R.string.downloads_storage_info),
                color = AppTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LinearProgressIndicator(
            progress = { 0.2f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = AppPrimary,
            trackColor = AppTextPrimary.copy(alpha = 0.1f),
        )
    }
}

@Composable
fun DownloadItem(title: String, episode: String, size: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(AppSurface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 100.dp, height = 60.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(AppBackgroundDarkGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = AppTextPrimary.copy(alpha = 0.5f)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = AppTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Text(
                text = "$episode • $size",
                color = AppTextSecondary,
                fontSize = 13.sp
            )
        }
        
        IconButton(onClick = { /* Delete download */ }) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.downloads_delete),
                tint = AppTextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}
