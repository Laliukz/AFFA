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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Player
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.lkz.affa.domain.model.AnimeDetails
import com.lkz.affa.domain.model.Episode as DomainEpisode
import com.lkz.affa.domain.model.HomeAnimeItem
import com.lkz.affa.ui.theme.*


@UnstableApi
@Composable
fun NowPlayingScreen(
    animeTitle: String,
    animeImageUrl: String,
    aliases: List<String>,
    episode: DomainEpisode?,
    episodes: List<DomainEpisode>,
    viewModel: PlayerViewModel?,
    details: AnimeDetails? = null,
    animeScore: Double? = null,
    similarItems: List<HomeAnimeItem> = emptyList(),
    similarTitle: String = "Similares",
    episodeUrl: String? = null,
    onBackClick: () -> Unit = {},
    onSimilarClick: (HomeAnimeItem) -> Unit = {}
) {
    LaunchedEffect(episode?.url) {
        if (episode != null) viewModel?.load(animeTitle, aliases, episode, episodes, episodeUrl)
    }
    val context = LocalContext.current
    var fullscreen by remember { mutableStateOf(false) }
    // ponytail: rotación real vía Activity — LOCKED la dejaba clavada en retrato
    fun setFullscreen(on: Boolean) {
        val activity = context as? android.app.Activity ?: return
        fullscreen = on
        activity.requestedOrientation = if (on)
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else
            android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        android.util.Log.d(
            "PlayerFullscreen",
            "request=$on cfgOrientation=${activity.resources.configuration.orientation}"
        )
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        if (on) controller.hide(WindowInsetsCompat.Type.systemBars())
        else controller.show(WindowInsetsCompat.Type.systemBars())
    }
    BackHandler(enabled = fullscreen) { setFullscreen(false) }
    DisposableEffect(Unit) {
        onDispose {
            // teardown al cambiar de pantalla: nada del episodio anterior sobrevive
            viewModel?.player?.playWhenReady = false
            viewModel?.player?.stop()
            viewModel?.player?.clearMediaItems()
            // restaura por si se sale en fullscreen (gesto atrás, similar, etc.)
            (context as? android.app.Activity)?.let {
                it.requestedOrientation =
                    android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                WindowCompat.getInsetsController(it.window, it.window.decorView)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
    val state = viewModel?.uiState?.collectAsStateWithLifecycle()?.value
    val current = viewModel?.currentEpisode?.collectAsStateWithLifecycle()?.value ?: episode
    val all = viewModel?.allEpisodes()?.takeIf { it.isNotEmpty() } ?: episodes
    val prev = all.filter { it.number < (current?.number ?: Float.MAX_VALUE) }.maxByOrNull { it.number }
    val next = all.filter { it.number > (current?.number ?: Float.MIN_VALUE) }.minByOrNull { it.number }
    val upcoming = all.filter { it.number > (current?.number ?: Float.MIN_VALUE) }.sortedBy { it.number }.take(3)
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel?.notice?.collect { snackbar.showSnackbar(it) }
    }
    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbar) }
    ) { paddingValues ->
        LazyColumn(
            modifier = if (fullscreen) Modifier.fillMaxSize()
            else Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = if (fullscreen) PaddingValues(0.dp) else PaddingValues(bottom = 32.dp),
            userScrollEnabled = !fullscreen
        ) {
            item {
                val player = viewModel?.player
                var isPlaying by remember { mutableStateOf(false) }
                var position by remember { mutableStateOf(0L) }
                var duration by remember { mutableStateOf(0L) }
                var controlsVisible by remember { mutableStateOf(true) }
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
                        override fun onPlayerError(error: PlaybackException) {
                            viewModel?.onPlaybackError()
                        }
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
                    modifier = if (fullscreen) Modifier.fillParentMaxSize().background(Color.Black)
                    else Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    // ponytail: rotación real vía Activity — sin marcos rotados por Compose
                    Box(Modifier.fillMaxSize()) {
                    if (player != null) {
                        AndroidView(
                            factory = { ctx ->
                                // ponytail: PlayerView con FIT por defecto (barras negras
                                // según aspecto, jamás deforma ni recorta); useController=false
                                // porque los controles son los Compose propios
                                PlayerView(ctx).also {
                                    it.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                                    it.useController = false
                                    it.player = player
                                }
                            },
                            update = { it.player = player },
                            onRelease = { it.player = null },
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
                    // ponytail: saltos flotantes sobre el video (estilo YouTube) —
                    // fuera de la barra para no apretarla
                    if (controlsVisible) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel?.skipBackward() },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    .size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Replay5,
                                    contentDescription = "Atrasar 5 segundos",
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { viewModel?.skipForward(10000L) },
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                    .size(56.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Forward10,
                                    contentDescription = "Adelantar 10 segundos",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                    IconButton(
                        onClick = { if (fullscreen) setFullscreen(false) else onBackClick() },
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
                                IconButton(onClick = { setFullscreen(!fullscreen) }) {
                                    Icon(
                                        imageVector = if (fullscreen) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
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
            }

            if (!fullscreen) {
                item {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TitleBlock(animeTitle = animeTitle, episode = current)
                        AnimeInfoBlock(details = details, score = animeScore, totalEps = all.size)
                        ServerSelector(state = state, onSelect = { viewModel?.switchServer(it) })
                        EpisodeNav(
                            prevLabel = prev?.let { "Ep ${epNum(it.number)}" },
                            nextLabel = next?.let { "Ep ${epNum(it.number)}" },
                            currentLabel = current?.number?.let { "EP ${epNum(it)}" } ?: "",
                            onPrev = { prev?.let { viewModel?.playEpisode(it) } },
                            onNext = { next?.let { viewModel?.playEpisode(it) } }
                        )
                        UpNextSection(
                            episodes = upcoming,
                            imageUrl = animeImageUrl,
                            onClick = { viewModel?.playEpisode(it) }
                        )
                        SimilarRow(items = similarItems, title = similarTitle, onClick = onSimilarClick)
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
private fun TitleBlock(animeTitle: String, episode: DomainEpisode?) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            episode?.number?.let {
                Text(
                    text = "EP ${epNum(it)}",
                    color = AppTextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(PrimaryPurple)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = animeTitle,
                color = AppTextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        episode?.title?.takeIf { it.isNotBlank() }?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = it,
                color = TextGray,
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ServerSelector(state: PlayerUiState?, onSelect: (Int) -> Unit) {
    Column {
        Text(
            text = "Servidores",
            color = AppTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        val ready = state as? PlayerUiState.Ready
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (ready == null) {
                Text(text = "Resolviendo…", color = TextGray, fontSize = 13.sp)
            } else {
                ready.servers.forEachIndexed { index, server ->
                    val selected = index == ready.selectedIndex
                    val bg = if (selected) PrimaryPurple else CardBackground
                    val textColor = if (selected) AppTextPrimary else TextGray
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(bg)
                            .clickable(enabled = !ready.switching) { onSelect(index) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = server.name,
                            color = textColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeNav(
    prevLabel: String?,
    nextLabel: String?,
    currentLabel: String,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardBackground)
            .padding(vertical = 4.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = prevLabel != null, onClick = onPrev)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = prevLabel ?: "EP Anterior",
                color = if (prevLabel != null) AppTextPrimary else TextGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkBackground)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentLabel,
                color = AppTextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = nextLabel != null, onClick = onNext)
                .padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = nextLabel ?: "Siguiente Episodio",
                color = if (nextLabel != null) AppTextPrimary else TextGray,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AnimeInfoBlock(details: AnimeDetails?, score: Double?, totalEps: Int) {
    val synopsis = details?.synopsis?.takeIf { it.isNotBlank() }
    val genres = details?.genres.orEmpty()
    val meta = listOfNotNull(
        details?.status?.takeIf { it.isNotBlank() },
        details?.type?.takeIf { it.isNotBlank() },
        totalEps.takeIf { it > 0 }?.let { "$it episodios" }
    )
    if (synopsis == null && genres.isEmpty() && meta.isEmpty() && score == null) return
    var expanded by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .padding(14.dp)
    ) {
        if (score != null || meta.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (score != null) {
                    Text(
                        text = "★ $score",
                        color = RatingYellow,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (meta.isNotEmpty()) {
                    if (score != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "•", color = TextGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = meta.joinToString(" • "),
                        color = TextGray,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        if (genres.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                genres.forEach { genre ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkBackground)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = genre,
                            color = TextGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        if (synopsis != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = synopsis,
                color = TextGray,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 4,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (expanded) "Ver menos" else "Ver más",
                color = AppPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clickable { expanded = !expanded }
            )
        }
    }
}

@Composable
private fun UpNextSection(
    episodes: List<DomainEpisode>,
    imageUrl: String,
    onClick: (DomainEpisode) -> Unit
) {
    if (episodes.isEmpty()) return
    Column {
        Text(
            text = if (episodes.size == 1) "Siguiente episodio" else "Próximos episodios",
            color = AppTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            episodes.forEach { episode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBackground)
                        .clickable { onClick(episode) }
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(68.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUrl.isNotEmpty()) {
                            coil.compose.AsyncImage(
                                model = imageUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "EPISODIO ${epNum(episode.number)}",
                            color = PrimaryPurple,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = episode.title.ifBlank { "Episodio ${epNum(episode.number)}" },
                            color = AppTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SimilarRow(items: List<HomeAnimeItem>, title: String, onClick: (HomeAnimeItem) -> Unit) {
    if (items.isEmpty()) return
    Column {
        Text(
            text = title,
            color = AppTextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .width(110.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onClick(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .width(110.dp)
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.imageUrl.isNotEmpty()) {
                            coil.compose.AsyncImage(
                                model = item.imageUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.title,
                        color = AppTextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (item.score != null) {
                        Text(
                            text = "★ ${item.score}",
                            color = RatingYellow,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
