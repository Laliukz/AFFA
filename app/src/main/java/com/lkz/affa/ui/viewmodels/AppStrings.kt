package com.lkz.affa.ui.viewmodels

import androidx.compose.runtime.compositionLocalOf

class AppStrings(private val language: String) {
    val isEnglish: Boolean get() = language == "English"

    val appName: String get() = "Anime FFA"
    val cancel: String get() = if (isEnglish) "Cancel" else "Cancelar"
    val save: String get() = if (isEnglish) "Save" else "Guardar"
    val close: String get() = if (isEnglish) "Close" else "Cerrar"
    val info: String get() = if (isEnglish) "Info" else "Información"
    val edit: String get() = if (isEnglish) "Edit" else "Editar"
    val delete: String get() = if (isEnglish) "Delete" else "Eliminar"
    val undo: String get() = if (isEnglish) "Undo" else "Deshacer"

    val home: String get() = if (isEnglish) "Home" else "Inicio"
    val explore: String get() = if (isEnglish) "Explore" else "Explorar"
    val myList: String get() = if (isEnglish) "My List" else "Mi Lista"
    val account: String get() = if (isEnglish) "Account" else "Cuenta"

    val continueWatching: String get() = if (isEnglish) "Continue Watching" else "Continuar Viendo"
    val trending: String get() = if (isEnglish) "Trending" else "Tendencias"
    val mostPopular: String get() = if (isEnglish) "Most Popular" else "Más Populares"
    val newReleases: String get() = if (isEnglish) "New Releases" else "Nuevos Lanzamientos"
    val todayEpisodes: String get() = if (isEnglish) "Today's Episodes" else "Episodios de hoy"
    val yesterdayEpisodes: String get() = if (isEnglish) "Yesterday's Episodes" else "Episodios de ayer"
    val viewAll: String get() = if (isEnglish) "View All" else "Ver Todo"

    val myAccount: String get() = if (isEnglish) "My Account" else "Mi Cuenta"
    val content: String get() = if (isEnglish) "CONTENT" else "CONTENIDO"
    val settings: String get() = if (isEnglish) "SETTINGS" else "AJUSTES"
    val history: String get() = if (isEnglish) "History" else "Historial"
    val continueWatchingShort: String get() = if (isEnglish) "Continue watching" else "Continuar viendo"
    val appPreferences: String get() = if (isEnglish) "App Preferences" else "Preferencias de App"
    val editName: String get() = if (isEnglish) "Edit Name" else "Editar Nombre"
    val name: String get() = if (isEnglish) "Name" else "Nombre"
    val chooseAvatar: String get() = if (isEnglish) "Choose an Avatar" else "Elige un Avatar"
    val watched: String get() = if (isEnglish) "WATCHED" else "VISTOS"
    val inList: String get() = if (isEnglish) "IN LIST" else "EN LISTA"

    val all: String get() = if (isEnglish) "All" else "Todos"
    val watching: String get() = if (isEnglish) "Watching" else "Viendo"
    val planned: String get() = if (isEnglish) "Planned" else "Planeado"
    val completed: String get() = if (isEnglish) "Completed" else "Completado"
    val orderRecent: String get() = if (isEnglish) "RECENT" else "RECIENTES"
    val orderAZ: String get() = "A-Z"
    val orderRating: String get() = if (isEnglish) "RATING" else "PUNTUACIÓN"
    val orderBy: String get() = if (isEnglish) "ORDER" else "ORDEN"
    val sortBy: String get() = if (isEnglish) "Sort by" else "Ordenar por"
    val animes: String get() = "Animes"
    val listView: String get() = if (isEnglish) "List view" else "Vista de lista"
    val gridView: String get() = if (isEnglish) "Grid view" else "Vista de cuadrícula"
    val done: String get() = if (isEnglish) "Done" else "Listo"
    val editMode: String get() = if (isEnglish) "Edit" else "Editar"
    val yourListIsEmpty: String get() = if (isEnglish) "Your list is empty" else "Tu lista está vacía"
    val addAnimeFromDetails: String get() = if (isEnglish) "Add anime from the details screen" else "Agrega anime desde la pantalla de detalles"
    val noAnimesInCategory: String get() = if (isEnglish) "No animes in this category" else "No hay animes en esta categoría"
    val animesWillAppearByProgress: String get() = if (isEnglish) "Animes will appear here based on your progress" else "Los animes aparecerán aquí según tu progreso"
    
    fun removedFromList(title: String): String = if (isEnglish) "$title removed from My List" else "$title eliminado de Mi Lista"
    val listUpdated: String get() = if (isEnglish) "List updated" else "Lista actualizada"

    val settingsTitle: String get() = if (isEnglish) "Settings" else "Ajustes"
    val general: String get() = "GENERAL"
    val support: String get() = if (isEnglish) "SUPPORT" else "SOPORTE"
    val appLanguage: String get() = if (isEnglish) "App language" else "Idioma de la aplicación"
    val notifications: String get() = if (isEnglish) "Notifications" else "Notificaciones"
    val notificationsDesc: String get() = if (isEnglish) "New episodes of airing anime" else "Nuevos episodios de anime en emisión"
    val helpAndSupport: String get() = if (isEnglish) "Help & Support" else "Ayuda y Soporte"
    val privacyPolicy: String get() = if (isEnglish) "Privacy Policy" else "Política de Privacidad"
    val about: String get() = if (isEnglish) "About" else "Acerca de"
    val editProfile: String get() = if (isEnglish) "Edit profile" else "Editar perfil"
    val animeFan: String get() = if (isEnglish) "Anime Fan" else "Fan del Anime"
    val selectLanguage: String get() = if (isEnglish) "Select Language" else "Seleccionar Idioma"
    val dataBackup: String get() = if (isEnglish) "Data Backup" else "Copia de Seguridad"
    val exportData: String get() = if (isEnglish) "Export Data" else "Exportar Datos"
    val exportDataDesc: String get() = if (isEnglish) "Save a copy of your data" else "Guardar una copia de tus datos"
    val importData: String get() = if (isEnglish) "Import Data" else "Importar Datos"
    val importDataDesc: String get() = if (isEnglish) "Restore your data from a backup" else "Restaurar tus datos desde una copia"
    val backupSuccess: String get() = if (isEnglish) "Backup saved to:" else "Copia guardada en:"
    val backupError: String get() = if (isEnglish) "Error creating backup" else "Error al crear la copia"
    val restoreSuccess: String get() = if (isEnglish) "Restore successful! Restart app" else "¡Restauración exitosa! Reinicia la app"
    val restoreError: String get() = if (isEnglish) "Error restoring data" else "Error al restaurar los datos"
    val restoreConfirmTitle: String get() = if (isEnglish) "Import Data" else "Importar Datos"
    val restoreConfirmDesc: String get() = if (isEnglish) "This will replace your current data. Are you sure?" else "Esto reemplazará tus datos actuales. ¿Estás seguro?"
    val madeWithLove: String get() = if (isEnglish) "Made with ❤️ for anime fans" else "Hecho con ❤️ para fans del anime"

    val helpTitle: String get() = if (isEnglish) "Help & Support" else "Ayuda y Soporte"
    val helpQuestion: String get() = if (isEnglish) "Have problems or questions?" else "¿Tienes problemas o preguntas?"
    val helpContent: String get() = if (isEnglish) "• To report bugs, contact us by email\n• Visit our FAQ for frequently asked questions\n• Join our community on Discord" else "• Para reportar errores, contáctanos por email\n• Visita nuestro FAQ para preguntas frecuentes\n• Únete a nuestra comunidad en Discord"

    val privacyTitle: String get() = if (isEnglish) "Privacy Policy" else "Política de Privacidad"
    val privacySummary: String get() = if (isEnglish) "Summary of our policy:" else "Resumen de nuestra política:"
    val privacyContent: String get() = if (isEnglish) "• We don't collect personal data without consent\n• Your watch history is stored locally\n• We don't share information with third parties\n• You can delete your data at any time" else "• No recopilamos datos personales sin tu consentimiento\n• Tu historial de visualización se almacena localmente\n• No compartimos información con terceros\n• Puedes eliminar tus datos en cualquier momento"

    val aboutTitle: String get() = if (isEnglish) "About Anime FFA" else "Acerca de la app"
    val aboutName: String get() = "Anime FFA"
    val aboutDesc: String get() = if (isEnglish) "An app to enjoy your favorite anime anytime, anywhere." else "Una aplicación para disfrutar de tu anime favorito en cualquier momento y lugar."
    val version: String get() = if (isEnglish) "Version" else "Versión"

    val episodes: String get() = if (isEnglish) "Episodes" else "Episodios"
    val related: String get() = if (isEnglish) "Related" else "Relacionado"
    val addToList: String get() = if (isEnglish) "My List" else "Mi Lista"
    val inMyList: String get() = if (isEnglish) "In My List" else "En Mi Lista"
    val play: String get() = if (isEnglish) "Play" else "Reproducir"
    fun playEpisode(number: String): String = if (isEnglish) "Play Episode $number" else "Reproducir Episodio $number"
    val episode: String get() = if (isEnglish) "Episode" else "Episodio"
    val episodeWatched: String get() = if (isEnglish) "Watched" else "Visto"
    val description: String get() = if (isEnglish) "Description" else "Descripción"
    val noDescription: String get() = if (isEnglish) "No description available" else "Descripción no disponible"
    val seasons: String get() = if (isEnglish) "Seasons" else "Temporadas"
    
    fun addedToList(title: String): String = if (isEnglish) "$title added to My List" else "$title añadido a Mi Lista"
    fun removedFromListShort(title: String): String = if (isEnglish) "$title removed from My List" else "$title eliminado de Mi Lista"

    val removeFromHistory: String get() = if (isEnglish) "Remove from History" else "Eliminar del Historial"
    fun removeConfirmation(title: String): String = if (isEnglish) "Remove $title from continue watching?" else "¿Eliminar $title de continuar viendo?"

    val servers: String get() = if (isEnglish) "Servers" else "Servidores"
    val nextEpisode: String get() = if (isEnglish) "Next Episode" else "Siguiente Episodio"
    val previousEpisode: String get() = if (isEnglish) "Previous Episode" else "Episodio Anterior"
    val noServersAvailable: String get() = if (isEnglish) "No servers available" else "No hay servidores disponibles"
    val loadingStreams: String get() = if (isEnglish) "Loading streams..." else "Cargando streams..."

    val search: String get() = if (isEnglish) "Search" else "Buscar"
    val searchHint: String get() = if (isEnglish) "Search anime..." else "Buscar anime..."
    val noResults: String get() = if (isEnglish) "No results found" else "No se encontraron resultados"
    val results: String get() = if (isEnglish) "Results" else "Resultados"
    val filter: String get() = if (isEnglish) "Filter" else "Filtrar"
    val apply: String get() = if (isEnglish) "Apply" else "Aplicar"
    val clear: String get() = if (isEnglish) "Clear" else "Limpiar"

    val donate: String get() = if (isEnglish) "Donate" else "Donar"

    val genre: String get() = if (isEnglish) "Genre" else "Género"
    val year: String get() = if (isEnglish) "Year" else "Año"
    val status: String get() = if (isEnglish) "Status" else "Estado"

    val sortNameAZ: String get() = if (isEnglish) "Name (A-Z)" else "Nombre (A-Z)"
    val sortNameZA: String get() = if (isEnglish) "Name (Z-A)" else "Nombre (Z-A)"
    val sortNewest: String get() = if (isEnglish) "Newest" else "Más Recientes"
    val sortOldest: String get() = if (isEnglish) "Oldest" else "Más Antiguos"

    val statusFinished: String get() = if (isEnglish) "Finished" else "Finalizado"
    val statusReleasing: String get() = if (isEnglish) "Releasing" else "En Emisión"
    val statusNotYetReleased: String get() = if (isEnglish) "Not Yet Released" else "Próximamente"
    val statusCancelled: String get() = if (isEnglish) "Cancelled" else "Cancelado"
    val statusHiatus: String get() = if (isEnglish) "Hiatus" else "En Pausa"

    val allOption: String get() = if (isEnglish) "All" else "Todos"

    val genreMap: Map<String, String> get() = mapOf(
        "Action" to if (isEnglish) "Action" else "Acción",
        "Adventure" to if (isEnglish) "Adventure" else "Aventura",
        "Comedy" to if (isEnglish) "Comedy" else "Comedia",
        "Drama" to "Drama",
        "Ecchi" to "Ecchi",
        "Fantasy" to if (isEnglish) "Fantasy" else "Fantasía",
        "Horror" to "Horror",
        "Mahou Shoujo" to if (isEnglish) "Mahou Shoujo" else "Chica Mágica",
        "Mecha" to "Mecha",
        "Music" to if (isEnglish) "Music" else "Música",
        "Mystery" to if (isEnglish) "Mystery" else "Misterio",
        "Psychological" to if (isEnglish) "Psychological" else "Psicológico",
        "Romance" to "Romance",
        "Sci-Fi" to if (isEnglish) "Sci-Fi" else "Ciencia Ficción",
        "Slice of Life" to if (isEnglish) "Slice of Life" else "Recuentos de la vida",
        "Sports" to if (isEnglish) "Sports" else "Deportes",
        "Supernatural" to if (isEnglish) "Supernatural" else "Sobrenatural",
        "Thriller" to "Thriller"
    )

    fun translateGenre(genre: String): String = genreMap[genre] ?: genre

    fun translateStatus(statusStr: String): String {
        return when (statusStr.uppercase()) {
            "FINISHED" -> statusFinished
            "RELEASING" -> statusReleasing
            "NOT_YET_RELEASED" -> statusNotYetReleased
            "CANCELLED" -> statusCancelled
            "HIATUS" -> statusHiatus
            else -> statusStr
        }
    }
}

val LocalAppStrings = compositionLocalOf { AppStrings("Español") }
