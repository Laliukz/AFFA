package com.lkz.affa.data.models

data class StreamLink(
    val url: String,
    val quality: String = "Auto",
    val server: String = "Unknown",
    val headers: Map<String, String>? = null,
    val isDub: Boolean = false
)
