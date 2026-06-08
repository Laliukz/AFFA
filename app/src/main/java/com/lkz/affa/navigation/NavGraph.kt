package com.lkz.affa.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.lkz.affa.ui.screens.*

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Home : Screen("home")
    object Explore : Screen("explore")
    object Search : Screen("search")
    object MyList : Screen("mylist?category={category}") {
        fun createRoute(category: String = "Viendo") = "mylist?category=$category"
    }
    object History : Screen("history")
    object Profile : Screen("profile")
    object Downloads : Screen("downloads")
    object Details : Screen("details/{animeId}") {
        fun createRoute(animeId: String) = "details/$animeId"
    }
    object Settings : Screen("settings")
    object VideoPlayer : Screen("video_player/{animeId}/{episodeNum}") {
        fun createRoute(animeId: String, episodeNum: Int) = "video_player/$animeId/$episodeNum"
    }
    object SearchResults : Screen("search_results/{query}") {
        fun createRoute(query: String) = "search_results/$query"
    }
}

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        composable(route = Screen.Splash.route) {
            LoadingScreen(navController = navController)
        }
        composable(route = Screen.Home.route) {
            AnimeHomeScreen(navController = navController)
        }
        composable(route = Screen.Explore.route) {
            ExploreScreen(navController = navController)
        }
        composable(route = Screen.Search.route) {
            SearchScreen(navController = navController)
        }
        composable(
            route = Screen.MyList.route,
            arguments = listOf(navArgument("category") { 
                type = NavType.StringType
                defaultValue = "Viendo"
            })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category") ?: "Viendo"
            MyListScreen(navController = navController, initialCategory = category)
        }
        composable(route = Screen.History.route) {
            HistoryScreen(
                navController = navController
            )
        }
        composable(route = Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
        composable(route = Screen.Downloads.route) {
            DownloadsScreen(navController = navController)
        }
        composable(
            route = Screen.Details.route,
            arguments = listOf(navArgument("animeId") { type = NavType.StringType })
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
            DetailsScreen(navController = navController, animeId = animeId)
        }
        composable(route = Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        composable(
            route = Screen.VideoPlayer.route,
            arguments = listOf(
                navArgument("animeId") { type = NavType.StringType },
                navArgument("episodeNum") { type = NavType.IntType }
            )
        ) { backStackEntry ->
            val animeId = backStackEntry.arguments?.getString("animeId") ?: ""
            val episodeNum = backStackEntry.arguments?.getInt("episodeNum") ?: 1
            VideoPlayerScreen(
                navController = navController,
                animeId = animeId,
                episodeNum = episodeNum
            )
        }
        composable(
            route = Screen.SearchResults.route,
            arguments = listOf(navArgument("query") { type = NavType.StringType })
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query") ?: ""
            SearchResultsScreen(
                initialSearchQuery = query,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToDetails = { animeId -> navController.navigate(Screen.Details.createRoute(animeId)) }
            )
        }
    }
}
