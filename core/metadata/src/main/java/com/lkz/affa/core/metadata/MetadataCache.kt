package com.lkz.affa.core.metadata

import android.content.Context
import com.lkz.affa.domain.model.AnimeMetadata
import okhttp3.Cache
import okhttp3.OkHttpClient
import java.io.File
import java.util.LinkedHashMap

class MetadataCache(context: Context) {

    private val memoryCache = object : LinkedHashMap<String, AnimeMetadata>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, AnimeMetadata>?) =
            size > MAX_MEMORY_SIZE
    }

    val diskCache: Cache = Cache(
        directory = File(context.cacheDir, "metadata_api"),
        maxSize = 10L * 1024 * 1024 // 10 MB
    )

    fun getMemory(key: String): AnimeMetadata? = synchronized(memoryCache) {
        memoryCache[key]
    }

    fun putMemory(key: String, metadata: AnimeMetadata) {
        synchronized(memoryCache) {
            memoryCache[key] = metadata
        }
    }

    fun searchKey(title: String) = "search:${title.lowercase().trim()}"
    fun idKey(id: Int) = "id:$id"

    fun clearMemory() {
        synchronized(memoryCache) { memoryCache.clear() }
    }

    fun clearAll() {
        clearMemory()
        diskCache.evictAll()
    }

    companion object {
        private const val MAX_MEMORY_SIZE = 128
    }
}
