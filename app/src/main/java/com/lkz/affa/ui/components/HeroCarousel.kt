package com.lkz.affa.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.lkz.affa.data.models.Content
import com.lkz.affa.ui.theme.AppPrimary
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import kotlinx.coroutines.delay
import com.lkz.affa.ui.theme.*

/**
 * Equivalent of Flutter's HeroCarousel (carousel_slider).
 * Uses Compose's [HorizontalPager] with auto-play, pause-on-interaction,
 * and animated pill-style page indicators.
 */
@Composable
fun HeroCarousel(
    contentList: List<Content>,
    onPlayClick: (Content) -> Unit = {},
    onAddClick: (Content) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (contentList.isEmpty()) return

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp
    val isWide = screenWidthDp >= 840
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val carouselHeightDp = when {
        isWide -> 550.dp
        isLandscape -> (configuration.screenHeightDp * 0.8f).dp
        else -> 500.dp
    }

    val pageCount = Int.MAX_VALUE
    val startIndex = (pageCount / 2) - ((pageCount / 2) % contentList.size)
    val pagerState = rememberPagerState(initialPage = startIndex, pageCount = { pageCount })

    val isDragged by pagerState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isDragged) {
        if (!isDragged) {
            while (true) {
                delay(5_000L)
                if (contentList.size > 1) {
                    pagerState.animateScrollToPage(
                        page = pagerState.currentPage + 1,
                        animationSpec = tween(durationMillis = 800)
                    )
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxWidth()) {

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(carouselHeightDp)
        ) { page ->
            val actualPage = page % contentList.size
            HeroSection(
                content = contentList[actualPage],
                onPlayClick = { onPlayClick(contentList[actualPage]) },
                onAddClick = { onAddClick(contentList[actualPage]) }
            )
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(contentList.size) { index ->
                val actualCurrentPage = pagerState.currentPage % contentList.size
                val isSelected = actualCurrentPage == index
                val indicatorWidth by animateDpAsState(
                    targetValue = if (isSelected) 24.dp else 8.dp,
                    animationSpec = tween(durationMillis = 500),
                    label = "indicatorWidth"
                )

                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .height(4.dp)
                        .width(indicatorWidth)
                        .shadow(
                            elevation = if (isSelected) 8.dp else 0.dp,
                            shape = RoundedCornerShape(2.dp),
                            ambientColor = AppPrimary,
                            spotColor = AppPrimary
                        )
                        .background(
                            color = if (isSelected) AppPrimary
                            else AppTextPrimary.copy(alpha = 0.4f),
                            shape = RoundedCornerShape(2.dp)
                        )
                )
            }
        }
    }
}
