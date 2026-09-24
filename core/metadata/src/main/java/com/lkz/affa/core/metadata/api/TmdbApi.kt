package com.lkz.affa.core.metadata.api

import com.lkz.affa.core.metadata.model.TmdbResponse
import com.lkz.affa.core.metadata.model.TmdbResult
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("search/tv")
    suspend fun searchTv(
        @Query("query") query: String,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-ES",
        @Query("page") page: Int = 1
    ): TmdbResponse

    @GET("tv/{tv_id}")
    suspend fun getTvDetails(
        @Path("tv_id") tvId: Int,
        @Query("api_key") apiKey: String,
        @Query("language") language: String = "es-ES"
    ): TmdbResult

    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_BASE = "https://image.tmdb.org/t/p/w500"
    }
}
