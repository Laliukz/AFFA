package com.lkz.affa.utils

object AdBlocker {
    private val blockedDomains = listOf(
        "googletagmanager.com",
        "doubleclick.net",
        "google-analytics.com",
        "facebook.com", // Trackers often use FB domain
        "facebook.net",
        "yandex.ru",
        "mc.yandex.ru",
        "cloudflareinsights.com",
        "hotjar.com",
        "scorecardresearch.com",
        "adnxs.com",
        "adsrvr.org",
        "bluekai.com",
        "criteo.com",
        "pubmatic.com",
        "rubiconproject.com",
        "taboola.com",
        "outbrain.com",
        "rlcdn.com", // Rapleaf
        "adroll.com",
        "moatads.com",
        "popads.net",
        "popcash.net",
        "propellerads.com",
        "bvtpk.com", // Found in logs
        "redirector.cdnsync.cloud", // Found in logs (blocked by CORS, likely tracker/ad redirect)
        "vast.js" // VAST ad tags
    )

    private val blockedExtensions = listOf(
        ".woff",
        ".woff2",
        ".ttf",
        ".svg" // Often not needed for video player core functionality and heavy
    )

    private val blockedKeywords = listOf(
        "/ad/",
        "/ads/",
        "advert",
        "analytics",
        "tracker",
        "pixel",
        "stats",
        "banner"
    )

    fun shouldBlock(url: String): Boolean {
        if (url.isEmpty() || url.startsWith("blob:") || url.startsWith("data:")) {
            return false
        }

        val lowerUrl = url.lowercase()

        if (lowerUrl.contains(".m3u8") ||
            lowerUrl.contains(".mp4") ||
            lowerUrl.contains(".ts")) {
            return false
        }

        for (domain in blockedDomains) {
            if (lowerUrl.contains(domain)) {
                return true
            }
        }

        for (ext in blockedExtensions) {
            if (lowerUrl.endsWith(ext) || lowerUrl.contains("$ext?")) {
                return true
            }
        }

        for (keyword in blockedKeywords) {
            if ((keyword == "/ad/" || keyword == "/ads/") && lowerUrl.contains(keyword)) {
                return true
            }
        }

        return false
    }
}
