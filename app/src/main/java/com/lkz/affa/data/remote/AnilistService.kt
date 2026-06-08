package com.lkz.affa.data.remote

import com.lkz.affa.data.models.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AnilistService {
    private val apiUrl = "https://graphql.anilist.co"

    private suspend fun executeQuery(query: String, variables: JSONObject): JSONObject = withContext(Dispatchers.IO) {
        val url = URL(apiUrl)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = true

        val requestBody = JSONObject().apply {
            put("query", query)
            put("variables", variables)
        }

        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(requestBody.toString())
            writer.flush()
        }

        val responseCode = connection.responseCode
        if (responseCode != HttpURLConnection.HTTP_OK) {
            val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }
            throw Exception("HTTP Error: ${'$'}responseCode - ${'$'}errorBody")
        }

        val responseBody = connection.inputStream.bufferedReader().use { it.readText() }
        JSONObject(responseBody)
    }

    suspend fun getTrending(page: Int = 1, perPage: Int = 10): List<Content> {
        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
        }
        val response = executeQuery(AnilistQueries.TRENDING_QUERY, variables)
        return parseMediaList(response)
    }

    suspend fun getPopular(page: Int = 1, perPage: Int = 10): List<Content> {
        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
        }
        val response = executeQuery(AnilistQueries.POPULAR_QUERY, variables)
        return parseMediaList(response)
    }

    suspend fun getNew(page: Int = 1, perPage: Int = 10): List<Content> {
        val variables = JSONObject().apply {
            put("page", page)
            put("perPage", perPage)
        }
        val response = executeQuery(AnilistQueries.NEW_QUERY, variables)
        return parseMediaList(response)
    }

    suspend fun getAnimeDetails(id: Int): Content {
        val variables = JSONObject().apply {
            put("id", id)
        }
        val response = executeQuery(AnilistQueries.DETAILS_QUERY, variables)
        val data = response.optJSONObject("data")?.optJSONObject("Media")
            ?: throw Exception("Invalid response or not found")
        val content = mapToContent(data)

        val isSpanish = java.util.Locale.getDefault().language == "es"
        if (isSpanish) {
            try {
                // Try to fetch Spanish description from TMDB using the title
                val tmdbId = TmdbService.instance.searchTvShow(content.title, null)
                if (tmdbId != null) {
                    val tmdbDetails = TmdbService.instance.getAnimeDetails(tmdbId)
                    if (!tmdbDetails.description.isNullOrEmpty()) {
                        return content.copy(description = tmdbDetails.description)
                    }
                }
            } catch (e: Exception) {
                // Ignore and use original AniList description
            }
        }
        return content
    }

    suspend fun getAnimeBatchDetails(ids: List<Int>): List<Content> {
        if (ids.isEmpty()) return emptyList()
        val variables = JSONObject().apply {
            val jsonArray = org.json.JSONArray()
            ids.forEach { jsonArray.put(it) }
            put("ids", jsonArray)
        }
        val response = executeQuery(AnilistQueries.BATCH_DETAILS_QUERY, variables)
        return parseMediaList(response)
    }

    suspend fun searchAnime(
        query: String, 
        page: Int = 1, 
        perPage: Int = 10,
        sort: String? = null,
        genre: String? = null,
        year: Int? = null,
        status: String? = null
    ): List<Content> {
        val variables = JSONObject().apply {
            if (query.isNotBlank()) put("search", query)
            put("page", page)
            put("perPage", perPage)
            
            val mappedSort = when(sort) {
                "Trending" -> "TRENDING_DESC"
                "Newest" -> "START_DATE_DESC"
                "Score" -> "SCORE_DESC"
                else -> "POPULARITY_DESC" // Default
            }
            put("sort", org.json.JSONArray().apply { put(mappedSort) })
            
            if (genre != null) put("genre", genre)
            if (year != null) put("year", year)
            
            val mappedStatus = when(status) {
                "Releasing" -> "RELEASING"
                "Finished" -> "FINISHED"
                else -> null
            }
            if (mappedStatus != null) put("status", mappedStatus)
        }
        val response = executeQuery(AnilistQueries.SEARCH_QUERY, variables)
        return parseMediaList(response)
    }

    suspend fun getTodayEpisodes(): List<Content> {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        val startOfDay = (calendar.timeInMillis / 1000).toInt()
        
        calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        val endOfDay = (calendar.timeInMillis / 1000).toInt()

        val variables = JSONObject().apply {
            put("start", startOfDay)
            put("end", endOfDay)
        }
        val response = executeQuery(AnilistQueries.TODAY_EPISODES_QUERY, variables)
        
        val data = response.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("airingSchedules")
            ?: return emptyList()

        val list = mutableListOf<Content>()
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i)
            if (item != null) {
                val media = item.optJSONObject("media")
                val episode = item.optInt("episode")
                if (media != null) {
                    val content = mapToContent(media)
                    val parts = mutableListOf<String>()
                    parts.add("Ep. ${'$'}episode")
                    content.tags?.firstOrNull()?.let { parts.add(it) }
                    
                    val updatedContent = content.copy(
                        subtitle = parts.joinToString(" • "),
                        label = if (java.util.Locale.getDefault().language == "es") "NUEVO EP" else "NEW EP",
                        nextAiringEpisode = episode
                    )
                    list.add(updatedContent)
                }
            }
        }
        return list
    }

    private fun parseMediaList(response: JSONObject): List<Content> {
        val data = response.optJSONObject("data")?.optJSONObject("Page")?.optJSONArray("media")
            ?: return emptyList()

        val list = mutableListOf<Content>()
        for (i in 0 until data.length()) {
            val item = data.optJSONObject(i)
            if (item != null) {
                list.add(mapToContent(item))
            }
        }
        return list
    }

    private fun mapToContent(json: JSONObject): Content {
        val titleObj = json.optJSONObject("title")
        val title = titleObj?.optString("userPreferred", null) 
            ?: titleObj?.optString("english", null) 
            ?: "Unknown Title"

        val coverImageObj = json.optJSONObject("coverImage")
        val imageUrl = coverImageObj?.optString("large", "") ?: ""

        val bannerUrl = if (json.isNull("bannerImage")) null else json.optString("bannerImage")
        val rawDescription = if (json.isNull("description")) null else json.optString("description")
        
        var rating: Double? = null
        if (!json.isNull("averageScore")) {
            rating = json.optInt("averageScore") / 10.0
        }

        val isSpanish = java.util.Locale.getDefault().language == "es"
        val genreMapEs = mapOf(
            "Action" to "Acción",
            "Adventure" to "Aventura",
            "Comedy" to "Comedia",
            "Drama" to "Drama",
            "Fantasy" to "Fantasía",
            "Horror" to "Terror",
            "Mahou Shoujo" to "Chicas Mágicas",
            "Mecha" to "Mecha",
            "Music" to "Música",
            "Mystery" to "Misterio",
            "Psychological" to "Psicológico",
            "Romance" to "Romance",
            "Sci-Fi" to "Ciencia Ficción",
            "Slice of Life" to "Recuentos de la Vida",
            "Sports" to "Deportes",
            "Supernatural" to "Sobrenatural",
            "Thriller" to "Suspenso"
        )

        val rawGenres = json.optJSONArray("genres")
        val tags = mutableListOf<String>()
        if (rawGenres != null) {
            for (i in 0 until rawGenres.length()) {
                val g = rawGenres.optString(i)
                tags.add(if (isSpanish) genreMapEs[g] ?: g else g)
            }
        }

        var episodesCount: Int? = null
        if (!json.isNull("episodes")) {
            episodesCount = json.optInt("episodes")
        }

        val releaseStatus = if (json.isNull("status")) null else json.optString("status")

        val nextAiringEpisodeObj = json.optJSONObject("nextAiringEpisode")
        val nextAiringEpisode = if (nextAiringEpisodeObj != null && !nextAiringEpisodeObj.isNull("episode")) {
            nextAiringEpisodeObj.optInt("episode")
        } else null

        val episodePart = if (releaseStatus?.uppercase() == "RELEASING" && nextAiringEpisode != null) {
            val aired = nextAiringEpisode - 1
            if (aired > 0) "Ep. ${'$'}aired" else null
        } else if (episodesCount != null && episodesCount > 0) {
            "${'$'}episodesCount Ep."
        } else null

        val subtitleParts = mutableListOf<String>()
        if (episodePart != null) {
            subtitleParts.add(episodePart)
        }
        if (tags.isNotEmpty()) {
            subtitleParts.add(tags.first())
        }
        val subtitle = subtitleParts.joinToString(" • ").takeIf { it.isNotEmpty() }

        val relationsObj = json.optJSONObject("relations")
        val relationsEdgesArr = relationsObj?.optJSONArray("edges")
        val relations = mutableListOf<Content>()
        if (relationsEdgesArr != null) {
            for (i in 0 until relationsEdgesArr.length()) {
                val edge = relationsEdgesArr.optJSONObject(i)
                val type = edge?.optString("relationType")
                if (type == "PREQUEL" || type == "SEQUEL" || type == "ALTERNATIVE" || type == "SIDE_STORY" || type == "SPIN_OFF") {
                    val node = edge.optJSONObject("node")
                    if (node != null) {
                        relations.add(mapToContent(node))
                    }
                }
            }
        }

        val recommendations = mutableListOf<Content>()
        val recommendationsObj = json.optJSONObject("recommendations")
        val edgesArr = recommendationsObj?.optJSONArray("edges")
        if (edgesArr != null) {
            for (i in 0 until edgesArr.length()) {
                val node = edgesArr.optJSONObject(i)?.optJSONObject("node")?.optJSONObject("mediaRecommendation")
                if (node != null) {
                    recommendations.add(mapToContent(node))
                }
            }
        }

        val streamingEpisodesObj = json.optJSONArray("streamingEpisodes")
        val streamingEpisodesList = mutableListOf<com.lkz.affa.data.models.ApiEpisode>()
        if (streamingEpisodesObj != null) {
            for (i in 0 until streamingEpisodesObj.length()) {
                val epJson = streamingEpisodesObj.optJSONObject(i)
                if (epJson != null) {
                    streamingEpisodesList.add(
                        com.lkz.affa.data.models.ApiEpisode(
                            title = epJson.optString("title", "Episodio ${'$'}{i + 1}"),
                            thumbnail = if (epJson.isNull("thumbnail")) null else epJson.optString("thumbnail"),
                            url = if (epJson.isNull("url")) null else epJson.optString("url")
                        )
                    )
                }
            }
        }

        val rawId = json.optString("id", "")
        return Content(
            id = if (rawId.isNotEmpty()) "anilist:${'$'}rawId" else "",
            title = title,
            imageUrl = imageUrl,
            bannerUrl = bannerUrl,
            subtitle = subtitle,
            description = stripHtml(rawDescription),
            rating = rating,
            tags = tags.takeIf { it.isNotEmpty() },
            episodesCount = episodesCount,
            relations = relations.takeIf { it.isNotEmpty() },
            recommendations = recommendations.takeIf { it.isNotEmpty() },
            releaseStatus = releaseStatus,
            nextAiringEpisode = nextAiringEpisode,
            streamingEpisodes = streamingEpisodesList.takeIf { it.isNotEmpty() }
        )
    }

    private fun stripHtml(htmlString: String?): String? {
        if (htmlString == null) return null
        return htmlString.replace(Regex("<[^>]*>"), "")
    }
}
