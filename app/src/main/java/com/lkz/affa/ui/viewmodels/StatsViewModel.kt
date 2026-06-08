package com.lkz.affa.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.local.DatabaseHelper
import com.lkz.affa.data.local.HistoryDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Equivalent of Riverpod's [watchedCountProvider].
 * Exposes a [watchedCount] StateFlow that holds the total number of anime
 * the user has watched (i.e. rows in the history table).
 */
class StatsViewModel(private val historyDao: HistoryDao) : ViewModel() {

    private val _watchedCount = MutableStateFlow<Int>(0)
    val watchedCount: StateFlow<Int> = _watchedCount.asStateFlow()

    init {
        loadWatchedCount()
    }

    fun loadWatchedCount() {
        viewModelScope.launch {
            _watchedCount.value = historyDao.getWatchedAnimeCount()
        }
    }


    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val dbHelper = DatabaseHelper.getInstance(context)
            return StatsViewModel(HistoryDao(dbHelper)) as T
        }
    }
}
