package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lkz.affa.ui.viewmodels.VideoPlayerViewModel
import com.lkz.affa.ui.viewmodels.VideoPlayerState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.lkz.affa.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.media3.ui.PlayerView
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.lkz.affa.ui.components.*
import com.lkz.affa.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun VideoPlayerScreen(
    navController: NavController,
    animeId: String = "",
    episodeNum: Int = 1,
    viewModel: VideoPlayerViewModel = viewModel()
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    val uiState by viewModel.uiState.collectAsState()
    var currentStream by remember { mutableStateOf<com.lkz.affa.data.models.StreamLink?>(null) }

    LaunchedEffect(animeId, episodeNum) {
        if (animeId.isNotEmpty()) {
            viewModel.loadVideoData(animeId, episodeNum)
        }
    }
    
    LaunchedEffect(uiState) {
        if (uiState is VideoPlayerState.Success) {
            val state = uiState as VideoPlayerState.Success
            if (currentStream == null && state.streams.isNotEmpty()) {
                currentStream = state.streams.first()
                viewModel.selectStream(currentStream!!)
            }
        }
    }

    val isLoading = uiState is VideoPlayerState.Loading || (uiState as? VideoPlayerState.Success)?.isLoadingStreams == true
    val globalError = (uiState as? VideoPlayerState.Error)?.message
    val streamError = (uiState as? VideoPlayerState.Success)?.streamError

    val content = (uiState as? VideoPlayerState.Success)?.content
    val streams = (uiState as? VideoPlayerState.Success)?.streams ?: emptyList()

    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    val rootModifier = if (isLandscape) {
        Modifier
            .fillMaxSize()
            .background(AppScaffoldBackground)
    } else {
        Modifier
            .fillMaxSize()
            .background(AppScaffoldBackground)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    }

    Column(
        modifier = rootModifier
    ) {

        val videoModifier = if (isLandscape) {
            Modifier.fillMaxSize().background(AppScaffoldBackground)
        } else {
            Modifier.fillMaxWidth().aspectRatio(16f/9f).background(AppScaffoldBackground)
        }
        
        Box(modifier = videoModifier) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppPrimary)
                }
            } else if (globalError != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = globalError, color = AppError, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                }
            } else {
                val successState = uiState as? VideoPlayerState.Success
                if (successState?.isResolvingVideo == true) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = AppPrimary, modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(stringResource(R.string.video_extracting, currentStream?.server ?: ""), color = AppTextPrimary, fontSize = 12.sp)
                        }
                    }
                } else if (successState?.resolvedVideoUrl != null) {
                    val context = LocalContext.current
                    
                    val exoPlayer = remember {
                        val originalUrl = currentStream?.url ?: "https://animeav1.com/"
                        val uri = try { java.net.URI(originalUrl) } catch (e: Exception) { null }
                        val refererUrl = if (uri != null) "${uri.scheme}://${uri.authority}/" else "https://animeav1.com/"
                        
                        android.util.Log.d("VideoPlayerScreen", "ExoPlayer URL: ${successState.resolvedVideoUrl}")
                        android.util.Log.d("VideoPlayerScreen", "ExoPlayer Referer: $refererUrl")
                        
                        val dataSourceFactory = DefaultHttpDataSource.Factory()
                            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                            .setAllowCrossProtocolRedirects(true)
                            .setDefaultRequestProperties(mapOf(
                                "Referer" to refererUrl,
                                "Origin" to (if (uri != null) "${uri.scheme}://${uri.authority}" else "https://animeav1.com"),
                                "Accept" to "*/*"
                            ))
                            
                        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
                        val mediaItemBuilder = androidx.media3.common.MediaItem.Builder()
                            .setUri(successState.resolvedVideoUrl)
                            
                        if (successState.resolvedVideoUrl.contains(".m3u8") || successState.resolvedVideoUrl.contains("/m3u8/")) {
                            mediaItemBuilder.setMimeType(androidx.media3.common.MimeTypes.APPLICATION_M3U8)
                        }

                        ExoPlayer.Builder(context)
                            .setMediaSourceFactory(mediaSourceFactory)
                            .build()
                            .apply {
                                setMediaItem(mediaItemBuilder.build())
                                prepare()
                                playWhenReady = true
                            }
                    }

                    DisposableEffect(exoPlayer) {
                        onDispose { 
                            val pos = (exoPlayer.currentPosition / 1000).toInt()
                            val dur = (exoPlayer.duration / 1000).toInt()
                            if (dur > 0 && pos > 0) {
                                viewModel.updateProgress(pos, dur)
                            }
                            exoPlayer.release() 
                        }
                    }
                    
                    LaunchedEffect(exoPlayer) {
                        while (isActive) {
                            delay(5000)
                            if (exoPlayer.isPlaying) {
                                val pos = (exoPlayer.currentPosition / 1000).toInt()
                                val dur = (exoPlayer.duration / 1000).toInt()
                                if (dur > 0 && pos > 0) {
                                    viewModel.updateProgress(pos, dur)
                                }
                            }
                        }
                    }
                    
                    Box(modifier = Modifier.fillMaxSize()) {
                        AndroidView(
                            factory = { ctx ->
                                PlayerView(ctx).apply {
                                    player = exoPlayer
                                    useController = true
                                    keepScreenOn = true
                                    setShowNextButton(false)
                                    setShowPreviousButton(false)
                                    setShowFastForwardButton(true)
                                    setShowRewindButton(true)
                                    
                                    val basicControls = findViewById<android.widget.LinearLayout>(androidx.media3.ui.R.id.exo_basic_controls)
                                    if (basicControls != null) {
                                        val customControlsView = androidx.compose.ui.platform.ComposeView(ctx).apply {
                                            setContent {
                                                var showVolume by remember { mutableStateOf(false) }
                                                var volLevel by remember { mutableStateOf(1f) }
                                                
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    TextButton(
                                                        onClick = { exoPlayer.seekTo(exoPlayer.currentPosition + 90_000) },
                                                        modifier = Modifier.padding(end = 4.dp)
                                                    ) {
                                                        Text("Saltar OP", color = AppTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                    
                                                    androidx.compose.animation.AnimatedVisibility(visible = showVolume) {
                                                        Slider(
                                                            value = volLevel,
                                                            onValueChange = { 
                                                                volLevel = it
                                                                exoPlayer.volume = it
                                                            },
                                                            modifier = Modifier.width(100.dp).padding(end = 4.dp),
                                                            colors = SliderDefaults.colors(
                                                                thumbColor = AppTextPrimary,
                                                                activeTrackColor = AppPrimary,
                                                                inactiveTrackColor = AppTextPrimary.copy(alpha = 0.3f)
                                                            )
                                                        )
                                                    }
                                                    IconButton(onClick = { showVolume = !showVolume }) {
                                                        Icon(
                                                            imageVector = if (volLevel == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                                                            contentDescription = "Volumen",
                                                            tint = AppTextPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        basicControls.addView(customControlsView, 0)
                                    }
                                    
                                    setFullscreenButtonClickListener { isFullScreen ->
                                        val activity = ctx as? android.app.Activity
                                        if (isFullScreen) {
                                            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                                            val window = activity?.window
                                            androidx.core.view.WindowCompat.getInsetsController(window!!, window.decorView).apply {
                                                hide(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                                                systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                                            }
                                        } else {
                                            activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            val window = activity?.window
                                            androidx.core.view.WindowCompat.getInsetsController(window!!, window.decorView).apply {
                                                show(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                                            }
                                        }
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else if (streamError != null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = streamError, color = AppError, fontSize = 14.sp, modifier = Modifier.padding(16.dp))
                    }
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = stringResource(R.string.video_select_server), color = AppTextPrimary, fontSize = 14.sp)
                    }
                }
            }
            
            if (!isLandscape || (isLandscape && (uiState as? VideoPlayerState.Success)?.resolvedVideoUrl != null)) {
            }
            IconButton(
                onClick = { 
                    if (isLandscape) {
                        val activity = context as? android.app.Activity
                        activity?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                        val window = activity?.window
                        if (window != null) {
                            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                                show(androidx.core.view.WindowInsetsCompat.Type.statusBars() or androidx.core.view.WindowInsetsCompat.Type.navigationBars())
                            }
                        }
                    } else {
                        navController.popBackStack() 
                    }
                },
                modifier = Modifier
                    .padding(12.dp)
                    .align(Alignment.TopStart)
                    .background(AppScaffoldBackground.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.video_back), tint = AppTextPrimary)
            }
        }

        if (!isLandscape) {
            Column(modifier = Modifier.padding(16.dp)) {
            VideoEpisodeInfo(
                episodeNum = episodeNum,
                title = content?.title,
                releaseStatus = content?.releaseStatus
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            if (!isLoading && globalError == null && streams.isNotEmpty()) {
                VideoServerSelector(
                    streams = streams,
                    currentStream = currentStream,
                    onStreamSelected = { stream ->
                        currentStream = stream
                        viewModel.selectStream(stream)
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            VideoNavigationButtons(
                episodeNum = episodeNum,
                maxEp = content?.episodesCount ?: Int.MAX_VALUE,
                onPreviousClick = {
                    navController.navigate(com.lkz.affa.navigation.Screen.VideoPlayer.createRoute(animeId, episodeNum - 1)) {
                        popUpTo(com.lkz.affa.navigation.Screen.VideoPlayer.route) { inclusive = true }
                    }
                },
                onNextClick = {
                    navController.navigate(com.lkz.affa.navigation.Screen.VideoPlayer.createRoute(animeId, episodeNum + 1)) {
                        popUpTo(com.lkz.affa.navigation.Screen.VideoPlayer.route) { inclusive = true }
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
        }

        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = AppScaffoldBackground,
            contentColor = AppPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = AppPrimary,
                    height = 3.dp
                )
            },
            divider = {
                HorizontalDivider(color = AppTextMuted.copy(alpha = 0.2f))
            }
        ) {
            val tabs = listOf(stringResource(R.string.video_tab_info), stringResource(R.string.video_tab_related))
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = title,
                            fontSize = 14.sp,
                            color = if (selectedTabIndex == index) AppPrimary else AppTextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            if (selectedTabIndex == 0) {
                Text(
                    text = content?.description?.replace(Regex("<[^>]*>"), "") ?: stringResource(R.string.video_loading_synopsis),
                    color = AppTextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            } else {
                val related = content?.recommendations ?: emptyList()
                if (related.isEmpty()) {
                    Text(
                        text = stringResource(R.string.details_no_related_content),
                        color = AppTextSecondary,
                        fontSize = 14.sp
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        related.forEach { rec ->
                            Box(modifier = Modifier.width(120.dp).height(180.dp)) {
                                com.lkz.affa.ui.components.ContentCard(
                                    content = rec,
                                    isFlexible = true,
                                    onClick = {
                                        rec.id?.let { id ->
                                            navController.navigate(com.lkz.affa.navigation.Screen.Details.createRoute(id))
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
        
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(text = stringResource(R.string.video_next_episode), style = AppTextStyles.cardTitle)
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(AppSurface)
                    .clickable { /* TODO: Go to next episode */ }
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(65.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AppBackgroundDarkGray)
                    ) {
                        CommonImage(
                            imageUrl = content?.imageUrl ?: "",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.video_episode_upper, (episodeNum + 1).toString()), color = AppPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = content?.title ?: stringResource(R.string.video_next_episode_fallback),
                            color = AppTextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        } // Fin del if(!isLandscape)
        
        Spacer(modifier = Modifier.height(100.dp))
    }
}
