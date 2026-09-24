package com.lkz.affa.core.network.scraper

import com.lkz.affa.domain.model.AnimeDetails
import com.lkz.affa.domain.model.AnimeSearchResult
import com.lkz.affa.domain.model.Episode
import com.lkz.affa.domain.model.RecentEpisode
import com.lkz.affa.domain.model.VideoServer
import org.jsoup.Jsoup

import java.net.URLEncoder

class TioAnimeScraper(
    private val baseUrl: String = "https://tioanime.com"
) {
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"


    /**
     * Busca animes en el directorio de TioAnime según el texto de búsqueda ingresado.
     */
    fun search(query: String): List<AnimeSearchResult> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val searchUrl = "$baseUrl/directorio?q=$encodedQuery"

        val doc = Jsoup.connect(searchUrl)
            .userAgent(userAgent)
            .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            .timeout(15000)
            .get()

        val results = mutableListOf<AnimeSearchResult>()
        val articles = doc.select("article.anime")

        for (article in articles) {
            val anchor = article.selectFirst("a") ?: continue
            val href = anchor.attr("href")
            val title = article.selectFirst(".title")?.text()?.trim() ?: ""
            val imgPath = article.selectFirst("img")?.attr("src") ?: ""

            val fullUrl = if (href.startsWith("http")) href else "$baseUrl$href"
            val fullImageUrl = if (imgPath.startsWith("http")) imgPath else "$baseUrl$imgPath"
            val slug = href.removePrefix("/anime/").trim('/')

            if (title.isNotEmpty()) {
                results.add(
                    AnimeSearchResult(
                        title = title,
                        slug = slug,
                        url = fullUrl,
                        imageUrl = fullImageUrl
                    )
                )
            }
        }

        return results
    }

    /**
     * Obtiene el detalle de un anime por su slug (ej: "naruto") o URL completa,
     * incluyendo sinopsis, géneros, estado y lista completa de episodios.
     */
    fun getAnimeDetails(slugOrUrl: String): AnimeDetails {
        val rawSlug = if (slugOrUrl.startsWith("http")) {
            slugOrUrl.removePrefix("$baseUrl/anime/").trim('/')
        } else {
            slugOrUrl.trim('/')
        }

        val animeUrl = "$baseUrl/anime/$rawSlug"

        val doc = Jsoup.connect(animeUrl)
            .userAgent(userAgent)
            .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            .timeout(15000)
            .get()

        val title = doc.selectFirst("h1.title")?.text()?.trim() ?: ""
        val synopsis = doc.selectFirst("p.sinopsis")?.text()?.trim() ?: ""
        val imgPath = doc.selectFirst("div.thumb img")?.attr("src") ?: ""
        val fullImageUrl = if (imgPath.startsWith("http")) imgPath else "$baseUrl$imgPath"
        val status = doc.selectFirst(".status")?.text()?.trim() ?: ""
        val type = doc.selectFirst("span[class*='anime-type']")?.text()?.trim() ?: ""

        val genres = doc.select("a[href*='genero=']")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val scripts = doc.select("script")
        var canonicalSlug = rawSlug
        val episodes = mutableListOf<Episode>()

        for (script in scripts) {
            val scriptContent = script.data()
            if (scriptContent.contains("anime_info")) {
                val infoMatch = Regex("""var\s+anime_info\s*=\s*\["([^"]+)",\s*"([^"]+)",\s*"([^"]+)"\];""").find(scriptContent)
                if (infoMatch != null && infoMatch.groupValues[2].isNotEmpty()) {
                    canonicalSlug = infoMatch.groupValues[2]
                }
            }

            if (scriptContent.contains("var episodes =")) {

                val match = Regex("""var\s+episodes\s*=\s*\[(.*?)\];""").find(scriptContent)
                if (match != null) {
                    val rawEpisodes = match.groupValues[1].trim()
                    if (rawEpisodes.isNotEmpty()) {
                        val epNumbers = rawEpisodes.split(",")
                            .mapNotNull { it.trim().toFloatOrNull() }

                        for (epNum in epNumbers) {
                            val epClean = if (epNum % 1 == 0f) epNum.toInt().toString() else epNum.toString()
                            episodes.add(
                                Episode(
                                    number = epNum,
                                    title = "Episodio $epClean",
                                    url = "$baseUrl/ver/$canonicalSlug-$epClean"
                                )
                            )
                        }
                    }
                }
            }
        }

        return AnimeDetails(
            title = title,
            slug = canonicalSlug,
            url = animeUrl,
            imageUrl = fullImageUrl,
            synopsis = synopsis,
            genres = genres,
            status = status,
            type = type,
            episodes = episodes
        )
    }

    /**
     * Obtiene los servidores de reproducción (embed URLs) disponibles para un episodio.
     * @param episodeUrl URL completa (ej. "https://tioanime.com/ver/naruto-1") o relativa ("/ver/naruto-1" o "naruto-1").
     */
    fun getVideoServers(episodeUrl: String): List<VideoServer> {
        val targetUrl = when {
            episodeUrl.startsWith("http") -> episodeUrl
            episodeUrl.startsWith("/ver/") -> "$baseUrl$episodeUrl"
            episodeUrl.startsWith("/") -> "$baseUrl$episodeUrl"
            else -> "$baseUrl/ver/$episodeUrl"
        }

        val doc = Jsoup.connect(targetUrl)
            .userAgent(userAgent)
            .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            .timeout(15000)
            .get()

        val servers = mutableListOf<VideoServer>()
        val scripts = doc.select("script")

        for (script in scripts) {
            val scriptContent = script.data()
            if (scriptContent.contains("var videos =")) {
                // Formato: [["Mega","https:\/\/mega.nz\/embed\/...",0,0],["YourUpload","..."]]
                val regex = Regex("""\["([^"]+)",\s*"([^"]+)"""")
                val matches = regex.findAll(scriptContent)

                for (match in matches) {
                    val serverName = match.groupValues[1].trim()
                    val rawUrl = match.groupValues[2].trim()
                    val cleanUrl = rawUrl.replace("""\/""", "/")

                    val fullEmbedUrl = when {
                        cleanUrl.startsWith("//") -> "https:$cleanUrl"
                        cleanUrl.startsWith("/") -> "$baseUrl$cleanUrl"
                        else -> cleanUrl
                    }

                    if (serverName.isNotEmpty() && fullEmbedUrl.isNotEmpty()) {
                        servers.add(
                            VideoServer(
                                name = serverName,
                                embedUrl = fullEmbedUrl
                            )
                        )
                    }
                }
                break
            }
        }

        return servers
    }

    fun getLatestEpisodes(): List<RecentEpisode> {
        val doc = Jsoup.connect(baseUrl)
            .userAgent(userAgent)
            .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            .timeout(15000)
            .get()

        val episodes = mutableListOf<RecentEpisode>()
        val articles = doc.select("ul.episodes article.episode")

        for (article in articles) {
            val anchor = article.selectFirst("a") ?: continue
            val href = anchor.attr("href")
            val fullUrl = if (href.startsWith("http")) href else "$baseUrl$href"
            val imgPath = article.selectFirst("img")?.attr("src") ?: ""
            val fullImageUrl = if (imgPath.startsWith("http")) imgPath else "$baseUrl$imgPath"
            val titleText = article.selectFirst("h3.title")?.text()?.trim() ?: ""

            val epMatch = Regex("""(.+?)\s+(\d+\.?\d*)$""").find(titleText)
            val animeTitle = epMatch?.groupValues?.get(1) ?: titleText
            val epNumber = epMatch?.groupValues?.get(2)?.toFloatOrNull() ?: 0f

            if (animeTitle.isNotEmpty()) {
                episodes.add(
                    RecentEpisode(
                        animeTitle = animeTitle,
                        episodeNumber = epNumber,
                        url = fullUrl,
                        imageUrl = fullImageUrl,
                        source = "TioAnime",
                        publishedAt = null,
                        timeLabel = null
                    )
                )
            }
        }

        return episodes
    }
}


