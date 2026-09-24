package com.lkz.affa.core.metadata.api

import com.lkz.affa.core.metadata.model.JikanResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface JikanApi {

    @GET("anime")
    suspend fun searchAnime(
        @Query("q") query: String,
        @Query("limit") limit: Int = 5
    ): JikanResponse

    companion object {
        const val BASE_URL = "https://api.jikan.moe/v4/"
    }
}
