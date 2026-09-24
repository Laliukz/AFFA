package com.lkz.affa.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkz.affa.ui.theme.*
import com.lkz.affa.ui.components.ContentCard
import com.lkz.affa.ui.components.BottomNavigationBar

class MyListActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MyListScreen()
            }
        }
    }
}

@Composable
fun MyListScreen(
    currentScreen: String = "mylist",
    onScreenSelected: (String) -> Unit = {}
) {
    val myListItems = listOf(
        AnimeResult("Demon Slayer", "Temp 3 • Ep 4/12", "4.8", "NUEVO", PrimaryPurple),
        AnimeResult("Jujutsu Kaisen", "Temp 2 • Completado", "4.9"),
        AnimeResult("One Piece", "Ep 1084", "4.7"),
        AnimeResult("Spy x Family: Code...", "Planeado", "4.6", "PELÍCULA", Color(0xFF4A4556)),
        AnimeResult("Chainsaw Man", "Temp 1 • Ep 6/12", "4.5"),
        AnimeResult("Blue Lock", "Planeado", "4.4")
    )

    Scaffold(
        bottomBar = { BottomNavigationBar(currentScreen, onScreenSelected) },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            MyListTopBar()
            FilterChipsRow()

            // Subtítulo de orden y contador
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ORDEN: RECIENTES",
                    color = TextGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "12 Animes",
                    color = TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Cuadrícula de tarjetas reutilizadas
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(myListItems) { anime ->
                    ContentCard(
                        anime = anime,
                        onClick = { onScreenSelected("details") }
                    )
                }
            }
        }
    }
}

@Composable
fun MyListTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Mi Lista",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Icono de cuadrícula/vista
            Icon(
                imageVector = Icons.Default.Menu, // Representación de cambio de vista
                contentDescription = "View Grid",
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            // Botón Editar estilo texto en morado
            Text(
                text = "Editar",
                color = PrimaryPurple,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun FilterChipsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val filters = listOf("Todos", "Viendo", "Planeado", "Completado")
        filters.forEachIndexed { index, filter ->
            val isSelected = index == 0 // "Todos" seleccionado por defecto
            Button(
                onClick = { },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected) PrimaryPurple else CardBackground
                ),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = filter,
                    color = if (isSelected) Color.White else TextGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
