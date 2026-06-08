package com.lkz.affa.ui.viewmodels

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DownloadItem(
    val showTitle: String,
    val episodeInfo: String,
    val size: String,
    val progress: Float = 0f, // 1.0f = downloaded
    val image: String
)

data class DownloadsState(
    val downloading: List<DownloadItem> = emptyList(),
    val downloaded: List<DownloadItem> = emptyList(),
    val usedStorageGb: Double = 12.4,
    val totalStorageGb: Double = 64.0
)

class DownloadsViewModel : ViewModel() {

    private val _state = MutableStateFlow(DownloadsState())
    val state: StateFlow<DownloadsState> = _state.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        _state.value = DownloadsState(
            downloading = listOf(
                DownloadItem(
                    showTitle = "Jujutsu Kaisen",
                    episodeInfo = "T2 E5 • Incidente en Shibuya",
                    size = "250 MB",
                    progress = 0.45f,
                    image = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=2670&auto=format&fit=crop"
                )
            ),
            downloaded = listOf(
                DownloadItem(
                    showTitle = "One Piece",
                    episodeInfo = "E1071 • Luffy Joyboy",
                    size = "340 MB",
                    progress = 1.0f,
                    image = "https://images.unsplash.com/photo-1533907650686-70576141c030?q=80&w=2670&auto=format&fit=crop"
                ),
                DownloadItem(
                    showTitle = "Demon Slayer",
                    episodeInfo = "T3 E1 • Swordsmith Village",
                    size = "410 MB",
                    progress = 1.0f,
                    image = "https://images.unsplash.com/photo-1618336753974-aae8e04506aa?q=80&w=2670&auto=format&fit=crop"
                )
            )
        )
    }

    fun deleteDownload(item: DownloadItem) {
        _state.update { currentState ->
            if (item.progress < 1.0f) {
                currentState.copy(
                    downloading = currentState.downloading.filter { it != item }
                )
            } else {
                currentState.copy(
                    downloaded = currentState.downloaded.filter { it != item }
                )
            }
        }
    }

    fun clearAll() {
        _state.update { currentState ->
            currentState.copy(
                downloading = emptyList(),
                downloaded = emptyList(),
                usedStorageGb = 0.0
            )
        }
    }
}
