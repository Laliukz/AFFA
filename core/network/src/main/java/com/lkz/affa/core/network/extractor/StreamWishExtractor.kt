package com.lkz.affa.core.network.extractor

import com.lkz.affa.core.common.util.JsUnpacker

import com.lkz.affa.domain.model.VideoStream
import org.jsoup.Jsoup
import java.net.URI

class StreamWishExtractor {
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    /**
     * Extrae el enlace de video (.m3u8 HLS) desde una URL embed de StreamWish (o sus mirrors sfastwish, etc.).
     */
    fun extract(embedUrl: String): VideoStream? {
        // ponytail: body crudo, doc.html() re-escapa & -> &amp; y rompe el token del CDN (502)
        val html = Jsoup.connect(embedUrl)
            .userAgent(userAgent)
            .header("Referer", "https://jkanime.net/")
            .timeout(15000)
            .ignoreContentType(true)
            .execute()
            .body()

        // 1. Desempaquetar código JS empaquetado con eval(function(p,a,c,k,e,d)...)
        val unpacked = JsUnpacker.unpack(html) ?: html

        // 2. Extraer URL de la playlist HLS (.m3u8)
        val m3u8Match = Regex("""sources:\s*\[\{\s*file:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""").find(unpacked)
            ?: Regex("""file:\s*["'](https?://[^"']+\.m3u8[^"']*)["']""").find(unpacked)
            ?: Regex("""["'](https?://[^"']+\.m3u8[^"']*)["']""").find(unpacked)

        val videoUrl = m3u8Match?.groupValues?.get(1)?.trim()?.replace("&amp;", "&") ?: return null
        val host = try {
            URI(embedUrl).host ?: "sfastwish.com"
        } catch (e: Exception) {
            "sfastwish.com"
        }

        return VideoStream(
            server = "StreamWish",
            videoUrl = videoUrl,
            quality = "Auto (HLS)",
            headers = mapOf(
                "Referer" to "https://$host/",
                "Origin" to "https://$host",
                "User-Agent" to userAgent
            )
        )
    }
}
