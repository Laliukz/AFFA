package com.lkz.affa.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.models.Content
import com.lkz.affa.data.repository.ContentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ContentState(
    val heroContent: Content? = null,
    val continueWatching: List<Content> = emptyList(),
    val popular: List<Content> = emptyList(),
    val recentlyAdded: List<Content> = emptyList(),
    val trends: List<Content> = emptyList(),
    val exploreGrid: List<Content> = emptyList(),
    val todayEpisodes: List<Content> = emptyList(),
    val isLoaded: Boolean = false
)

class ContentViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ContentState())
    val state: StateFlow<ContentState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _state.value = ContentRepository.getHomeContent(getApplication())
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            _state.value = ContentRepository.getHomeContent(getApplication(), forceRefresh = true)
            _isRefreshing.value = false
        }
    }

    fun refreshContinueWatching() {
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val continueWatching = com.lkz.affa.data.repository.ContentRepository.getContinueWatchingList(context)

                val currentState = _state.value
                val newState = currentState.copy(continueWatching = continueWatching)
                _state.value = newState
                
                com.lkz.affa.data.repository.ContentRepository.getHomeContent(context, false)
                val cacheField = com.lkz.affa.data.repository.ContentRepository::class.java.getDeclaredField("cachedState")
                cacheField.isAccessible = true
                cacheField.set(com.lkz.affa.data.repository.ContentRepository, newState)

            } catch (e: Exception) {
            }
        }
    }

    suspend fun search(
        query: String,
        sort: String? = null,
        genre: String? = null,
        year: Int? = null,
        status: String? = null
    ): List<Content> {
        return ContentRepository.search(query, sort, genre, year, status)
    }

    private var explorePage = 1
    private var isLoadingMore = false
    private var currentExploreGenre: String? = null

    fun setExploreGenre(genre: String?) {
        if (currentExploreGenre == genre) return
        currentExploreGenre = genre
        explorePage = 1
        _state.value = _state.value.copy(exploreGrid = emptyList())
        loadMoreExplore()
    }

    fun loadMoreExplore() {
        if (isLoadingMore) return
        isLoadingMore = true
        val pageToFetch = explorePage
        val genreToFetch = currentExploreGenre

        viewModelScope.launch {
            val moreContent = ContentRepository.getMoreExplore(pageToFetch, genreToFetch)
            if (moreContent.isNotEmpty()) {
                val currentState = _state.value
                val newList = if (pageToFetch == 1) moreContent else currentState.exploreGrid + moreContent
                _state.value = currentState.copy(exploreGrid = newList)
                explorePage++
            }
            isLoadingMore = false
        }
    }
}
