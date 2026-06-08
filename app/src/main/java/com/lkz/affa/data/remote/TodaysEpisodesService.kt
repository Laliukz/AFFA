package com.lkz.affa.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Servicio para obtener títulos de animes que publicaron episodios "hoy" o "ayer".
 * Scrapea FLV (episodios recientes) y AV1 (horario del día) para determinar
 * qué animes tienen nuevos episodios disponibles.
 */
class TodaysEpisodesService private constructor() {

    companion object {
        val instance = TodaysEpisodesService()
        private const val TAG = "TodaysEpisodes"
        private const val FLV_BASE = "https://www3.animeflv.net/"
        private const val AV1_BASE = "https://animeav1.com/"
        private const val AV1_SCHEDULE = "https://animeav1.com/horario"

        private val HEADERS = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
        )
    }


    /** Obtiene set de títulos normalizados de animes con episodios de hoy. */
    suspend fun fetchTodaysEpisodeTitles(): Set<String> = withContext(Dispatchers.IO) {
        try {
            coroutineScope {
                val flvDeferred = async { fetchFlvEpisodesByMarker("HOY") }
                val av1Deferred = async { fetchAv1TodaysEpisodes() }

                val flv = flvDeferred.await()
                val av1 = av1Deferred.await()
                val combined = flv + av1

                Log.i(TAG, "TodaysEpisodes: ${combined.size} títulos únicos (FLV: ${flv.size}, AV1: ${av1.size})")
                combined
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching today's episodes", e)
            emptySet()
        }
    }

    /** Obtiene set de títulos normalizados de animes con episodios de ayer. */
    suspend fun fetchYesterdayEpisodeTitles(): Set<String> = withContext(Dispatchers.IO) {
        try {
            fetchFlvEpisodesByMarker("AYER")
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching yesterday's episodes", e)
            emptySet()
        }
    }


    /**
     * Scrapea FLV para obtener episodios de la sección "HOY" o "AYER".
     * FLV estructura la lista como:
     *   <span>HOY</span>
     *   <ul class="ListEpisodios">...</ul>
     *   <span>AYER</span>
     *   <ul class="ListEpisodios">...</ul>
     */
    private suspend fun fetchFlvEpisodesByMarker(marker: String): Set<String> {
        val titles = mutableSetOf<String>()
        try {
            val response = HttpService.instance.get(FLV_BASE, headers = HEADERS) ?: return titles

            val markerPattern = Regex(
                """(?i)>\s*$marker\s*<[\s\S]*?(<ul[^>]*class=["'][^"']*ListEpisodios[^"']*["'][^>]*>[\s\S]*?</ul>)"""
            )
            val markerMatch = markerPattern.find(response)

            if (markerMatch != null) {
                val listBlock = markerMatch.groupValues[1]
                extractFlvAnimeNames(listBlock).forEach { name ->
                    val norm = normalize(name)
                    if (norm.isNotEmpty()) titles.add(norm)
                }
            } else {
                Log.d(TAG, "FLV: marker '$marker' not found, trying fallback")
                val listRegex = Regex("""<ul[^>]*class=["'][^"']*ListEpisodios[^"']*["'][^>]*>([\s\S]*?)</ul>""", RegexOption.IGNORE_CASE)
                val firstBlock = listRegex.find(response)
                if (firstBlock != null && marker == "HOY") {
                    extractFlvAnimeNames(firstBlock.value).forEach { name ->
                        val norm = normalize(name)
                        if (norm.isNotEmpty()) titles.add(norm)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scraping FLV $marker episodes", e)
        }
        return titles
    }

    /**
     * Extracts anime names from a FLV <ul class="ListEpisodios"> block.
     * Each <li><a> contains a <span class="Capi">Episodio X</span> and the title.
     */
    private fun extractFlvAnimeNames(listBlock: String): List<String> {
        val names = mutableListOf<String>()

        val liRegex = Regex("""<li[^>]*>([\s\S]*?)</li>""", RegexOption.IGNORE_CASE)
        liRegex.findAll(listBlock).forEach { li ->
            val liContent = li.groupValues[1]

            val capiRegex = Regex("""<span[^>]*class=["'][^"']*Capi[^"']*["'][^>]*>[\s\S]*?</span>""", RegexOption.IGNORE_CASE)
            val withoutCapi = capiRegex.replace(liContent, "")

            val rawText = withoutCapi.replace(Regex("<[^>]*>"), "").trim()
                .replace(Regex("""Episodio\s+\d+""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""\s+"""), " ")
                .trim()

            if (rawText.length > 2) names.add(rawText)
        }
        return names
    }


    /** AV1: intersección entre horario de hoy y últimos episodios publicados. */
    private suspend fun fetchAv1TodaysEpisodes(): Set<String> = coroutineScope {
        try {
            val scheduleDeferred = async { fetchAv1TodaysSchedule() }
            val latestDeferred = async { fetchAv1LatestReleases() }

            val schedule = scheduleDeferred.await()
            val latest = latestDeferred.await()
            val intersection = schedule.intersect(latest)

            Log.d(TAG, "AV1 intersection: Schedule(${schedule.size}) ∩ Latest(${latest.size}) = ${intersection.size}")
            intersection
        } catch (e: Exception) {
            Log.w(TAG, "Error in AV1 today's episodes intersection", e)
            emptySet()
        }
    }

    /** Obtiene animes programados para hoy del horario de AV1. */
    private suspend fun fetchAv1TodaysSchedule(): Set<String> {
        val titles = mutableSetOf<String>()
        try {
            val response = HttpService.instance.get(AV1_SCHEDULE, headers = HEADERS) ?: return titles

            val gridRegex = Regex(
                """class=["'][^"']*grid-cols-2[^"']*gap-4[^"']*["'][^>]*>([\s\S]*?)</div>\s*</div>""",
                RegexOption.IGNORE_CASE
            )
            val grids = gridRegex.findAll(response).toList()
            Log.i(TAG, "AV1 schedule: ${grids.size} grids found")

            for (grid in grids) {
                val cardRegex = Regex("""<div[^>]*class=["'][^"']*\brelative\b[^"']*["'][^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE)
                cardRegex.findAll(grid.groupValues[1]).forEach { card ->
                    val headerRegex = Regex("""<header[^>]*>([\s\S]*?)</header>""", RegexOption.IGNORE_CASE)
                    val header = headerRegex.find(card.groupValues[1])?.groupValues?.get(1) ?: return@forEach
                    val text = header.replace(Regex("<[^>]*>"), "").trim()
                    if (text.isNotEmpty()) titles.add(normalize(text))
                }
            }

            if (titles.isEmpty()) {
                val tabsRegex = Regex("""<div[^>]*class=["'][^"']*\btabs\b[^"']*["'][^>]*>([\s\S]*?)(?=<div[^>]*class=["'][^"']*\btabs\b|$)""", RegexOption.IGNORE_CASE)
                val tabsContent = tabsRegex.find(response)?.groupValues?.get(1) ?: ""
                val headerRegex = Regex("""<header[^>]*>([\s\S]*?)</header>""", RegexOption.IGNORE_CASE)
                Log.i(TAG, "AV1 schedule fallback: scanning headers in tabs")
                headerRegex.findAll(tabsContent).forEach { h ->
                    val text = h.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                    if (text.isNotEmpty()) titles.add(normalize(text))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching AV1 schedule", e)
        }
        return titles
    }

    /** Obtiene los últimos episodios publicados en la home de AV1. */
    private suspend fun fetchAv1LatestReleases(): Set<String> {
        val titles = mutableSetOf<String>()
        try {
            val response = HttpService.instance.get(AV1_BASE) ?: return titles

            val cardRegex = Regex(
                """<(?:div|a)[^>]*class=["'][^"']*(?:anime-card|episode-card|latest)[^"']*["'][^>]*>([\s\S]*?)</(?:div|a)>""",
                RegexOption.IGNORE_CASE
            )
            cardRegex.findAll(response).forEach { card ->
                val text = card.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                if (text.lowercase().contains("episodio") || text.lowercase().contains("episode")) {
                    val title = text.replace(Regex("""Episodio\s+\d+.*""", RegexOption.IGNORE_CASE), "").trim()
                    if (title.length > 2) titles.add(normalize(title))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching AV1 latest releases", e)
        }
        return titles
    }


    private fun normalize(value: String): String {
        var cleaned = value.lowercase()
        cleaned = cleaned.replace(Regex("""\(.*?\)|\[.*?\]"""), " ")
        cleaned = cleaned.replace(Regex("""[^a-z0-9]+"""), " ")
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
        return cleaned
    }
}
