package com.lkz.affa.data.remote

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLEncoder

data class SpanishAnimeInfo(
    val description: String?,
    val genres: List<String> = emptyList()
)

val genreTranslations = mapOf(
    "Action" to "Acción",
    "Adventure" to "Aventuras",
    "Comedy" to "Comedia",
    "Drama" to "Drama",
    "Ecchi" to "Ecchi",
    "Fantasy" to "Fantasía",
    "Horror" to "Terror",
    "Mahou Shoujo" to "Mahou Shoujo",
    "Mecha" to "Mecha",
    "Music" to "Música",
    "Mystery" to "Misterio",
    "Psychological" to "Psicológico",
    "Romance" to "Romance",
    "Sci-Fi" to "Ciencia Ficción",
    "Slice of Life" to "Recuentos de la vida",
    "Sports" to "Deportes",
    "Supernatural" to "Sobrenatural",
    "Thriller" to "Suspenso"
)

fun translateGenre(genre: String, toEnglish: Boolean = false): String {
    return if (toEnglish) {
        val lowerGenre = genre.lowercase()
        genreTranslations.entries.firstOrNull { it.value.lowercase() == lowerGenre }?.key ?: genre
    } else {
        genreTranslations[genre] ?: genre
    }
}

fun translateGenres(genres: List<String>, toEnglish: Boolean = false): List<String> {
    return genres.map { translateGenre(it, toEnglish) }
}

class SpanishContentService private constructor() {

    companion object {
        val instance = SpanishContentService()
        private const val TAG = "SpanishContentService"
        private const val BASE_URL_FLV = "https://www3.animeflv.net"
    }

    /**
     * Searches AnimeFLV for an anime and returns its Spanish description and genres
     */
    suspend fun getSpanishInfo(animeTitle: String): SpanishAnimeInfo? = withContext(Dispatchers.IO) {
        try {
            val slug = searchAnimeFLV(animeTitle) ?: return@withContext null

            val url = "$BASE_URL_FLV/anime/$slug"
            Log.d(TAG, "Fetching $url for \"$animeTitle\"")

            val response = HttpService.instance.get(url)

            if (response.isNullOrEmpty()) {
                Log.w(TAG, "Non-200 or empty response for $url")
                return@withContext null
            }

            var description: String? = null
            val descRegex = Regex("""class=["']Description["'][^>]*>[\s\S]*?<p>(.*?)</p>""", RegexOption.IGNORE_CASE)
            val descMatch = descRegex.find(response)
            
            if (descMatch != null) {
                description = descMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
            } else {
                val fallbackRegex = Regex("""class=["']Description["'][^>]*>(.*?)</div>""", RegexOption.IGNORE_CASE)
                val fallbackMatch = fallbackRegex.find(response)
                if (fallbackMatch != null) {
                    description = fallbackMatch.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                }
            }

            description = description?.replace("&quot;", "\"")
                ?.replace("&amp;", "&")
                ?.replace("&#039;", "'")
                ?.replace("&lt;", "<")
                ?.replace("&gt;", ">")

            val genres = mutableListOf<String>()
            val navRegex = Regex("""class=["']Nvgnrs["'][^>]*>([\s\S]*?)</nav>""", RegexOption.IGNORE_CASE)
            val navMatch = navRegex.find(response)
            
            if (navMatch != null) {
                val navContent = navMatch.groupValues[1]
                val anchorRegex = Regex("""<a[^>]*>(.*?)</a>""", RegexOption.IGNORE_CASE)
                val anchors = anchorRegex.findAll(navContent)
                for (anchor in anchors) {
                    val genreText = anchor.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                    if (genreText.isNotEmpty()) {
                        genres.add(genreText)
                    }
                }
            }

            if (description.isNullOrEmpty() && genres.isEmpty()) return@withContext null

            SpanishAnimeInfo(description = description, genres = genres)
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching Spanish info for $animeTitle", e)
            null
        }
    }

    private suspend fun searchAnimeFLV(query: String): String? {
        try {
            val words = query.split(" ").take(4).joinToString(" ")
            val encodedQuery = URLEncoder.encode(words, "UTF-8")
            val url = "$BASE_URL_FLV/browse?q=$encodedQuery"
            
            val response = HttpService.instance.get(url) ?: return null

            val linkRegex = Regex("""<a[^>]+href=["']([^"']*?/anime/([^"'/]+))/?["'][^>]*>""", RegexOption.IGNORE_CASE)
            val matches = linkRegex.findAll(response)

            val normalizedQuery = query.lowercase()
                .replace(Regex("[^a-z0-9\\s]"), "")
                .replace(Regex("\\s+"), " ")
                .trim()

            var bestMatch: String? = null
            var bestScore = 0.0

            for (match in matches) {
                var slug = match.groupValues[2]
                if (slug.contains("/")) slug = slug.substringBefore("/")
                if (slug.contains("?")) slug = slug.substringBefore("?")
                if (slug.contains("#")) slug = slug.substringBefore("#")

                if (slug.isEmpty()) continue

                val slugWords = slug.replace("-", " ")
                var score = 0.0

                if (slugWords.startsWith(normalizedQuery) || normalizedQuery.startsWith(slugWords)) {
                    score += 50.0
                }

                val queryParts = normalizedQuery.split(" ").filter { it.length > 2 }
                val slugParts = slugWords.split(" ").filter { it.length > 2 }

                for (part in queryParts) {
                    if (slugWords.contains(part)) {
                        score += 20.0
                    }
                }

                if (queryParts.size == slugParts.size) {
                    score += 15.0
                }

                if (score > bestScore) {
                    bestScore = score
                    bestMatch = slug
                }
            }

            return bestMatch
        } catch (e: Exception) {
            Log.w(TAG, "Error searching AnimeFLV for $query", e)
        }
        return null
    }
}
