package com.lkz.affa.domain.model

import java.time.Instant

data class RecentEpisode(
    val animeTitle: String,
    val episodeNumber: Float,
    val url: String,
    val imageUrl: String,
    val source: String,
    val publishedAt: Instant?,
    val timeLabel: String?
)
