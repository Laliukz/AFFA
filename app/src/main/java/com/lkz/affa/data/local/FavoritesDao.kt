package com.lkz.affa.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FavoritesDao(private val dbHelper: DatabaseHelper) {

    suspend fun addFavorite(id: String, title: String, image: String?) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("id", id)
            put("title", title)
            put("image", image)
            put("added_at", System.currentTimeMillis())
        }
        db.insertWithOnConflict("favorites", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    suspend fun removeFavorite(id: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete("favorites", "id = ?", arrayOf(id))
    }

    suspend fun isFavorite(id: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "favorites",
            arrayOf("id"),
            "id = ?",
            arrayOf(id),
            null,
            null,
            null
        )
        val exists = cursor.count > 0
        cursor.close()
        return@withContext exists
    }

    suspend fun getFavorites(): List<Map<String, Any>> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "favorites",
            null,
            null,
            null,
            null,
            null,
            "added_at DESC"
        )
        
        val list = mutableListOf<Map<String, Any>>()
        with(cursor) {
            while (moveToNext()) {
                val map = mutableMapOf<String, Any>()
                map["id"] = getString(getColumnIndexOrThrow("id"))
                map["title"] = getString(getColumnIndexOrThrow("title"))
                
                val imageIndex = getColumnIndexOrThrow("image")
                if (!isNull(imageIndex)) {
                    map["image"] = getString(imageIndex)
                }
                
                map["added_at"] = getLong(getColumnIndexOrThrow("added_at"))
                list.add(map)
            }
            close()
        }
        return@withContext list
    }
}
