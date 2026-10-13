package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkz.affa.ui.theme.PrimaryPurple
import com.lkz.affa.ui.components.AssetImage

@Composable
fun TopBar(onSearchClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AssetImage(
                path = "images/app_icon.png",
                contentDescription = "Logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp).clip(CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "AFFA",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Spacer(modifier = Modifier.width(16.dp))
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search",
                tint = Color.White,
                modifier = Modifier
                    .size(24.dp)
                    .clickable(onClick = onSearchClick)
            )
            Spacer(modifier = Modifier.width(16.dp))
            AssetImage(
                path = "images/Avatar1.png",
                contentDescription = "Avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(32.dp).clip(CircleShape)
            )
        }
    }
}