package com.lkz.affa.core.network.extractor

import com.lkz.affa.domain.model.VideoStream
import org.jsoup.Jsoup

class Mp4UploadExtractor {
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    /**
     * Extrae el enlace directo de video (.mp4) desde una URL embed de Mp4Upload.
     */
    fun extract(embedUrl: String): VideoStream? {
        // ponytail: body crudo, doc.html() re-escapa & -> &amp; y rompe URLs con query
        val html = Jsoup.connect(embedUrl)
            .userAgent(userAgent)
            .header("Referer", "https://www.mp4upload.com/")
            .timeout(15000)
            .ignoreContentType(true)
            .execute()
            .body()

        // Buscar: player.src({ type: "video/mp4", src: "https://...video.mp4" });
        val match = Regex("""player\.src\(\s*\{\s*type:\s*["'][^"']+["'],\s*src:\s*["']([^"']+\.mp4[^"']*)["']""").find(html)
            ?: Regex("""src:\s*["'](https?://[^"']+\.mp4[^"']*)["']""").find(html)

        val videoUrl = match?.groupValues?.get(1)?.trim()?.replace("&amp;", "&") ?: return null

        return VideoStream(
            server = "Mp4Upload",
            videoUrl = videoUrl,
            quality = "HD",
            headers = mapOf(
                "Referer" to "https://www.mp4upload.com/",
                "User-Agent" to userAgent
            )
        )
    }
}
