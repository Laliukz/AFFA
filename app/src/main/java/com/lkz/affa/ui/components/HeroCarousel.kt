package com.lkz.affa.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import com.lkz.affa.ui.theme.AppTextPrimary
import com.lkz.affa.ui.theme.DarkBackground
import com.lkz.affa.ui.theme.HeroBadge
import com.lkz.affa.ui.theme.HeroSynopsis
import com.lkz.affa.ui.theme.HeroTitle

@Composable
fun HeroCarousel(
    items: List<HomeAnimeItem>,
    onAnimeClick: (HomeAnimeItem) -> Unit = {},
    onAddClick: (HomeAnimeItem) -> Unit = {}
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
                // Fondo: el póster agrandado con blur (mismos tonos, sin huecos)
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(28.dp)
                )

                // Frente: póster completo
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .fillMaxHeight()
                        .padding(top = 4.dp, bottom = 24.dp)
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
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    if (item.latestEpisode != null) {
                        Box(
                            modifier = Modifier
                                .border(1.dp, AppPrimary, RoundedCornerShape(6.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(text = "NUEVO EPISODIO", style = HeroBadge)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = item.title,
                        style = HeroTitle,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    val synopsis = item.synopsis
                    if (!synopsis.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = synopsis,
                            style = HeroSynopsis,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { onAnimeClick(item) },
                            colors = ButtonDefaults.buttonColors(containerColor = AppPrimary),
                            shape = RoundedCornerShape(24.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = null,
                                tint = AppTextPrimary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Ver Ahora",
                                color = AppTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = { onAddClick(item) },
                            shape = CircleShape,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                AppTextPrimary.copy(alpha = 0.5f)
                            ),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkBackground.copy(alpha = 0.55f)
                            ),
                            contentPadding = PaddingValues(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Agregar a Mi Lista",
                                tint = AppTextPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
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

