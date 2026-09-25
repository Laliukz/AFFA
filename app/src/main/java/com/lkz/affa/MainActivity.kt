package com.lkz.affa

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.activity.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.lkz.affa.core.metadata.MetadataModule
import com.lkz.affa.core.network.scraper.AnimeAv1Scraper
import com.lkz.affa.core.network.scraper.JkAnimeScraper
import com.lkz.affa.core.network.scraper.TioAnimeScraper
import com.lkz.affa.data.repository.HomeRepository
import com.lkz.affa.ui.screens.AnimeHomeScreen
import com.lkz.affa.ui.screens.DetailsViewModel
import com.lkz.affa.ui.screens.HomeViewModel
import com.lkz.affa.ui.screens.PlayerViewModel

@androidx.media3.common.util.UnstableApi
class MainActivity : ComponentActivity() {

    private val repository by lazy {
        HomeRepository(AnimeAv1Scraper(), JkAnimeScraper(), TioAnimeScraper(), MetadataModule.createProvider())
    }

    private val homeViewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(repository) as T
            }
        }
    }

    val detailsViewModel: DetailsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DetailsViewModel(repository) as T
            }
        }
    }

    val playerViewModel: PlayerViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PlayerViewModel(applicationContext, repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MetadataModule.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            AnimeHomeScreen(
                viewModel = homeViewModel,
                detailsViewModel = detailsViewModel,
                playerViewModel = playerViewModel
            )
        }
    }
}


