package com.lkz.affa.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.local.FavoritesDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class FavoritesState {
    object Loading : FavoritesState()
    data class Success(val favorites: List<Map<String, Any>>) : FavoritesState()
    data class Error(val message: String) : FavoritesState()
}

class FavoritesViewModel(
    private val dao: FavoritesDao
) : ViewModel() {

    private val _state = MutableStateFlow<FavoritesState>(FavoritesState.Loading)
    val state: StateFlow<FavoritesState> = _state.asStateFlow()

    init {
        loadFavorites()
    }

    fun loadFavorites() {
        viewModelScope.launch {
            _state.value = FavoritesState.Loading
            try {
                val favorites = dao.getFavorites()
                _state.value = FavoritesState.Success(favorites)
            } catch (e: Exception) {
                _state.value = FavoritesState.Error(e.message ?: "Unknown error")
            }
        }
    }

    suspend fun isFavorite(id: String): Boolean {
        return try {
            dao.isFavorite(id)
        } catch (e: Exception) {
            false
        }
    }

    fun toggleFavorite(id: String, title: String, image: String?) {
        viewModelScope.launch {
            try {
                val isFav = dao.isFavorite(id)
                if (isFav) {
                    dao.removeFavorite(id)
                } else {
                    dao.addFavorite(id, title, image)
                }
                loadFavorites() // Refresh the list
            } catch (e: Exception) {
            }
        }
    }
}
