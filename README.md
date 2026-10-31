# AFFA

AFFA es una aplicación Android para explorar anime, consultar información y episodios, y reproducir contenido disponible en fuentes externas.

## Funciones

- Explorar anime y buscar títulos.
- Ver detalles y episodios disponibles.
- Reproducir episodios desde servidores externos compatibles.
- Guardar títulos en una lista personal y marcar episodios como vistos.

## Tecnologías

Desarrollada en Kotlin con Jetpack Compose y Material 3. La información se obtiene mediante scrapers de AnimeAv1, JkAnime y TioAnime; los metadatos se complementan con AniList, Jikan y TMDB.

## Módulos

- `app`: interfaz, reproducción y lógica de presentación.
- `domain`: modelos compartidos de la aplicación.
- `core:network`: scrapers y extracción de fuentes de video.
- `core:metadata`: proveedores y caché de metadatos.
- `core:common`: utilidades compartidas.
