package com.lkz.affa.core.network.extractor

import com.lkz.affa.domain.model.VideoStream
import org.jsoup.Jsoup

class YourUploadExtractor {
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    /**
     * Extrae el enlace directo al archivo de video (.mp4) desde una URL embed de YourUpload.
     */
    fun extract(embedUrl: String): VideoStream? {
        val doc = Jsoup.connect(embedUrl)
            .userAgent(userAgent)
            .header("Referer", "https://tioanime.com/")
            .timeout(15000)
            .get()

        // 1. Intentar desde la etiqueta <meta property="og:video" ...>
        var videoUrl = doc.selectFirst("meta[property='og:video']")?.attr("content")?.trim() ?: ""

        // 2. Si no está en el meta, buscar en scripts JS: file: 'https://...video.mp4'
        if (videoUrl.isEmpty() || !videoUrl.contains(".mp4")) {
            val scripts = doc.select("script")
            for (script in scripts) {
                val data = script.data()
                val match = Regex("""file:\s*['"]([^'"]+\.mp4[^'"]*)['"]""").find(data)
                if (match != null) {
                    videoUrl = match.groupValues[1].trim()
                    break
                }
            }
        }

        if (videoUrl.isEmpty()) return null

        return VideoStream(
            server = "YourUpload",
            videoUrl = videoUrl,
            quality = "HD",
            headers = mapOf(
                "Referer" to "https://www.yourupload.com/",
                "User-Agent" to userAgent
            )
        )
    }
}
