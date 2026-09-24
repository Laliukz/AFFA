package com.lkz.affa.core.network.extractor

import com.lkz.affa.core.common.util.JsUnpacker

import com.lkz.affa.domain.model.VideoStream
import org.jsoup.Jsoup
import java.net.URI

class MixdropExtractor {
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    /**
     * Extrae el .mp4 desde un embed de Mixdrop (dominios rotativos: mdbekjwqa.pw, etc.).
     * Desempaqueta el JS (Dean Edwards) y lee MDCore.wurl.
     */
    fun extract(embedUrl: String): VideoStream? {
        // ponytail: body crudo, doc.html() re-escapa & -> &amp;
        val html = Jsoup.connect(embedUrl)
            .userAgent(userAgent)
            .header("Referer", "https://jkanime.net/")
            .timeout(15000)
            .ignoreContentType(true)
            .execute()
            .body()

        val unpacked = JsUnpacker.unpack(html) ?: html
        val raw = Regex("""MDCore\.wurl\s*=\s*"([^"]+)"""").find(unpacked)
            ?.groupValues?.get(1)?.trim()?.replace("&amp;", "&")
            ?: return null
        val videoUrl = if (raw.startsWith("//")) "https:$raw" else raw
        if (!videoUrl.startsWith("http")) return null

        val host = try { URI(embedUrl).host ?: "mixdrop.co" } catch (_: Exception) { "mixdrop.co" }
        return VideoStream(
            server = "Mixdrop",
            videoUrl = videoUrl,
            quality = "HD",
            headers = mapOf("Referer" to "https://$host/", "User-Agent" to userAgent)
        )
    }
}
