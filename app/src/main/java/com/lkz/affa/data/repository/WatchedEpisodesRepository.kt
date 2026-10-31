package com.lkz.affa.data.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Persiste las URLs de episodios vistos. */
class WatchedEpisodesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("watched_episodes_prefs", Context.MODE_PRIVATE)
    private val _watched = MutableStateFlow(prefs.getStringSet(KEY, emptySet()).orEmpty().toSet())
    val watched: StateFlow<Set<String>> = _watched.asStateFlow()

    fun toggle(episodeUrl: String) {
        val new = _watched.value.let { if (episodeUrl in it) it - episodeUrl else it + episodeUrl }
        _watched.value = new
        prefs.edit().putStringSet(KEY, new).apply()
    }

    companion object {
        private const val KEY = "watched_urls"
    }
}
