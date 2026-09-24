package com.lkz.affa.core.metadata.model

import com.google.gson.annotations.SerializedName

data class TmdbResponse(
    @SerializedName("results") val results: List<TmdbResult>
)

data class TmdbResult(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("original_name") val originalName: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("backdrop_path") val backdropPath: String?,
    @SerializedName("first_air_date") val firstAirDate: String?,
    @SerializedName("vote_average") val voteAverage: Double?,
    @SerializedName("genre_ids") val genreIds: List<Int>?,
    @SerializedName("origin_country") val originCountry: List<String>?
)
