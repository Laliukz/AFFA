package com.lkz.affa.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.repository.MyListRepository
import com.lkz.affa.domain.model.HomeAnimeItem
import com.lkz.affa.domain.model.MyListItem
import com.lkz.affa.domain.model.MyListStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

sealed interface MyListFilter {
    object All : MyListFilter
    data class ByStatus(val status: MyListStatus) : MyListFilter
}

sealed interface MyListUiState {
    object Empty : MyListUiState
    data class Success(
        val items: List<MyListItem>,
        val totalCount: Int,
        val filter: MyListFilter,
        val isEditMode: Boolean
    ) : MyListUiState
}

class MyListViewModel(
    private val repository: MyListRepository
) : ViewModel() {

    val items = repository.itemsFlow

    private val _filter = MutableStateFlow<MyListFilter>(MyListFilter.All)
    val filter: StateFlow<MyListFilter> = _filter

    private val _isEditMode = MutableStateFlow(false)
    val isEditMode: StateFlow<Boolean> = _isEditMode

    val uiState: StateFlow<MyListUiState> = combine(
        repository.itemsFlow,
        _filter,
        _isEditMode
    ) { items, currentFilter, editMode ->
        val filtered = when (currentFilter) {
            is MyListFilter.All -> items
            is MyListFilter.ByStatus -> items.filter { it.status == currentFilter.status }
        }
        if (items.isEmpty()) {
            MyListUiState.Empty
        } else {
            MyListUiState.Success(
                items = filtered,
                totalCount = items.size,
                filter = currentFilter,
                isEditMode = editMode
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MyListUiState.Empty)

    fun setFilter(newFilter: MyListFilter) {
        _filter.value = newFilter
    }

    fun toggleEditMode() {
        _isEditMode.value = !_isEditMode.value
    }

    fun toggle(item: HomeAnimeItem) = repository.toggle(item)

    fun updateStatus(title: String, status: MyListStatus) {
        repository.updateStatus(title, status)
    }

    fun remove(title: String) {
        repository.remove(title)
    }

    fun isInList(title: String): Boolean = repository.isInList(title)
}
