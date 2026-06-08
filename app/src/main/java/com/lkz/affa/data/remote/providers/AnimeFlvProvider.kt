package com.lkz.affa.data.remote.providers

import com.lkz.affa.data.models.AnimeEpisode
import com.lkz.affa.data.models.StreamLink
import com.lkz.affa.data.remote.HttpService
import com.lkz.affa.domain.repositories.AnimeProviderRepository
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder

class AnimeFlvProvider(
    private val httpService: HttpService = HttpService.instance
) : AnimeProviderRepository {

    override val domain: String = "animeflv.net"
    private val baseUrl = "https://www3.animeflv.net"

    override suspend fun searchAnime(query: String): Result<String> {
        return try {
            val words = query.split(" ").take(4).joinToString(" ")
            val encodedQuery = URLEncoder.encode(words, "UTF-8")
            val url = "$baseUrl/browse?q=$encodedQuery"
            
            val response = httpService.get(url) ?: throw Exception("Error al buscar en AnimeFLV")

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

            if (bestMatch != null) {
                Result.success("$baseUrl/anime/$bestMatch")
            } else {
                throw Exception("No se encontró el anime '$query' en AnimeFLV")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEpisodes(animeUrl: String): Result<List<AnimeEpisode>> {
        return Result.failure(Exception("Not required for this flow"))
    }

    override suspend fun getStreamLinks(episodeUrl: String): Result<List<StreamLink>> {
        return try {
            val url = if (episodeUrl.contains("/anime/")) {
                val base = episodeUrl.substringBeforeLast("/")
                val epNum = episodeUrl.substringAfterLast("/")
                base.replace("/anime/", "/ver/") + "-" + epNum
            } else episodeUrl
            
            val html = httpService.get(url)
            
            val videosRegex = Regex("""var\s+videos\s*=\s*(\{.*?\});""")
            val match = videosRegex.find(html)
            
            if (match == null) {
                throw Exception("No se encontraron servidores en el código de AnimeFLV")
            }
            
            val jsonStr = match.groupValues[1]
            val jsonObj = JSONObject(jsonStr)
            val subArr = jsonObj.optJSONArray("SUB") ?: JSONArray()
            
            val streamLinks = mutableListOf<StreamLink>()
            
            for (i in 0 until subArr.length()) {
                val serverObj = subArr.getJSONObject(i)
                val serverName = serverObj.optString("title", "Unknown")
                
                if (serverName.equals("MEGA", ignoreCase = true) ||
                    serverName.equals("SW", ignoreCase = true) ||
                    serverName.equals("Netu", ignoreCase = true) ||
                    serverName.equals("UPNShare", ignoreCase = true)
                ) {
                    continue
                }
                
                val code = serverObj.optString("code")
                val streamUrl = code.takeIf { it.isNotEmpty() } ?: serverObj.optString("url")
                
                if (streamUrl.isNotEmpty()) {
                    var cleanUrl = streamUrl.replace("\\/", "/")
                    if (cleanUrl.startsWith("//")) cleanUrl = "https:$cleanUrl"
                    
                    val finalServerName = when {
                        serverName.equals("SW", ignoreCase = true) -> "Streamwish"
                        serverName.equals("Stape", ignoreCase = true) -> "Streamtape"
                        else -> serverName
                    }
                    
                    streamLinks.add(StreamLink(url = cleanUrl, server = finalServerName))
                }
            }
            
            if (streamLinks.isEmpty()) {
                throw Exception("No se encontraron servidores compatibles")
            }
            
            Result.success(streamLinks.distinctBy { it.server })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
