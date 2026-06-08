package com.lkz.affa.domain.repositories

import com.lkz.affa.data.models.AnimeEpisode
import com.lkz.affa.data.models.Content
import com.lkz.affa.data.models.StreamLink

interface AnimeProviderRepository {
    /**
     * El dominio base del proveedor (ej. "animeav1.com").
     */
    val domain: String

    /**
     * Busca animes que coincidan con la consulta proporcionada.
     */
    suspend fun searchAnime(query: String): Result<String>

    /**
     * Obtiene los episodios de un anime dado su URL o slug.
     */
    suspend fun getEpisodes(animeUrl: String): Result<List<AnimeEpisode>>

    /**
     * Obtiene los enlaces de streaming (servidores y calidades) dado la URL de un episodio.
     */
    suspend fun getStreamLinks(episodeUrl: String): Result<List<StreamLink>>
}
