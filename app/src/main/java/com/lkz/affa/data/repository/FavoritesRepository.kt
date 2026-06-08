package com.lkz.affa.data.repository

import android.content.ContentValues
import android.content.Context
import com.lkz.affa.data.local.DatabaseHelper
import com.lkz.affa.data.models.Content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class FavoritesRepository(private val context: Context) {
    companion object {
        private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
        val favoriteIds: StateFlow<Set<String>> = _favoriteIds.asStateFlow()
        private var isInitialized = false
    }

    private val dbHelper = DatabaseHelper.getInstance(context)

    init {
        if (!isInitialized) {
            isInitialized = true
            GlobalScope.launch(Dispatchers.IO) {
                reloadFavoriteIds()
            }
        }
    }

    private fun reloadFavoriteIds() {
        val db = dbHelper.readableDatabase
        val cursor = db.query("favorites", arrayOf("id"), null, null, null, null, null)
        val ids = mutableSetOf<String>()
        while (cursor.moveToNext()) {
            ids.add(cursor.getString(0))
        }
        cursor.close()
        _favoriteIds.value = ids
    }

    suspend fun addFavorite(content: Content, status: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", content.id)
            put("title", content.title)
            put("image", content.imageUrl)
            put("total_episodes", content.episodesCount ?: 0)
            put("status", status)
            put("release_status", content.releaseStatus)
            put("next_airing_episode", content.nextAiringEpisode)
            put("added_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("favorites", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
        reloadFavoriteIds()
    }

    suspend fun removeFavorite(id: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("favorites", "id = ?", arrayOf(id))
        reloadFavoriteIds()
    }

    suspend fun getFavorites(status: String? = null): List<Content> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val selection = if (status != null && status != "Todos") "status = ?" else null
        val selectionArgs = if (status != null && status != "Todos") arrayOf(status) else null

        val query = if (selection != null) {
            "SELECT f.*, (SELECT last_episode_number FROM history h WHERE h.anime_id = f.id) as history_ep, (SELECT COUNT(*) FROM episode_progress ep WHERE ep.anime_id = f.id AND ep.is_watched IN (1, '1', 'true')) as prog_count FROM favorites f WHERE $selection ORDER BY f.added_at DESC"
        } else {
            "SELECT f.*, (SELECT last_episode_number FROM history h WHERE h.anime_id = f.id) as history_ep, (SELECT COUNT(*) FROM episode_progress ep WHERE ep.anime_id = f.id AND ep.is_watched IN (1, '1', 'true')) as prog_count FROM favorites f ORDER BY f.added_at DESC"
        }

        val cursor = db.rawQuery(query, selectionArgs)

        val list = mutableListOf<Content>()
        with(cursor) {
            while (moveToNext()) {
                val id = getString(getColumnIndexOrThrow("id"))
                val title = getString(getColumnIndexOrThrow("title"))
                val image = getString(getColumnIndexOrThrow("image"))
                val totalEpisodes = getInt(getColumnIndexOrThrow("total_episodes"))
                val itemStatus = getString(getColumnIndexOrThrow("status"))
                val releaseStatusIndex = getColumnIndex("release_status")
                val releaseStatus = if (releaseStatusIndex != -1 && !isNull(releaseStatusIndex)) getString(releaseStatusIndex) else null
                
                val nextAiringIndex = getColumnIndex("next_airing_episode")
                val nextAiring = if (nextAiringIndex != -1 && !isNull(nextAiringIndex)) getInt(nextAiringIndex) else 0
                
                val historyEpIndex = getColumnIndex("history_ep")
                val historyEpStr = if (historyEpIndex != -1 && !isNull(historyEpIndex)) getString(historyEpIndex) else null
                val lastEpNumber = historyEpStr?.replace(Regex("[^0-9]"), "")?.toIntOrNull() ?: 0
                
                val progCountIndex = getColumnIndex("prog_count")
                val progCount = if (progCountIndex != -1) getInt(progCountIndex) else 0
                
                val watchedCount = maxOf(lastEpNumber, progCount)

                val isReleasing = releaseStatus?.uppercase() == "RELEASING"
                val maxAvailable = if (isReleasing && nextAiring > 0) nextAiring - 1 else totalEpisodes
                val remaining = if (maxAvailable > 0) maxAvailable - watchedCount else 0
                
                val displayLabel = if (itemStatus != "Completado" && remaining > 0) {
                    "+$remaining"
                } else null

                list.add(
                    Content(
                        id = id,
                        title = title,
                        imageUrl = image,
                        episodesCount = totalEpisodes,
                        label = displayLabel,
                        releaseStatus = releaseStatus,
                        nextAiringEpisode = if (nextAiring > 0) nextAiring else null
                    )
                )
            }
        }
        cursor.close()
        list.distinctBy { it.id }.distinctBy { it.title }
    }

    suspend fun isFavorite(id: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("favorites", arrayOf("id"), "id = ?", arrayOf(id), null, null, null)
        val exists = cursor.count > 0
        cursor.close()
        exists
    }
    
    suspend fun getFavoriteStatus(id: String): String? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query("favorites", arrayOf("status"), "id = ?", arrayOf(id), null, null, null)
        var status: String? = null
        if (cursor.moveToFirst()) {
            status = cursor.getString(cursor.getColumnIndexOrThrow("status"))
        }
        cursor.close()
        status
    }
    
    suspend fun updateTotalEpisodes(id: String, totalEpisodes: Int) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("total_episodes", totalEpisodes)
        }
        db.update("favorites", values, "id = ?", arrayOf(id))
    }

    suspend fun updateReleaseStatus(id: String, releaseStatus: String?, nextAiring: Int?) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            if (releaseStatus != null) put("release_status", releaseStatus)
            if (nextAiring != null) put("next_airing_episode", nextAiring)
        }
        if (values.size() > 0) {
            db.update("favorites", values, "id = ?", arrayOf(id))
        }
    }

    /**
     * Sincroniza el estado de los animes en base a su progreso.
     * Viendo -> Completado si:
     *   - release_status = FINISHED y total_episodes > 0 y watchedCount >= total_episodes
     *   - O: total_episodes > 0 y watchedCount >= total_episodes (independiente del release_status)
     */
    suspend fun syncAutoCompletions() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        
        val cursor = db.query("favorites", arrayOf("id", "total_episodes", "release_status", "status"), "status IN (?, ?)", arrayOf("Viendo", "Completado"), null, null, null)
        val toComplete = mutableListOf<String>()
        val toWatching = mutableListOf<String>()
        
        with(cursor) {
            while (moveToNext()) {
                val id = getString(0)
                val totalEpisodes = getInt(1)
                val releaseStatus = if (!isNull(2)) getString(2) else null
                val currentStatus = getString(3)
                
                val cHist = db.rawQuery("SELECT last_episode_number FROM history WHERE anime_id = ?", arrayOf(id))
                var lastEpNumber = 0
                if (cHist.moveToFirst() && !cHist.isNull(0)) {
                    val epStr = cHist.getString(0)
                    lastEpNumber = epStr.replace(Regex("[^0-9]"), "").toIntOrNull() ?: 0
                }
                cHist.close()

                val cProg = db.rawQuery("SELECT COUNT(*) FROM episode_progress WHERE anime_id = ? AND is_watched IN (1, '1', 'true')", arrayOf(id))
                var progCount = 0
                if (cProg.moveToFirst()) progCount = cProg.getInt(0)
                cProg.close()

                val watchedCount = maxOf(lastEpNumber, progCount)
                
                val isFinished = releaseStatus?.uppercase()?.let { 
                    it == "FINISHED" || it == "ENDED" || it == "FINALIZADO"
                } ?: false

                android.util.Log.d("SyncAutoComplete", "Anime=$id | status=$currentStatus | release=$releaseStatus | isFinished=$isFinished | totalEps=$totalEpisodes | lastEp=$lastEpNumber | progCount=$progCount | watchedCount=$watchedCount")
                
                val shouldBeCompleted = totalEpisodes > 0 && watchedCount >= totalEpisodes && (isFinished || releaseStatus == null)
                
                if (currentStatus == "Viendo" && shouldBeCompleted) {
                    android.util.Log.i("SyncAutoComplete", ">>> Moving '$id' from Viendo -> Completado")
                    toComplete.add(id)
                } else if (currentStatus == "Completado" && !shouldBeCompleted && totalEpisodes > 0) {
                    android.util.Log.i("SyncAutoComplete", ">>> Reverting '$id' from Completado -> Viendo")
                    toWatching.add(id)
                }
            }
        }
        cursor.close()
        
        var hasUpdates = false
        
        if (toComplete.isNotEmpty()) {
            val values = ContentValues().apply { put("status", "Completado") }
            val placeholders = toComplete.joinToString(",") { "?" }
            val updatedRows = db.update("favorites", values, "id IN ($placeholders)", toComplete.toTypedArray())
            android.util.Log.i("SyncAutoComplete", "Auto-completed $updatedRows anime(s)")
            if (updatedRows > 0) hasUpdates = true
        }

        if (toWatching.isNotEmpty()) {
            val values = ContentValues().apply { put("status", "Viendo") }
            val placeholders = toWatching.joinToString(",") { "?" }
            val updatedRows = db.update("favorites", values, "id IN ($placeholders)", toWatching.toTypedArray())
            android.util.Log.i("SyncAutoComplete", "Auto-reverted $updatedRows anime(s) to Viendo")
            if (updatedRows > 0) hasUpdates = true
        }

        if (hasUpdates) {
            reloadFavoriteIds()
        }
    }
}
