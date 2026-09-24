package com.lkz.affa.core.network.extractor

import com.lkz.affa.domain.model.VideoStream
import org.jsoup.Jsoup

class StreamtapeExtractor {
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    /**
     * Extrae el .mp4 desde un embed de Streamtape vía div#robotlink (/get_video?...).
     * URL firmada con expiración: usar de inmediato, no cachear.
     */
    fun extract(embedUrl: String): VideoStream? {
        val doc = Jsoup.connect(embedUrl)
            .userAgent(userAgent)
            .header("Referer", "https://animeav1.com/")
            .timeout(15000)
            .get()

        val raw = doc.selectFirst("#robotlink")?.text()?.trim()?.replace("&amp;", "&")
            ?: return null
        val videoUrl = when {
            raw.startsWith("//") -> "https:$raw"
            raw.startsWith("/") -> "https:/$raw"
            raw.startsWith("http") -> raw
            else -> "https://$raw"
        }
        if (!videoUrl.contains("/get_video")) return null

        return VideoStream(
            server = "Streamtape",
            videoUrl = videoUrl,
            quality = "HD",
            headers = mapOf(
                "Referer" to embedUrl,
                "User-Agent" to userAgent
            )
        )
    }
}
