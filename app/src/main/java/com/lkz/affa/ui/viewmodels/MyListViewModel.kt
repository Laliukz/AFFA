package com.lkz.affa.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.models.Content
import com.lkz.affa.data.repository.FavoritesRepository
import com.lkz.affa.data.remote.AnilistService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.launch

class MyListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FavoritesRepository(application)
    private val anilistService = AnilistService()
    private val tmdbService = com.lkz.affa.data.remote.TmdbService.instance

    private val _savedList = MutableStateFlow<List<Content>>(emptyList())
    val savedList: StateFlow<List<Content>> = _savedList.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var currentCategory = "Viendo"

    init {
        loadFavorites()
        viewModelScope.launch {
            syncEpisodes()
        }
    }

    fun forceRefresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            syncEpisodes()
            _isRefreshing.value = false
        }
    }

    fun loadFavorites(category: String = currentCategory) {
        currentCategory = category
        viewModelScope.launch {
            _savedList.value = repository.getFavorites(category)
        }
    }

    fun removeFavorite(id: String) {
        viewModelScope.launch {
            repository.removeFavorite(id)
            loadFavorites() // reload after removing
        }
    }

    fun addFavorite(content: Content, status: String) {
        viewModelScope.launch {
            repository.addFavorite(content, status)
            loadFavorites() // reload after adding
        }
    }
    
    private suspend fun syncEpisodes() {
        try {
                val allFavorites = repository.getFavorites("Todos")
                
                val anilistIdsToSync = allFavorites.mapNotNull { 
                    if (it.id?.startsWith("tmdb:") == true) null
                    else it.id?.removePrefix("anilist:")?.toIntOrNull() ?: it.id?.toIntOrNull()
                }
                
                val tmdbIdsToSync = allFavorites.mapNotNull { 
                    if (it.id?.startsWith("tmdb:") == true) it.id.removePrefix("tmdb:").toIntOrNull()
                    else null
                }
                
                var hasUpdates = false

                if (anilistIdsToSync.isNotEmpty()) {
                    try {
                        val updatedDetails = anilistService.getAnimeBatchDetails(anilistIdsToSync)
                        updatedDetails.forEach { updatedContent ->
                            val localContent = allFavorites.find { 
                                it.id == updatedContent.id || it.id == updatedContent.id?.removePrefix("anilist:") 
                            }
                            if (localContent != null) {
                                var needsUpdate = false
                                if (updatedContent.episodesCount != null && 
                                    (localContent.episodesCount == null || updatedContent.episodesCount > localContent.episodesCount)) {
                                    repository.updateTotalEpisodes(localContent.id!!, updatedContent.episodesCount)
                                    needsUpdate = true
                                }
                                if (updatedContent.releaseStatus != null && updatedContent.releaseStatus != localContent.releaseStatus || updatedContent.nextAiringEpisode != localContent.nextAiringEpisode) {
                                    repository.updateReleaseStatus(localContent.id!!, updatedContent.releaseStatus ?: localContent.releaseStatus, updatedContent.nextAiringEpisode)
                                    needsUpdate = true
                                }
                                if (needsUpdate) hasUpdates = true
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
                
                if (tmdbIdsToSync.isNotEmpty()) {
                    try {
                        val updatedDetails = tmdbService.getAnimeBatchDetails(tmdbIdsToSync)
                        updatedDetails.forEach { updatedContent ->
                            val localContent = allFavorites.find { it.id == updatedContent.id }
                            if (localContent != null) {
                                var needsUpdate = false
                                if (updatedContent.episodesCount != null && 
                                    (localContent.episodesCount == null || updatedContent.episodesCount > localContent.episodesCount)) {
                                    repository.updateTotalEpisodes(localContent.id!!, updatedContent.episodesCount)
                                    needsUpdate = true
                                }
                                if (updatedContent.releaseStatus != null && updatedContent.releaseStatus != localContent.releaseStatus || updatedContent.nextAiringEpisode != localContent.nextAiringEpisode) {
                                    repository.updateReleaseStatus(localContent.id!!, updatedContent.releaseStatus ?: localContent.releaseStatus, updatedContent.nextAiringEpisode)
                                    needsUpdate = true
                                }
                                if (needsUpdate) hasUpdates = true
                            }
                        }
                    } catch (e: Exception) {
                    }
                }
                repository.syncAutoCompletions()
                
                loadFavorites()
        } catch (e: Exception) {
        }
    }
}
