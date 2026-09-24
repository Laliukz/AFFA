package com.lkz.affa.domain.model

data class HomeAnimeItem(
    val title: String,
    val slug: String,
    val imageUrl: String,
    val bannerUrl: String? = null,
    val synopsis: String? = null,
    val score: Double? = null,
    val genres: List<String> = emptyList(),
    val latestEpisode: Float? = null,
    val episodeUrl: String? = null
)
