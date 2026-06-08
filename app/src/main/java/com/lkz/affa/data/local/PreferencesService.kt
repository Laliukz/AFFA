package com.lkz.affa.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PreferencesService(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("affa_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_SOURCE = "favorite_source"
    }

    suspend fun getThemeMode(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_THEME, null)
    }

    suspend fun setThemeMode(mode: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_THEME, mode).apply()
    }

    suspend fun getPreferredSource(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_SOURCE, null)
    }

    suspend fun setPreferredSource(source: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_SOURCE, source).apply()
    }
}
