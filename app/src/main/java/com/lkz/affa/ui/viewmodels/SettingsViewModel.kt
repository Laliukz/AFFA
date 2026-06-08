package com.lkz.affa.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lkz.affa.data.local.PreferencesService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM
)

class SettingsViewModel(
    private val preferencesService: PreferencesService
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val themeStr = preferencesService.getThemeMode()
            val theme = when (themeStr) {
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM
            }

            _state.value = SettingsState(themeMode = theme)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            _state.update { it.copy(themeMode = mode) }
            val str = when (mode) {
                ThemeMode.LIGHT -> "light"
                ThemeMode.DARK -> "dark"
                else -> "system"
            }
            preferencesService.setThemeMode(str)
        }
    }
}
