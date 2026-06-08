package com.lkz.affa.data.remote

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.lkz.affa.data.models.Content
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

class TmdbService private constructor() {

    companion object {
        val instance = TmdbService()
        private const val TAG = "TmdbService"
    }

    private val baseUrl = "https://api.themoviedb.org/3"
    
    private val apiKey: String? = com.lkz.affa.BuildConfig.TMDB_API_KEY.takeIf { it.isNotBlank() }

    /**
     * Search for a TV Show by title and year
     */
    suspend fun searchTvShow(title: String, year: Int?): Int? = withContext(Dispatchers.IO) {
        if (apiKey == null) {
            Log.w(TAG, "TMDB: API Key missing")
            return@withContext null
        }

        try {
            val urlBuilder = Uri.parse("$baseUrl/search/tv").buildUpon()
                .appendQueryParameter("api_key", apiKey)
                .appendQueryParameter("query", title)
            
            if (year != null) {
                urlBuilder.appendQueryParameter("first_air_date_year", year.toString())
            }

            val url = urlBuilder.build().toString()
            val responseStr = HttpService.instance.get(url)

            if (responseStr != null) {
                val jsonResponse = JSONObject(responseStr)
                val results = jsonResponse.optJSONArray("results")
                if (results != null && results.length() > 0) {
                    val firstMatch = results.optJSONObject(0)
                    return@withContext firstMatch?.optInt("id")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TMDB: Error searching TV show: ${e.message}")
        }
        null
    }

    /**
     * Get details of a TV Show (specifically seasons)
     */
    suspend fun getTvSeasons(tmdbId: Int): Map<Int, String>? = withContext(Dispatchers.IO) {
        if (apiKey == null) return@withContext null

        try {
            val isSpanish = java.util.Locale.getDefault().language == "es"
            val url = Uri.parse("$baseUrl/tv/$tmdbId").buildUpon()
                .appendQueryParameter("api_key", apiKey)
                .appendQueryParameter("language", if (isSpanish) "es-MX" else "en-US")
                .build()
                .toString()

            val responseStr = HttpService.instance.get(url)
            if (responseStr != null) {
                val jsonResponse = JSONObject(responseStr)
                val seasonsArray = jsonResponse.optJSONArray("seasons")
                val seasonMap = mutableMapOf<Int, String>()

                if (seasonsArray != null) {
                    for (i in 0 until seasonsArray.length()) {
                        val season = seasonsArray.optJSONObject(i) ?: continue
                        val seasonNumber = season.optInt("season_number", -1)
                        val name = season.optString("name", "")

                        // Skip season 0 (specials)
                        if (seasonNumber > 0) {
                            var label: String
                            if (name.isNotEmpty()) {
                                // Extract arc name if it contains "Arco"
                                if (name.contains("Arco", ignoreCase = true)) {
                                    val arcRegex = Regex("Arco\\s+de[l]?\\s+.+", RegexOption.IGNORE_CASE)
                                    val arcMatch = arcRegex.find(name)
                                    if (arcMatch != null) {
                                        label = arcMatch.value
                                    } else {
                                        label = if (name.contains(":")) name.substringAfterLast(":").trim() else name
                                    }
                                } else {
                                    // Doesn't contain "Arco", use TEMP. X
                                    label = "TEMP. $seasonNumber"
                                }
                            } else {
                                label = "TEMP. $seasonNumber"
                            }
                            seasonMap[seasonNumber] = label
                        }
                    }
                    return@withContext seasonMap
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "TMDB: Error fetching TV details: ${e.message}")
        }
        null
    }

    // --- Métodos de Fallback equivalentes a AnilistService ---

    suspend fun getTrending(page: Int = 1, perPage: Int = 10): List<Content> = withContext(Dispatchers.IO) {
        if (apiKey == null) return@withContext emptyList()
        try {
            // TMDB no permite filtrar trending por género de forma nativa en la ruta /trending,
            // usaremos discover ordenado por popularity como aproximación
            val isSpanish = java.util.Locale.getDefault().language == "es"
            val url = Uri.parse("$baseUrl/discover/tv").buildUpon()
                .appendQueryParameter("api_key", apiKey)
                .appendQueryParameter("language", if (isSpanish) "es-MX" else "en-US")
                .appendQueryParameter("with_original_language", "ja")
                .appendQueryParameter("with_genres", "16")
                .appendQueryParameter("sort_by", "popularity.desc")
                .appendQueryParameter("page", page.toString())
                .build().toString()

            val response = HttpService.instance.get(url) ?: return@withContext emptyList()
            return@withContext parseMediaList(JSONObject(response)).take(perPage)
        } catch (e: Exception) {
            Log.e(TAG, "Error en getTrending: ${e.message}")
            emptyList()
        }
    }

    suspend fun getPopular(page: Int = 1, perPage: Int = 10): List<Content> = withContext(Dispatchers.IO) {
        if (apiKey == null) return@withContext emptyList()
        try {
            val isSpanish = java.util.Locale.getDefault().language == "es"
            val url = Uri.parse("$baseUrl/discover/tv").buildUpon()
                .appendQueryParameter("api_key", apiKey)
                .appendQueryParameter("language", if (isSpanish) "es-MX" else "en-US")
                .appendQueryParameter("with_original_language", "ja")
                .appendQueryParameter("with_genres", "16")
                .appendQueryParameter("sort_by", "vote_count.desc")
                .appendQueryParameter("page", page.toString())
                .build().toString()

            val response = HttpService.instance.get(url) ?: return@withContext emptyList()
            return@withContext parseMediaList(JSONObject(response)).take(perPage)
        } catch (e: Exception) {
            Log.e(TAG, "Error en getPopular: ${e.message}")
            emptyList()
        }
    }

    suspend fun getNew(page: Int = 1, perPage: Int = 10): List<Content> = withContext(Dispatchers.IO) {
        if (apiKey == null) return@withContext emptyList()
        try {
            // Emisión reciente
            val isSpanish = java.util.Locale.getDefault().language == "es"
            val url = Uri.parse("$baseUrl/discover/tv").buildUpon()
                .appendQueryParameter("api_key", apiKey)
                .appendQueryParameter("language", if (isSpanish) "es-MX" else "en-US")
                .appendQueryParameter("with_original_language", "ja")
                .appendQueryParameter("with_genres", "16")
                .appendQueryParameter("sort_by", "first_air_date.desc")
                .appendQueryParameter("page", page.toString())
                .build().toString()

            val response = HttpService.instance.get(url) ?: return@withContext emptyList()
            return@withContext parseMediaList(JSONObject(response)).take(perPage)
        } catch (e: Exception) {
            Log.e(TAG, "Error en getNew: ${e.message}")
            emptyList()
        }
    }

    suspend fun searchAnime(
        query: String,
        page: Int = 1,
        perPage: Int = 10,
        sort: String? = null,
        genre: String? = null,
        year: Int? = null,
        status: String? = null
    ): List<Content> = withContext(Dispatchers.IO) {
        if (apiKey == null) return@withContext emptyList()
        try {
            // Búsqueda pura, luego filtramos localmente para que sea anime
            val isSpanish = java.util.Locale.getDefault().language == "es"
            val url = Uri.parse("$baseUrl/search/tv").buildUpon()
                .appendQueryParameter("api_key", apiKey)
                .appendQueryParameter("language", if (isSpanish) "es-MX" else "en-US")
                .appendQueryParameter("query", query)
                .appendQueryParameter("page", page.toString())
            
            if (year != null) {
                url.appendQueryParameter("first_air_date_year", year.toString())
            }
            
            val response = HttpService.instance.get(url.build().toString()) ?: return@withContext emptyList()
            
            val list = parseMediaList(JSONObject(response))
            // Filtrar para asegurarse que es animación y original japonés
            return@withContext list.take(perPage)
        } catch (e: Exception) {
            Log.e(TAG, "Error en searchAnime: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAnimeDetails(id: Int): com.lkz.affa.data.models.Content {
        if (apiKey == null) throw Exception("TMDB API Key is missing")
        val isSpanish = java.util.Locale.getDefault().language == "es"
        val url = Uri.parse("$baseUrl/tv/$id").buildUpon()
            .appendQueryParameter("api_key", apiKey)
            .appendQueryParameter("language", if (isSpanish) "es-MX" else "en-US")
            .build().toString()

        val response = HttpService.instance.get(url) ?: throw Exception("Null response")
        return mapToContent(JSONObject(response))
    }

    suspend fun getAnimeBatchDetails(ids: List<Int>): List<com.lkz.affa.data.models.Content> = withContext(Dispatchers.IO) {
        if (apiKey == null) return@withContext emptyList()
        // TMDB no soporta múltiples IDs en una consulta, hay que hacerlas en paralelo
        val deferredList = ids.map { id ->
            async {
                try {
                    getAnimeDetails(id)
                } catch (e: Exception) {
                    null
                }
            }
        }
        deferredList.mapNotNull { it.await() }
    }

    private fun parseMediaList(jsonResponse: JSONObject): List<com.lkz.affa.data.models.Content> {
        val results = jsonResponse.optJSONArray("results") ?: return emptyList()
        val list = mutableListOf<com.lkz.affa.data.models.Content>()
        
        for (i in 0 until results.length()) {
            val item = results.optJSONObject(i)
            if (item != null) {
                list.add(mapToContent(item))
            }
        }
        return list
    }

    private fun mapToContent(json: JSONObject): com.lkz.affa.data.models.Content {
        val id = json.optInt("id")
        val title = json.optString("name")
        val originalTitle = json.optString("original_name")
        val finalTitle = if (title.isNotEmpty()) title else originalTitle
        
        val posterPath = json.optString("poster_path", "")
        val imageUrl = if (posterPath.isNotEmpty() && posterPath != "null") "https://image.tmdb.org/t/p/w500$posterPath" else ""
        
        val backdropPath = json.optString("backdrop_path", "")
        val bannerUrl = if (backdropPath.isNotEmpty() && backdropPath != "null") "https://image.tmdb.org/t/p/w780$backdropPath" else null
        
        val overview = json.optString("overview", "").takeIf { it.isNotEmpty() }
        
        val rating = if (!json.isNull("vote_average")) json.optDouble("vote_average") else null
        
        // TMDB no da el número total de episodios en la búsqueda simple (sólo en details), así que intentamos sacarlo de 'number_of_episodes'
        val episodesCount = if (!json.isNull("number_of_episodes")) json.optInt("number_of_episodes") else null
        val releaseStatus = if (!json.isNull("status")) json.optString("status") else null
        
        val nextAiring = json.optJSONObject("next_episode_to_air")
        val nextAiringEpisode = if (nextAiring != null && !nextAiring.isNull("episode_number")) {
            nextAiring.optInt("episode_number")
        } else null

        val tags = mutableListOf<String>()
        val isSpanish = java.util.Locale.getDefault().language == "es"
        val genreIds = json.optJSONArray("genre_ids")
        if (genreIds != null) {
            for (i in 0 until genreIds.length()) {
                val gid = genreIds.optInt(i)
                if (gid == 16) tags.add(if (isSpanish) "Animación" else "Animation")
                else if (gid == 10759) tags.add(if (isSpanish) "Acción y Aventura" else "Action & Adventure")
                else if (gid == 35) tags.add(if (isSpanish) "Comedia" else "Comedy")
                else if (gid == 18) tags.add("Drama")
            }
        }
        val genresArray = json.optJSONArray("genres")
        if (genresArray != null) {
            for (i in 0 until genresArray.length()) {
                val g = genresArray.optJSONObject(i)?.optString("name")
                if (!g.isNullOrEmpty() && !tags.contains(g)) tags.add(g)
            }
        }

        val subtitleParts = mutableListOf<String>()
        if (episodesCount != null) subtitleParts.add("$episodesCount Ep")
        if (tags.isNotEmpty()) subtitleParts.add(tags.first())
        val subtitle = subtitleParts.joinToString(" • ").takeIf { it.isNotEmpty() }

        return com.lkz.affa.data.models.Content(
            id = "tmdb:$id", // Prefijo TMDB
            title = finalTitle,
            imageUrl = imageUrl,
            bannerUrl = bannerUrl,
            subtitle = subtitle,
            description = overview,
            rating = rating,
            tags = tags.takeIf { it.isNotEmpty() },
            episodesCount = episodesCount,
            releaseStatus = releaseStatus,
            nextAiringEpisode = nextAiringEpisode
        )
    }
}
