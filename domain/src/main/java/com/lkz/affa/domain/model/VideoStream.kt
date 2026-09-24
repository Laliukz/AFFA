package com.lkz.affa.domain.model

data class VideoStream(
    val server: String,
    val videoUrl: String,
    val quality: String = "HD",
    val headers: Map<String, String> = emptyMap()
)
