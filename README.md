# AFFA — Aplicación Android para explorar anime

<p align="center">
  <img src="https://developer.android.com/static/images/brand/Android_Robot.png" width="140" alt="Android" />
</p>

<p align="center">
  <strong>Aplicación Android desarrollada en Kotlin y Jetpack Compose para explorar anime, consultar episodios y reproducir contenido desde fuentes externas.</strong>
</p>

---

## Características

- **Exploración y búsqueda:** descubre títulos y consulta sus detalles y episodios.
- **Reproducción de episodios:** obtiene servidores disponibles desde las fuentes compatibles.
- **Lista personal:** guarda títulos para tenerlos a mano.
- **Seguimiento de episodios:** marca episodios como vistos; el estado se conserva localmente.
- **Información complementaria:** muestra metadatos obtenidos de proveedores externos.

---

## Arquitectura y módulos

El proyecto está organizado en módulos Gradle para separar la aplicación, los modelos de dominio y los componentes compartidos:

```text
affa/
├── app/                 # Interfaz Compose, reproducción y lógica de presentación
├── domain/              # Modelos de anime, episodios y video
└── core/
    ├── common/          # Utilidades compartidas
    ├── metadata/        # Proveedores y caché de metadatos
    └── network/         # Scrapers y extractores de video
```

La interfaz está construida con **Jetpack Compose** y **Material 3**. La aplicación consulta fuentes externas para obtener listados, detalles y servidores de reproducción.

---

## Fuentes de información

- **Anime:** scrapers para AnimeAv1, JkAnime y TioAnime.
- **Metadatos:** proveedores AniList, Jikan y TMDB.
- **Reproducción:** extractores para los servidores compatibles implementados en `core:network`.

La disponibilidad del catálogo, los episodios y los servidores depende de las fuentes externas.

---

## Tecnologías

| Tecnología | Uso |
| :--- | :--- |
| Kotlin | Lenguaje de desarrollo |
| Jetpack Compose | Interfaz de usuario declarativa |
| Material 3 | Componentes y estilos de interfaz |
| Coroutines y Flow | Operaciones asíncronas y estado observable |
| Retrofit | Acceso a APIs de metadatos |
| Jsoup | Lectura y análisis de páginas web |
| Media3 ExoPlayer | Reproducción de video |
| Coil | Carga de imágenes |

---

## Requisitos

- Android Studio y Android SDK.
- JDK 11.
- Android 7.0 (API 24) o posterior.

## Compilar

Desde la raíz del proyecto, ejecuta:

```bash
./gradlew assembleDebug
```

En Windows:

```powershell
./gradlew.bat assembleDebug
```
