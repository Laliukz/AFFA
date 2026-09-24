package com.lkz.affa.domain.model

data class AnimeMetadata(
    val anilistId: Int?,
    val malId: Int?,
    val tmdbId: Int?,
    val title: String,
    val titleJp: String?,
    val synopsis: String?,
    val coverImage: String?,
    val bannerImage: String?,
    val status: String?,
    val episodes: Int?,
    val score: Double?,
    val genres: List<String>,
    val source: MetadataSource
)

enum class MetadataSource { ANILIST, JIKAN, TMDB }
