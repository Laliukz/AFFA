package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.lkz.affa.domain.model.HomeAnimeItem
import com.lkz.affa.ui.theme.AppPrimary
import com.lkz.affa.ui.theme.DarkBackground
import com.lkz.affa.ui.theme.PrimaryPurple
import com.lkz.affa.ui.theme.TextGray

@Composable
fun HeroCarousel(
    items: List<HomeAnimeItem>,
    onAnimeClick: (HomeAnimeItem) -> Unit = {}
) {
    if (items.isEmpty()) return

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWide = screenWidthDp >= 840
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val carouselHeightDp = when {
        isWide -> 420.dp
        isLandscape -> (configuration.screenHeightDp * 0.75f).dp
        else -> 360.dp
    }

    val pagerState = rememberPagerState(pageCount = { items.size })

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(carouselHeightDp)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val item = items[page]
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable { onAnimeClick(item) }
            ) {
                AsyncImage(
                    model = item.bannerUrl ?: item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    DarkBackground.copy(alpha = 0.5f),
                                    DarkBackground
                                )
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    if (item.genres.isNotEmpty()) {
                        Text(
                            text = item.genres.take(3).joinToString(" • "),
                            color = PrimaryPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = item.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val synopsis = item.synopsis
                    if (!synopsis.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = synopsis,
                            color = TextGray,
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(items.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 3.dp)
                        .height(4.dp)
                        .width(if (isSelected) 18.dp else 6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (isSelected) AppPrimary else Color.White.copy(alpha = 0.4f))
                )
            }
        }
    }
}

