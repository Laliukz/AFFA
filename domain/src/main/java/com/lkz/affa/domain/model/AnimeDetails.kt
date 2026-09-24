package com.lkz.affa.domain.model

data class AnimeDetails(
    val title: String,
    val slug: String,
    val url: String,
    val imageUrl: String,
    val synopsis: String,
    val genres: List<String>,
    val status: String,
    val type: String,
    val episodes: List<Episode>
)
