package com.lkz.affa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.repository.HomeData
import com.lkz.affa.data.repository.HomeRepository
import com.lkz.affa.data.repository.SeeAllSection
import com.lkz.affa.domain.model.HomeAnimeItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val data: HomeData) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(
    private val repository: HomeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // ponytail: memoria de sesión — Ver Todo no re-pide; el refresh la limpia
    private val seeAllCache = mutableMapOf<SeeAllSection, List<HomeAnimeItem>>()

    init {
        loadHome()
    }

    fun loadHome() {
        val keepList = _uiState.value is HomeUiState.Success
        viewModelScope.launch {
            if (keepList) _isRefreshing.value = true else _uiState.value = HomeUiState.Loading
            seeAllCache.clear()
            repository.getHomeData()
                .onSuccess { _uiState.value = HomeUiState.Success(it) }
                .onFailure {
                    if (!keepList) _uiState.value =
                        HomeUiState.Error(it.localizedMessage ?: "Error desconocido")
                }
            _isRefreshing.value = false
        }
    }

    suspend fun loadSeeAll(section: SeeAllSection) =
        seeAllCache.getOrPut(section) { repository.getSeeAll(section) }
}
