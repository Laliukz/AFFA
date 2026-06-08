package com.lkz.affa.data.remote

import android.content.Context
import android.net.http.HttpResponseCache
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class HttpService private constructor() {

    companion object {
        val instance = HttpService()
        private const val TAG = "HttpService"
    }

    fun init(context: Context) {
        try {
            val httpCacheDir = File(context.cacheDir, "http_cache")
            val httpCacheSize = 50L * 1024 * 1024 // 50 MiB
            HttpResponseCache.install(httpCacheDir, httpCacheSize)
            Log.d(TAG, "HTTP cache initialized at ${'$'}{httpCacheDir.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "HTTP response cache installation failed: ${'$'}e")
        }
    }

    suspend fun get(urlString: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val retryDelays = listOf(1000L, 2000L, 3000L)
        var attempt = 0
        
        while (true) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000 // 10 seconds
                connection.readTimeout = 10000 // 10 seconds
                
                headers.forEach { (key, value) ->
                    connection.setRequestProperty(key, value)
                }
                
                connection.useCaches = true
                connection.addRequestProperty("Cache-Control", "max-stale=${'$'}{7 * 24 * 60 * 60}")

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return@withContext connection.inputStream.bufferedReader().use { it.readText() }
                } else if (responseCode == 401 || responseCode == 403) {
                    throw Exception("HTTP Auth/Forbidden Error $responseCode")
                } else if (responseCode == 404) {
                    throw Exception("HTTP Not Found $responseCode")
                } else {
                    throw Exception("HTTP Error $responseCode")
                }
            } catch (e: Exception) {
                connection?.disconnect()
                if (e.message?.contains("Auth/Forbidden") == true || e.message?.contains("Not Found") == true) {
                    throw e
                }
                if (attempt < retryDelays.size) {
                    Log.d(TAG, "Request failed, retrying in ${'$'}{retryDelays[attempt]}ms. Error: ${'$'}{e.message}")
                    delay(retryDelays[attempt])
                    attempt++
                } else {
                    throw e
                }
            }
        }
        throw Exception("Unknown request error")
    }

    suspend fun post(urlString: String, jsonBody: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val retryDelays = listOf(1000L, 2000L, 3000L)
        var attempt = 0
        
        while (true) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                
                headers.forEach { (key, value) ->
                    connection.setRequestProperty(key, value)
                }
                
                connection.outputStream.writer().use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    return@withContext connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    throw Exception("HTTP POST Error ${'$'}responseCode")
                }
            } catch (e: Exception) {
                connection?.disconnect()
                if (attempt < retryDelays.size) {
                    delay(retryDelays[attempt])
                    attempt++
                } else {
                    throw e
                }
            }
        }
        throw Exception("Unknown request error")
    }

    suspend fun postForm(urlString: String, formBody: String, headers: Map<String, String> = emptyMap()): String = withContext(Dispatchers.IO) {
        val retryDelays = listOf(1000L, 2000L, 3000L)
        var attempt = 0
        
        while (true) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                connection.setRequestProperty("Accept", "*/*")
                
                headers.forEach { (key, value) ->
                    connection.setRequestProperty(key, value)
                }
                
                connection.outputStream.writer().use { writer ->
                    writer.write(formBody)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..399) {
                    val location = connection.getHeaderField("Location")
                    if (location != null) return@withContext location
                    return@withContext connection.inputStream.bufferedReader().use { it.readText() }
                } else {
                    throw Exception("HTTP POST Error $responseCode")
                }
            } catch (e: Exception) {
                connection?.disconnect()
                if (attempt < retryDelays.size) {
                    delay(retryDelays[attempt])
                    attempt++
                } else {
                    throw e
                }
            }
        }
        throw Exception("Unknown request error")
    }

    suspend fun getWithHeaders(urlString: String, headers: Map<String, String> = emptyMap()): Pair<String, Map<String, List<String>>> = withContext(Dispatchers.IO) {
        val retryDelays = listOf(1000L, 2000L, 3000L)
        var attempt = 0
        
        while (true) {
            var connection: HttpURLConnection? = null
            try {
                val url = URL(urlString)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000 // 10 seconds
                connection.readTimeout = 10000 // 10 seconds
                
                headers.forEach { (key, value) ->
                    connection.setRequestProperty(key, value)
                }
                
                connection.useCaches = true
                connection.addRequestProperty("Cache-Control", "max-stale=${7 * 24 * 60 * 60}")

                val responseCode = connection.responseCode
                if (responseCode in 200..399) {
                    val body = connection.inputStream.bufferedReader().use { it.readText() }
                    return@withContext Pair(body, connection.headerFields)
                } else if (responseCode == 401 || responseCode == 403) {
                    throw Exception("HTTP Auth/Forbidden Error $responseCode")
                } else {
                    throw Exception("HTTP Error $responseCode")
                }
            } catch (e: Exception) {
                connection?.disconnect()
                if (attempt < retryDelays.size) {
                    delay(retryDelays[attempt])
                    attempt++
                } else {
                    throw e
                }
            }
        }
        throw Exception("Unknown request error")
    }
}
