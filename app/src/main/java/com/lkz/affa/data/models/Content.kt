package com.lkz.affa.data.models

data class ApiEpisode(
    val title: String,
    val thumbnail: String?,
    val url: String?
)

data class Content(
    val id: String? = null,
    val title: String,
    val imageUrl: String,
    val bannerUrl: String? = null,
    val subtitle: String? = null,
    val label: String? = null,
    val episodesCount: Int? = null,
    val progress: Float? = null, // 0.0f to 1.0f
    val rating: Double? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val relations: List<Content>? = null,
    val recommendations: List<Content>? = null,
    val releaseStatus: String? = null,
    val nextAiringEpisode: Int? = null,
    val streamingEpisodes: List<ApiEpisode>? = null
)

val continueWatchingList = listOf(
    Content(
        title = "Batalla Final en Neo-Tokyo",
        imageUrl = "https://images.unsplash.com/photo-1541562232579-512a21360020?q=80&w=2670&auto=format&fit=crop",
        subtitle = "T1:E12 • 24m restantes",
        progress = 0.7f
    ),
    Content(
        title = "El Secreto de...",
        imageUrl = "https://images.unsplash.com/photo-1578632767115-351597cf2477?q=80&w=2670&auto=format&fit=crop",
        subtitle = "T2:E4 • 15m restantes",
        progress = 0.3f
    )
)

val popularList = listOf(
    Content(
        title = "Demon Slayer",
        imageUrl = "https://images.unsplash.com/photo-1618336753974-aae8e04506aa?q=80&w=2670&auto=format&fit=crop",
        subtitle = "Acción, Fantasía",
        label = "#1"
    ),
    Content(
        title = "One Piece",
        imageUrl = "https://images.unsplash.com/photo-1533907650686-70576141c030?q=80&w=2670&auto=format&fit=crop",
        subtitle = "Aventura",
        label = "#2"
    ),
    Content(
        title = "Naruto Shippuden",
        imageUrl = "https://images.unsplash.com/photo-1571757767119-68b8dbed8c97?q=80&w=2670&auto=format&fit=crop",
        subtitle = "Ninja, Shonen",
        label = "#3"
    )
)

val newList = listOf(
    Content(
        title = "Neon Genesis",
        imageUrl = "https://images.unsplash.com/photo-1620215900223-b1d62c2f6d50?q=80&w=2670&auto=format&fit=crop",
        subtitle = "Mecha",
        label = "Nuevo"
    ),
    Content(
        title = "Psycho-Pass",
        imageUrl = "https://images.unsplash.com/photo-1478720568477-152d9b164e63?q=80&w=2698&auto=format&fit=crop",
        subtitle = "Crimen, Sci-Fi"
    ),
    Content(
        title = "Blue Spring",
        imageUrl = "https://images.unsplash.com/photo-1516726817505-f5ed825624d8?q=80&w=2667&auto=format&fit=crop",
        subtitle = "Romance"
    )
)
