package com.lkz.affa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.repository.HomeRepository
import com.lkz.affa.domain.model.HomeAnimeItem
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch

class ExploreViewModel(private val repository: HomeRepository) : ViewModel() {
    val query = MutableStateFlow("")
    val genre = MutableStateFlow<String?>(null)
    val year = MutableStateFlow<Int?>(null)
    val status = MutableStateFlow<String?>(null)
    val sort = MutableStateFlow("POPULARITY_DESC")

    private val _results = MutableStateFlow<List<HomeAnimeItem>>(emptyList())
    val results: StateFlow<List<HomeAnimeItem>> = _results.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _loadingMore = MutableStateFlow(false)
    val loadingMore: StateFlow<Boolean> = _loadingMore.asStateFlow()

    private val _hasMore = MutableStateFlow(true)
    val hasMore: StateFlow<Boolean> = _hasMore.asStateFlow()

    private var currentPage = 1
    private var currentParams = ExploreParams("", null, null, null, "POPULARITY_DESC")

    // Posicion del scroll: sobrevive al ir a detalles y volver
    var gridIndex: Int = 0
    var gridOffset: Int = 0

    @OptIn(FlowPreview::class)
    fun observe() {
        viewModelScope.launch {
            combine(
                query.debounce(500L), genre, year, status, sort
            ) { q, g, y, s, o -> ExploreParams(q, g, y, s, o) }
                .collect { p ->
                    currentParams = p
                    currentPage = 1
                    _hasMore.value = true
                    _loading.value = true
                    val initialItems = if (p.query.isBlank()) {
                        repository.getBrowseItems(p.genre, p.year, p.status, p.sort, page = 1)
                    } else {
                        repository.searchExplore(p.query)
                    }
                    _results.value = initialItems
                    if (p.query.isNotBlank() || initialItems.size < 20) {
                        _hasMore.value = false
                    }
                    _loading.value = false
                }
        }
    }

    fun loadMore() {
        if (_loading.value || _loadingMore.value || !_hasMore.value) return
        if (currentParams.query.isNotBlank()) return

        val nextPage = currentPage + 1
        _loadingMore.value = true
        viewModelScope.launch {
            val nextItems = repository.getBrowseItems(
                currentParams.genre,
                currentParams.year,
                currentParams.status,
                currentParams.sort,
                page = nextPage
            )
            if (nextItems.isEmpty()) {
                _hasMore.value = false
            } else {
                currentPage = nextPage
                val existing = _results.value
                val combined = (existing + nextItems).distinctBy { it.title.lowercase().trim() }
                _results.value = combined
                if (nextItems.size < 20) {
                    _hasMore.value = false
                }
            }
            _loadingMore.value = false
        }
    }

    private data class ExploreParams(
        val query: String,
        val genre: String?,
        val year: Int?,
        val status: String?,
        val sort: String
    )
}
