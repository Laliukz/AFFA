package com.lkz.affa.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStreamReader
import java.time.Instant

class BackupService private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BackupService"
        private const val BACKUP_FOLDER = "AnimeFFA"
        private const val BACKUP_FILE = "backup.json"

        @Volatile
        private var instance: BackupService? = null

        fun getInstance(context: Context): BackupService {
            return instance ?: synchronized(this) {
                instance ?: BackupService(context.applicationContext).also { instance = it }
            }
        }
    }

    private fun getBackupDirectory(): File {
        val dir = context.getExternalFilesDir(null)
        val backupDir = File(dir, BACKUP_FOLDER)
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        return backupDir
    }

    suspend fun hasBackup(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(getBackupDirectory(), BACKUP_FILE)
            file.exists()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking backup existence", e)
            false
        }
    }

    suspend fun backup(): String? = withContext(Dispatchers.IO) {
        try {
            val dbHelper = DatabaseHelper.getInstance(context)
            val db = dbHelper.readableDatabase
            val prefs = context.getSharedPreferences("affa_storage", Context.MODE_PRIVATE)

            val historyArray = cursorToJsonArray(db.query("history", null, null, null, null, null, null))
            val favoritesArray = cursorToJsonArray(db.query("favorites", null, null, null, null, null, null))

            val prefsJson = JSONObject()
            prefs.all.forEach { (key, value) ->
                prefsJson.put(key, value)
            }

            val backupData = JSONObject().apply {
                put("version", 1)
                put("timestamp", Instant.now().toString())
                put("database", JSONObject().apply {
                    put("history", historyArray)
                    put("favorites", favoritesArray)
                })
                put("preferences", prefsJson)
            }

            val file = File(getBackupDirectory(), BACKUP_FILE)
            file.writeText(backupData.toString(2))

            Log.i(TAG, "Backup saved successfully to ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Error creating backup", e)
            null
        }
    }

    suspend fun restore(): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(getBackupDirectory(), BACKUP_FILE)
            if (!file.exists()) return@withContext false

            val content = file.readText()
            performRestore(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring backup", e)
            false
        }
    }

    suspend fun exportDataUserFolder(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val dbHelper = DatabaseHelper.getInstance(context)
            val db = dbHelper.readableDatabase
            val prefs = context.getSharedPreferences("affa_storage", Context.MODE_PRIVATE)

            val historyArray = cursorToJsonArray(db.query("history", null, null, null, null, null, null))
            val favoritesArray = cursorToJsonArray(db.query("favorites", null, null, null, null, null, null))

            val prefsJson = JSONObject()
            prefs.all.forEach { (key, value) ->
                prefsJson.put(key, value)
            }

            val backupData = JSONObject().apply {
                put("version", 1)
                put("timestamp", Instant.now().toString())
                put("database", JSONObject().apply {
                    put("history", historyArray)
                    put("favorites", favoritesArray)
                })
                put("preferences", prefsJson)
            }

            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(backupData.toString(2).toByteArray())
            }
            Log.i(TAG, "Backup exported successfully to Uri: $uri")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting backup", e)
            false
        }
    }

    suspend fun importDataUserFile(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val content = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                InputStreamReader(inputStream).readText()
            } ?: return@withContext false

            performRestore(content)
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring from user file", e)
            false
        }
    }

    private fun performRestore(jsonContent: String): Boolean {
        return try {
            val data = JSONObject(jsonContent)
            
            val prefs = context.getSharedPreferences("affa_storage", Context.MODE_PRIVATE)
            val prefsJson = data.optJSONObject("preferences")
            if (prefsJson != null) {
                val editor = prefs.edit()
                val keys = prefsJson.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    if (prefsJson.isNull(key)) continue
                    
                    when (val value = prefsJson.get(key)) {
                        is Boolean -> editor.putBoolean(key, value)
                        is Int -> editor.putInt(key, value)
                        is Long -> editor.putLong(key, value)
                        is Double -> editor.putFloat(key, value.toFloat())
                        is String -> editor.putString(key, value)
                    }
                }
                editor.apply()
            }

            val dbHelper = DatabaseHelper.getInstance(context)
            val db = dbHelper.writableDatabase
            val databaseJson = data.optJSONObject("database")
            
            if (databaseJson != null) {
                db.beginTransaction()
                try {
                    val history = databaseJson.optJSONArray("history")
                    if (history != null) {
                        insertJsonArrayToTable(db, "history", history)
                    }

                    val favorites = databaseJson.optJSONArray("favorites")
                    if (favorites != null) {
                        insertJsonArrayToTable(db, "favorites", favorites)
                    }
                    
                    try {
                        db.execSQL("DELETE FROM history WHERE anime_id NOT LIKE '%:%'")
                        db.execSQL("DELETE FROM favorites WHERE id NOT LIKE '%:%'")
                        db.execSQL("DELETE FROM episode_progress WHERE anime_id NOT LIKE '%:%'")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error cleaning up old records after restore", e)
                    }
                    
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
            }
            
            Log.i(TAG, "Backup restored successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error performing restore", e)
            false
        }
    }

    private fun cursorToJsonArray(cursor: Cursor): JSONArray {
        val array = JSONArray()
        cursor.use {
            if (it.moveToFirst()) {
                do {
                    val obj = JSONObject()
                    for (i in 0 until it.columnCount) {
                        val columnName = it.getColumnName(i)
                        when (it.getType(i)) {
                            Cursor.FIELD_TYPE_INTEGER -> obj.put(columnName, it.getLong(i))
                            Cursor.FIELD_TYPE_FLOAT -> obj.put(columnName, it.getDouble(i))
                            Cursor.FIELD_TYPE_STRING -> obj.put(columnName, it.getString(i))
                            Cursor.FIELD_TYPE_BLOB -> { /* Ignore blobs for JSON export */ }
                            Cursor.FIELD_TYPE_NULL -> obj.put(columnName, JSONObject.NULL)
                        }
                    }
                    array.put(obj)
                } while (it.moveToNext())
            }
        }
        return array
    }

    private fun insertJsonArrayToTable(db: SQLiteDatabase, tableName: String, array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            
            val idKey = if (tableName == "favorites") "id" else "anime_id"
            if (obj.has(idKey)) {
                val oldId = obj.getString(idKey)
                if (!oldId.contains(":")) {
                    obj.put(idKey, "anilist:$oldId")
                }
            }
            
            val cv = ContentValues()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (obj.isNull(key)) {
                    cv.putNull(key)
                } else {
                    when (val value = obj.get(key)) {
                        is Int -> cv.put(key, value)
                        is Long -> cv.put(key, value)
                        is Double -> cv.put(key, value)
                        is Boolean -> cv.put(key, value)
                        is String -> cv.put(key, value)
                    }
                }
            }
            db.insertWithOnConflict(tableName, null, cv, SQLiteDatabase.CONFLICT_REPLACE)
        }
    }
}
