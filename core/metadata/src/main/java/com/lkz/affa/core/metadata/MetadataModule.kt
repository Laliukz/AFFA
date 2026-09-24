package com.lkz.affa.core.metadata

import android.content.Context
import com.lkz.affa.core.metadata.api.AniListApi
import com.lkz.affa.core.metadata.api.JikanApi
import com.lkz.affa.core.metadata.api.TmdbApi
import com.lkz.affa.core.metadata.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object MetadataModule {

    private var cache: MetadataCache? = null

    fun init(context: Context) {
        cache = MetadataCache(context.applicationContext)
    }

    private val client by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            })

        cache?.let { builder.cache(it.diskCache) }

        builder.build()
    }

    private fun <T> createApi(baseUrl: String, clazz: Class<T>): T =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(clazz)

    fun anilistApi(): AniListApi = createApi(AniListApi.BASE_URL, AniListApi::class.java)
    fun jikanApi(): JikanApi = createApi(JikanApi.BASE_URL, JikanApi::class.java)
    fun tmdbApi(): TmdbApi = createApi(TmdbApi.BASE_URL, TmdbApi::class.java)

    fun createProvider(): MetadataProviderChain {
        val apiKey = BuildConfig.TMDB_API_KEY.ifEmpty { null }
        return MetadataProviderChain.create(
            anilistApi = anilistApi(),
            jikanApi = jikanApi(),
            tmdbApiKey = apiKey,
            cache = cache
        )
    }

    fun getCache(): MetadataCache? = cache
}
