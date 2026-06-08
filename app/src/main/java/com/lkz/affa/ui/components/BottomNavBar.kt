package com.lkz.affa.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lkz.affa.navigation.Screen
import com.lkz.affa.ui.theme.*

@Composable
fun BottomNavBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(
        containerColor = AppScaffoldBackground,
        contentColor = AppTextSecondary
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio", style = AppTextStyles.label) },
            selected = currentRoute == Screen.Home.route,
            onClick = {
                if (currentRoute != Screen.Home.route){
                    navController.navigate(Screen.Home.route)
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppPrimary,
                selectedTextColor = AppPrimary,
                unselectedIconColor = AppTextSecondary,
                unselectedTextColor = AppTextSecondary,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Explore, contentDescription = "Explorar") },
            label = { Text("Explorar", style = AppTextStyles.label) },
            selected = currentRoute == Screen.Explore.route,
            onClick = {
                if (currentRoute != Screen.Explore.route){
                    navController.navigate(Screen.Explore.route)
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppPrimary,
                selectedTextColor = AppPrimary,
                unselectedIconColor = AppTextSecondary,
                unselectedTextColor = AppTextSecondary,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Bookmark, contentDescription = "Mi Lista") },
            label = { Text("Mi Lista", style = AppTextStyles.label) },
            selected = currentRoute == Screen.MyList.route,
            onClick = {
                if (currentRoute != Screen.MyList.route){
                    navController.navigate(Screen.MyList.createRoute())
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppPrimary,
                selectedTextColor = AppPrimary,
                unselectedIconColor = AppTextSecondary,
                unselectedTextColor = AppTextSecondary,
                indicatorColor = Color.Transparent
            )
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Cuenta") },
            label = { Text("Cuenta", style = AppTextStyles.label) },
            selected = currentRoute == Screen.Profile.route,
            onClick = {
                if (currentRoute != Screen.Profile.route){
                    navController.navigate(Screen.Profile.route)
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AppPrimary,
                selectedTextColor = AppPrimary,
                unselectedIconColor = AppTextSecondary,
                unselectedTextColor = AppTextSecondary,
                indicatorColor = Color.Transparent
            )
        )
    }
}
