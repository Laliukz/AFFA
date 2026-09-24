package com.lkz.affa.core.metadata.api

import com.lkz.affa.core.metadata.model.AniListResponse
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.http.Body
import retrofit2.http.POST

interface AniListApi {

    @POST(".")
    suspend fun searchAnime(@Body body: RequestBody): AniListResponse

    companion object {
        const val BASE_URL = "https://graphql.anilist.co"

        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun buildSearchBody(search: String): RequestBody {
            val query = """query (${'$'}search: String!) { Page { media(search: ${'$'}search, type: ANIME) { id idMal title { romaji english native } description(asHtml: false) coverImage { large } bannerImage status episodes meanScore genres } } }"""
            val json = """{"query":"$query","variables":{"search":"$search"}}"""
            return json.toRequestBody(JSON_MEDIA)
        }

        fun buildByIdBody(id: Int): RequestBody {
            val query = """query (${'$'}id: Int!) { Media(id: ${'$'}id, type: ANIME) { id idMal title { romaji english native } description(asHtml: false) coverImage { large } bannerImage status episodes meanScore genres } }"""
            val json = """{"query":"$query","variables":{"id":$id}}"""
            return json.toRequestBody(JSON_MEDIA)
        }

        fun buildTrendingBody(page: Int = 1, perPage: Int = 10): RequestBody {
            val query = """query (${'$'}page: Int, ${'$'}perPage: Int) { Page(page: ${'$'}page, perPage: ${'$'}perPage) { media(type: ANIME, sort: POPULARITY_DESC, status: RELEASING) { id idMal title { romaji english native } coverImage { large } bannerImage meanScore genres popularity } } }"""
            val json = """{"query":"$query","variables":{"page":$page,"perPage":$perPage}}"""
            return json.toRequestBody(JSON_MEDIA)
        }

        fun buildSeasonalBody(season: String, year: Int, page: Int = 1, perPage: Int = 50): RequestBody {
            require(season in setOf("WINTER", "SPRING", "SUMMER", "FALL"))
            val query = """query (${'$'}page: Int, ${'$'}perPage: Int) { Page(page: ${'$'}page, perPage: ${'$'}perPage) { pageInfo { hasNextPage } media(type: ANIME, season: $season, seasonYear: $year, sort: POPULARITY_DESC) { id idMal title { romaji english native } coverImage { large } bannerImage meanScore genres startDate { year month day } nextAiringEpisode { episode airingAt } } } }"""
            val json = """{"query":"$query","variables":{"page":$page,"perPage":$perPage}}"""
            return json.toRequestBody(JSON_MEDIA)
        }

        fun currentSeason(month: Int): String {
            require(month in 1..12)
            return when (month) {
                in 1..3 -> "WINTER"; in 4..6 -> "SPRING"; in 7..9 -> "SUMMER"; else -> "FALL"
            }
        }
    }
}
