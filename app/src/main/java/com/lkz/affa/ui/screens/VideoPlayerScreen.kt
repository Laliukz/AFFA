package com.lkz.affa.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.exoplayer.offline.Download
import com.lkz.affa.ui.components.EpisodeItem
import com.lkz.affa.ui.theme.*

class NowPlayingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                NowPlayingScreen()
            }
        }
    }
}

@Composable
fun NowPlayingScreen(onBackClick: () -> Unit = {}) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Info", "Similares")

    Scaffold(
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Reproductor de video superior
            item {
                VideoPlayerSection(onBackClick = onBackClick)
            }

            // Metadatos y Acciones del Episodio
            item {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "EPISODIO 1072",
                            color = PrimaryPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Icon(
                            imageVector = Icons.Default.Bookmark,
                            contentDescription = "Bookmark",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "The Power of Gear 5! The Drum of Liberation",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(text = "Released Aug 2023", color = TextGray, fontSize = 12.sp)
                        Text(text = "•", color = TextGray, fontSize = 12.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = CardBackground),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            modifier = Modifier.height(40.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Save", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = Color(0xFF262030), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Selector de Servidores
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Server",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ServerChip(name = "Mega Cloud", isSelected = true, indicatorColor = ServerIndicatorGreen)
                        ServerChip(name = "VidStream", isSelected = false, indicatorColor = ServerIndicatorGreen)
                        ServerChip(name = "Mirror 1", isSelected = false)
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Botones de Navegación de Episodios (Ep 1071 / Ep 1073)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { },
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = CardBackground),
                            border = ButtonDefaults.outlinedButtonBorder.copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Transparent)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Icon(imageVector = Icons.Default.SkipPrevious, contentDescription = "Prev", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Ep 1071", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(text = "Ep 1073", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(imageVector = Icons.Default.SkipNext, contentDescription = "Next", tint = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pestañas (Info, Comments, Related)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(32.dp)
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
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .width(28.dp)
                                            .height(3.dp)
                                            .background(PrimaryPurple, RoundedCornerShape(2.dp))
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Descripción / Sinopsis
                    Text(
                        text = "As the battle on the rooftop rages on, Luffy awakens a new power that baffles Kaido. The heartbeat of the Drums of Liberation resonates across Onigashima. Is this the return of Joy Boy?",
                        color = TextGray,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Etiquetas de género
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val genres = listOf("Action", "Adventure", "Fantasy", "Shonen")
                        genres.forEach { genre ->
                            Box(
                                modifier = Modifier
                                    .background(CardBackground, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(text = genre, color = TextGray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Sección "Next Episode"
                    Text(
                        text = "Next Episode",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tarjeta del siguiente episodio reutilizando EpisodeItem
                    val nextEpisode = Episode(
                        number = "E1073",
                        title = "No Way Out! A Hellish Scene on Onigashima",
                        duration = "23m",
                        description = "The battle intense on Onigashima as fire spreads through the castle."
                    )
                    EpisodeItem(episode = nextEpisode)
                }
            }
        }
    }
}

@Composable
fun VideoPlayerSection(onBackClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .background(Color.Black)
    ) {
        // Placeholder del video / fotograma principal
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.DarkGray)
        )

        // Barra superior del reproductor (Atrás, Título, Ajustes, Cast)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    .size(36.dp)
            ) {
                Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }

            Text(
                text = "NOW PLAYING",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // Controles centrales del reproductor (Retroceder 10s, Play, Adelantar 10s)
        Row(
            modifier = Modifier.align(Alignment.Center),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            Icon(imageVector = Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White, modifier = Modifier.size(32.dp))

            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(PrimaryPurple, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = "Play", tint = Color.White, modifier = Modifier.size(32.dp))
            }

            Icon(imageVector = Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(32.dp))
        }

        // Barra inferior de progreso del reproductor
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomStart)
                .background(Color.Black.copy(alpha = 0.3f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "04:20", color = Color.White, fontSize = 10.sp)
                Text(text = "23:14", color = Color.White, fontSize = 10.sp)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(2.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.25f)
                        .height(3.dp)
                        .background(PrimaryPurple, RoundedCornerShape(2.dp))
                )
            }
        }

        // Icono de pantalla completa flotante abajo a la derecha
        Icon(
            imageVector = Icons.Default.Fullscreen,
            contentDescription = "Fullscreen",
            tint = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(24.dp)
        )
    }
}

@Composable
fun ServerChip(name: String, isSelected: Boolean, indicatorColor: Color? = null) {
    Button(
        onClick = { },
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) PrimaryPurple else CardBackground
        ),
        shape = RoundedCornerShape(50),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = Modifier.height(38.dp)
    ) {
        if (indicatorColor != null) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(indicatorColor, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = name,
            color = if (isSelected) Color.White else TextGray,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )
    }
}