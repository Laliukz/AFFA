package com.lkz.affa.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "afa.db"
        private const val DATABASE_VERSION = 10

        @Volatile private var instance: DatabaseHelper? = null

        fun getInstance(context: Context): DatabaseHelper {
            return instance
                    ?: synchronized(this) {
                        instance
                                ?: DatabaseHelper(context.applicationContext).also { instance = it }
                    }
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
                """
            CREATE TABLE favorites (
                id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                image TEXT,
                total_episodes INTEGER,
                status TEXT,
                release_status TEXT,
                next_airing_episode INTEGER,
                added_at INTEGER NOT NULL
            )
        """.trimIndent()
        )

        db.execSQL(
                """
            CREATE TABLE history (
                anime_id TEXT PRIMARY KEY,
                last_episode_id TEXT NOT NULL,
                last_episode_number TEXT,
                position_seconds INTEGER NOT NULL,
                duration_seconds INTEGER,
                anime_title TEXT,
                anime_image TEXT,
                total_episodes INTEGER,
                status TEXT,
                updated_at INTEGER NOT NULL
            )
        """.trimIndent()
        )

        db.execSQL(
                """
            CREATE TABLE episode_progress (
                anime_id TEXT NOT NULL,
                episode_id TEXT NOT NULL,
                episode_number TEXT,
                position_seconds INTEGER NOT NULL,
                duration_seconds INTEGER,
                is_watched INTEGER DEFAULT 0,
                updated_at INTEGER NOT NULL,
                PRIMARY KEY (anime_id, episode_id)
            )
        """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            try {
                db.execSQL("ALTER TABLE history ADD COLUMN anime_title TEXT")
            } catch (e: Exception) {}
            try {
                db.execSQL("ALTER TABLE history ADD COLUMN anime_image TEXT")
            } catch (e: Exception) {}
        }
        if (oldVersion < 3) {
            try {
                db.execSQL("ALTER TABLE history ADD COLUMN duration_seconds INTEGER")
            } catch (e: Exception) {}
        }
        if (oldVersion < 4) {
            try {
                db.execSQL(
                        """
                    CREATE TABLE IF NOT EXISTS episode_progress (
                        anime_id TEXT NOT NULL,
                        episode_id TEXT NOT NULL,
                        episode_number TEXT,
                        position_seconds INTEGER NOT NULL,
                        duration_seconds INTEGER,
                        is_watched INTEGER DEFAULT 0,
                        updated_at INTEGER NOT NULL,
                        PRIMARY KEY (anime_id, episode_id)
                    )
                """.trimIndent()
                )
            } catch (e: Exception) {}
        }
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE favorites ADD COLUMN total_episodes INTEGER")
            } catch (e: Exception) {}
            try {
                db.execSQL("ALTER TABLE favorites ADD COLUMN status TEXT")
            } catch (e: Exception) {}
        }
        if (oldVersion < 6) {
            try {
                db.execSQL("ALTER TABLE history ADD COLUMN total_episodes INTEGER")
            } catch (e: Exception) {}
            try {
                db.execSQL("ALTER TABLE history ADD COLUMN status TEXT")
            } catch (e: Exception) {}
        }
        if (oldVersion < 8) {
            try {
                db.execSQL("ALTER TABLE favorites ADD COLUMN release_status TEXT")
            } catch (e: Exception) {}
        }
        if (oldVersion < 9) {
            try {
                db.execSQL("INSERT OR REPLACE INTO history (anime_id, last_episode_id, last_episode_number, position_seconds, duration_seconds, anime_title, anime_image, total_episodes, status, updated_at) SELECT 'anilist:' || anime_id, last_episode_id, last_episode_number, position_seconds, duration_seconds, anime_title, anime_image, total_episodes, status, updated_at FROM history WHERE anime_id NOT LIKE '%:%'")
                db.execSQL("DELETE FROM history WHERE anime_id NOT LIKE '%:%'")
                
                db.execSQL("INSERT OR REPLACE INTO favorites (id, title, image, total_episodes, status, release_status, added_at) SELECT 'anilist:' || id, title, image, total_episodes, status, release_status, added_at FROM favorites WHERE id NOT LIKE '%:%'")
                db.execSQL("DELETE FROM favorites WHERE id NOT LIKE '%:%'")
                
                db.execSQL("INSERT OR REPLACE INTO episode_progress (anime_id, episode_id, episode_number, position_seconds, duration_seconds, is_watched, updated_at) SELECT 'anilist:' || anime_id, episode_id, episode_number, position_seconds, duration_seconds, is_watched, updated_at FROM episode_progress WHERE anime_id NOT LIKE '%:%'")
                db.execSQL("DELETE FROM episode_progress WHERE anime_id NOT LIKE '%:%'")
            } catch (e: Exception) {}
        }
        if (oldVersion < 10) {
            try {
                db.execSQL("ALTER TABLE favorites ADD COLUMN next_airing_episode INTEGER")
            } catch (e: Exception) {}
        }
    }
}
