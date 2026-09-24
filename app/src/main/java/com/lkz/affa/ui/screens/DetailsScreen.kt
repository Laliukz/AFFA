package com.lkz.affa.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lkz.affa.ui.theme.*
import com.lkz.affa.ui.components.EpisodeItem


class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AnimeDetailScreen()
            }
        }
    }
}

data class Episode(
    val number: String,
    val title: String,
    val duration: String,
    val description: String,
    val progress: Float? = null // Progreso opcional para la barrita morada del episodio
)

@Composable
fun AnimeDetailScreen(
    onBackClick: () -> Unit = {},
    onEpisodeClick: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Episodios", "Similares")

    val episodes = listOf(
        Episode("E1", "Sound Hashira Tengen Uzui", "24m", "The team arrives at the entertainment district looking fo...", 1.0f),
        Episode("E2", "Infiltrating the District", "23m", "Tanjiro disguises himself to gather intel on the upper rank demon.", 0.3f),
        Episode("E3", "What Are You?", "24m", "Zenitsu disappears while investigating the Kyogoku House."),
        Episode("E4", "Tonight", "24m", "The battle begins as the demon reveals her true form.")
    )

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Cabecera con Imagen de Fondo y Botones Superiores
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(420.dp)
                ) {
                    // Placeholder de la imagen principal (reemplazar con AsyncImage)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.DarkGray)
                    )

                    // Gradiente superior e inferior para fundir con el fondo oscuro
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.5f), Color.Transparent, DarkBackground),
                                    startY = 0f,
                                    endY = 1200f
                                )
                            )
                    )

                    // Botones de la barra superior (Atrás, Cast, Compartir)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                                .size(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    }
                }
            }

            // Información principal del anime
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    // Etiquetas (NUEVO, HD)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "NUEVO",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(PrimaryPurple, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Título del Anime
                    Text(
                        text = "Kimetsu no Yaiba:\nEntertainment District Arc",
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 32.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Metadatos (Año, Categoría, Genero, Episodios)
                    Text(
                        text = "2022 • Acción • Shonen • 11 Episodios",
                        color = TextGray,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Valoración con Estrellas y Reseñas
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "4.9",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Row {
                            repeat(5) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Star",
                                    tint = RatingYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de Acción (Reproducir E1 y Agregar)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Reproducir E1", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = { },
                            modifier = Modifier
                                .background(CardBackground, CircleShape)
                                .size(48.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Sinopsis
                    Text(
                        text = "Tanjiro y sus amigos acompañan al Hashira del Sonido, Tengen Uzui, a un distrito donde moran demonios. Las luces brillantes ocultan sombras profundas y secretos peligrosos mientras buscan a las esposas desaparecidas de Tengen. ",
                        color = TextGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Leer más",
                        color = PrimaryPurple,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pestañas Internas (Episodios, Similares, Detalles)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        tabs.forEachIndexed { index, tabName ->
                            val isSelected = selectedTab == index
                            Column(
                                modifier = Modifier.clickable { selectedTab = index },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = tabName,
                                    color = if (isSelected) Color.White else TextGray,
                                    fontSize = 16.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(24.dp)
                                            .height(3.dp)
                                            .background(PrimaryPurple, RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Selector de Temporada y Contador de Episodios
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Temporada 2",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = Color.White
                            )
                        }
                        Text(
                            text = "11 EPS",
                            color = TextGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }

            // Lista de Episodios
            items(episodes) { episode ->
                EpisodeItem(
                    episode = episode,
                    onClick = onEpisodeClick
                )
            }
        }
    }
}
