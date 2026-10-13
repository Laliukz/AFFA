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
import java.time.Instant
import java.time.OffsetDateTime

class AnimeAv1Scraper(
    private val baseUrl: String = "https://animeav1.com"
) {
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    private fun raw(url: String): String = Jsoup.connect(url)
        .userAgent(userAgent)
        .header("Accept-Language", "es-ES,es;q=0.9,en;q=0.8")
        .timeout(15000)
        .ignoreContentType(true)
        .execute()
        .body()

    // Lee string con escapes ("...\"...\n...") desde la comilla de apertura.
    private fun readQuoted(s: String, openQuote: Int): String? {
        val sb = StringBuilder()
        var i = openQuote + 1
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                val n = s[i + 1]
                sb.append(if (n == 'n') ' ' else n)
                i += 2
            } else if (c == '"') {
                return sb.toString()
            } else {
                sb.append(c)
                i++
            }
        }
        return null
    }

    private fun strField(s: String, key: String, from: Int = 0): String? {
        val k = s.indexOf(key, from)
        if (k < 0) return null
        val q = s.indexOf('"', k)
        if (q < 0) return null
        return readQuoted(s, q)
    }

    /**
     * Busca animes en el catálogo de AnimeAV1.
     */
    fun search(query: String): List<AnimeSearchResult> {
        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val doc = Jsoup.parse(raw("$baseUrl/catalogo?search=$encodedQuery"), baseUrl)

        val results = mutableListOf<AnimeSearchResult>()
        for (a in doc.select("a[href^=/media/]")) {
            val href = a.attr("href")
            if (href.count { it == '/' } != 2) continue
            var p = a.parent()
            while (p != null && p.tagName() != "article") p = p.parent()
            val title = p?.selectFirst("h3")?.text()?.trim()
                ?: a.selectFirst(".sr-only")?.text()?.removePrefix("Ver ")?.trim()
                ?: continue
            val img = p?.selectFirst("img")?.attr("abs:src") ?: ""
            val slug = href.substringAfterLast('/')
            if (title.isNotEmpty() && slug.isNotEmpty()) {
                results.add(AnimeSearchResult(title, slug, "$baseUrl/media/$slug", img))
            }
        }
        return results.distinctBy { it.slug }
    }

    /**
     * Obtiene el detalle desde el __data embebido de /media/{slug}
     * (título, sinopsis, géneros, estado, tipo y lista completa de episodios en 1 request).
     */
    fun getAnimeDetails(slugOrUrl: String): AnimeDetails {
        val slug = slugOrUrl.removePrefix(baseUrl).removePrefix("/media").trim('/').substringBefore('/')
        val animeUrl = "$baseUrl/media/$slug"
        val body = raw(animeUrl)
        val mediaStart = body.indexOf("media:{")

        val title = strField(body, "title:\"", mediaStart) ?: ""
        val synopsis = strField(body, "synopsis:\"", mediaStart) ?: ""
        val id = body.indexOf("id:", mediaStart).let {
            if (it < 0) "" else body.substring(it + 3).takeWhile { c -> c.isDigit() }
        }
        val imageUrl = if (id.isNotEmpty()) "https://cdn.animeav1.com/covers/$id.jpg" else ""
        val status = Regex("status:(\\d)").find(body)?.groupValues?.get(1)?.let {
            when (it) {
                "0" -> "Concluido"
                "2" -> "En emisión"
                else -> it
            }
        } ?: ""
        val catStart = body.indexOf("category:{", mediaStart)
        val type = strField(body, "name:\"", catStart) ?: ""

        val genres = mutableListOf<String>()
        val gStart = body.indexOf("genres:[", mediaStart)
        val gEnd = body.indexOf(']', gStart)
        if (gStart >= 0 && gEnd > gStart) {
            var i = gStart
            while (true) {
                val n = body.indexOf("name:\"", i)
                if (n < 0 || n > gEnd) break
                readQuoted(body, body.indexOf('"', n))?.let { genres.add(it) }
                i = n + 5
            }
        }

        val episodes = mutableListOf<Episode>()
        val eStart = body.indexOf("episodes:[", mediaStart)
        val eEnd = body.indexOf(']', eStart)
        if (eStart >= 0 && eEnd > eStart) {
            var i = eStart
            while (true) {
                val numKey = body.indexOf("number:", i)
                if (numKey < 0 || numKey > eEnd) break
                val num = body.substring(numKey + 7).takeWhile { it.isDigit() }.toFloatOrNull()
                if (num != null) {
                    val clean = if (num % 1 == 0f) num.toInt().toString() else num.toString()
                    episodes.add(Episode(num, "Episodio $clean", "$baseUrl/media/$slug/$clean"))
                }
                i = numKey + 7
            }
        }

        return AnimeDetails(title, slug, animeUrl, imageUrl, synopsis, genres.distinct(), status, type, episodes.distinctBy { it.number }.sortedBy { it.number })
    }

    /**
     * Obtiene los servidores desde embeds.SUB (fallback DUB) del __data de /media/{slug}/{num}.
     */
    fun getVideoServers(episodeUrl: String): List<VideoServer> {
        val targetUrl = when {
            episodeUrl.startsWith("http") -> episodeUrl
            episodeUrl.startsWith("/") -> "$baseUrl$episodeUrl"
            episodeUrl.startsWith("media/") -> "$baseUrl/$episodeUrl"
            episodeUrl.contains("/") -> "$baseUrl/media/$episodeUrl"
            else -> "$baseUrl/media/$episodeUrl/1"
        }

        val body = raw(targetUrl)
        val servers = mutableListOf<VideoServer>()
        for (lang in listOf("SUB", "DUB")) {
            val lStart = body.indexOf("$lang:[")
            if (lStart < 0) continue
            val lEnd = body.indexOf(']', lStart)
            if (lEnd < 0) continue
            var i = lStart
            while (true) {
                val sKey = body.indexOf("{server:\"", i)
                if (sKey < 0 || sKey > lEnd) break
                val name = readQuoted(body, sKey + 8) ?: break
                val uKey = body.indexOf("url:\"", sKey)
                if (uKey < 0 || uKey > lEnd) break
                val url = readQuoted(body, uKey + 4) ?: break
                if (name.isNotBlank() && url.startsWith("http")) {
                    servers.add(VideoServer(name.trim(), url.trim(), lang))
                }
                i = uKey + 5
            }
        }
        return servers
    }

    fun getLatestEpisodes(zone: java.time.ZoneId = java.time.ZoneId.systemDefault()): List<RecentEpisode> {
        val body = raw(baseUrl)
        val dataStart = body.indexOf("kit.start(")
        if (dataStart < 0) return emptyList()

        val dataBody = body.substring(dataStart)
        val episodesStart = dataBody.indexOf("latestEpisodes:[")
        if (episodesStart < 0) return emptyList()

        val arrStart = dataBody.indexOf('[', episodesStart)
        val arrEnd = findMatchingBracket(dataBody, arrStart)
        if (arrEnd < 0) return emptyList()

        val arrContent = dataBody.substring(arrStart + 1, arrEnd)
        val episodes = mutableListOf<RecentEpisode>()

        var i = 0
        while (i < arrContent.length) {
            val objStart = arrContent.indexOf('{', i)
            if (objStart < 0) break
            val objEnd = findMatchingBrace(arrContent, objStart)
            if (objEnd < 0) break

            val obj = arrContent.substring(objStart, objEnd + 1)

            val title = extractMediaTitle(obj)
            val epNum = extractField(obj, "number:")?.toFloatOrNull()
            val slug = extractMediaSlug(obj)
            val publishedAtStr = extractField(obj, "publishedAt:\"")

            if (title != null && epNum != null && slug != null && publishedAtStr != null) {
                val normalizedTs = publishedAtStr
                    .replace(" ", "T")
                    .let { if (!it.contains(":") || it.endsWith("+00")) it.replace("+00", "+00:00") else it }
                val publishedAt = try {
                    OffsetDateTime.parse(normalizedTs).toInstant()
                } catch (_: Exception) {
                    try {
                        Instant.parse(normalizedTs)
                    } catch (_: Exception) {
                        null
                    }
                }

                val timeCategory = TimeHelper.classifyByInstant(publishedAt, zone)
                if (timeCategory == EpisodeTimeCategory.TODAY) {
                    // ponytail: Av1 quitó coverId del feed — el thumb sale del CDN con el media id
                    val mediaId = extractMediaId(obj)
                    val imageUrl = mediaId?.let { "https://cdn.animeav1.com/covers/$it.jpg" } ?: ""
                    episodes.add(
                        RecentEpisode(
                            animeTitle = title,
                            episodeNumber = epNum,
                            url = "$baseUrl/media/$slug/$epNum",
                            imageUrl = imageUrl,
                            source = "AnimeAv1",
                            publishedAt = publishedAt,
                            timeLabel = null,
                            mediaId = mediaId
                        )
                    )
                }
            }

            i = objEnd + 1
        }

        return episodes
    }

    private fun findMatchingBracket(s: String, start: Int): Int {
        var depth = 0
        for (i in start until s.length) {
            when (s[i]) {
                '[' -> depth++
                ']' -> { depth--; if (depth == 0) return i }
            }
        }
        return -1
    }

    private fun findMatchingBrace(s: String, start: Int): Int {
        var depth = 0
        for (i in start until s.length) {
            when (s[i]) {
                '{' -> depth++
                '}' -> { depth--; if (depth == 0) return i }
            }
        }
        return -1
    }

    private fun extractField(obj: String, key: String): String? {
        val k = obj.indexOf(key)
        if (k < 0) return null
        val qStart = k + key.length
        if (qStart >= obj.length) return null
        if (key.endsWith('"')) {
            return readQuoted(obj, qStart - 1)
        }
        val end = obj.indexOfAny(charArrayOf(',', '}'), qStart)
        if (end < 0) return null
        return obj.substring(qStart, end).trim()
    }

    // ponytail: extrae title del nested media:{} — busca media:{...} y de ahí saca title:"..."
    private fun extractMediaTitle(obj: String): String? {
        val mediaStart = obj.indexOf("media:{")
        if (mediaStart < 0) return null
        val mediaObjStart = mediaStart + 6
        val mediaObjEnd = findMatchingBrace(obj, mediaObjStart)
        if (mediaObjEnd < 0) return null
        val mediaObj = obj.substring(mediaObjStart, mediaObjEnd + 1)
        return extractField(mediaObj, "title:\"")
    }

    // ponytail: extrae slug del nested media:{}
    private fun extractMediaSlug(obj: String): String? {
        val mediaStart = obj.indexOf("media:{")
        if (mediaStart < 0) return null
        val mediaObjStart = mediaStart + 6
        val mediaObjEnd = findMatchingBrace(obj, mediaObjStart)
        if (mediaObjEnd < 0) return null
        val mediaObj = obj.substring(mediaObjStart, mediaObjEnd + 1)
        return extractField(mediaObj, "slug:\"")
    }

    // ponytail: id del anime dentro de media:{} (difiere del id del episodio)
    private fun extractMediaId(obj: String): String? {
        val mediaStart = obj.indexOf("media:{")
        if (mediaStart < 0) return null
        val mediaObjStart = mediaStart + 6
        val mediaObjEnd = findMatchingBrace(obj, mediaObjStart)
        if (mediaObjEnd < 0) return null
        val mediaObj = obj.substring(mediaObjStart, mediaObjEnd + 1)
        return extractField(mediaObj, "id:")
    }
}
