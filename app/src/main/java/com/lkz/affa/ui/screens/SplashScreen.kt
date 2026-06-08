package com.lkz.affa.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lkz.affa.ui.viewmodels.ContentViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.navigation.NavHostController
import com.lkz.affa.navigation.Screen
import com.lkz.affa.ui.components.AssetImage
import com.lkz.affa.ui.components.AssetImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.res.stringResource
import com.lkz.affa.R
import com.lkz.affa.ui.theme.*

@Composable
fun LoadingScreen(navController: NavHostController, contentViewModel: ContentViewModel = viewModel()) {
    val contentState by contentViewModel.state.collectAsState()
    
    val fadeAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.8f) }
    val loadingAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            fadeAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
            )
        }
        launch {
            loadingAnim.animateTo(
                targetValue = 0.9f,
                animationSpec = tween(durationMillis = 5000, easing = LinearOutSlowInEasing)
            )
        }
    }

    LaunchedEffect(contentState.isLoaded) {
        if (contentState.isLoaded) {
            loadingAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 300, easing = FastOutLinearInEasing)
            )
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Splash.route) { inclusive = true }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppScaffoldBackground) // Background Color
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val gridSize = 40.dp.toPx()
            val paintColor = AppTextPrimary.copy(alpha = 0.08f)
            
            var x = 0f
            while (x < size.width) {
                drawLine(
                    color = paintColor,
                    start = Offset(x, 0f),
                    end = Offset(x, size.height),
                    strokeWidth = 1f
                )
                x += gridSize
            }

            var y = 0f
            while (y < size.height) {
                drawLine(
                    color = paintColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f
                )
                y += gridSize
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f) // Top half
        ) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.Center)
                    .offset(y = (-40).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                AppPrimary.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .offset(x = 80.dp, y = 300.dp)
                    .size(6.dp)
                    .background(AppError, CircleShape)
                    .shadow(10.dp, CircleShape, spotColor = AppError, ambientColor = AppError)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-80).dp, y = 250.dp)
                    .size(8.dp)
                    .background(AppSecondary, CircleShape)
                    .shadow(10.dp, CircleShape, spotColor = AppSecondary, ambientColor = AppSecondary)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-120).dp, y = 400.dp)
                    .size(4.dp)
                    .background(AppTextPrimary.copy(alpha = 0.24f), CircleShape)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(fadeAnim.value)
                .scale(scaleAnim.value),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(modifier = Modifier.size(130.dp).clip(CircleShape)) {
                    AssetImage(
                        path = "images/app_icon.png",
                        contentDescription = "Anime FFA",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                Box {
                    Text(
                        text = "Anime FFA",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppSecondary,
                        modifier = Modifier.offset(x = 2.dp, y = 2.dp)
                    )
                    Text(
                        text = "Anime FFA",
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppTextPrimary
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .width(200.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(AppTextPrimary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(loadingAnim.value)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(AppPrimary, AppSecondary)
                                )
                            )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.splash_loading),
                    color = AppTextSecondary,
                    fontSize = 10.sp,
                    letterSpacing = 2.sp
                )
            }
        }
    }
}
