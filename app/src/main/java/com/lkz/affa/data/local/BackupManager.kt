package com.lkz.affa.data.local

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    suspend fun exportData(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val dbHelper = DatabaseHelper.getInstance(context)
            val db = dbHelper.readableDatabase

            val historyArray = JSONArray()
            val historyCursor = db.query("history", null, null, null, null, null, null)
            with(historyCursor) {
                while (moveToNext()) {
                    val obj = JSONObject().apply {
                        put("anime_id", getString(getColumnIndexOrThrow("anime_id")))
                        put("last_episode_id", getString(getColumnIndexOrThrow("last_episode_id")))
                        put("last_episode_number", getString(getColumnIndexOrThrow("last_episode_number")))
                        put("position_seconds", getInt(getColumnIndexOrThrow("position_seconds")))
                        put("duration_seconds", getInt(getColumnIndexOrThrow("duration_seconds")))
                        put("anime_title", getString(getColumnIndexOrThrow("anime_title")))
                        put("anime_image", getString(getColumnIndexOrThrow("anime_image")))
                        put("total_episodes", getInt(getColumnIndexOrThrow("total_episodes")))
                        put("status", getString(getColumnIndexOrThrow("status")))
                        put("updated_at", getLong(getColumnIndexOrThrow("updated_at")))
                    }
                    historyArray.put(obj)
                }
            }
            historyCursor.close()

            val favoritesArray = JSONArray()
            val favoritesCursor = db.query("favorites", null, null, null, null, null, null)
            with(favoritesCursor) {
                while (moveToNext()) {
                    val obj = JSONObject().apply {
                        put("id", getString(getColumnIndexOrThrow("id")))
                        put("title", getString(getColumnIndexOrThrow("title")))
                        put("image", getString(getColumnIndexOrThrow("image")))
                        put("total_episodes", getInt(getColumnIndexOrThrow("total_episodes")))
                        put("status", getString(getColumnIndexOrThrow("status")))
                        put("added_at", getLong(getColumnIndexOrThrow("added_at")))
                    }
                    favoritesArray.put(obj)
                }
            }
            favoritesCursor.close()

            val databaseObj = JSONObject().apply {
                put("history", historyArray)
                put("favorites", favoritesArray)
            }

            val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val prefsObj = JSONObject().apply {
                put("user_avatar_url", sharedPrefs.getString("user_avatar_url", "assets/images/Avatar2.png"))
                put("user_name", sharedPrefs.getString("user_name", "Lkz"))
                put("is_app_initialized", sharedPrefs.getBoolean("is_app_initialized", true))
            }

            val rootObj = JSONObject().apply {
                put("version", 1)
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSS", Locale.US)
                put("timestamp", sdf.format(Date()))
                put("database", databaseObj)
                put("preferences", prefsObj)
            }

            val jsonString = rootObj.toString(2)

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
                outputStream.flush()
            } ?: return@withContext Result.failure(Exception("Cannot open OutputStream"))

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun importData(context: Context, uri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).readText()
            } ?: return@withContext Result.failure(Exception("Cannot open InputStream"))

            val rootObj = JSONObject(jsonString)
            val dbObj = rootObj.optJSONObject("database") ?: return@withContext Result.failure(Exception("No database object found"))

            val dbHelper = DatabaseHelper.getInstance(context)
            val db = dbHelper.writableDatabase

            db.beginTransaction()
            try {
                val historyArray = dbObj.optJSONArray("history")
                if (historyArray != null) {
                    for (i in 0 until historyArray.length()) {
                        val item = historyArray.getJSONObject(i)
                        val animeId = item.getString("anime_id")
                        val newEpisodeNum = item.optString("last_episode_number", "0").toIntOrNull() ?: 0

                        var existingEpisodeNum = -1
                        val cursor = db.query("history", arrayOf("last_episode_number"), "anime_id = ?", arrayOf(animeId), null, null, null)
                        if (cursor.moveToFirst()) {
                            existingEpisodeNum = cursor.getString(0)?.toIntOrNull() ?: 0
                        }
                        cursor.close()

                        if (newEpisodeNum >= existingEpisodeNum) {
                            val values = ContentValues().apply {
                                put("anime_id", animeId)
                                put("last_episode_id", item.optString("last_episode_id", ""))
                                put("last_episode_number", item.optString("last_episode_number", ""))
                                put("position_seconds", item.optInt("position_seconds", 0))
                                put("duration_seconds", item.optInt("duration_seconds", 0))
                                put("anime_title", item.optString("anime_title", ""))
                                put("anime_image", item.optString("anime_image", ""))
                                put("total_episodes", item.optInt("total_episodes", 0))
                                put("status", item.optString("status", ""))
                                put("updated_at", item.optLong("updated_at", System.currentTimeMillis()))
                            }
                            db.insertWithOnConflict("history", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                        }
                    }
                }

                val favoritesArray = dbObj.optJSONArray("favorites")
                if (favoritesArray != null) {
                    for (i in 0 until favoritesArray.length()) {
                        val item = favoritesArray.getJSONObject(i)
                        
                        var userStatus = item.optString("status", "")
                        val originalStatus = userStatus
                        if (userStatus == "FINISHED" || userStatus == "RELEASING" || userStatus.isEmpty() || userStatus == "NOT_YET_RELEASED" || userStatus == "CANCELLED") {
                            val totalEps = item.optInt("total_episodes", 0)
                            var epsWatched = 0
                            
                            val cursor = db.query("history", arrayOf("last_episode_number"), "anime_id = ?", arrayOf(item.getString("id")), null, null, null)
                            if (cursor.moveToFirst()) {
                                epsWatched = cursor.getString(0)?.toIntOrNull() ?: 0
                            }
                            cursor.close()
                            
                            userStatus = when {
                                epsWatched > 0 && totalEps > 0 && epsWatched >= totalEps && originalStatus != "RELEASING" -> "Completado"
                                epsWatched > 0 -> "Viendo"
                                else -> "Planeado"
                            }
                        }

                        val values = ContentValues().apply {
                            put("id", item.getString("id"))
                            put("title", item.optString("title", ""))
                            put("image", item.optString("image", ""))
                            put("total_episodes", item.optInt("total_episodes", 0))
                            put("status", userStatus)
                            put("added_at", item.optLong("added_at", System.currentTimeMillis()))
                        }
                        db.insertWithOnConflict("favorites", null, values, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
                    }
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }

            val prefsObj = rootObj.optJSONObject("preferences")
            if (prefsObj != null) {
                val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                with(sharedPrefs.edit()) {
                    if (prefsObj.has("user_avatar_url")) putString("user_avatar_url", prefsObj.getString("user_avatar_url"))
                    if (prefsObj.has("user_name")) putString("user_name", prefsObj.getString("user_name"))
                    if (prefsObj.has("is_app_initialized")) putBoolean("is_app_initialized", prefsObj.getBoolean("is_app_initialized"))
                    apply()
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
