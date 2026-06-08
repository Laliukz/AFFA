package com.lkz.affa.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

/**
 * Obtiene títulos disponibles en las fuentes de streaming (AnimeFLV y AnimeAV1)
 * para filtrar animes que aún no tienen episodios.
 */
class SeasonalAvailabilityService private constructor() {

    companion object {
        val instance = SeasonalAvailabilityService()
        private const val TAG = "SeasonalAvailability"
        private const val FLV_BASE = "https://www3.animeflv.net/"
        private const val AV1_SCHEDULE = "https://animeav1.com/horario"
    }

    suspend fun fetchSeasonalAvailableTitles(): Set<String> = withContext(Dispatchers.IO) {
        try {
            val flvDeferred = async { fetchFlvSeasonalTitles() }
            val av1Deferred = async { fetchAv1SeasonalTitles() }

            val flv = flvDeferred.await()
            val av1 = av1Deferred.await()

            val combined = flv + av1
            val sortedTitles = combined.sorted()

            sortedTitles.chunked(20).forEachIndexed { index, chunk ->
                Log.i(TAG, "fetchSeasonalAvailableTitles (Chunk ${index + 1}): ${chunk.joinToString(", ")}")
            }

            combined
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching seasonal availability", e)
            emptySet()
        }
    }

    private suspend fun fetchFlvSeasonalTitles(): Set<String> {
        val titles = mutableSetOf<String>()
        try {
            val response = HttpService.instance.get(
                FLV_BASE,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                )
            ) ?: return titles

            val sidebarRegex = Regex("""class=["']ListSdbr["'][^>]*>[\s\S]*?</ul>""", RegexOption.IGNORE_CASE)
            val sidebarMatch = sidebarRegex.find(response)

            val section = sidebarMatch?.value ?: response

            val linkRegex = Regex("""<a[^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
            val found = linkRegex.findAll(section)
                .map { it.groupValues[1].replace(Regex("<[^>]*>"), "").trim() }
                .filter { it.isNotEmpty() }
                .map { normalize(it) }
                .filter { it.isNotEmpty() }
                .toSet()

            titles.addAll(found)

            titles.toList().chunked(20).forEachIndexed { index, chunk ->
                Log.i(TAG, "FLV Scrape (Chunk ${index + 1}): ${chunk.joinToString(", ")}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scraping FLV seasonal list", e)
        }
        return titles
    }

    private suspend fun fetchAv1SeasonalTitles(): Set<String> {
        val titles = mutableSetOf<String>()
        try {
            val rawHtml = HttpService.instance.get(
                AV1_SCHEDULE,
                headers = mapOf(
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36",
                    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                )
            ) ?: return titles

            if (rawHtml.contains("Digimon") || rawHtml.contains("Beatbreak")) {
                Log.i(TAG, "DEBUG: Content found in raw HTML — SSR page.")
            } else {
                Log.w(TAG, "DEBUG: Content NOT in raw HTML — likely CSR/JS rendered. Trying embedded JSON...")
            }

            val scriptRegex = Regex("""<script[^>]*>([\s\S]*?)</script>""", RegexOption.IGNORE_CASE)
            val scripts = scriptRegex.findAll(rawHtml)

            for (script in scripts) {
                val content = script.groupValues[1]
                if (!content.contains("title:") && !content.contains("\"title\"")) continue

                Log.i(TAG, "Found potential data script. Trying regex extraction...")

                val titleRegex = Regex("""(?:"title"|title)\s*:\s*"([^"]+)"""", RegexOption.IGNORE_CASE)
                titleRegex.findAll(content).forEach { m ->
                    val rawTitle = m.groupValues[1]
                    if (rawTitle.length > 1 && !rawTitle.lowercase().contains("animeav1")) {
                        titles.add(normalize(rawTitle))
                    }
                }

                val slugRegex = Regex("""(?:"slug"|slug)\s*:\s*"([^"]+)"""")
                slugRegex.findAll(content).forEach { m ->
                    val rawSlug = m.groupValues[1]
                    if (rawSlug.isNotEmpty()) {
                        titles.add(normalize(rawSlug.replace("-", " ")))
                    }
                }
            }

            val cardRegex = Regex("""<div[^>]+class=["'][^"']*\brelative\b[^"']*["'][^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE)
            val cards = cardRegex.findAll(rawHtml).toList()
            Log.i(TAG, "AV1 Selectors: Found ${cards.size} div.relative elements")

            for (card in cards) {
                val cardContent = card.groupValues[1]
                val headerRegex = Regex("""<header[^>]*>([\s\S]*?)</header>""", RegexOption.IGNORE_CASE)
                val header = headerRegex.find(cardContent)?.groupValues?.get(1) ?: continue

                val titleTagRegex = Regex("""<h[3-5][^>]*>([\s\S]*?)</h[3-5]>|<span[^>]*>([\s\S]*?)</span>""", RegexOption.IGNORE_CASE)
                val titleMatch = titleTagRegex.find(header)
                val text = (titleMatch?.groupValues?.drop(1)?.firstOrNull { it.isNotBlank() }
                    ?: header)
                    .replace(Regex("<[^>]*>"), "").trim()

                if (text.isNotEmpty()) titles.add(normalize(text))
            }

            if (titles.size < 5) {
                Log.i(TAG, "AV1: Few results, falling back to link scraping...")
                val animeLinks = Regex("""<a[^>]+href=["']/anime/[^"']+["'][^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
                animeLinks.findAll(rawHtml).forEach { m ->
                    val text = m.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                    if (text.length > 2) titles.add(normalize(text))
                }

                val mediaLinks = Regex("""<a[^>]+href=["'](/media/[^"']+)["'][^>]*>([\s\S]*?)</a>""", RegexOption.IGNORE_CASE)
                mediaLinks.findAll(rawHtml).forEach { m ->
                    var text = m.groupValues[2].replace(Regex("<[^>]*>"), "").trim()
                    if (text.isEmpty()) {
                        val path = m.groupValues[1]
                        val slug = path.substringAfterLast("/").ifEmpty { null }
                        if (slug != null) text = slug.replace("-", " ")
                    }
                    if (text.isNotEmpty()) titles.add(normalize(text))
                }
            }

            titles.filter { it.isNotEmpty() }.toList().chunked(20).forEachIndexed { index, chunk ->
                Log.i(TAG, "AV1 Scrape (Chunk ${index + 1}): ${chunk.joinToString(", ")}")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scraping AV1 schedule", e)
        }
        return titles.filter { it.isNotEmpty() }.toMutableSet()
    }

    fun normalize(value: String): String {
        var cleaned = value.lowercase()

        cleaned = cleaned.replace(Regex("""\(.*?\)"""), " ")

        if (cleaned.contains("[latino]") || cleaned.contains("[castellano]")) {
            cleaned = cleaned.replace(Regex("""\[.*?\]"""), " ")
        } else {
            cleaned = cleaned.replace("[", " ").replace("]", " ")
        }

        if (cleaned.trimEnd().endsWith(" anime")) {
            cleaned = cleaned.trimEnd().dropLast(6)
        }
        if (cleaned.trimEnd().endsWith(" tv")) {
            cleaned = cleaned.trimEnd().dropLast(3)
        }

        cleaned = cleaned.replace(Regex("""[^a-z0-9]+"""), " ")
        cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()

        return if (cleaned == "anime") "" else cleaned
    }

    suspend fun testFetchFlv() = fetchFlvSeasonalTitles()
    suspend fun testFetchAv1() = fetchAv1SeasonalTitles()
}
