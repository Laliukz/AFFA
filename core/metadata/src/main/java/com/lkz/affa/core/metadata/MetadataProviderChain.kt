package com.lkz.affa.core.metadata

import com.lkz.affa.core.metadata.api.AniListApi
import com.lkz.affa.core.metadata.api.JikanApi
import com.lkz.affa.core.metadata.api.TmdbApi
import com.lkz.affa.core.metadata.model.AniListMedia
import com.lkz.affa.domain.model.AnimeMetadata
import com.lkz.affa.domain.model.MetadataSource
import java.util.Calendar

class MetadataProviderChain(
    private val anilistApi: AniListApi,
    private val jikanApi: JikanApi,
    private val tmdbApi: TmdbApi? = null,
    private val tmdbApiKey: String? = null,
    private val cache: MetadataCache? = null
) : MetadataProvider {

    override suspend fun search(title: String): AnimeMetadata? {
        val key = cache?.searchKey(title) ?: return searchFromNetwork(title)
        cache.getMemory(key)?.let { return it }
        return searchFromNetwork(title)?.also { cache.putMemory(key, it) }
    }

    private suspend fun searchFromNetwork(title: String): AnimeMetadata? {
        return tryAnilist(title) ?: tryJikan(title) ?: tryTmdb(title)
    }

    override suspend fun getById(anilistId: Int?, malId: Int?): AnimeMetadata? {
        if (anilistId == null) return null
        val key = cache?.idKey(anilistId)
        if (key != null) {
            cache.getMemory(key)?.let { return it }
        }
        return tryAnilistById(anilistId)?.also {
            if (key != null) cache?.putMemory(key, it)
        }
    }

    private suspend fun tryAnilist(title: String): AnimeMetadata? = try {
        val body = AniListApi.buildSearchBody(title)
        val response = anilistApi.searchAnime(body)
        response.data.page.media.firstOrNull()?.toMetadata()?.takeIf { !isHentai(it) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private suspend fun tryJikan(title: String): AnimeMetadata? = try {
        val response = jikanApi.searchAnime(title)
        response.data.firstOrNull()?.let {
            AnimeMetadata(
                anilistId = null,
                malId = it.malId,
                tmdbId = null,
                title = it.title,
                titleJp = it.titleJapanese,
                synopsis = it.synopsis,
                coverImage = it.images?.jpg?.largeImageUrl,
                bannerImage = null,
                status = it.status,
                episodes = it.episodes,
                score = it.score,
                genres = it.genres?.map { g -> g.name } ?: emptyList(),
                source = MetadataSource.JIKAN
            )
        }?.takeIf { !isHentai(it) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private suspend fun tryTmdb(title: String): AnimeMetadata? {
        val api = tmdbApi ?: return null
        val key = tmdbApiKey ?: return null
        return try {
            val response = api.searchTv(title, key)
            response.results.firstOrNull()?.let {
                AnimeMetadata(
                    anilistId = null,
                    malId = null,
                    tmdbId = it.id,
                    title = it.name,
                    titleJp = it.originalName,
                    synopsis = it.overview,
                    coverImage = it.posterPath?.let { p -> "${TmdbApi.IMAGE_BASE}$p" },
                    bannerImage = it.backdropPath?.let { b -> "${TmdbApi.IMAGE_BASE}$b" },
                    status = null,
                    episodes = null,
                    score = it.voteAverage?.div(10.0),
                    genres = emptyList(),
                    source = MetadataSource.TMDB
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun tryAnilistById(id: Int): AnimeMetadata? = try {
        val body = AniListApi.buildByIdBody(id)
        val response = anilistApi.searchAnime(body)
        response.data.page.media.firstOrNull()?.toMetadata()?.takeIf { !isHentai(it) }
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun AniListMedia.toMetadata() = AnimeMetadata(
        anilistId = id,
        malId = idMal,
        tmdbId = null,
        title = title.english ?: title.romaji ?: "",
        titleJp = title.native,
        synopsis = description,
        coverImage = coverImage?.large,
        bannerImage = bannerImage,
        status = status,
        episodes = episodes,
        score = meanScore?.toDouble()?.div(10.0),
        genres = genres ?: emptyList(),
        source = MetadataSource.ANILIST
    )

    override suspend fun getTrending(limit: Int): List<AnimeMetadata> = try {
        val body = AniListApi.buildTrendingBody(perPage = limit)
        val response = anilistApi.searchAnime(body)
        response.data.page.media.mapNotNull { it.toMetadata().takeIf { m -> !isHentai(m) } }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    override suspend fun getSeasonal(limit: Int): List<AnimeMetadata> = try {
        val cal = Calendar.getInstance()
        val season = AniListApi.currentSeason(cal.get(Calendar.MONTH) + 1)
        val year = cal.get(Calendar.YEAR)
        val body = AniListApi.buildSeasonalBody(season, year, 1, limit)
        val response = anilistApi.searchAnime(body)
        response.data.page.media.mapNotNull { it.toMetadata().takeIf { m -> !isHentai(m) } }
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }

    companion object {
        private val HentaiGenres = setOf("hentai")

        fun isHentai(metadata: AnimeMetadata): Boolean =
            metadata.genres.any { it.lowercase() in HentaiGenres }

        fun create(
            anilistApi: AniListApi,
            jikanApi: JikanApi,
            tmdbApiKey: String? = null,
            cache: MetadataCache? = null
        ): MetadataProviderChain {
            val tmdbApi = if (tmdbApiKey != null) MetadataModule.tmdbApi() else null
            return MetadataProviderChain(anilistApi, jikanApi, tmdbApi, tmdbApiKey, cache)
        }
    }
}
