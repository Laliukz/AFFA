package com.lkz.affa.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.local.DatabaseHelper
import com.lkz.affa.data.local.HistoryDao
import com.lkz.affa.data.models.Content
import com.lkz.affa.data.repository.ContentRepository
import com.lkz.affa.data.remote.AnilistService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DetailsState {
    object Loading : DetailsState()
    data class Success(
        val content: Content,
        val lastWatchedEpisode: Int = 0,
        val similarAnime: List<Content> = emptyList(),
        val watchedEpisodes: Set<Int> = emptySet()
    ) : DetailsState()
    data class Error(val message: String) : DetailsState()
}

class DetailsViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {
    private val anilistService = AnilistService()

    private val _uiState = MutableStateFlow<DetailsState>(DetailsState.Loading)
    val uiState: StateFlow<DetailsState> = _uiState.asStateFlow()

    init {
        val animeIdStr: String? = savedStateHandle.get<String>("animeId")
        if (animeIdStr != null) {
            loadAnimeDetails(animeIdStr)
        } else {
            _uiState.value = DetailsState.Error("ID de anime no válido")
        }
    }

    private fun loadAnimeDetails(rawIdStr: String) {
        val idStr = if (rawIdStr.isNotEmpty() && !rawIdStr.contains(":")) "anilist:$rawIdStr" else rawIdStr
        viewModelScope.launch {
            _uiState.value = DetailsState.Loading
            try {
                var details: Content
                if (idStr.startsWith("tmdb:")) {
                    val tmdbId = idStr.removePrefix("tmdb:").toInt()
                    details = com.lkz.affa.data.remote.TmdbService.instance.getAnimeDetails(tmdbId)
                } else {
                    val anilistId = idStr.removePrefix("anilist:").toIntOrNull() ?: idStr.toInt()
                    details = anilistService.getAnimeDetails(anilistId)
                }
                
                var lastWatchedEp = 0
                val watchedEps = mutableSetOf<Int>()
                try {
                    val dbHelper = DatabaseHelper.getInstance(getApplication())
                    val historyDao = HistoryDao(dbHelper)
                    val historyList = historyDao.getHistory()
                    val lastWatchedStr = historyList.firstOrNull { it["anime_id"] == idStr }?.get("last_episode_number") as? String
                    lastWatchedEp = lastWatchedStr?.toIntOrNull() ?: 0
                    
                    val explicitlyUnwatched = historyDao.getUnwatchedEpisodes(idStr)
                    val explicitlyWatched = historyDao.getWatchedEpisodes(idStr)
                    
                    if (lastWatchedEp > 0) {
                        for (i in 1..lastWatchedEp) {
                            if (!explicitlyUnwatched.contains(i)) {
                                watchedEps.add(i)
                            }
                        }
                    }
                    
                    watchedEps.addAll(explicitlyWatched)
                } catch (e: Exception) {}
                
                val similares = details.recommendations ?: emptyList()
                
                _uiState.value = DetailsState.Success(details, lastWatchedEp, similares, watchedEps)
                ContentRepository.currentDetails = details
                
                val currentLocales = androidx.core.app.LocaleManagerCompat.getApplicationLocales(getApplication())
                val currentLanguage = if (!currentLocales.isEmpty) currentLocales.get(0)?.language else java.util.Locale.getDefault().language
                
                if (currentLanguage == "es") {
                    launch {
                        try {
                            val spanishInfo = com.lkz.affa.data.remote.SpanishContentService.instance.getSpanishInfo(details.title)
                            if (spanishInfo != null) {
                                val finalDetails = details.copy(
                                    description = spanishInfo.description ?: details.description,
                                    tags = if (spanishInfo.genres.isNotEmpty()) spanishInfo.genres else details.tags
                                )
                                val currentState = _uiState.value as? DetailsState.Success
                                _uiState.value = DetailsState.Success(
                                    content = finalDetails,
                                    lastWatchedEpisode = currentState?.lastWatchedEpisode ?: lastWatchedEp,
                                    similarAnime = similares,
                                    watchedEpisodes = currentState?.watchedEpisodes ?: watchedEps
                                )
                                ContentRepository.currentDetails = finalDetails
                            }
                        } catch (e: Exception) {
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = DetailsState.Error(e.localizedMessage ?: "Error al cargar detalles")
            }
        }
    }
    
    fun toggleEpisodeWatched(animeId: String, episodeNum: Int, isWatched: Boolean) {
        viewModelScope.launch {
            try {
                val dbHelper = DatabaseHelper.getInstance(getApplication())
                val historyDao = HistoryDao(dbHelper)
                val currentState = _uiState.value
                var title: String? = null
                var image: String? = null
                if (currentState is DetailsState.Success) {
                    title = currentState.content.title
                    image = currentState.content.imageUrl
                    
                    val updatedWatched = currentState.watchedEpisodes.toMutableSet()
                    if (isWatched) {
                        updatedWatched.add(episodeNum)
                        _uiState.value = currentState.copy(
                            lastWatchedEpisode = maxOf(currentState.lastWatchedEpisode, episodeNum),
                            watchedEpisodes = updatedWatched
                        )
                    } else {
                        updatedWatched.remove(episodeNum)
                        _uiState.value = currentState.copy(
                            lastWatchedEpisode = if (currentState.lastWatchedEpisode == episodeNum) maxOf(0, episodeNum - 1) else currentState.lastWatchedEpisode,
                            watchedEpisodes = updatedWatched
                        )
                    }
                }

                if (isWatched) {
                    historyDao.saveProgress(
                        animeId = animeId,
                        episodeId = "ep_$episodeNum",
                        episodeNumber = episodeNum.toString(),
                        positionSeconds = 0,
                        title = title,
                        imageUrl = image,
                        durationSeconds = 0
                    )
                } else {
                    val prevEp = maxOf(0, episodeNum - 1)
                    if (prevEp > 0) {
                        historyDao.saveProgress(
                            animeId = animeId,
                            episodeId = "ep_$prevEp",
                            episodeNumber = prevEp.toString(),
                            positionSeconds = 0,
                            title = title,
                            imageUrl = image,
                            durationSeconds = 0
                        )
                    }
                    
                    val progressValues = android.content.ContentValues().apply {
                        put("anime_id", animeId)
                        put("episode_id", "ep_$episodeNum")
                        put("episode_number", episodeNum.toString())
                        put("position_seconds", 0)
                        put("duration_seconds", 0)
                        put("is_watched", 0)
                        put("updated_at", System.currentTimeMillis())
                    }
                    dbHelper.writableDatabase.insertWithOnConflict("episode_progress", null, progressValues, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                }
                
                com.lkz.affa.data.repository.FavoritesRepository(getApplication()).syncAutoCompletions()
            } catch (e: Exception) {
            }
        }
    }
}
