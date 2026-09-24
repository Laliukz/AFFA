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
import com.lkz.affa.ui.screens.HomeViewModel

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = HomeRepository(AnimeAv1Scraper(), JkAnimeScraper(), TioAnimeScraper(), MetadataModule.createProvider())
                return HomeViewModel(repo) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MetadataModule.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            AnimeHomeScreen(viewModel = homeViewModel)
        }
    }
}


