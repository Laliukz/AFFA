package com.lkz.affa.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.models.Content
import com.lkz.affa.data.models.StreamLink
import com.lkz.affa.data.remote.AnilistService
import com.lkz.affa.data.remote.TmdbService
import com.lkz.affa.data.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

sealed class VideoPlayerState {
    object Loading : VideoPlayerState()
    data class Success(
        val content: Content,
        val episodeNum: Int,
        val streams: List<StreamLink> = emptyList(),
        val isLoadingStreams: Boolean = false,
        val streamError: String? = null,
        val isResolvingVideo: Boolean = false,
        val resolvedVideoUrl: String? = null
    ) : VideoPlayerState()
    data class Error(val message: String) : VideoPlayerState()
}

class VideoPlayerViewModel(application: Application) : AndroidViewModel(application) {
    private val anilistService = AnilistService()

    private val _uiState = MutableStateFlow<VideoPlayerState>(VideoPlayerState.Loading)
    val uiState: StateFlow<VideoPlayerState> = _uiState.asStateFlow()

    fun loadVideoData(animeIdStr: String, episodeNum: Int) {
        viewModelScope.launch {
            _uiState.value = VideoPlayerState.Loading
            try {
                var details = ContentRepository.currentDetails
                
                if (details == null || details.id != animeIdStr) {
                    if (animeIdStr.startsWith("tmdb:")) {
                        val tmdbId = animeIdStr.removePrefix("tmdb:").toInt()
                        details = TmdbService.instance.getAnimeDetails(tmdbId)
                    } else {
                        val anilistId = animeIdStr.removePrefix("anilist:").toIntOrNull() ?: animeIdStr.toInt()
                        details = anilistService.getAnimeDetails(anilistId)
                    }
                    ContentRepository.currentDetails = details
                }

                
                _uiState.value = VideoPlayerState.Success(
                    content = details,
                    episodeNum = episodeNum,
                    streams = emptyList(),
                    isLoadingStreams = true,
                    streamError = null
                )

                val flvProvider = com.lkz.affa.data.remote.providers.AnimeFlvProvider()
                val av1Provider = com.lkz.affa.data.remote.providers.AnimeAv1Provider()
                
                val searchTitle = details.title
                
                val flvDeferred = async {
                    try {
                        val flvUrl = flvProvider.searchAnime(searchTitle).getOrThrow()
                        val flvEpisodeUrl = if (flvUrl.endsWith("/")) "${flvUrl}$episodeNum" else "${flvUrl}/$episodeNum"
                        flvProvider.getStreamLinks(flvEpisodeUrl).getOrDefault(emptyList())
                            .map { it.copy(server = "${it.server} (FLV)") }
                    } catch (e: Exception) {
                        android.util.Log.e("VideoPlayerViewModel", "Error en AnimeFLV: ${e.message}")
                        emptyList()
                    }
                }
                
                val av1Deferred = async {
                    try {
                        val av1Url = av1Provider.searchAnime(searchTitle).getOrThrow()
                        val av1EpisodeUrl = if (av1Url.endsWith("/")) "${av1Url}$episodeNum" else "${av1Url}/$episodeNum"
                        av1Provider.getStreamLinks(av1EpisodeUrl).getOrDefault(emptyList())
                            .map { it.copy(server = "${it.server} (AV1)") }
                    } catch (e: Exception) {
                        android.util.Log.e("VideoPlayerViewModel", "Error en AnimeAV1: ${e.message}")
                        emptyList()
                    }
                }
                
                val streams = (flvDeferred.await() + av1Deferred.await())
                    .filter { !it.server.contains("1fichier", ignoreCase = true) && !it.server.contains("1f", ignoreCase = true) }
                    .sortedWith(compareByDescending<com.lkz.affa.data.models.StreamLink> {
                        it.server.contains("AV1") && it.server.contains("HLS", ignoreCase = true)
                    }.thenByDescending {
                        it.server.contains("AV1")
                    })
                
                if (streams.isEmpty()) {
                    throw Exception("No se encontraron servidores en ninguna de las fuentes")
                }

                _uiState.value = VideoPlayerState.Success(
                    content = details,
                    episodeNum = episodeNum,
                    streams = streams,
                    isLoadingStreams = false,
                    streamError = if (streams.isEmpty()) "No se encontraron servidores en ninguna fuente." else null
                )
            } catch (e: Exception) {
                _uiState.value = VideoPlayerState.Error(e.localizedMessage ?: "Error al cargar el video")
            }
        }
    }

    fun selectStream(stream: StreamLink) {
        val currentState = _uiState.value as? VideoPlayerState.Success ?: return
        viewModelScope.launch {
            _uiState.value = currentState.copy(isResolvingVideo = true, resolvedVideoUrl = null, streamError = null)
            try {
                val resolver = com.lkz.affa.data.remote.resolvers.VideoResolverService()
                val result = resolver.resolve(stream.url)
                val url = result.getOrThrow()
                
                _uiState.value = (_uiState.value as VideoPlayerState.Success).copy(
                    isResolvingVideo = false,
                    resolvedVideoUrl = url
                )
            } catch (e: Exception) {
                _uiState.value = (_uiState.value as VideoPlayerState.Success).copy(
                    isResolvingVideo = false,
                    streamError = "Error al extraer video: ${e.message}"
                )
            }
        }
    }
    
    fun updateProgress(positionSeconds: Int, durationSeconds: Int) {
        val currentState = _uiState.value as? VideoPlayerState.Success ?: return
        viewModelScope.launch {
            try {
                val dbHelper = com.lkz.affa.data.local.DatabaseHelper.getInstance(getApplication())
                val historyDao = com.lkz.affa.data.local.HistoryDao(dbHelper)
                historyDao.saveProgress(
                    animeId = currentState.content.id ?: "",
                    episodeId = "ep_${currentState.episodeNum}",
                    episodeNumber = currentState.episodeNum.toString(),
                    positionSeconds = positionSeconds,
                    title = currentState.content.title,
                    imageUrl = currentState.content.imageUrl,
                    durationSeconds = durationSeconds
                )
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerViewModel", "Error saving progress: ${e.message}")
            }
        }
    }
}
