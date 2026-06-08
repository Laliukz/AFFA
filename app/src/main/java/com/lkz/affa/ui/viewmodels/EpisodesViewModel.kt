package com.lkz.affa.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.models.AnimeEpisode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class EpisodesState {
    object Loading : EpisodesState()
    data class Success(val episodes: List<AnimeEpisode>) : EpisodesState()
    data class Error(val message: String) : EpisodesState()
}

class EpisodesViewModel : ViewModel() {

    private val _state = MutableStateFlow<EpisodesState>(EpisodesState.Loading)
    val state: StateFlow<EpisodesState> = _state.asStateFlow()

    fun loadEpisodes(content: com.lkz.affa.data.models.Content) {
        viewModelScope.launch {
            _state.value = EpisodesState.Loading
            try {
                val episodesList = mutableListOf<AnimeEpisode>()
                
                val maxAired = if (content.nextAiringEpisode != null) {
                    content.nextAiringEpisode - 1
                } else {
                    content.episodesCount ?: 0
                }
                
                if (maxAired > 0) {
                    for (i in 1..maxAired) {
                        val apiEp = content.streamingEpisodes?.find { 
                            it.title.contains("Episode $i ", ignoreCase = true) || 
                            it.title.endsWith("Episode $i", ignoreCase = true) 
                        } ?: content.streamingEpisodes?.getOrNull(i - 1)

                        episodesList.add(
                            AnimeEpisode(
                                id = "ep-$i",
                                number = i,
                                title = apiEp?.title ?: "Episodio $i",
                                image = null
                            )
                        )
                    }
                }
                
                _state.value = EpisodesState.Success(episodesList)
            } catch (e: Exception) {
                _state.value = EpisodesState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
