package com.lkz.affa.core.metadata.model

import com.google.gson.annotations.SerializedName

data class JikanResponse(
    @SerializedName("data") val data: List<JikanAnime>
)

data class JikanAnime(
    @SerializedName("mal_id") val malId: Int,
    @SerializedName("title") val title: String,
    @SerializedName("title_japanese") val titleJapanese: String?,
    @SerializedName("synopsis") val synopsis: String?,
    @SerializedName("images") val images: JikanImages?,
    @SerializedName("episodes") val episodes: Int?,
    @SerializedName("score") val score: Double?,
    @SerializedName("genres") val genres: List<JikanGenre>?,
    @SerializedName("status") val status: String?
)

data class JikanImages(
    @SerializedName("jpg") val jpg: JikanImage?
)

data class JikanImage(
    @SerializedName("large_image_url") val largeImageUrl: String?
)

data class JikanGenre(
    @SerializedName("name") val name: String
)
