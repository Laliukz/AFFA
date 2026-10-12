package com.lkz.affa.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.lkz.affa.core.network.extractor.VideoExtractorManager
import com.lkz.affa.data.repository.HomeRepository
import com.lkz.affa.domain.model.Episode as DomainEpisode
import com.lkz.affa.domain.model.VideoServer
import com.lkz.affa.domain.model.VideoStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed interface PlayerUiState {
    data object LoadingServers : PlayerUiState
    data class Ready(
        val stream: VideoStream,
        val servers: List<VideoServer>,
        val selectedIndex: Int,
        val switching: Boolean = false
    ) : PlayerUiState
    data class Error(val message: String) : PlayerUiState
}

@UnstableApi
class PlayerViewModel(
    private val appContext: Context,
    private val repository: HomeRepository,
    private val extractor: VideoExtractorManager = VideoExtractorManager()
) : ViewModel() {

    val player: ExoPlayer = ExoPlayer.Builder(appContext).build()

    private val _uiState = MutableStateFlow<PlayerUiState>(PlayerUiState.LoadingServers)
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private val _currentEpisode = MutableStateFlow<DomainEpisode?>(null)
    val currentEpisode: StateFlow<DomainEpisode?> = _currentEpisode.asStateFlow()

    // ponytail: avisos de un disparo (servidor caído, etc.) — el player sigue con lo actual
    private val _notice = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val notice: SharedFlow<String> = _notice.asSharedFlow()

    fun allEpisodes(): List<DomainEpisode> = episodes

    private var title = ""
    private var synonyms: List<String> = emptyList()
    private var episodeUrl: String? = null
    private var episodes: List<DomainEpisode> = emptyList()
    private var current: DomainEpisode? = null

    // ponytail: memoria de sesión por episodio — volver no re-resuelve
    // ponytail: clave anime+episodio — solo número servía los servidores del anime anterior
    private val serverCache = mutableMapOf<Pair<String, Float>, List<VideoServer>>()
    private var playJob: kotlinx.coroutines.Job? = null

    // ponytail: servidores ya intentados del episodio actual — el auto-avance termina siempre
    private val tried = mutableSetOf<Int>()

    // ponytail: cada play/switch invalida lo anterior — la resolución lenta no pisa lo nuevo
    private var generation = 0

    fun load(
        animeTitle: String,
        aliases: List<String>,
        episode: DomainEpisode,
        allEpisodes: List<DomainEpisode>,
        episodeUrl: String? = null
    ) {
        title = animeTitle
        synonyms = aliases
        this.episodeUrl = episodeUrl
        episodes = allEpisodes.sortedBy { it.number }
        play(episode)
    }

    fun retry() {
        current?.let { play(it) }
    }

    fun switchServer(index: Int) {
        val ready = _uiState.value as? PlayerUiState.Ready ?: return
        if (index == ready.selectedIndex || index !in ready.servers.indices) return
        val gen = generation
        _uiState.value = ready.copy(switching = true)
        viewModelScope.launch(Dispatchers.IO) {
            // ponytail: tope por servidor — sin esto un embed colgado deja los chips muertos
            val stream = withTimeoutOrNull(15_000L) { extractor.extract(ready.servers[index]) }
            withContext(Dispatchers.Main) {
                if (gen != generation) return@withContext
                val now = _uiState.value as? PlayerUiState.Ready
                if (now != null && stream != null && runCatching { playStream(stream) }.isSuccess) {
                    _uiState.value = now.copy(selectedIndex = index, stream = stream, switching = false)
                } else {
                    _uiState.value = (now ?: ready).copy(switching = false)
                    _notice.tryEmit("Servidor sin respuesta, prueba otro")
                }
            }
        }
    }

    fun playEpisode(episode: DomainEpisode) = play(episode)

    // ponytail: URL que parecía válida pero el player la rechaza (expirada, HTML, etc.)
    fun onPlaybackError() {
        val ready = _uiState.value as? PlayerUiState.Ready ?: return
        tried += ready.selectedIndex
        val next = ready.servers.indices.firstOrNull { it !in tried }
        if (next == null) {
            _uiState.value = PlayerUiState.Error("Sin servidores disponibles")
        } else {
            _notice.tryEmit("Probando otro servidor…")
            switchServer(next)
        }
    }

    fun previous(): DomainEpisode? {
        val cur = current ?: return null
        return episodes.filter { it.number < cur.number }.maxByOrNull { it.number }
    }

    fun next(): DomainEpisode? {
        val cur = current ?: return null
        return episodes.filter { it.number > cur.number }.minByOrNull { it.number }
    }

    fun skipForward(ms: Long = 85000L) {
        player.seekTo((player.currentPosition + ms).coerceAtMost(player.duration.coerceAtLeast(0L)))
    }

    fun skipBackward(ms: Long = 5000L) {
        player.seekTo((player.currentPosition - ms).coerceAtLeast(0L))
    }

    private fun play(episode: DomainEpisode) {
        current = episode
        _currentEpisode.value = episode
        tried.clear()
        // ponytail: corta lo anterior — sin esto la resolución lenta gana al episodio nuevo
        playJob?.cancel()
        player.stop()
        val gen = ++generation
        android.util.Log.d("PlayerFlow", "play ep=${episode.number} url=${episode.url} title=$title gen=$gen")
        _uiState.value = PlayerUiState.LoadingServers
        playJob = viewModelScope.launch(Dispatchers.IO) {
            val raw = serverCache.getOrPut(title to episode.number) {
                repository.getEpisodeServers(title, synonyms, episode.number, episodeUrl = episodeUrl)
            }
            if (raw.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _uiState.value = PlayerUiState.Error("Sin servidores disponibles")
                }
                return@launch
            }
            // ponytail: solo servidores con extractor — el resto son chips muertos
            val dropped = raw.filterNot { extractor.supports(it) }
            if (dropped.isNotEmpty()) {
                android.util.Log.d("PlayerServers", "sin extractor: ${dropped.map { it.name }}")
            }
            val servers = raw.filter { extractor.supports(it) }
            if (servers.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _uiState.value = PlayerUiState.Error("Sin servidores compatibles")
                }
                return@launch
            }
            var resolved: Pair<VideoServer, VideoStream>? = null
            var selected = -1
            for ((i, server) in servers.withIndex()) {
                val stream = withTimeoutOrNull(15_000L) { extractor.extract(server) }
                if (stream != null) {
                    resolved = server to stream
                    selected = i
                    break
                }
            }
            withContext(Dispatchers.Main) {
                if (gen != generation) return@withContext
                val resolvedNow = resolved
                    ?: return@withContext run {
                        _uiState.value = PlayerUiState.Error("Sin servidores disponibles")
                    }
                val stream = resolvedNow.second
                android.util.Log.d(
                    "PlayerFlow",
                    "resolved ep=${episode.number} server=${resolvedNow.first.name} stream=${stream.videoUrl}"
                )
                // ponytail: URL rota no tumba la app — va al error con Reintentar
                if (runCatching { playStream(stream) }.isFailure) {
                    _uiState.value = PlayerUiState.Error("No se pudo reproducir el video")
                    return@withContext
                }
                serverCache[title to episode.number] = servers
                _uiState.value = PlayerUiState.Ready(stream, servers, selected)
            }
        }
    }

    private fun playStream(stream: VideoStream) {
        val http = DefaultHttpDataSource.Factory()
        if (stream.headers.isNotEmpty()) http.setDefaultRequestProperties(stream.headers)
        val source = DefaultMediaSourceFactory(http, androidx.media3.extractor.DefaultExtractorsFactory())
            .createMediaSource(MediaItem.fromUri(stream.videoUrl))
        player.setMediaSource(source)
        player.prepare()
        player.playWhenReady = true
    }

    override fun onCleared() {
        player.release()
        super.onCleared()
    }
}
