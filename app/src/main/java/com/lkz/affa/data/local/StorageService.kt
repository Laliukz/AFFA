package com.lkz.affa.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class StorageService(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("affa_storage", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "theme_mode"
        private const val KEY_TOKEN = "auth_token"
    }

    suspend fun saveThemeMode(mode: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_THEME, mode).apply()
    }

    suspend fun getThemeMode(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_THEME, null)
    }

    suspend fun saveToken(token: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    suspend fun getToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_TOKEN, null)
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }
}
