package com.lkz.affa.data.repository

import com.lkz.affa.core.metadata.MetadataProvider
import com.lkz.affa.core.network.scraper.AnimeAv1Scraper
import com.lkz.affa.core.network.scraper.JkAnimeScraper
import com.lkz.affa.core.network.scraper.TioAnimeScraper
import com.lkz.affa.domain.model.AnimeMetadata
import com.lkz.affa.domain.model.HomeAnimeItem
import com.lkz.affa.domain.model.RecentEpisode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class HomeRepository(
    private val animeAv1Scraper: AnimeAv1Scraper,
    private val jkAnimeScraper: JkAnimeScraper,
    private val tioAnimeScraper: TioAnimeScraper,
    private val metadataProvider: MetadataProvider
) {
    suspend fun getHomeData(): Result<HomeData> = withContext(Dispatchers.IO) {
        runCatching {
            // Each section fetches independently — failure in one doesn't affect others
            val recentEpisodesDeferred = async { fetchRecentEpisodes() }
            val popularDeferred = async { fetchPopular() }
            val recentlyAddedDeferred = async { fetchRecentlyAdded() }

            val recentEpisodes = recentEpisodesDeferred.await()
            val popularItems = popularDeferred.await()
            val recentlyAddedItems = recentlyAddedDeferred.await()

            val heroPool = (popularItems + recentlyAddedItems)
                .filter { !it.bannerUrl.isNullOrEmpty() || !it.imageUrl.isNullOrEmpty() }
                .distinctBy { it.title.lowercase().trim() }

            HomeData(
                heroItems = heroPool.take(5),
                recentEpisodes = recentEpisodes,
                popularItems = popularItems,
                recentlyAddedItems = recentlyAddedItems
            )
        }
    }

    // ── Section 1: Episodios Recientes ──────────────────────────────
    // Source: scrapers only (today's episodes)
    private suspend fun fetchRecentEpisodes(): List<HomeAnimeItem> = coroutineScope {
        runCatching {
            val userZone = java.time.ZoneId.systemDefault()

            val episodes = listOf(
                async { runCatching { animeAv1Scraper.getLatestEpisodes(userZone) }.getOrDefault(emptyList()) },
                async { runCatching { jkAnimeScraper.getLatestEpisodes() }.getOrDefault(emptyList()) },
                async { runCatching { tioAnimeScraper.getLatestEpisodes() }.getOrDefault(emptyList()) }
            ).awaitAll().flatten()
                .distinctBy { "${it.animeTitle.lowercase().trim()}_${it.episodeNumber}" }
                .take(15)

            episodes.map { ep -> async { enrichRecent(ep) } }.awaitAll()
        }.getOrDefault(emptyList())
    }

    // ── Section 2: Populares ────────────────────────────────────────
    // Source: AniList trending only
    private suspend fun fetchPopular(): List<HomeAnimeItem> = runCatching {
        metadataProvider.getTrending(15)
            .map { it.toHomeAnimeItem() }
            .take(10)
    }.getOrDefault(emptyList())

    // ── Section 3: Recién Añadidos ──────────────────────────────────
    // Source: AniList seasonal, filtered by scraper availability
    private suspend fun fetchRecentlyAdded(): List<HomeAnimeItem> = coroutineScope {
        runCatching {
            val seasonal = metadataProvider.getSeasonal(15)

            seasonal.map { meta ->
                async {
                    val available = isAvailableInScrapers(meta.title)
                    if (available) meta.toHomeAnimeItem() else null
                }
            }.awaitAll().filterNotNull().take(10)
        }.getOrDefault(emptyList())
    }

    private suspend fun isAvailableInScrapers(title: String): Boolean = coroutineScope {
        val results = listOf(
            async { runCatching { animeAv1Scraper.search(title).isNotEmpty() }.getOrDefault(false) },
            async { runCatching { jkAnimeScraper.search(title).isNotEmpty() }.getOrDefault(false) },
            async { runCatching { tioAnimeScraper.search(title).isNotEmpty() }.getOrDefault(false) }
        ).awaitAll()
        results.any { it }
    }

    // ── Helpers ─────────────────────────────────────────────────────

    private fun AnimeMetadata.toHomeAnimeItem(): HomeAnimeItem {
        val calculatedSlug = title.lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .trim()
            .replace(Regex("\\s+"), "-")

        return HomeAnimeItem(
            title = title,
            slug = calculatedSlug,
            imageUrl = coverImage ?: "",
            bannerUrl = bannerImage ?: coverImage,
            synopsis = synopsis,
            score = score,
            genres = genres,
            latestEpisode = null,
            episodeUrl = null
        )
    }

    private suspend fun enrichRecent(ep: RecentEpisode): HomeAnimeItem {
        val slug = ep.url
            .removePrefix("https://animeav1.com")
            .removePrefix("/media")
            .trim('/')
            .substringBefore('/')

        val meta = runCatching {
            withTimeoutOrNull(4000L) {
                metadataProvider.search(ep.animeTitle)
            }
        }.getOrNull()

        return HomeAnimeItem(
            title = meta?.title ?: ep.animeTitle,
            slug = slug,
            imageUrl = meta?.coverImage ?: ep.imageUrl,
            bannerUrl = meta?.bannerImage ?: meta?.coverImage ?: ep.imageUrl,
            synopsis = meta?.synopsis,
            score = meta?.score,
            genres = meta?.genres ?: emptyList(),
            latestEpisode = ep.episodeNumber,
            episodeUrl = ep.url
        )
    }
}

data class HomeData(
    val heroItems: List<HomeAnimeItem>,
    val recentEpisodes: List<HomeAnimeItem>,
    val popularItems: List<HomeAnimeItem>,
    val recentlyAddedItems: List<HomeAnimeItem>
)
