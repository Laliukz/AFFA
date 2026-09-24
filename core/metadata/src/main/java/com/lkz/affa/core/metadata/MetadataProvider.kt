package com.lkz.affa.core.metadata

import com.lkz.affa.domain.model.AnimeMetadata

interface MetadataProvider {
    suspend fun search(title: String): AnimeMetadata?
    suspend fun getById(anilistId: Int? = null, malId: Int? = null): AnimeMetadata?
    suspend fun getTrending(limit: Int = 10): List<AnimeMetadata>
    suspend fun getSeasonal(limit: Int = 10): List<AnimeMetadata>
}
