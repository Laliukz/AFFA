package com.lkz.affa.core.network.extractor

import com.lkz.affa.domain.model.VideoServer
import com.lkz.affa.domain.model.VideoStream

/**
 * Administrador central de extractores de video.
 * Recibe un [VideoServer] y delega al extractor correspondiente según el nombre o URL.
 */
class VideoExtractorManager(
    private val yourUploadExtractor: YourUploadExtractor = YourUploadExtractor(),
    private val mp4UploadExtractor: Mp4UploadExtractor = Mp4UploadExtractor(),
    private val streamWishExtractor: StreamWishExtractor = StreamWishExtractor(),
    private val mixdropExtractor: MixdropExtractor = MixdropExtractor(),
    private val streamtapeExtractor: StreamtapeExtractor = StreamtapeExtractor()
) {
    /**
     * Indica si existe extractor para el servidor (sin hacer red).
     * Útil para distinguir "sin extractor" de "extractor falló".
     */
    fun supports(server: VideoServer): Boolean {
        val name = server.name.lowercase()
        val url = server.embedUrl.lowercase()

        return name.contains("yourupload") || url.contains("yourupload") ||
            name.contains("mp4upload") || url.contains("mp4upload") ||
            name.contains("streamwish") || url.contains("sfastwish") || url.contains("streamwish") || url.contains("wishembed") ||
            name.contains("vidhide") || url.contains("vidhide") || url.contains("vidmoly") ||
            name.contains("mixdrop") || url.contains("mixdrop") || url.contains("mdbekjwqa") ||
            name.contains("streamtape") || url.contains("streamtape")
    }

    /**
     * Intenta extraer el enlace reproducible directo (.mp4 o .m3u8) para el servidor dado.
     * Retorna null si el servidor aún no tiene extractor implementado o si el enlace no es accesible.
     */
    fun extract(server: VideoServer): VideoStream? {
        if (!supports(server)) return null
        val name = server.name.lowercase()
        val url = server.embedUrl.lowercase()

        return when {
            name.contains("yourupload") || url.contains("yourupload") -> {
                yourUploadExtractor.extract(server.embedUrl)
            }
            name.contains("mp4upload") || url.contains("mp4upload") -> {
                mp4UploadExtractor.extract(server.embedUrl)
            }
            name.contains("streamwish") || url.contains("sfastwish") || url.contains("streamwish") || url.contains("wishembed") ||
                name.contains("vidhide") || url.contains("vidhide") || url.contains("vidmoly") -> {
                streamWishExtractor.extract(server.embedUrl)
            }
            name.contains("mixdrop") || url.contains("mixdrop") || url.contains("mdbekjwqa") -> {
                mixdropExtractor.extract(server.embedUrl)
            }
            name.contains("streamtape") || url.contains("streamtape") -> {
                streamtapeExtractor.extract(server.embedUrl)
            }
            else -> null
        }
    }
}
