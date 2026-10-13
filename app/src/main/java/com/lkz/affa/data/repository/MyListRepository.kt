package com.lkz.affa.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.lkz.affa.domain.model.HomeAnimeItem
import com.lkz.affa.domain.model.MyListItem
import com.lkz.affa.domain.model.MyListStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MyListRepository(context: Context) {
    private val prefs = context.getSharedPreferences("my_list_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val _itemsFlow = MutableStateFlow<List<MyListItem>>(emptyList())
    val itemsFlow: StateFlow<List<MyListItem>> = _itemsFlow.asStateFlow()

    init {
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val json = prefs.getString(KEY_ITEMS, null) ?: return
        val type = object : TypeToken<List<MyListItem>>() {}.type
        val list: List<MyListItem> = runCatching { gson.fromJson<List<MyListItem>>(json, type) }
            .getOrNull() ?: emptyList()
        _itemsFlow.value = list
    }

    private fun persist(list: List<MyListItem>) {
        _itemsFlow.value = list
        prefs.edit().putString(KEY_ITEMS, gson.toJson(list)).apply()
    }

    fun isInList(title: String): Boolean {
        val t = title.lowercase().trim()
        return _itemsFlow.value.any { it.title.lowercase().trim() == t }
    }

    fun toggle(item: HomeAnimeItem, defaultStatus: MyListStatus = MyListStatus.PLAN_TO_WATCH): Boolean {
        val current = _itemsFlow.value.toMutableList()
        val index = current.indexOfFirst { it.title.equals(item.title, ignoreCase = true) }
        return if (index >= 0) {
            current.removeAt(index)
            persist(current)
            false
        } else {
            val newItem = MyListItem(
                title = item.title,
                slug = item.slug,
                imageUrl = item.imageUrl,
                bannerUrl = item.bannerUrl,
                score = item.score,
                genres = item.genres,
                status = defaultStatus,
                addedAt = System.currentTimeMillis()
            )
            current.add(0, newItem)
            persist(current)
            true
        }
    }

    fun updateStatus(title: String, newStatus: MyListStatus) {
        val current = _itemsFlow.value.map {
            if (it.title.equals(title, ignoreCase = true)) it.copy(status = newStatus) else it
        }
        persist(current)
    }

    fun remove(title: String) {
        val current = _itemsFlow.value.filterNot { it.title.equals(title, ignoreCase = true) }
        persist(current)
    }

    companion object {
        private const val KEY_ITEMS = "saved_items"
    }
}
