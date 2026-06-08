package com.lkz.affa.ui.viewmodels

import androidx.lifecycle.ViewModel
import com.lkz.affa.data.models.Content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HistoryItemModel(
    val showTitle: String,
    val episodeNumber: String,
    val episodeTitle: String? = null,
    val image: String,
    val viewedAt: String,
    val timeLeft: String? = null,
    val duration: String? = null,
    val progress: Float = 0f,
    val isCompleted: Boolean = false,
    val section: String
)

data class ListsState(
    val myList: List<Content> = emptyList(),
    val history: List<HistoryItemModel> = emptyList()
)

class ListsViewModel : ViewModel() {

    private val _state = MutableStateFlow(ListsState())
    val state: StateFlow<ListsState> = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        _state.value = ListsState(
            myList = listOf(
                Content(
                    title = "Demon Slayer",
                    imageUrl = "https://images.unsplash.com/photo-1618336753974-aae8e04506aa?q=80&w=2670&auto=format&fit=crop",
                    subtitle = "Temp 3 • Ep 4/12",
                    label = "NUEVO",
                    progress = 0.3f
                ),
                Content(
                    title = "Jujutsu Kaisen",
                    imageUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=2670&auto=format&fit=crop",
                    subtitle = "Temp 2 • Completado",
                    progress = 1.0f
                ),
                Content(
                    title = "One Piece",
                    imageUrl = "https://images.unsplash.com/photo-1533907650686-70576141c030?q=80&w=2670&auto=format&fit=crop",
                    subtitle = "Ep 1084",
                    progress = 0.8f
                ),
                Content(
                    title = "Spy x Family: Code...",
                    imageUrl = "https://images.unsplash.com/photo-1605810230434-7631ac76ec81?q=80&w=2670&auto=format&fit=crop",
                    subtitle = "Planeado",
                    label = "PELÍCULA"
                )
            ),
            history = listOf(
                HistoryItemModel(
                    showTitle = "One Piece",
                    episodeNumber = "Episodio 1072",
                    episodeTitle = "El poder ridículo, Gear 5",
                    image = "https://images.unsplash.com/photo-1618336753974-aae8e04506aa?q=80&w=2670&auto=format&fit=crop",
                    viewedAt = "Visto hace 2h",
                    timeLeft = "Restan 4m",
                    duration = "21:14",
                    progress = 0.8f,
                    section = "HOY"
                ),
                HistoryItemModel(
                    showTitle = "Jujutsu Kaisen",
                    episodeNumber = "Temporada 2, Ep 5",
                    episodeTitle = "Incidente en Shibuya",
                    image = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=2670&auto=format&fit=crop",
                    viewedAt = "Visto hace 5h",
                    duration = "23:40",
                    progress = 0.1f,
                    section = "HOY"
                ),
                HistoryItemModel(
                    showTitle = "Demon Slayer",
                    episodeNumber = "Arco del Distrito Rojo",
                    episodeTitle = "Episodio 11 - ¡No te rindas!",
                    image = "https://images.unsplash.com/photo-1618336753974-aae8e04506aa?q=80&w=2670&auto=format&fit=crop",
                    viewedAt = "Completado",
                    isCompleted = true,
                    section = "AYER"
                )
            )
        )
    }

    fun addToMyList(content: Content) {
        _state.update { currentState ->
            currentState.copy(
                myList = currentState.myList + content
            )
        }
    }

    fun removeFromMyList(content: Content) {
        _state.update { currentState ->
            currentState.copy(
                myList = currentState.myList.filter { it != content }
            )
        }
    }

    fun isInMyList(content: Content): Boolean {
        return _state.value.myList.contains(content)
    }

    fun clearHistory() {
        _state.update { currentState ->
            currentState.copy(
                history = emptyList()
            )
        }
    }
}
