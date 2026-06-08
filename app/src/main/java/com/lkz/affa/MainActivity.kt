package com.lkz.affa

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.lkz.affa.navigation.Screen
import com.lkz.affa.navigation.SetupNavGraph
import com.lkz.affa.ui.components.BottomNavBar
import com.lkz.affa.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.os.Build
import android.Manifest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.Constraints
import androidx.work.NetworkType
import java.util.concurrent.TimeUnit
import com.lkz.affa.workers.EpisodeSyncWorker

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val workRequest = PeriodicWorkRequestBuilder<EpisodeSyncWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "EpisodeSyncWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )

        val requestPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
            } else {
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AppSecondary,
                    background = AppBackgroundDarkGray,
                    surface = AppBackgroundDarkGray
                )
            ) {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        val baseRoute = currentRoute?.substringBefore("?")?.substringBefore("/")
                        val bottomBarRoutes = listOf("home", "explore", "search", "mylist", "profile", "history")
                        val showBottomBar = baseRoute in bottomBarRoutes

                        androidx.compose.animation.AnimatedVisibility(
                            visible = showBottomBar,
                            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { it }) + androidx.compose.animation.fadeIn(),
                            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { it }) + androidx.compose.animation.fadeOut()
                        ) {
                            BottomNavBar(navController = navController)
                        }
                    },
                    containerColor = AppScaffoldBackground
                ) { paddingValues ->
                    SetupNavGraph(
                        navController = navController,
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
        }
    }
}
