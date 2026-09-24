package com.lkz.affa.core.metadata.model

import com.google.gson.annotations.SerializedName

data class AniListResponse(
    @SerializedName("data") val data: AniListData
)

data class AniListData(
    @SerializedName("Page") val page: AniListPage
)

data class AniListPage(
    @SerializedName("pageInfo") val pageInfo: AniListPageInfo?,
    @SerializedName("media") val media: List<AniListMedia>
)

data class AniListMedia(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: AniListTitle,
    @SerializedName("description") val description: String?,
    @SerializedName("coverImage") val coverImage: AniListCoverImage?,
    @SerializedName("bannerImage") val bannerImage: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("episodes") val episodes: Int?,
    @SerializedName("meanScore") val meanScore: Int?,
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("idMal") val idMal: Int?,
    @SerializedName("popularity") val popularity: Int?,
    @SerializedName("startDate") val startDate: AniListStartDate?,
    @SerializedName("nextAiringEpisode") val nextAiringEpisode: AniListNextAiring?
)

data class AniListStartDate(
    @SerializedName("year") val year: Int?,
    @SerializedName("month") val month: Int?,
    @SerializedName("day") val day: Int?
)

data class AniListTitle(
    @SerializedName("romaji") val romaji: String?,
    @SerializedName("english") val english: String?,
    @SerializedName("native") val native: String?
)

data class AniListCoverImage(
    @SerializedName("large") val large: String?
)

data class AniListNextAiring(
    @SerializedName("episode") val episode: Int?,
    @SerializedName("airingAt") val airingAt: Long?
)

data class AniListPageInfo(
    @SerializedName("hasNextPage") val hasNextPage: Boolean?
)
