package com.lkz.affa.data.repository

import android.content.Context
import com.lkz.affa.data.local.DatabaseHelper
import com.lkz.affa.data.local.HistoryDao
import com.lkz.affa.data.models.Content
import com.lkz.affa.data.remote.AnilistService
import com.lkz.affa.data.remote.HttpService
import com.lkz.affa.data.remote.SeasonalAvailabilityService
import com.lkz.affa.data.remote.TmdbService
import com.lkz.affa.ui.viewmodels.ContentState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

/**
 * Singleton Repository to manage network requests and cache them in memory. This prevents
 * unnecessary API calls and UI flickering when navigating between screens.
 */
object ContentRepository {
    private val anilistService = AnilistService()

    private var cachedState: ContentState? = null
    var currentDetails: Content? = null

    suspend fun getHomeContent(
            context: Context? = null,
            forceRefresh: Boolean = false
    ): ContentState =
            withContext(Dispatchers.IO) {
                if (!forceRefresh && cachedState != null) {
                    return@withContext cachedState!!
                }

                try {
                    val seasonalDeferred = async {
                        SeasonalAvailabilityService.instance.fetchSeasonalAvailableTitles()
                    }
                    val trendingDeferred = async<List<Content>> { 
                        try { anilistService.getTrending() } 
                        catch (e: Exception) { TmdbService.instance.getTrending() }
                    }
                    val popularDeferred = async<List<Content>> { 
                        try { anilistService.getPopular(perPage = 50) } 
                        catch (e: Exception) { TmdbService.instance.getPopular(perPage = 50) }
                    }
                    val newReleasesDeferred = async<List<Content>> { 
                        try { anilistService.getNew(perPage = 50) } 
                        catch (e: Exception) { TmdbService.instance.getNew(perPage = 50) }
                    }
                    val todayEpisodesDeferred = async<List<Content>> {
                        try { anilistService.getTodayEpisodes() }
                        catch (e: Exception) { emptyList() }
                    }

                    val seasonalTitles = seasonalDeferred.await()
                    var trending = trendingDeferred.await()
                    val popular = popularDeferred.await()
                    val rawNewReleases = newReleasesDeferred.await()
                    val rawTodayEpisodes = todayEpisodesDeferred.await()

                    val isSpanish = java.util.Locale.getDefault().language == "es"
                    
                    if (isSpanish && trending.isNotEmpty()) {
                        val translatedTop5 = trending.take(5).map { content ->
                            async {
                                try {
                                    val tmdbId = TmdbService.instance.searchTvShow(content.title, null)
                                    if (tmdbId != null) {
                                        val tmdbDetails = TmdbService.instance.getAnimeDetails(tmdbId)
                                        if (!tmdbDetails.description.isNullOrEmpty()) {
                                            return@async content.copy(description = tmdbDetails.description)
                                        }
                                    }
                                } catch (e: Exception) {}
                                content
                            }
                        }.awaitAll()
                        trending = translatedTop5 + trending.drop(5)
                    }

                    var newReleases = rawNewReleases
                    var todayEpisodes = rawTodayEpisodes

                    if (seasonalTitles.isNotEmpty()) {
                        val filtered =
                                rawNewReleases.filter { anime ->
                                    val normalized =
                                            SeasonalAvailabilityService.instance.normalize(
                                                    anime.title
                                            )
                                    seasonalTitles.any {
                                        it.contains(normalized) || normalized.contains(it)
                                    }
                                }

                        newReleases =
                                if (filtered.isNotEmpty()) {
                                    filtered // Keep all filtered for the "Ver Todo" screen
                                } else {
                                    rawNewReleases
                                }
                    } else {
                        newReleases = rawNewReleases
                    }

                    if (seasonalTitles.isNotEmpty()) {
                        val filteredToday = rawTodayEpisodes.filter { anime ->
                            val normalized = SeasonalAvailabilityService.instance.normalize(anime.title)
                            seasonalTitles.any { it.contains(normalized) || normalized.contains(it) }
                        }
                        todayEpisodes = filteredToday
                    } else {
                        todayEpisodes = emptyList() // If we couldn't load seasonal availability, assume nothing is available today, or we can use rawTodayEpisodes. The safest is to use filtered only.
                    }

                    var continueWatching = emptyList<Content>()
                    if (context != null) {
                        try {
                            continueWatching = getContinueWatchingList(context)
                        } catch (e: Exception) {
                        }
                    }

                    val newState =
                            ContentState(
                                    heroContent = trending.firstOrNull(),
                                    continueWatching = continueWatching,
                                    popular = popular,
                                    recentlyAdded = newReleases,
                                    trends = trending,
                                    exploreGrid = popular.reversed(),
                                    todayEpisodes = todayEpisodes,
                                    isLoaded = true
                            )

                    cachedState = newState
                    return@withContext newState
                } catch (e: Exception) {
                    return@withContext cachedState ?: ContentState(isLoaded = true)
                }
            }

    suspend fun search(
            query: String,
            sort: String? = null,
            genre: String? = null,
            year: Int? = null,
            status: String? = null
    ): List<Content> =
            withContext(Dispatchers.IO) {
                val anilistResultsDeferred = async<List<Content>> {
                    try {
                        anilistService.searchAnime(query, 1, 20, sort, genre, year, status)
                    } catch (e: Exception) {
                        TmdbService.instance.searchAnime(query, 1, 20, sort, genre, year, status)
                    }
                }

                if (query.isBlank()) {
                    return@withContext anilistResultsDeferred.await()
                }

                val flvTitlesDeferred = async {
                    val titles = mutableSetOf<String>()
                    try {
                        val words = query.split(" ").take(4).joinToString(" ")
                        val encodedQuery = java.net.URLEncoder.encode(words, "UTF-8")
                        val url = "https://www3.animeflv.net/browse?q=${'$'}encodedQuery"

                        val response = HttpService.instance.get(url) ?: return@async titles
                        val titleRegex =
                                Regex(
                                        """<h3[^>]*class=["']Title["'][^>]*>([\s\S]*?)</h3>""",
                                        RegexOption.IGNORE_CASE
                                )

                        titleRegex.findAll(response).forEach { match ->
                            val text = match.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                            if (text.isNotEmpty()) {
                                titles.add(SeasonalAvailabilityService.instance.normalize(text))
                            }
                        }
                    } catch (e: Exception) {
                    }
                    titles
                }

                val anilistResults = anilistResultsDeferred.await()
                val flvTitles = flvTitlesDeferred.await()

                if (flvTitles.isEmpty()) {
                    return@withContext anilistResults // Fallback if scrape fails
                }

                anilistResults.filter { anime ->
                    val normAnilistTitle =
                            SeasonalAvailabilityService.instance.normalize(anime.title)
                    val parts = normAnilistTitle.split(" ").filter { it.length > 2 }

                    flvTitles.any { flvTitle ->
                        flvTitle == normAnilistTitle ||
                                (normAnilistTitle.isNotEmpty() &&
                                        flvTitle.contains(normAnilistTitle)) ||
                                (flvTitle.isNotEmpty() && normAnilistTitle.contains(flvTitle)) ||
                                (parts.isNotEmpty() &&
                                        parts.count { flvTitle.contains(it) } >= parts.size / 2 + 1)
                    }
                }
            }

    suspend fun getMoreExplore(page: Int, genre: String? = null): List<Content> =
            withContext(Dispatchers.IO) {
                try {
                    if (genre == null) {
                        try {
                            anilistService.getPopular(page = page, perPage = 10).reversed()
                        } catch (e: Exception) {
                            TmdbService.instance.getPopular(page = page, perPage = 10).reversed()
                        }
                    } else {
                        try {
                            anilistService.searchAnime(
                                query = "",
                                page = page,
                                perPage = 10,
                                genre = genre
                            )
                        } catch (e: Exception) {
                            TmdbService.instance.searchAnime(
                                query = "",
                                page = page,
                                perPage = 10,
                                genre = genre
                            )
                        }
                    }
                } catch (e: Exception) {
                    emptyList()
                }
            }
            
    suspend fun getContinueWatchingList(context: Context): List<Content> = withContext(Dispatchers.IO) {
        val dbHelper = DatabaseHelper.getInstance(context)
        val historyDao = HistoryDao(dbHelper)
        val historyList = historyDao.getHistory()

        val preliminaryList = historyList.map { map ->
            val rawId = map["anime_id"] as? String ?: ""
            val animeId = if (rawId.isNotEmpty() && !rawId.contains(":")) "anilist:$rawId" else rawId

            val title = map["anime_title"] as? String ?: "Unknown"
            val imageUrl = map["anime_image"] as? String ?: ""
            val epNum = map["last_episode_number"] as? String ?: ""
            val pos = map["position_seconds"] as? Int ?: 0
            val dur = map["duration_seconds"] as? Int ?: 0

            val progress = if (dur > 0) {
                (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f)
            } else if (pos == 0 && dur == 0) {
                1f // Marcado como visto manualmente
            } else {
                null
            }

            Content(
                id = animeId,
                title = title,
                imageUrl = imageUrl,
                subtitle = if (epNum.isNotEmpty()) "Ep $epNum" else "",
                progress = progress
            )
        }.distinctBy { it.id }

        val anilistIds = preliminaryList.mapNotNull { it.id?.removePrefix("anilist:")?.toIntOrNull() }
        val tmdbIds = preliminaryList.mapNotNull { it.id?.removePrefix("tmdb:")?.toIntOrNull() }

        val batchAnilist = if (anilistIds.isNotEmpty()) anilistService.getAnimeBatchDetails(anilistIds) else emptyList()
        val batchTmdb = if (tmdbIds.isNotEmpty()) TmdbService.instance.getAnimeBatchDetails(tmdbIds) else emptyList()
        val batchDetails = batchAnilist + batchTmdb

        val continueWatching = preliminaryList.mapNotNull { content ->
            val details = batchDetails.find { it.id == content.id }
            if (details == null) return@mapNotNull content // Si no hay detalles, mostramos tal cual

            val currentEpNum = content.subtitle?.replace("Ep ", "")?.toIntOrNull() ?: 1
            val isFinished = (content.progress ?: 1f) >= 0.9f
            
            val maxAvailableEpisode = if (details.releaseStatus?.uppercase() == "RELEASING" && details.nextAiringEpisode != null) {
                details.nextAiringEpisode!! - 1
            } else {
                details.episodesCount ?: Int.MAX_VALUE // Si es null asumimos que hay más
            }

            if (isFinished) {
                if (currentEpNum < maxAvailableEpisode) {
                    content.copy(
                        subtitle = "Ep ${currentEpNum + 1}",
                        progress = null // Barra vacía para el nuevo episodio
                    )
                } else {
                    null
                }
            } else {
                content
            }
        }.take(10) // Mostramos hasta 10 recientes
        
        continueWatching
    }
}
