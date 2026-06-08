<h1 align="center">AFFA (Anime Free For All)</h1>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-green.svg?logo=android" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Media3-ExoPlayer-red.svg" alt="ExoPlayer">
  <img src="https://img.shields.io/badge/Architecture-MVVM-orange.svg" alt="Architecture">
</p>

<p align="center">
  AFFA es una aplicación nativa, rápida y moderna para Android orientada a la visualización y gestión de anime. Construida enteramente con <b>Jetpack Compose</b> y <b>Kotlin</b>, consume las APIs de AniList y TMDB para ofrecer una experiencia fluida, rica en información y visualmente inmersiva.
</p>

---

## 🌟 Características Principales

*   **Exploración de Catálogo:** Revisa animes en tendencia, populares, y nuevos lanzamientos gracias a la integración con AniList (GraphQL).
*   **Búsqueda Avanzada:** Filtra contenido por nombre, género, temporada, y estado de emisión.
*   **Detalles Ricos:** Sinopsis traducidas dinámicamente mediante TMDB, información de episodios, animes relacionados y recomendaciones.
*   **Reproductor Integrado:** Soporte nativo y acelerado por hardware usando **Media3 (ExoPlayer)**. Selecciona múltiples servidores y salta episodios rápidamente.
*   **Modo Oscuro & Material Design 3:** Interfaz moderna y adaptable diseñada bajo los estándares de Material 3.
*   **Manejo de Estados Persistentes:** Sigue tu progreso gracias a un gestor de preferencias locales y copias de seguridad de datos.

## 🛠️ Tecnologías y Arquitectura

Este proyecto fue desarrollado utilizando las mejores prácticas y el stack tecnológico recomendado por Google para Android moderno:

*   **[Kotlin](https://kotlinlang.org/):** Lenguaje principal.
*   **[Jetpack Compose](https://developer.android.com/jetpack/compose):** Toolkit UI declarativo moderno.
*   **[Media3 ExoPlayer](https://developer.android.com/media/media3/exoplayer):** Reproducción de video fluida con soporte HLS/M3U8.
*   **[Coil](https://coil-kt.github.io/coil/):** Carga de imágenes asíncrona optimizada para Compose.
*   **Arquitectura MVVM:** Separación clara entre Capa de UI (Pantallas y Componentes) y Capa de Datos (Repositorios, Servicios).
*   **Corrutinas y Flow:** Manejo de tareas asíncronas y programación reactiva.
*   **GraphQL y REST:** Consumo mixto de APIs. `AnilistService` (GraphQL) y `TmdbService` (REST).

### 📂 Estructura de Directorios

```text
com.lkz.affa
 ┣ 📂 data           # Capa de datos (Modelos, Repositorios, Servicios Remotos, Manejo Local)
 ┣ 📂 navigation     # Grafo de navegación de Compose
 ┗ 📂 ui             # Capa de presentación
   ┣ 📂 components   # Componentes atómicos (Cards, Dialogs, Headers, Players)
   ┣ 📂 screens      # Pantallas principales (Home, Details, VideoPlayer, Settings)
   ┣ 📂 theme        # Tipografías, colores y temas (Material 3)
   ┗ 📂 viewmodels   # Manejo de estado inyectado en las pantallas
```

## 🚀 Instalación y Uso

### Prerrequisitos

*   [Android Studio](https://developer.android.com/studio) (Versión recomendada: Ladybug o superior).
*   JDK 17.

### Pasos para compilar

1.  **Clona este repositorio:**
    ```bash
    git clone https://github.com/tu-usuario/AFFA.git
    ```
2.  **Abre el proyecto en Android Studio.**
3.  **Configura el SDK de Android:** Sincroniza el proyecto con los archivos de Gradle.
4.  **Ejecuta la app:** Selecciona un emulador o conecta tu dispositivo físico (con depuración USB habilitada) y pulsa en "Run" (Shift + F10).

### Configuración de API Keys

Para que la información de los animes cargue correctamente (como descripciones traducidas, posters HD, etc.), la app necesita una llave de la API de **TMDB**.

1. Abre el archivo `local.properties.example` que viene en el proyecto.
2. Renómbralo a `local.properties`.
3. Consigue tu API Key gratuita en [TMDB Settings](https://www.themoviedb.org/settings/api).
4. Pégala en el archivo bajo la variable `TMDB_API_KEY=tu_api_key`.

## 🤝 Contribución

Si deseas contribuir, mejorar el reproductor de video o añadir nuevas funcionalidades, ¡los PRs son bienvenidos!

1. Realiza un *Fork* del proyecto.
2. Crea una nueva rama para tu característica (`git checkout -b feature/NuevaCaracteristica`).
3. Realiza tus cambios y haz *commit* (`git commit -m 'feat: Añadir nueva característica'`).
4. Sube la rama (`git push origin feature/NuevaCaracteristica`).
5. Abre un *Pull Request*.

## ⚖️ Descargo de Responsabilidad

**AFFA (Anime Free For All)** no aloja, almacena ni distribuye ningún tipo de contenido multimedia con derechos de autor en sus repositorios ni servidores. 

La aplicación funciona exclusivamente como un cliente interactivo y motor de búsqueda que indexa enlaces de video provenientes de sitios públicos y proveedores de terceros en internet. Todo el contenido multimedia, marcas registradas, logos y derechos de autor mostrados pertenecen a sus respectivos dueños legales.

Cualquier reclamo o problema legal relacionado con la reproducción o distribución del contenido debe ser tratado directamente con los servicios de alojamiento web (hosts) externos que almacenan dichos archivos.

## 📄 Uso y Licencia

Este proyecto ha sido desarrollado con fines **puramente educativos y como demostración técnica para portafolio**. 

El código fuente está disponible bajo la Licencia MIT para que otros desarrolladores puedan estudiar la arquitectura y las implementaciones técnicas. No se ofrece soporte técnico ni se fomenta su uso para fines comerciales. Mira el archivo [LICENSE](LICENSE) para más detalles.
