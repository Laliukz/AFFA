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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun allEpisodes(): List<DomainEpisode> = episodes

    private var title = ""
    private var synonyms: List<String> = emptyList()
    private var episodes: List<DomainEpisode> = emptyList()
    private var current: DomainEpisode? = null

    // ponytail: memoria de sesión por episodio — volver no re-resuelve
    private val serverCache = mutableMapOf<Float, List<VideoServer>>()

    fun load(
        animeTitle: String,
        aliases: List<String>,
        episode: DomainEpisode,
        allEpisodes: List<DomainEpisode>
    ) {
        title = animeTitle
        synonyms = aliases
        episodes = allEpisodes.sortedBy { it.number }
        play(episode)
    }

    fun retry() {
        current?.let { play(it) }
    }

    fun switchServer(index: Int) {
        val ready = _uiState.value as? PlayerUiState.Ready ?: return
        if (index == ready.selectedIndex || index !in ready.servers.indices) return
        _uiState.value = ready.copy(switching = true)
        viewModelScope.launch(Dispatchers.IO) {
            val stream = extractor.extract(ready.servers[index])
            withContext(Dispatchers.Main) {
                if (stream != null) {
                    playStream(stream)
                    _uiState.value = ready.copy(selectedIndex = index, stream = stream, switching = false)
                } else {
                    _uiState.value = ready.copy(switching = false)
                }
            }
        }
    }

    fun playEpisode(episode: DomainEpisode) = play(episode)

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

    private fun play(episode: DomainEpisode) {
        current = episode
        _currentEpisode.value = episode
        _uiState.value = PlayerUiState.LoadingServers
        viewModelScope.launch(Dispatchers.IO) {
            val servers = serverCache.getOrPut(episode.number) {
                repository.getEpisodeServers(title, synonyms, episode.number)
            }
            var resolved: Pair<VideoServer, VideoStream>? = null
            var selected = -1
            for ((i, server) in servers.withIndex()) {
                val stream = extractor.extract(server)
                if (stream != null) {
                    resolved = server to stream
                    selected = i
                    break
                }
            }
            withContext(Dispatchers.Main) {
                val resolvedNow = resolved
                    ?: return@withContext run {
                        _uiState.value = PlayerUiState.Error("Sin servidores disponibles")
                    }
                val stream = resolvedNow.second
                serverCache[episode.number] = servers
                playStream(stream)
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
