# AFFA — Anime app (v2)

Android app for browsing seasonal anime and watching episodes via external
sources. Kotlin + Jetpack Compose + Material 3.

## Modules

- `app` — UI screens (Home, Details, Explore, MyList) + `HomeRepository`
- `domain` — models (`AnimeMetadata`, `HomeAnimeItem`, `Episode`, …)
- `core:metadata` — AniList / Jikan / TMDB metadata chain (Retrofit)
- `core:network` — Jsoup scrapers (AnimeAv1, JkAnime, TioAnime) + extractors
- `core:common` — shared utils

## Setup

1. Copy `local.properties.example` to `local.properties`
2. Set `sdk.dir` and (optional) `TMDB_API_KEY`
3. `./gradlew assembleDebug`

## Workflow

- `main` is stable, only via PR. Short-lived `feature/*` branches.
- Commit style: `feat|fix|chore(scope): message`
