package com.lkz.affa.data.remote.providers

import com.lkz.affa.data.models.AnimeEpisode
import com.lkz.affa.data.models.StreamLink
import com.lkz.affa.data.remote.HttpService
import com.lkz.affa.domain.repositories.AnimeProviderRepository
import org.json.JSONArray
import org.json.JSONObject
import java.net.URL

class AnimeAv1Provider(
    private val httpService: HttpService = HttpService.instance
) : AnimeProviderRepository {

    override val domain: String = "animeav1.com"
    
    private val baseUrl = "https://animeav1.com"

    override suspend fun searchAnime(query: String): Result<String> {
        return try {
            val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
            val searchUrl = "$baseUrl/catalogo?search=$encodedQuery"
            val html = httpService.get(searchUrl)
            
            val regex = Regex("href=\"(/media/[^\"]+)\"")
            val match = regex.find(html)
            
            if (match != null) {
                val path = match.groupValues[1]
                Result.success(resolveUrl(path, baseUrl))
            } else {
                throw Exception("No se encontraron resultados para: $query")
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEpisodes(animeUrl: String): Result<List<AnimeEpisode>> {
        return try {
            val html = httpService.get(animeUrl)
            val svelteData = extractSvelteData(html) ?: throw Exception("No se encontró estado de SvelteKit")
            
            val episodes = mutableListOf<AnimeEpisode>()
            
            findKeyInSvelteData(svelteData, "episodes") { value ->
                if (value is JSONArray) {
                    for (i in 0 until value.length()) {
                        val epObj = value.optJSONObject(i)
                        if (epObj != null) {
                            val numStr = epObj.optString("number", "").ifEmpty {
                                epObj.optString("episode", "")
                            }
                            val num = numStr.toIntOrNull() ?: (i + 1)
                            
                            val epUrlStr = epObj.optString("url", "").ifEmpty {
                                epObj.optString("href", "")
                            }
                            
                            val resolvedUrl = resolveUrl(epUrlStr, animeUrl)
                            
                            episodes.add(
                                AnimeEpisode(
                                    id = epObj.optString("id", num.toString()),
                                    number = num,
                                    title = epObj.optString("title", "Episodio $num"),
                                    image = resolveUrl(epObj.optString("image", ""), animeUrl)
                                )
                            )
                        }
                    }
                }
            }
            
            
            if (episodes.isEmpty()) {
                throw Exception("No se encontraron episodios en los datos extraídos")
            }
            
            Result.success(episodes.distinctBy { it.number }.sortedBy { it.number })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStreamLinks(episodeUrl: String): Result<List<StreamLink>> {
        return try {
            val html = httpService.get(episodeUrl)
            val streamLinks = mutableListOf<StreamLink>()
            
            val svelteData = extractSvelteData(html)
            if (svelteData != null) {
                val keysToSearch = listOf("streamLinks", "servers", "downloadLinks")
                
                for (key in keysToSearch) {
                    findKeyInSvelteData(svelteData, key) { container ->
                        if (container is JSONObject) {
                            val subLinks = container.optJSONArray("SUB") ?: container.optJSONArray("sub")
                            if (subLinks != null) {
                                extractLinksFromJsonArray(subLinks, streamLinks, true)
                            } else {
                                val keys = container.keys()
                                while (keys.hasNext()) {
                                    val k = keys.next()
                                    val v = container.get(k)
                                    if (v is String && v.startsWith("http")) {
                                        streamLinks.add(StreamLink(url = v, server = k))
                                    } else if (v is JSONObject) {
                                        val url = v.optString("url", "").ifEmpty { v.optString("file", "") }
                                        if (url.isNotEmpty()) {
                                            val server = v.optString("server", "").ifEmpty { k }
                                            streamLinks.add(StreamLink(url = url, server = server))
                                        }
                                    }
                                }
                            }
                        } else if (container is JSONArray) {
                            extractLinksFromJsonArray(container, streamLinks, false)
                        }
                    }
                }
            }
            
            val regex = Regex("https?://(?:www\\.)?(?:player\\.[^\\s\"\'<>]+|[^\\s\"\'<>]*zilla[^\\s\"\'<>]*|mp4upload\\.com|1fichier\\.com|streamtape\\.com|upnshare\\.com|mega\\.nz|uns\\.bio)[^\\s\"\'<>]*(?=\")", RegexOption.IGNORE_CASE)
            val matches = regex.findAll(html)
            for (match in matches) {
                streamLinks.add(StreamLink(url = match.value, server = guessServerName(match.value)))
            }
            
            if (streamLinks.isEmpty()) {
                throw Exception("No se encontraron enlaces en $episodeUrl")
            }
            
            val supportedServers = streamLinks.filter { it.server != "Mega" && it.server != "UPNShare" }
            
            val uniqueServers = supportedServers.distinctBy { it.server }
            
            Result.success(uniqueServers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun guessServerName(url: String): String {
        return when {
            url.contains("zilla") || url.contains("player") -> "Zilla/HLS"
            url.contains("mp4upload") -> "Mp4Upload"
            url.contains("1fichier") -> "1Fichier"
            url.contains("streamtape") -> "Streamtape"
            url.contains("mega") -> "Mega"
            url.contains("upnshare") || url.contains("uns.bio") -> "UPNShare"
            else -> "Auto"
        }
    }
    
    private fun extractLinksFromJsonArray(array: JSONArray, result: MutableList<StreamLink>, isDub: Boolean) {
        for (i in 0 until array.length()) {
            val item = array.get(i)
            if (item is JSONObject) {
                val url = item.optString("url", "").ifEmpty { 
                    item.optString("href", "").ifEmpty { item.optString("file", "") }
                }
                if (url.isNotEmpty()) {
                    val server = item.optString("server", "").ifEmpty { item.optString("name", "Unknown") }
                    val quality = item.optString("quality", "Auto")
                    result.add(StreamLink(url = url, server = server, quality = quality, isDub = isDub))
                }
            } else if (item is String && item.startsWith("http")) {
                result.add(StreamLink(url = item, server = "Auto", isDub = isDub))
            }
        }
    }

    private fun extractSvelteData(html: String): JSONArray? {
        val marker = "data:"
        var pointer = html.indexOf(marker)
        if (pointer == -1) return null
        
        val startBracket = html.indexOf("[", pointer)
        if (startBracket == -1) return null
        
        var depth = 0
        var endIndex = -1
        for (i in startBracket until html.length) {
            when (html[i]) {
                '[' -> depth++
                ']' -> {
                    depth--
                    if (depth == 0) {
                        endIndex = i + 1
                        break
                    }
                }
            }
        }
        
        if (endIndex != -1) {
            val jsonString = html.substring(startBracket, endIndex)
            return try {
                val cleanJson = jsonString.replace(Regex("(?<=[\\[,])\\s*undefined\\s*(?=[,\\]])"), "null")
                JSONArray(cleanJson)
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    private fun findKeyInSvelteData(array: JSONArray, targetKey: String, action: (Any) -> Unit) {
        for (i in 0 until array.length()) {
            val item = array.get(i)
            if (item is JSONObject) {
                if (item.has(targetKey)) {
                    action(item.get(targetKey))
                }
                val keys = item.keys()
                while (keys.hasNext()) {
                    val k = keys.next()
                    val v = item.get(k)
                    if (v is JSONObject || v is JSONArray) {
                        findInContainer(v, targetKey, action)
                    }
                }
            } else if (item is JSONArray) {
                findInContainer(item, targetKey, action)
            }
        }
    }
    
    private fun findInContainer(container: Any, targetKey: String, action: (Any) -> Unit) {
        if (container is JSONObject) {
            if (container.has(targetKey)) action(container.get(targetKey))
            val keys = container.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = container.get(k)
                if (v is JSONObject || v is JSONArray) findInContainer(v, targetKey, action)
            }
        } else if (container is JSONArray) {
            for (i in 0 until container.length()) {
                val item = container.get(i)
                if (item is JSONObject || item is JSONArray) findInContainer(item, targetKey, action)
            }
        }
    }
    
    private fun resolveUrl(path: String, baseUrl: String): String {
        if (path.isEmpty()) return ""
        if (path.startsWith("http")) return path
        if (path.startsWith("//")) return "https:$path"
        return try {
            val base = URL(baseUrl)
            URL(base, path).toString()
        } catch (e: Exception) {
            path
        }
    }
}
