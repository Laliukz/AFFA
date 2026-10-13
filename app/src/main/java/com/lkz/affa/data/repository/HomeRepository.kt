package com.lkz.affa.data.repository

import com.lkz.affa.core.metadata.MetadataProvider
import com.lkz.affa.core.network.scraper.AnimeAv1Scraper
import com.lkz.affa.core.network.scraper.JkAnimeScraper
import com.lkz.affa.core.network.scraper.TioAnimeScraper
import com.lkz.affa.domain.model.AnimeMetadata
import com.lkz.affa.domain.model.AnimeSearchResult
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
            // Each section fetches independently â€” failure in one doesn't affect others
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

    // â”€â”€ Section 1: Episodios Recientes â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Solo Av1 + Jk (filtran "hoy" con fecha real). Sin respaldo de Tio:
    // si ambas fallan la secciÃ³n queda vacÃ­a â€” mejor que mostrar dÃ­as viejos.
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

    // â”€â”€ Section 2: Populares â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Source: AniList trending only (1 cheap request)
    private suspend fun fetchPopular(limit: Int = 5): List<HomeAnimeItem> = runCatching {
        metadataProvider.getTrending(limit)
            .map { it.toHomeAnimeItem() }
            .take(limit)
    }.getOrDefault(emptyList())

    // â”€â”€ Section 3: ReciÃ©n AÃ±adidos â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Seasonal ya estrenado (AniList) + tÃ­tulo exacto en scrapers + â‰¥1 episodio confirmado.
    // Home pide need=5 (rÃ¡pido); Ver Todo valida el resto con mÃ¡s margen.
    private suspend fun fetchRecentlyAdded(need: Int = 5, timeoutMs: Long = 25000L): List<HomeAnimeItem> {
        // ponytail: parcial, no todo-o-nada â€” al cortar el timeout se muestra lo validado
        val valid = mutableListOf<HomeAnimeItem>()
        withTimeoutOrNull(timeoutMs) {
            val seasonal = metadataProvider.getSeasonal(40)
            for (meta in seasonal) {
                if (valid.size >= need) break
                if (isConfirmed(meta)) valid += meta.toHomeAnimeItem()
            }
        }
        return valid
    }

    // ponytail: un reintento con pausa â€” la red mÃ³vil falla mÃ¡s que el wifi de pruebas
    private suspend fun latestWithRetry(block: suspend () -> List<RecentEpisode>): List<RecentEpisode> {
        runCatching { return block() }
        kotlinx.coroutines.delay(1500L)
        val second = runCatching { block() }
        second.onFailure { android.util.Log.e("HomeRecents", "scraper failed twice", it) }
        return second.getOrDefault(emptyList())
    }

    // â”€â”€ Details: lista de episodios â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    // Av1 â†’ Jk â†’ Tio con match canÃ³nico; primer scraper con lista no vacÃ­a.
    // ponytail: memoria de sesiÃ³n â€” volver atrÃ¡s o desde segundo plano no re-pide
    private val detailsCache = java.util.Collections.synchronizedMap(
        mutableMapOf<String, com.lkz.affa.domain.model.AnimeDetails>()
    )

    suspend fun getAnimeDetails(
        title: String,
        synonyms: List<String>,
        timeoutMs: Long = 30000L,
        episodeUrl: String? = null
    ): com.lkz.affa.domain.model.AnimeDetails? = withContext(Dispatchers.IO) {
        val key = normalizeTitle(title).joinToString(" ")
        detailsCache[key]?.let { return@withContext it }
        runCatching {
            withTimeoutOrNull(timeoutMs) {
                val aliases = (listOf(title) + synonyms).toSet()
                val queries = (listOf(title) + synonyms).filter { it.isNotBlank() }
                // ponytail: slug conocido del feed â€” directo, sin buscar ni matchear tÃ­tulos
                directSlug(episodeUrl)?.let { (src, slug) ->
                    val d = runCatching {
                        if (src == "av1") animeAv1Scraper.getAnimeDetails(slug)
                        else jkAnimeScraper.getAnimeDetails(slug)
                    }.getOrNull()
                    if (d != null && d.episodes.isNotEmpty()) return@withTimeoutOrNull d
                }
                val av1Hit = searchHit(animeAv1Scraper::search, queries, aliases)
                if (av1Hit != null) {
                    val d = runCatching { animeAv1Scraper.getAnimeDetails(av1Hit.slug) }.getOrNull()
                    if (d != null && d.episodes.isNotEmpty()) return@withTimeoutOrNull d
                }
                val jkHit = searchHit(jkAnimeScraper::search, queries, aliases)
                if (jkHit != null) {
                    val d = runCatching { jkAnimeScraper.getAnimeDetails(jkHit.slug) }.getOrDefault(null)
                    if (d != null && d.episodes.isNotEmpty()) return@withTimeoutOrNull d
                }
                val tioHit = searchHit(tioAnimeScraper::search, queries, aliases)
                if (tioHit != null) {
                    runCatching { tioAnimeScraper.getAnimeDetails(tioHit.slug) }.getOrNull()
                        ?.takeIf { it.episodes.isNotEmpty() }
                } else null
            }
        }.getOrNull()?.also { detailsCache[key] = it }
    }

    suspend fun getAnimeEpisodes(
        title: String,
        synonyms: List<String>,
        timeoutMs: Long = 30000L
    ): List<com.lkz.affa.domain.model.Episode> =
        getAnimeDetails(title, synonyms, timeoutMs)?.episodes.orEmpty()

    // Servidores agregados Tio â†’ Jk â†’ Av1 (mp4 primero por compatibilidad).
    // En paralelo; lo que no responda en el tope queda fuera sin romper.
    suspend fun getEpisodeServers(
        title: String,
        synonyms: List<String>,
        episodeNumber: Float,
        timeoutMs: Long = 20000L,
        episodeUrl: String? = null
    ): List<com.lkz.affa.domain.model.VideoServer> = withContext(Dispatchers.IO) {
        runCatching {
            withTimeoutOrNull(timeoutMs) {
                val aliases = (listOf(title) + synonyms).toSet()
                val queries = (listOf(title) + synonyms).filter { it.isNotBlank() }
                suspend fun fromDirect() = runCatching {
                    val (src, slug) = directSlug(episodeUrl) ?: return@runCatching emptyList()
                    val clean = if (episodeNumber % 1 == 0f) episodeNumber.toInt().toString() else episodeNumber.toString()
                    val servers = if (src == "av1") animeAv1Scraper.getVideoServers("https://animeav1.com/media/$slug/$clean")
                    else jkAnimeScraper.getVideoServers("https://jkanime.net/$slug/$clean/")
                    servers.map { it.copy(name = "${it.name} Â· ${if (src == "av1") "Av1" else "Jk"}") }
                }.getOrDefault(emptyList())
                suspend fun fromTio() = runCatching {
                    val hit = searchHit(tioAnimeScraper::search, queries, aliases)
                        ?: return@runCatching emptyList()
                    val ep = tioAnimeScraper.getAnimeDetails(hit.slug).episodes
                        .firstOrNull { it.number == episodeNumber } ?: return@runCatching emptyList()
                    tioAnimeScraper.getVideoServers(ep.url)
                        .map { it.copy(name = "${it.name} Â· Tio") }
                }.getOrDefault(emptyList())
                suspend fun fromJk() = runCatching {
                    val hit = searchHit(jkAnimeScraper::search, queries, aliases)
                        ?: return@runCatching emptyList()
                    val ep = jkAnimeScraper.getAnimeDetails(hit.slug).episodes
                        .firstOrNull { it.number == episodeNumber } ?: return@runCatching emptyList()
                    jkAnimeScraper.getVideoServers(ep.url)
                        .map { it.copy(name = "${it.name} Â· Jk") }
                }.getOrDefault(emptyList())
                suspend fun fromAv1() = runCatching {
                    val hit = searchHit(animeAv1Scraper::search, queries, aliases)
                        ?: return@runCatching emptyList()
                    val ep = animeAv1Scraper.getAnimeDetails(hit.slug).episodes
                        .firstOrNull { it.number == episodeNumber } ?: return@runCatching emptyList()
                    animeAv1Scraper.getVideoServers(ep.url)
                        .map { it.copy(name = "${it.name} Â· Av1") }
                }.getOrDefault(emptyList())
                coroutineScope {
                    val direct = async { fromDirect() }
                    val tio = async { fromTio() }
                    val jk = async { fromJk() }
                    val av1 = async { fromAv1() }
                    (direct.await() + tio.await() + jk.await() + av1.await())
                        .distinctBy { it.embedUrl }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
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
        val queries = (listOf(meta.title) + meta.synonyms).filter { it.isNotBlank() }
        // Jk cubre todo su catÃ¡logo en 1 search: match exacto alcanza.
        // ponytail: sin details en Jk (su ajax de episodios pagina hasta 50 veces)
        if (searchHit(jkAnimeScraper::search, queries, aliases) != null) return true
        // Av1/Tio: match exacto + details confirma episodios (1 req c/u).
        val av1Hit = searchHit(animeAv1Scraper::search, queries, aliases)
        if (av1Hit != null && runCatching { animeAv1Scraper.getAnimeDetails(av1Hit.slug).episodes.isNotEmpty() }.getOrDefault(false)) return true
        val tioHit = searchHit(tioAnimeScraper::search, queries, aliases)
        if (tioHit != null && runCatching { tioAnimeScraper.getAnimeDetails(tioHit.slug).episodes.isNotEmpty() }.getOrDefault(false)) return true
        return false
    }

    // ponytail: buscar agrega fuentes sin matchear â€” el match exacto vive en detalles
    suspend fun searchExplore(query: String): List<HomeAnimeItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        runCatching {
            withTimeoutOrNull(15000L) {
                coroutineScope {
                    val av1 = async { runCatching { animeAv1Scraper.search(query) }.getOrDefault(emptyList()) }
                    val jk = async { runCatching { jkAnimeScraper.search(query) }.getOrDefault(emptyList()) }
                    val tio = async { runCatching { tioAnimeScraper.search(query) }.getOrDefault(emptyList()) }
                    (av1.await() + jk.await() + tio.await())
                        .filter { it.title.isNotBlank() }
                        .distinctBy { normalizeTitle(it.title).joinToString(" ") }
                        .take(20)
                        .map {
                            HomeAnimeItem(
                                title = it.title,
                                slug = it.slug,
                                imageUrl = it.imageUrl,
                                synonyms = listOf(it.title),
                                episodeUrl = it.url.takeIf { u -> u.isNotBlank() }
                            )
                        }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    suspend fun getTrendingItems(limit: Int = 15): List<HomeAnimeItem> = withContext(Dispatchers.IO) {
        runCatching {
            withTimeoutOrNull(15000L) {
                metadataProvider.getTrending(limit).map { it.toHomeAnimeItem() }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    suspend fun getBrowseItems(
        genre: String?,
        year: Int?,
        status: String?,
        sort: String,
        page: Int = 1,
        limit: Int = 20
    ): List<HomeAnimeItem> = withContext(Dispatchers.IO) {
        runCatching {
            withTimeoutOrNull(30000L) {
                metadataProvider.browse(genre, year, status, sort, page, limit).map { it.toHomeAnimeItem() }
            }.orEmpty()
        }.onFailure {
            android.util.Log.w("ExploreBrowse", "browse fallo: ${it.message}")
        }.getOrDefault(emptyList())
    }

    // â”€â”€ Helpers â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€

    // ponytail: las pÃ¡ginas acortan tÃ­tulos largos â€” se pregunta con cada variante
    // (tÃ­tulo + sinÃ³nimos); el match sigue siendo exacto, solo hay mÃ¡s intentos
    private fun searchHit(
        search: (String) -> List<AnimeSearchResult>,
        queries: List<String>,
        aliases: Set<String>
    ): AnimeSearchResult? {
        for (q in queries.distinct().take(4)) {
            if (q.isBlank()) continue
            val hit = runCatching { search(q).firstOrNull { titleMatches(it.title, aliases) } }.getOrNull()
            if (hit != null) return hit
        }
        return null
    }

    // ponytail: el feed de recientes ya trae el slug â€” directo sin buscar ni matchear
    private fun directSlug(episodeUrl: String?): Pair<String, String>? {
        if (episodeUrl.isNullOrBlank()) return null
        Regex("""animeav1\.com/media/([^/?#]+)""").find(episodeUrl)?.let { return "av1" to it.groupValues[1] }
        Regex("""jkanime\.net/([^/?#]+)""").find(episodeUrl)?.let { return "jk" to it.groupValues[1] }
        return null
    }

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
            episodeUrl = null,
            synonyms = synonyms
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

// Match de tÃ­tulos por tokens canÃ³nicos (internal para test local).
// Igualdad difusa fue el bug ("Golden" âŠ‚ "Golden Kamuy"); igualdad exacta
// tambiÃ©n falla ("III" vs "3rd", orden distinto). Multiset canÃ³nico: ni mÃ¡s ni menos.
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
