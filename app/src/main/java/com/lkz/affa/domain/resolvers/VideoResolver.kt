package com.lkz.affa.domain.resolvers

interface VideoResolver {
    /**
     * Intenta resolver una URL de un reproductor (ej. Streamtape, Mp4Upload)
     * y devolver el enlace crudo del video (.mp4 o .m3u8) listo para reproducir.
     */
    suspend fun resolve(url: String): Result<String>
}
