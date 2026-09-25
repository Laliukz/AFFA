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

    suspend fun getSeeAll(section: SeeAllSection): List<HomeAnimeItem> = withContext(Dispatchers.IO) {
        when (section) {
            SeeAllSection.RECENT -> fetchRecentEpisodes(Int.MAX_VALUE)
            SeeAllSection.POPULAR -> fetchPopular(20)
            SeeAllSection.ADDED -> fetchRecentlyAdded(Int.MAX_VALUE, 60000L)
        }
    }

    // ── Section 1: Episodios Recientes ──────────────────────────────
    // Solo Av1 + Jk (filtran "hoy" con fecha real). Sin respaldo de Tio:
    // si ambas fallan la sección queda vacía — mejor que mostrar días viejos.
    private suspend fun fetchRecentEpisodes(need: Int = 5): List<HomeAnimeItem> = coroutineScope {
        runCatching {
            val userZone = java.time.ZoneId.systemDefault()

            val av1 = async { latestWithRetry { animeAv1Scraper.getLatestEpisodes(userZone) } }
            val jk = async { latestWithRetry { jkAnimeScraper.getLatestEpisodes() } }

            (av1.await() + jk.await())
                .distinctBy { "${it.animeTitle.lowercase().trim()}_${it.episodeNumber}" }
                .take(need).map { ep -> async { enrichRecent(ep) } }.awaitAll()
        }.getOrDefault(emptyList())
    }

    // ── Section 2: Populares ────────────────────────────────────────
    // Source: AniList trending only (1 cheap request)
    private suspend fun fetchPopular(limit: Int = 5): List<HomeAnimeItem> = runCatching {
        metadataProvider.getTrending(limit)
            .map { it.toHomeAnimeItem() }
            .take(limit)
    }.getOrDefault(emptyList())

    // ── Section 3: Recién Añadidos ──────────────────────────────────
    // Seasonal ya estrenado (AniList) + título exacto en scrapers + ≥1 episodio confirmado.
    // Home pide need=5 (rápido); Ver Todo valida el resto con más margen.
    private suspend fun fetchRecentlyAdded(need: Int = 5, timeoutMs: Long = 25000L): List<HomeAnimeItem> {
        return runCatching {
            withTimeoutOrNull(timeoutMs) {
                val seasonal = metadataProvider.getSeasonal(40)
                val valid = mutableListOf<HomeAnimeItem>()
                for (meta in seasonal) {
                    if (valid.size >= need) break
                    if (isConfirmed(meta)) valid += meta.toHomeAnimeItem()
                }
                valid
            }
        }.getOrNull().orEmpty()
    }

    // ponytail: un reintento con pausa — la red móvil falla más que el wifi de pruebas
    private suspend fun latestWithRetry(block: suspend () -> List<RecentEpisode>): List<RecentEpisode> {
        runCatching { return block() }
        kotlinx.coroutines.delay(1500L)
        val second = runCatching { block() }
        second.onFailure { android.util.Log.e("HomeRecents", "scraper failed twice", it) }
        return second.getOrDefault(emptyList())
    }
    private val availabilityCache = java.util.Collections.synchronizedMap(
        mutableMapOf<String, Pair<Boolean, Long>>()
    )

    private suspend fun isConfirmed(meta: AnimeMetadata): Boolean {
        val key = normalizeTitle(meta.title).joinToString(" ")
        availabilityCache[key]?.let { (v, ts) ->
            if (System.currentTimeMillis() - ts < 24 * 60 * 60 * 1000L) return v
        }
        return confirmInScrapers(meta).also { availabilityCache[key] = it to System.currentTimeMillis() }
    }

    private suspend fun confirmInScrapers(meta: AnimeMetadata): Boolean {
        val aliases = (listOf(meta.title) + meta.synonyms).toSet()
        // Jk cubre todo su catálogo en 1 search: match exacto alcanza.
        // ponytail: sin details en Jk (su ajax de episodios pagina hasta 50 veces)
        if (runCatching { jkAnimeScraper.search(meta.title).any { titleMatches(it.title, aliases) } }.getOrDefault(false)) return true
        // Av1/Tio: match exacto + details confirma episodios (1 req c/u).
        val av1Hit = runCatching { animeAv1Scraper.search(meta.title).firstOrNull { titleMatches(it.title, aliases) } }.getOrNull()
        if (av1Hit != null && runCatching { animeAv1Scraper.getAnimeDetails(av1Hit.slug).episodes.isNotEmpty() }.getOrDefault(false)) return true
        val tioHit = runCatching { tioAnimeScraper.search(meta.title).firstOrNull { titleMatches(it.title, aliases) } }.getOrNull()
        if (tioHit != null && runCatching { tioAnimeScraper.getAnimeDetails(tioHit.slug).episodes.isNotEmpty() }.getOrDefault(false)) return true
        return false
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

enum class SeeAllSection { RECENT, POPULAR, ADDED }

// Match de títulos por tokens canónicos (internal para test local).
// Igualdad difusa fue el bug ("Golden" ⊂ "Golden Kamuy"); igualdad exacta
// también falla ("III" vs "3rd", orden distinto). Multiset canónico: ni más ni menos.
private val DROP_TOKENS = setOf("season", "part", "cour", "cours")
private val ROMAN = mapOf(
    "i" to "1", "ii" to "2", "iii" to "3", "iv" to "4", "v" to "5",
    "vi" to "6", "vii" to "7", "viii" to "8", "ix" to "9", "x" to "10"
)

internal fun normalizeTitle(s: String): List<String> {
    val deburred = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return deburred.lowercase()
        .replace(Regex("[^a-z0-9 ]"), " ")
        .split(Regex("\\s+")).filter { it.isNotEmpty() }
        .map { ROMAN[it] ?: it.replace(Regex("^(\\d+)(st|nd|rd|th)$"), "$1") }
        .filter { it !in DROP_TOKENS }
}

internal fun titleMatches(scraperTitle: String, aliases: Collection<String>): Boolean {
    val n = normalizeTitle(scraperTitle).sorted()
    if (n.isEmpty()) return false
    return aliases.any { normalizeTitle(it).sorted() == n }
}
