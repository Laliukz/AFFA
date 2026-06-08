package com.lkz.affa.ui.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UserState(
    val isLoggedIn: Boolean = true, // Default to true for testing
    val userName: String = "Alex Kun",
    val email: String = "alex.kun@email.com",
    val avatarUrl: String = "https://images.unsplash.com/photo-1570295999919-56ceb5ecca61?q=80&w=2000&auto=format&fit=crop",
    val notificationsEnabled: Boolean = true,
    val language: String = "Español"
)

class UserViewModel : ViewModel() {

    private val _state = MutableStateFlow(UserState())
    val state: StateFlow<UserState> = _state.asStateFlow()

    fun login(email: String) {
        _state.update { it.copy(isLoggedIn = true, email = email) }
    }

    fun logout() {
        _state.update { it.copy(isLoggedIn = false) }
    }

    fun updateProfile(name: String? = null, email: String? = null, avatarUrl: String? = null) {
        _state.update { currentState ->
            currentState.copy(
                userName = name ?: currentState.userName,
                email = email ?: currentState.email,
                avatarUrl = avatarUrl ?: currentState.avatarUrl
            )
        }
    }

    fun toggleNotifications() {
        _state.update { it.copy(notificationsEnabled = !it.notificationsEnabled) }
    }

    fun setLanguage(language: String) {
        _state.update { it.copy(language = language) }
    }
}
