package com.lkz.affa.core.network.scraper

import com.lkz.affa.core.common.util.TimeHelper
import com.lkz.affa.domain.model.AnimeDetails
import com.lkz.affa.domain.model.AnimeSearchResult
import com.lkz.affa.domain.model.Episode
import com.lkz.affa.domain.model.EpisodeTimeCategory
import com.lkz.affa.domain.model.RecentEpisode
import com.lkz.affa.domain.model.VideoServer
import org.jsoup.Jsoup
import java.net.URLEncoder

class JkAnimeScraper(
    private val baseUrl: String = "https://jkanime.net"
) {
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private fun conn(url: String) = Jsoup.connect(url)
        .userAgent(userAgent)
        .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
        .timeout(15000)

    /**
     * Busca animes en JkAnime según el texto de búsqueda ingresado.
     */
    fun search(query: String): List<AnimeSearchResult> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val doc = conn("$baseUrl/buscar?q=$encodedQuery").get()

        val results = mutableListOf<AnimeSearchResult>()
        for (item in doc.select("div.page_directorio div.anime__item")) {
            val anchor = item.selectFirst("a[href]") ?: continue
            val href = anchor.attr("href")
            val title = item.selectFirst("h5 a")?.text()?.trim() ?: continue
            val pic = item.selectFirst(".anime__item__pic")
            val img = pic?.attr("data-setbg")?.takeIf { it.isNotBlank() }
                ?: Regex("""url\(["']?([^"')]+)""").find(pic?.attr("style") ?: "")?.groupValues?.get(1)
                ?: ""

            val slug = href.trimEnd('/').substringAfterLast('/')
            if (title.isNotEmpty() && slug.isNotEmpty()) {
                results.add(AnimeSearchResult(title, slug, "$baseUrl/$slug/", img))
            }
        }
        return results.distinctBy { it.slug }
    }

    /**
     * Obtiene el detalle de un anime por su slug (ej: "naruto") o URL completa,
     * incluyendo sinopsis, géneros, estado y lista completa de episodios
     * (vía POST ajax con token CSRF fresco y cookies de sesión).
     */
    fun getAnimeDetails(slugOrUrl: String): AnimeDetails {
        val slug = slugOrUrl.removePrefix(baseUrl).trim('/')
            .let { if (it.contains('/')) it.substringBefore('/') else it }
        val animeUrl = "$baseUrl/$slug/"

        val session = Jsoup.newSession().userAgent(userAgent).timeout(15000)
        val doc = session.newRequest(animeUrl)
            .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
            .get()

        val title = doc.selectFirst(".anime_info h3")?.text()?.trim() ?: ""
        val synopsis = doc.selectFirst(".anime_info p.scroll")?.text()?.trim() ?: ""
        val imageUrl = doc.selectFirst(".anime_pic img")?.attr("src")
            ?: doc.selectFirst(".movpic img")?.attr("src")
            ?: doc.selectFirst("meta[property=og:image]")?.attr("content")
            ?: ""
        val status = doc.selectFirst(".enemision")?.text()?.trim()
            ?: doc.selectFirst("[data-status]")?.attr("data-status")?.trim()
            ?: ""
        val type = doc.selectFirst("li[rel=tipo]")?.text()
            ?.removePrefix("Tipo:")?.trim() ?: ""
        val genres = doc.select("a[href*='/genero/']")
            .map { it.text().trim() }.filter { it.isNotEmpty() }.distinct()
        val animeId = doc.selectFirst("#guardar-anime")?.attr("data-anime")?.trim() ?: ""
        val token = doc.selectFirst("meta[name=csrf-token]")?.attr("content")?.trim() ?: ""

        val episodes = mutableListOf<Episode>()
        if (animeId.isNotEmpty() && token.isNotEmpty()) {
            var total = Int.MAX_VALUE
            var pag = 1
            while (episodes.size < total && pag <= 50) {
                val body = session.newRequest("$baseUrl/ajax/episodes/$animeId/$pag")
                    .header("X-Requested-With", "XMLHttpRequest")
                    .data("_token", token)
                    .ignoreContentType(true)
                    .post()?.body()?.text() ?: break
                total = Regex(""""total"\s*:\s*(\d+)""").find(body)
                    ?.groupValues?.get(1)?.toIntOrNull() ?: total
                val nums = Regex(""""number"\s*:\s*(\d+)""").findAll(body)
                    .mapNotNull { it.groupValues[1].toFloatOrNull() }.toList()
                if (nums.isEmpty()) break
                for (n in nums) {
                    val clean = if (n % 1 == 0f) n.toInt().toString() else n.toString()
                    episodes.add(Episode(n, "Episodio $clean", "$baseUrl/$slug/$clean/"))
                }
                if (nums.size < 2) break
                pag++
            }
        }

        return AnimeDetails(title, slug, animeUrl, imageUrl, synopsis, genres, status, type, episodes.distinctBy { it.number }.sortedBy { it.number })
    }

    /**
     * Obtiene los servidores de reproducción disponibles para un episodio.
     * @param episodeUrl URL completa (ej. "https://jkanime.net/naruto/1/") o "slug/num".
     * Decodifica el array `var servers` (remote en Base64) del player.
     */
    fun getVideoServers(episodeUrl: String): List<VideoServer> {
        val targetUrl = when {
            episodeUrl.startsWith("http") -> episodeUrl
            episodeUrl.startsWith("/") -> "$baseUrl$episodeUrl"
            episodeUrl.contains("/") -> "$baseUrl/$episodeUrl"
            else -> "$baseUrl/$episodeUrl/1/"
        }

        val doc = conn(targetUrl).get()
        val servers = mutableListOf<VideoServer>()

        for (script in doc.select("script")) {
            val content = script.data()
            if (!content.contains("var servers")) continue
            // ponytail: parseo manual con indexOf (el Regex equivalente tumba ICU en Android)
            var i = 0
            while (true) {
                val rKey = content.indexOf("\"remote\"", i)
                if (rKey < 0) break
                val sKey = content.indexOf("\"server\"", rKey)
                if (sKey < 0) break
                val blockEnd = content.indexOf('}', sKey)
                if (blockEnd < 0) break
                val remote = quotedValue(content, rKey)
                val name = quotedValue(content, sKey)
                i = blockEnd + 1
                if (remote == null || name == null) continue
                if (content.lastIndexOf('{', rKey) < 0) continue
                val decoded = b64decode(remote)?.trim() ?: continue
                if (name.trim().isNotEmpty() && decoded.startsWith("http")) {
                    servers.add(VideoServer(name.trim(), decoded))
                }
            }
            break
        }
        return servers
    }

    private fun quotedValue(s: String, keyIndex: Int): String? {
        val colon = s.indexOf(':', keyIndex)
        if (colon < 0) return null
        val q1 = s.indexOf('"', colon)
        if (q1 < 0) return null
        val q2 = s.indexOf('"', q1 + 1)
        if (q2 < 0) return null
        return s.substring(q1 + 1, q2)
    }

    // ponytail: decode Base64 puro (java.util exige API 26+, minSdk 24; android.util es stub en tests JVM)
    private fun b64decode(s: String): String? = try {
        val t = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val clean = s.filter { it in t || it == '=' }
        val out = mutableListOf<Byte>()
        var i = 0
        while (i < clean.length) {
            val v = (0..3).map { k -> if (clean[i + k] == '=') 0 else t.indexOf(clean[i + k]) }
            out.add(((v[0] shl 2) or (v[1] shr 4)).toByte())
            if (clean[i + 2] != '=') out.add((((v[1] and 15) shl 4) or (v[2] shr 2)).toByte())
            if (clean[i + 3] != '=') out.add((((v[2] and 3) shl 6) or v[3]).toByte())
            i += 4
        }
        String(out.toByteArray(), Charsets.UTF_8)
    } catch (_: Exception) { null }

    fun getLatestEpisodes(): List<RecentEpisode> {
        val doc = conn(baseUrl).get()
        val episodes = mutableListOf<RecentEpisode>()

        val cards = doc.select("div.card")
        for (card in cards) {
            val anchor = card.selectFirst("a[href]") ?: continue
            val href = anchor.attr("href")
            if (!href.contains(Regex("""/\d+/?$"""))) continue

            val titleEl = card.selectFirst("h5.card-title") ?: continue
            val animeTitle = titleEl.text().trim()

            val epBadge = card.selectFirst(".badge-primary") ?: continue
            val epText = epBadge.text().trim()
            val epNumber = Regex("""Ep\s+(\d+\.?\d*)""").find(epText)
                ?.groupValues?.get(1)?.toFloatOrNull() ?: continue

            val img = card.selectFirst("img")?.attr("src") ?: ""
            val timeBadge = card.selectFirst(".badge-secondary")
            val rawTimeLabel = timeBadge?.text()?.trim() ?: ""

            val timeCategory = TimeHelper.parseJkAnimeLabel(rawTimeLabel)
            if (timeCategory != EpisodeTimeCategory.TODAY) continue

            val fullUrl = if (href.startsWith("http")) href else "$baseUrl$href"

            episodes.add(
                RecentEpisode(
                    animeTitle = animeTitle,
                    episodeNumber = epNumber,
                    url = fullUrl,
                    imageUrl = img,
                    source = "JkAnime",
                    publishedAt = null,
                    timeLabel = rawTimeLabel
                )
            )
        }

        return episodes
    }
}
