package com.lkz.affa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.repository.HomeRepository
import com.lkz.affa.domain.model.AnimeDetails
import com.lkz.affa.domain.model.Episode as DomainEpisode
import com.lkz.affa.domain.model.HomeAnimeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface DetailsUiState {
    data object Loading : DetailsUiState
    data class Success(
        val item: HomeAnimeItem,
        val details: AnimeDetails?,
        val episodes: List<DomainEpisode>
    ) : DetailsUiState
    data class Error(val message: String) : DetailsUiState
}

class DetailsViewModel(
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailsUiState>(DetailsUiState.Loading)
    val uiState: StateFlow<DetailsUiState> = _uiState.asStateFlow()

    private var lastItem: HomeAnimeItem? = null

    fun load(item: HomeAnimeItem?) {
        // ponytail: volver a la pantalla no re-pide si es el mismo anime
        if (item != null && (_uiState.value as? DetailsUiState.Success)?.item?.title == item.title) return
        lastItem = item
        if (item == null) {
            _uiState.value = DetailsUiState.Error("Sin anime seleccionado")
            return
        }
        _uiState.value = DetailsUiState.Loading
        viewModelScope.launch {
            val details = repository.getAnimeDetails(item.title, item.synonyms)
            _uiState.value = DetailsUiState.Success(item, details, details?.episodes.orEmpty())
        }
    }

    fun retry() = load(lastItem)
}
