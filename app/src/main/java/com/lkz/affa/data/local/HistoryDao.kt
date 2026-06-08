package com.lkz.affa.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryDao(private val dbHelper: DatabaseHelper) {

    suspend fun saveProgress(
        animeId: String,
        episodeId: String,
        episodeNumber: String?,
        positionSeconds: Int,
        title: String? = null,
        imageUrl: String? = null,
        durationSeconds: Int? = null
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        
        var existingTitle: String? = title
        var existingImage: String? = imageUrl
        var existingDuration: Int? = durationSeconds
        
        if (title == null || imageUrl == null) {
            val cursor = db.query("history", arrayOf("anime_title", "anime_image", "duration_seconds"), "anime_id = ?", arrayOf(animeId), null, null, null)
            if (cursor.moveToFirst()) {
                if (existingTitle == null && !cursor.isNull(0)) existingTitle = cursor.getString(0)
                if (existingImage == null && !cursor.isNull(1)) existingImage = cursor.getString(1)
                if (existingDuration == null && !cursor.isNull(2)) existingDuration = cursor.getInt(2)
            }
            cursor.close()
        }
        
        val values = ContentValues().apply {
            put("anime_id", animeId)
            put("last_episode_id", episodeId)
            put("last_episode_number", episodeNumber)
            put("position_seconds", positionSeconds)
            if (existingTitle != null) put("anime_title", existingTitle)
            if (existingImage != null) put("anime_image", existingImage)
            if (existingDuration != null) put("duration_seconds", existingDuration)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("history", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        
        val progressValues = ContentValues().apply {
            put("anime_id", animeId)
            put("episode_id", episodeId)
            put("episode_number", episodeNumber)
            put("position_seconds", positionSeconds)
            put("duration_seconds", durationSeconds ?: existingDuration ?: 0)
            put("is_watched", if (positionSeconds == 0 && (durationSeconds == 0 || durationSeconds == null)) 1 else 0)
            put("updated_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("episode_progress", null, progressValues, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun removeEpisodeProgress(animeId: String, episodeId: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("episode_progress", "anime_id = ? AND episode_id = ?", arrayOf(animeId, episodeId))
    }

    suspend fun getWatchedEpisodes(animeId: String): Set<Int> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val result = mutableSetOf<Int>()
        val cursor = db.rawQuery(
            "SELECT episode_number FROM episode_progress WHERE anime_id = ? AND is_watched IN (1, '1', 'true')",
            arrayOf(animeId)
        )
        cursor.use {
            while (it.moveToNext()) {
                if (!it.isNull(0)) {
                    it.getString(0).toIntOrNull()?.let { num ->
                        result.add(num)
                    }
                }
            }
        }
        result
    }

    suspend fun getUnwatchedEpisodes(animeId: String): Set<Int> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val result = mutableSetOf<Int>()
        val cursor = db.rawQuery(
            "SELECT episode_number FROM episode_progress WHERE anime_id = ? AND is_watched IN (0, '0', 'false')",
            arrayOf(animeId)
        )
        cursor.use {
            while (it.moveToNext()) {
                if (!it.isNull(0)) {
                    it.getString(0).toIntOrNull()?.let { num ->
                        result.add(num)
                    }
                }
            }
        }
        result
    }

    suspend fun getProgress(animeId: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "history",
            null,
            "anime_id = ?",
            arrayOf(animeId),
            null,
            null,
            null
        )
        
        var result: Map<String, Any>? = null
        with(cursor) {
            if (moveToFirst()) {
                val map = mutableMapOf<String, Any>()
                map["anime_id"] = getString(getColumnIndexOrThrow("anime_id"))
                map["last_episode_id"] = getString(getColumnIndexOrThrow("last_episode_id"))
                
                val epNumIndex = getColumnIndexOrThrow("last_episode_number")
                if (!isNull(epNumIndex)) {
                    map["last_episode_number"] = getString(epNumIndex)
                }
                
                map["position_seconds"] = getInt(getColumnIndexOrThrow("position_seconds"))
                map["updated_at"] = getLong(getColumnIndexOrThrow("updated_at"))
                result = map
            }
            close()
        }
        return@withContext result
    }

    suspend fun getHistory(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "history",
            null,
            null,
            null,
            null,
            null,
            "updated_at DESC"
        )
        
        val list = mutableListOf<Map<String, Any>>()
        with(cursor) {
            while (moveToNext()) {
                val map = mutableMapOf<String, Any>()
                map["anime_id"] = getString(getColumnIndexOrThrow("anime_id"))
                map["last_episode_id"] = getString(getColumnIndexOrThrow("last_episode_id"))
                
                val epNumIndex = getColumnIndexOrThrow("last_episode_number")
                if (!isNull(epNumIndex)) {
                    map["last_episode_number"] = getString(epNumIndex)
                }
                
                
                map["position_seconds"] = getInt(getColumnIndexOrThrow("position_seconds"))
                
                val titleIndex = getColumnIndex("anime_title")
                if (titleIndex != -1 && !isNull(titleIndex)) {
                    map["anime_title"] = getString(titleIndex)
                }
                
                val imageIndex = getColumnIndex("anime_image")
                if (imageIndex != -1 && !isNull(imageIndex)) {
                    map["anime_image"] = getString(imageIndex)
                }

                val durationIndex = getColumnIndex("duration_seconds")
                if (durationIndex != -1 && !isNull(durationIndex)) {
                    map["duration_seconds"] = getInt(durationIndex)
                }
                
                map["updated_at"] = getLong(getColumnIndexOrThrow("updated_at"))
                list.add(map)
            }
            close()
        }
        return@withContext list
    }

    /** Returns the total number of distinct anime entries in history. */
    suspend fun getWatchedAnimeCount(): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM history", null)
        var count = 0
        cursor.use {
            if (it.moveToFirst()) count = it.getInt(0)
        }
        count
    }
}
