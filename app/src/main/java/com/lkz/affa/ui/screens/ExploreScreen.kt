package com.lkz.affa.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.lkz.affa.ui.components.BottomNavigationBar



import com.lkz.affa.ui.components.ContentCard

class SearchActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AnimeSearchScreen()
            }
        }
    }
}

data class AnimeResult(
    val title: String,
    val info: String,
    val rating: String,
    val tag: String? = null,
    val tagColor: Color = PrimaryPurple
)

@Composable
fun AnimeSearchScreen(
    currentScreen: String = "explore",
    onScreenSelected: (String) -> Unit = {}
) {
    val searchResults = listOf(
        AnimeResult("Jujutsu Kaisen", "TV Series • 2020", "4.9", "NEW EP", PrimaryPurple),
        AnimeResult("Jujutsu Kaisen 0", "Movie • 2021", "4.8"),
        AnimeResult("Attack on Titan", "TV Series • 2013", "4.5"),
        AnimeResult("Demon Slayer", "TV Series • 2019", "4.2", "DUB", TagGreen),
        AnimeResult("Chainsaw Man", "TV Series • 2022", "4.7"),
        AnimeResult("Bleach: Thousand-...", "TV Series • 2022", "3.8")
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
            SearchTopBar()
            FilterRow()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Animes Populares",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .background(CardBackground, RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "12 Resultados",
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(searchResults) { _, anime ->
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
fun SearchTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Botón Atrás
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = Color.White,
            modifier = Modifier
                .size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))

        // Barra de búsqueda simulada
        Row(
            modifier = Modifier
                .weight(1f)
                .height(44.dp)
                .background(CardBackground, RoundedCornerShape(24.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search icon",
                tint = TextGray,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Buscar",
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                tint = TextGray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun FilterRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Botón Sort By (Rojo/Rosa)
        Button(
            onClick = { },
            colors = ButtonDefaults.buttonColors(containerColor = FilterActiveRed),
            shape = RoundedCornerShape(50),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            modifier = Modifier.height(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Menu,
                contentDescription = "Sort",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Sort By", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Filtros secundarios
        val filters = listOf("Genre", "Year", "Status")
        filters.forEach { filterName ->
            OutlinedButton(
                onClick = { },
                colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBackground),
                border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Transparent)),
                shape = RoundedCornerShape(50),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(text = filterName, color = Color.White, fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Dropdown",
                    tint = TextGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun AnimeCardItem(anime: AnimeResult) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Gray) // Placeholder de la imagen (reemplazar con AsyncImage si se requiere)
        ) {
            // Insignia superior izquierda (ej. NEW EP o DUB)
            if (anime.tag != null) {
                Text(
                    text = anime.tag,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .background(anime.tagColor, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Calificación (Rating) en la esquina inferior derecha de la tarjeta
            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Rating",
                    tint = RatingYellow,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = anime.rating,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = anime.title,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = anime.info,
            color = TextGray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
