package com.lkz.affa.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.local.DatabaseHelper
import com.lkz.affa.data.local.HistoryDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class HistoryState {
    object Loading : HistoryState()
    data class Success(val history: List<Map<String, Any>>) : HistoryState()
    data class Error(val message: String) : HistoryState()
}

class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = HistoryDao(DatabaseHelper.getInstance(application))
    
    private val _state = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val state: StateFlow<HistoryState> = _state.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _state.value = HistoryState.Loading
            try {
                val history = dao.getHistory()
                _state.value = HistoryState.Success(history)
            } catch (e: Exception) {
                _state.value = HistoryState.Error(e.message ?: "Unknown error")
            }
        }
    }

    suspend fun getProgress(animeId: String): Map<String, Any>? {
        return try {
            dao.getProgress(animeId)
        } catch (e: Exception) {
            null
        }
    }

    fun saveProgress(
        animeId: String, 
        episodeId: String, 
        episodeNumber: String?, 
        positionSeconds: Int,
        title: String? = null,
        imageUrl: String? = null,
        durationSeconds: Int? = null
    ) {
        viewModelScope.launch {
            try {
                dao.saveProgress(animeId, episodeId, episodeNumber, positionSeconds, title, imageUrl, durationSeconds)
                loadHistory() // Optionally refresh history if needed
            } catch (e: Exception) {
            }
        }
    }
}
