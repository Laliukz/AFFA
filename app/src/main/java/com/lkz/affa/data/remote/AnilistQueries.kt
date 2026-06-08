package com.lkz.affa.data.remote

object AnilistQueries {
    const val TRENDING_QUERY = """
        query GetTrending(${'$'}page: Int, ${'$'}perPage: Int) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            media(sort: TRENDING_DESC, type: ANIME, isAdult: false, genre_not_in: ["Hentai"], status_not: NOT_YET_RELEASED) {
              id
              title { userPreferred english }
              coverImage { large extraLarge }
              bannerImage
              description
              averageScore
              genres
              episodes
              status
              nextAiringEpisode { episode }
            }
          }
        }
    """

    const val SEARCH_QUERY = """
        query GetSearch(${'$'}search: String, ${'$'}page: Int, ${'$'}perPage: Int, ${'$'}sort: [MediaSort], ${'$'}genre: String, ${'$'}year: Int, ${'$'}status: MediaStatus) {
          Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            media(search: ${'$'}search, type: ANIME, sort: ${'$'}sort, genre: ${'$'}genre, seasonYear: ${'$'}year, status: ${'$'}status, isAdult: false, genre_not_in: ["Hentai"], status_not: NOT_YET_RELEASED) {
              id
              title { userPreferred }
              coverImage { large }
              averageScore
              genres
              episodes
              status
              nextAiringEpisode { episode }
            }
          }
        }
    """

    const val POPULAR_QUERY = """
        query GetPopular(${'$'}page: Int, ${'$'}perPage: Int) {
           Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            media(sort: POPULARITY_DESC, type: ANIME, isAdult: false, genre_not_in: ["Hentai"], status_not: NOT_YET_RELEASED) {
              id
              title { userPreferred }
              coverImage { large }
              averageScore
              genres
              bannerImage
              description
              episodes
              status
              nextAiringEpisode { episode }
            }
          }
        }
    """

    const val NEW_QUERY = """
        query GetNew(${'$'}page: Int, ${'$'}perPage: Int) {
           Page(page: ${'$'}page, perPage: ${'$'}perPage) {
            media(sort: START_DATE_DESC, type: ANIME, status: RELEASING, isAdult: false, genre_not_in: ["Hentai"]) {
              id
              title { userPreferred }
              coverImage { large }
              averageScore
              genres
              episodes
              status
              nextAiringEpisode { episode }
            }
          }
        }
    """

    const val DETAILS_QUERY = """
        query GetAnimeDetails(${'$'}id: Int) {
          Media(id: ${'$'}id, type: ANIME) {
            id
            title { userPreferred }
            coverImage { large extraLarge }
            bannerImage
            description
            averageScore
            genres
            episodes
            status
            nextAiringEpisode { episode }
            streamingEpisodes { title thumbnail url }
            relations {
              edges {
                relationType
                node {
                  id
                  title { userPreferred }
                  coverImage { large }
                  averageScore
                }
              }
            }
            recommendations(sort: RATING_DESC, perPage: 6) {
              edges {
                node {
                  mediaRecommendation {
                    id
                    title { userPreferred }
                    coverImage { large }
                    averageScore
                  }
                }
              }
            }
          }
        }
    """

    const val BATCH_DETAILS_QUERY = """
        query GetAnimeBatch(${'$'}ids: [Int]) {
          Page(page: 1, perPage: 50) {
            media(id_in: ${'$'}ids, type: ANIME) {
              id
              episodes
              status
              nextAiringEpisode {
                episode
              }
            }
          }
        }
    """

    const val TODAY_EPISODES_QUERY = """
        query GetTodayEpisodes(${'$'}start: Int, ${'$'}end: Int) {
          Page(page: 1, perPage: 25) {
            airingSchedules(airingAt_greater: ${'$'}start, airingAt_lesser: ${'$'}end, sort: TIME) {
              episode
              media {
                id
                title { userPreferred }
                coverImage { large }
                averageScore
                genres
                status
              }
            }
          }
        }
    """
}
