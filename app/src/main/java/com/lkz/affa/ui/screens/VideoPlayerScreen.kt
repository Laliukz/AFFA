package com.lkz.affa.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.lkz.affa.domain.model.Episode as DomainEpisode
import com.lkz.affa.ui.components.EpisodeItem
import com.lkz.affa.ui.components.toUiEpisode
import com.lkz.affa.ui.theme.AppPrimary
import com.lkz.affa.ui.theme.AppTextPrimary
import com.lkz.affa.ui.theme.CardBackground
import com.lkz.affa.ui.theme.DarkBackground
import com.lkz.affa.ui.theme.PrimaryPurple
import com.lkz.affa.ui.theme.ServerIndicatorGreen
import com.lkz.affa.ui.theme.TextGray

@UnstableApi
@Composable
fun NowPlayingScreen(
    animeTitle: String,
    animeImageUrl: String,
    aliases: List<String>,
    episode: DomainEpisode?,
    episodes: List<DomainEpisode>,
    viewModel: PlayerViewModel?,
    onBackClick: () -> Unit = {}
) {
    LaunchedEffect(episode?.url) {
        if (episode != null) viewModel?.load(animeTitle, aliases, episode, episodes)
    }
    DisposableEffect(Unit) {
        onDispose {
            viewModel?.player?.playWhenReady = false
            viewModel?.player?.stop()
        }
    }
    val state = viewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val current = viewModel?.currentEpisode?.collectAsStateWithLifecycle()?.value ?: episode
    val all = viewModel?.allEpisodes()?.takeIf { it.isNotEmpty() } ?: episodes
    val prev = all.filter { it.number < (current?.number ?: Float.MAX_VALUE) }.maxByOrNull { it.number }
    val next = all.filter { it.number > (current?.number ?: Float.MIN_VALUE) }.minByOrNull { it.number }

    Scaffold(containerColor = DarkBackground) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                val player = viewModel?.player
                var isPlaying by remember { mutableStateOf(false) }
                var position by remember { mutableStateOf(0L) }
                var duration by remember { mutableStateOf(0L) }
                var controlsVisible by remember { mutableStateOf(true) }
                val context = LocalContext.current
                val audio = remember {
                    context.getSystemService(android.content.Context.AUDIO_SERVICE)
                        as android.media.AudioManager
                }
                val maxVol = remember {
                    audio.getStreamMaxVolume(android.media.AudioManager.STREAM_MUSIC)
                }
                var volume by remember {
                    mutableStateOf(audio.getStreamVolume(android.media.AudioManager.STREAM_MUSIC))
                }
                DisposableEffect(player) {
                    val listener = object : Player.Listener {
                        override fun onIsPlayingChanged(v: Boolean) { isPlaying = v }
                    }
                    player?.addListener(listener)
                    onDispose { player?.removeListener(listener) }
                }
                LaunchedEffect(player) {
                    while (true) {
                        position = player?.currentPosition ?: 0L
                        duration = player?.duration?.takeIf { it > 0 } ?: duration
                        kotlinx.coroutines.delay(500L)
                    }
                }
                // Auto-oculta controles a los 3s reproduciendo; en pausa quedan visibles
                LaunchedEffect(isPlaying, controlsVisible) {
                    if (controlsVisible && isPlaying) {
                        kotlinx.coroutines.delay(3000L)
                        controlsVisible = false
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)
                ) {
                    if (player != null) {
                        AndroidView(
                            factory = {
                                PlayerView(it).apply {
                                    this.player = player
                                    useController = false
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    // Tap alterna controles
                    Box(
                        modifier = Modifier.fillMaxSize().clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { controlsVisible = !controlsVisible }
                    )
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .padding(12.dp)
                            .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color.White
                        )
                    }
                    if (controlsVisible) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Slider(
                                value = position.toFloat(),
                                onValueChange = { player?.seekTo(it.toLong()) },
                                valueRange = 0f..duration.coerceAtLeast(1L).toFloat(),
                                colors = SliderDefaults.colors(
                                    thumbColor = PrimaryPurple,
                                    activeTrackColor = PrimaryPurple,
                                    inactiveTrackColor = Color.Gray
                                ),
                                modifier = Modifier.fillMaxWidth().height(16.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                IconButton(onClick = {
                                    if (isPlaying) player?.pause() else player?.play()
                                }) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                                        tint = Color.White
                                    )
                                }
                                IconButton(onClick = {
                                    viewModel?.next()?.let { viewModel.playEpisode(it) }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.SkipNext,
                                        contentDescription = "Siguiente",
                                        tint = Color.White
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.VolumeUp,
                                    contentDescription = "Volumen",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                                Slider(
                                    value = volume.toFloat(),
                                    onValueChange = {
                                        volume = it.toInt()
                                        audio.setStreamVolume(
                                            android.media.AudioManager.STREAM_MUSIC, volume, 0
                                        )
                                    },
                                    valueRange = 0f..maxVol.toFloat(),
                                    steps = maxVol - 1,
                                    modifier = Modifier.width(90.dp).height(20.dp)
                                )
                                Text(
                                    text = "${fmtTime(position)} / ${fmtTime(duration)}",
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Text(
                                    text = "+85s",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable { viewModel?.skipForward() }
                                        .padding(10.dp)
                                )
                                IconButton(onClick = {
                                    (context as? android.app.Activity)?.let { activity ->
                                        activity.requestedOrientation =
                                            if (activity.resources.configuration.orientation ==
                                                android.content.res.Configuration.ORIENTATION_LANDSCAPE
                                            ) {
                                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                            } else {
                                                android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                            }
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Fullscreen,
                                        contentDescription = "Pantalla completa",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                    if (state == null || state is PlayerUiState.LoadingServers) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = AppPrimary)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Buscando servidores…",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                    if (state is PlayerUiState.Error) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(text = state.message, color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel?.retry() },
                                colors = ButtonDefaults.buttonColors(containerColor = AppPrimary),
                                shape = RoundedCornerShape(16.dp)
                            ) { Text("Reintentar", color = AppTextPrimary) }
                        }
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = current?.let {
                            val n = if (it.number % 1 == 0f) it.number.toInt().toString() else it.number.toString()
                            "EPISODIO $n"
                        } ?: "",
                        color = PrimaryPurple,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = animeTitle,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.weight(1f))
                        // ponytail: descarga sin funcionalidad por ahora
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Descargar",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Descargar",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Servidores",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    val ready = state as? PlayerUiState.Ready
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (ready == null) {
                            Text(text = "Resolviendo…", color = TextGray, fontSize = 13.sp)
                        } else {
                            ready.servers.forEachIndexed { index, server ->
                                ServerChip(
                                    name = server.name,
                                    isSelected = index == ready.selectedIndex,
                                    indicatorColor = ServerIndicatorGreen,
                                    enabled = !ready.switching,
                                    onClick = { viewModel?.switchServer(index) }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = { prev?.let { viewModel?.playEpisode(it) } },
                            enabled = prev != null,
                            colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Anterior",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = prev?.let { "Ep ${epNum(it.number)}" } ?: "—",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                        Button(
                            onClick = { next?.let { viewModel?.playEpisode(it) } },
                            enabled = next != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f).height(46.dp)
                        ) {
                            Text(
                                text = next?.let { "Ep ${epNum(it.number)}" } ?: "—",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Siguiente",
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(28.dp))

                    if (next != null) {
                        Text(
                            text = "Next Episode",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        EpisodeItem(
                            episode = next.toUiEpisode(),
                            imageUrl = animeImageUrl,
                            onClick = { viewModel?.playEpisode(next) }
                        )
                    }
                }
            }
        }
    }
}

private fun epNum(n: Float): String = if (n % 1 == 0f) n.toInt().toString() else n.toString()

private fun fmtTime(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val s = (ms / 1000).toInt()
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}

@Composable
fun ServerChip(
    name: String,
    isSelected: Boolean,
    indicatorColor: Color? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryPurple else CardBackground
        ),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.height(38.dp)
    ) {
        if (indicatorColor != null) {
            Box(
                modifier = Modifier.size(8.dp).background(indicatorColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = name,
            color = if (isSelected) Color.White else TextGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
