package com.lkz.affa.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.lkz.affa.ui.theme.PrimaryPurple
import com.lkz.affa.ui.theme.TextGray

@Composable
fun BottomNavigationBar(
    currentScreen: String = "home",
    onScreenSelected: (String) -> Unit = {}
) {
    NavigationBar(
        containerColor = Color(0xFF0F0C16),
        contentColor = Color.White
    ) {
        val itemColors = NavigationBarItemDefaults.colors(
            selectedIconColor = PrimaryPurple,
            selectedTextColor = PrimaryPurple,
            unselectedIconColor = TextGray,
            unselectedTextColor = TextGray,
            indicatorColor = Color.Transparent
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            selected = currentScreen == "home",
            onClick = { onScreenSelected("home") },
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Search, contentDescription = "Explorar") },
            label = { Text("Explorar") },
            selected = currentScreen == "explore",
            onClick = { onScreenSelected("explore") },
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Mi Lista") },
            label = { Text("Mi Lista") },
            selected = currentScreen == "mylist",
            onClick = { onScreenSelected("mylist") },
            colors = itemColors
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Cuenta") },
            label = { Text("Cuenta") },
            selected = currentScreen == "profile",
            onClick = { onScreenSelected("profile") },
            colors = itemColors
        )
    }
}