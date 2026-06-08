package com.lkz.affa.data.remote.resolvers

import com.lkz.affa.data.remote.HttpService
import com.lkz.affa.domain.resolvers.VideoResolver
import org.json.JSONObject
import org.json.JSONArray
import android.util.Base64

class VideoResolverService(
    private val httpService: HttpService = HttpService.instance
) : VideoResolver {

    override suspend fun resolve(url: String): Result<String> {
        val lowerUrl = url.lowercase()
        
        // Zilla requires no scraping; Animev1 rebuilds it directly.
        if (lowerUrl.contains("zilla-networks.com") && url.contains("/play/")) {
            return Result.success(url.replace("/play/", "/m3u8/"))
        }

        return try {
            when {
                lowerUrl.contains("mp4upload") -> resolveMp4Upload(url)
                lowerUrl.contains("streamtape") -> resolveStreamtape(url)
                lowerUrl.contains("voe") || lowerUrl.contains("player") -> {
                    val voe = resolveVoe(url)
                    if (voe.isSuccess) voe else resolveGeneric(url, emptyList())
                }
                lowerUrl.contains("streamwish") || lowerUrl.contains("wish") || lowerUrl.contains("hl") -> resolveStreamwish(url)
                lowerUrl.contains("1fichier.com") -> resolve1Fichier(url)
                lowerUrl.contains("mega.nz") -> throw Exception("Mega no es compatible con el reproductor nativo (requiere WebView)")
                lowerUrl.contains("upnshare.com") || lowerUrl.contains("uns.bio") -> throw Exception("UPNShare no es compatible con el reproductor nativo (requiere WebView)")
                else -> resolveGeneric(url, emptyList())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun resolveGeneric(url: String, patterns: List<Regex>): Result<String> {
        val html = httpService.get(url)
        
        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) return Result.success(normalizeUrl(match.groupValues[1]))
        }
        
        val permissivePatterns = listOf(
            Regex("file\\s*:\\s*[\"'](https?://[^\"']+)[\"']", RegexOption.IGNORE_CASE),
            Regex("sources?\\s*:\\s*\\[\\s*\\{[^}]*(?:file|src)\\s*:\\s*[\"'](https?://[^\"']+)[\"']", RegexOption.IGNORE_CASE),
            Regex("[\"']([^\"']+\\.m3u8[^\"']*)[\"']", RegexOption.IGNORE_CASE) // Aggressive pattern for m3u8 playlists
        )
        for (pattern in permissivePatterns) {
            val match = pattern.find(html)
            if (match != null && !match.groupValues[1].endsWith(".js") && !match.groupValues[1].endsWith(".css")) {
                val foundUrl = match.groupValues[1]
                if (foundUrl.startsWith("http")) return Result.success(normalizeUrl(foundUrl))
                else if (foundUrl.startsWith("/")) return Result.success(normalizeUrl(java.net.URI(url).resolve(foundUrl).toString()))
            }
        }
        
        val genericMatch = Regex("(https?://[^\\s\"'<>]+\\.(?:m3u8|mp4)[^\\s\"'<>]*)", RegexOption.IGNORE_CASE).find(html)
        if (genericMatch != null) return Result.success(normalizeUrl(genericMatch.groupValues[1]))
        
        throw Exception("No se encontró URL de video en el código fuente.")
    }

    // Streamtape Resolver (Ported from Node.js)
    private suspend fun resolveStreamtape(url: String): Result<String> {
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
        val html = httpService.get(url, mapOf("User-Agent" to userAgent))
        
        val scriptRegex = Regex("<script[^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
        val scripts = scriptRegex.findAll(html).map { it.groupValues[1] }
        
        // Use findAll to iterate through all matches (mimics Node.js regex.exec with /g)
        val innerHtmlRegex = Regex("document\\.getElementById\\(['\"]([^'\"]+)['\"]\\)\\.innerHTML\\s*=\\s*(.+);")
        
        for (script in scripts) {
            val allMatches = innerHtmlRegex.findAll(script)
            
            for (match in allMatches) {
                var expr = match.groupValues[2].replace(" ", "")
                val parts = expr.split("+")
                var link = ""
                for (part in parts) {
                    // Ignore empty string parts
                    val cleanPart = part.trim()
                    if (cleanPart == "''" || cleanPart == "\"\"") continue
                    
                    var str = Regex("['\"]([^'\"]+)['\"]").find(cleanPart)?.groupValues?.get(1) ?: ""
                    val subs = Regex("\\.substring\\((\\d+)\\)").findAll(cleanPart)
                    for (sub in subs) {
                        val skip = sub.groupValues[1].toInt()
                        if (skip < str.length) {
                            str = str.substring(skip)
                        }
                    }
                    link += str
                }
                
                if (link.startsWith("//")) link = "https:$link"
                else if (link.startsWith("/")) link = "https://streamtape.com$link"
                else if (!link.startsWith("http")) link = "https://$link"
                
                // Validate via HEAD request, mimicking reference checkSTLink()
                val validUrl = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    checkStreamtapeLink(link, userAgent)
                }
                if (validUrl != null) {
                    return Result.success(normalizeUrl(validUrl))
                }
            }
        }
        throw Exception("No se pudo extraer Streamtape")
    }
    
    /**
     * Mimics checkSTLink() from streamtape.resolver.js:
     * Performs a HEAD request without following redirects; if 3xx, returns Location.
     */
    private fun checkStreamtapeLink(url: String, userAgent: String): String? {
        return try {
            val conn = java.net.URL(url).openConnection() as java.net.HttpURLConnection
            conn.requestMethod = "HEAD"
            conn.instanceFollowRedirects = false
            conn.setRequestProperty("User-Agent", userAgent)
            conn.setRequestProperty("Referer", "https://streamtape.com/")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.connect()
            
            val code = conn.responseCode
            if (code in 301..308) {
                val location = conn.getHeaderField("Location")
                if (location != null) {
                    if (location.startsWith("//")) "https:$location" else location
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    // Voe Resolver (Ported from Node.js)
    private suspend fun resolveVoe(url: String): Result<String> {
        return try {
            var html = httpService.get(url)
            
            // Detect and follow redirects
            val redirMatch = Regex("window\\.location\\.href\\s*=\\s*['\"]([^'\"]+)['\"]").find(html)
            if (redirMatch != null) {
                html = httpService.get(redirMatch.groupValues[1])
            }
            
            // Locate script containing JSON array
            val scriptRegex = Regex("<script[^>]*type=[\"']application/json[\"'][^>]*>(.*?)</script>", RegexOption.DOT_MATCHES_ALL)
            val scripts = scriptRegex.findAll(html)
            
            for (match in scripts) {
                val content = match.groupValues[1]
                try {
                    val array = JSONArray(content)
                    if (array.length() > 0 && array.get(0) is String) {
                        val decoded = decodeVoeObfuscated(content)
                        if (decoded != null) {
                            val jsonObj = JSONObject(decoded)
                            val finalUrl = jsonObj.optString("direct_access_url").takeIf { it.isNotEmpty() }
                                ?: jsonObj.optString("source").takeIf { it.isNotEmpty() }
                                
                            if (finalUrl != null) return Result.success(finalUrl)
                        }
                    }
                } catch (e: Exception) { continue }
            }
            Result.failure(Exception("No se encontró el bloque VOE"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun decodeVoeObfuscated(obfuscated: String): String? {
        try {
            var step = obfuscated.map { c ->
                when (c) {
                    in 'a'..'z' -> if (c < 'n') c + 13 else c - 13
                    in 'A'..'Z' -> if (c < 'N') c + 13 else c - 13
                    else -> c
                }
            }.joinToString("")
            
            listOf("@$", "^^", "~@", "%?", "*~", "!!", "#&").forEach { p ->
                step = step.replace(p, "_")
            }
            step = step.replace("_", "")
            step = String(Base64.decode(step, Base64.DEFAULT))
            step = step.map { (it.code - 3).toChar() }.joinToString("")
            step = step.reversed()
            step = String(Base64.decode(step, Base64.DEFAULT))
            return step
        } catch (e: Exception) { return null }
    }

    // Streamwish Resolver (Ported from Node.js)
    private suspend fun resolveStreamwish(url: String): Result<String> {
        val html = httpService.get(url)
        val scriptMatch = Regex("eval\\(function\\(p,a,c,k,e,d\\)[\\s\\S]*?\\)\\s*</script>", RegexOption.IGNORE_CASE).find(html)
        
        if (scriptMatch != null) {
            val packed = scriptMatch.value
            val unpacked = unpackJs(packed)
            val linksMatch = Regex("var\\s+links\\s*=\\s*(\\{[\\s\\S]*?\\});").find(unpacked)
            if (linksMatch != null) {
                val json = JSONObject(linksMatch.groupValues[1])
                val hls = json.optString("hls4").takeIf { it.isNotEmpty() }
                    ?: json.optString("hls3").takeIf { it.isNotEmpty() }
                    ?: json.optString("hls1").takeIf { it.isNotEmpty() }
                    ?: json.optString("hls2").takeIf { it.isNotEmpty() }
                    
                if (hls != null) {
                    var masterUrl = hls
                    if (masterUrl.startsWith("/")) {
                        val startIndex = url.indexOf("://") + 3
                        val pathIndex = url.indexOf("/", startIndex)
                        val base = if (pathIndex != -1) url.substring(0, pathIndex) else url
                        masterUrl = base + masterUrl
                    }
                    return Result.success(masterUrl)
                }
            }
        }
        
        // Fallback: use direct regex if unpacking fails
        val fallbackMatch = Regex("(https?:[^\\s\"']+\\.m3u8[^\\s\"']*)").find(html)
        if (fallbackMatch != null) return Result.success(fallbackMatch.groupValues[1])
        
        throw Exception("No se pudo desempaquetar Streamwish")
    }

    private fun unpackJs(packed: String): String {
        val regex = Regex("""\}\?\('([^']*)',\s*(\d+),\s*(\d+),\s*'([^']*)'\.split\('\|'\)""")
        val match = regex.find(packed) ?: return packed
        
        val p = match.groupValues[1]
        val a = match.groupValues[2].toInt()
        val c = match.groupValues[3].toInt()
        val k = match.groupValues[4].split("|")

        fun e(value: Int): String {
            val first = if (value < a) "" else e(value / a)
            val rem = value % a
            val second = if (rem > 35) (rem + 29).toChar().toString() else rem.toString(36)
            return first + second
        }

        var result = p
        for (i in c - 1 downTo 0) {
            if (k.getOrNull(i)?.isNotEmpty() == true) {
                val search = e(i)
                result = result.replace(Regex("\\b$search\\b"), k[i])
            }
        }
        return result
    }

    private suspend fun resolveMp4Upload(url: String): Result<String> {
        val html = httpService.get(url, mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Referer" to "https://www.mp4upload.com/"
        ))
        
        android.util.Log.d("Mp4Upload", "HTML length: ${html.length}")
        
        // Match specific patterns used by Mp4Upload (e.g., player.src or player.setup)
        val patterns = listOf(
            // Matches player.src with src: "URL"
            Regex("""player\.src\(\s*\{[^}]*src\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            // Matches player.setup with sources array
            Regex("""player\.setup\(\s*\{[\s\S]*?sources\s*:\s*\[\s*\{[\s\S]*?src\s*:\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            // Matches sources array containing file or src objects
            Regex("""sources\s*:\s*\[\s*\{[^}]*(?:file|src)\s*:\s*["'](https?://[^"']+)["']""", RegexOption.IGNORE_CASE),
            // Matches "file": "URL" containing video extensions
            Regex(""""file"\s*:\s*"(https?://[^"]+\.(?:mp4|m3u8)[^"]*)"""", RegexOption.IGNORE_CASE),
            // Matches unquoted file: "URL"
            Regex("""file\s*:\s*["'](https?://[^"']+\.(?:mp4|m3u8)[^"']*)["']""", RegexOption.IGNORE_CASE)
        )
        
        for (pattern in patterns) {
            val match = pattern.find(html)
            if (match != null) {
                val found = match.groupValues[1]
                android.util.Log.d("Mp4Upload", "Found video URL: $found")
                // Ensure the extracted URL points to an actual video, filtering out static assets
                if (!found.endsWith(".css") && !found.endsWith(".js") && !found.endsWith(".png") && !found.endsWith(".ico")) {
                    return Result.success(normalizeUrl(found))
                }
            }
        }
        
        // Fallback: find explicit video URLs while avoiding site assets
        val genericMatch = Regex("""(https?://(?!www\.mp4upload\.com/player)[^\s"'<>]+\.mp4[^\s"'<>]*)""", RegexOption.IGNORE_CASE).find(html)
        if (genericMatch != null) {
            android.util.Log.d("Mp4Upload", "Generic fallback URL: ${genericMatch.groupValues[1]}")
            return Result.success(normalizeUrl(genericMatch.groupValues[1]))
        }

        throw Exception("No se encontró video en Mp4Upload.")
    }

    private suspend fun resolve1Fichier(url: String): Result<String> {
        val defaultHeaders = mapOf(
            "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
            "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Accept-Language" to "en-US,en;q=0.9"
        )
        val (html, headers) = httpService.getWithHeaders(url, defaultHeaders)
        
        // Check for a direct video link in the initial GET response
        var simpleMatch = Regex("https?://[^\\s'\"]+\\.(?:mp4|mkv|avi|mov|webm)", RegexOption.IGNORE_CASE).find(html)
        if (simpleMatch != null) return Result.success(normalizeUrl(simpleMatch.value))
        
        try {
            // Extract required cookies from response headers
            val setCookieHeaders = headers.entries.firstOrNull { it.key?.equals("Set-Cookie", ignoreCase = true) == true }?.value
            val cookieString = setCookieHeaders?.joinToString("; ") { it.substringBefore(";") } ?: ""
            
            android.util.Log.d("1Fichier", "Cookies extraidas: $cookieString")
            
            val postHeaders = mutableMapOf<String, String>(
                "Referer" to url,
                "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36",
                "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
                "Accept-Language" to "en-US,en;q=0.9",
                "Origin" to "https://1fichier.com"
            )
            if (cookieString.isNotEmpty()) postHeaders["Cookie"] = cookieString

            val postResponse = httpService.postForm(url, "dl=1", postHeaders)
            
            // Handle direct redirects without HTML bodies
            if (postResponse.startsWith("http", ignoreCase = true) && !postResponse.contains("<html")) {
                return Result.success(normalizeUrl(postResponse))
            }
            
            // Extract the actual 1Fichier download link format
            val downloadMatch = Regex("""(https?://[a-z0-9-]+\.download\.[^"'\s<>]+)""", RegexOption.IGNORE_CASE).find(postResponse)
            if (downloadMatch != null) {
                return Result.success(normalizeUrl(downloadMatch.groupValues[1]))
            }
            
            // Extract direct video URL
            simpleMatch = Regex("https?://[^\\s'\"]+\\.(?:mp4|mkv|avi|mov|webm|zip|rar)", RegexOption.IGNORE_CASE).find(postResponse)
            if (simpleMatch != null) return Result.success(normalizeUrl(simpleMatch.value))
            
        } catch (e: Exception) {
            android.util.Log.e("1Fichier", "Error en resolve: ${e.message}")
        }

        throw Exception("1Fichier no entregó el video. Es probable que tu IP esté en tiempo de espera (límite de descargas de 1Fichier).")
    }

    private fun normalizeUrl(url: String): String {
        val clean = url.replace("\\u0026", "&").replace("\\/", "/")
        if (clean.startsWith("//")) return "https:$clean"
        return clean
    }
}
