package com.lkz.affa.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lkz.affa.MainActivity
import com.lkz.affa.R
import com.lkz.affa.data.remote.TodaysEpisodesService
import com.lkz.affa.data.repository.FavoritesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EpisodeSyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val TAG = "EpisodeSyncWorker"
        const val CHANNEL_ID = "new_episodes_channel"
        const val PREFS_NAME = "episode_sync_prefs"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting EpisodeSyncWorker...")
            val appPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            if (!appPrefs.getBoolean("enable_notifications", true)) {
                Log.d(TAG, "Notifications are disabled in settings. Exiting worker.")
                return@withContext Result.success()
            }
            
            val repository = FavoritesRepository(context)
            
            val watching = repository.getFavorites("Viendo")
            if (watching.isEmpty()) {
                Log.d(TAG, "No watching anime found. Exiting worker.")
                return@withContext Result.success()
            }

            val todaysEpisodes = TodaysEpisodesService.instance.fetchTodaysEpisodeTitles()
            if (todaysEpisodes.isEmpty()) {
                Log.d(TAG, "No episodes released today on providers.")
                return@withContext Result.success()
            }

            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var notifiedCount = 0

            fun normalize(value: String): String {
                var cleaned = value.lowercase()
                cleaned = cleaned.replace(Regex("""\(.*?\)"""), " ")
                cleaned = cleaned.replace(Regex("""\[.*?\]"""), " ")
                cleaned = cleaned.replace(Regex("""[^a-z0-9]+"""), " ")
                cleaned = cleaned.replace(Regex("""\s+"""), " ").trim()
                return cleaned
            }

            val todayStr = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())

            for (anime in watching) {
                val title = anime.title ?: continue
                val normalizedTitle = normalize(title)

                if (todaysEpisodes.any { it == normalizedTitle || normalizedTitle.contains(it) || it.contains(normalizedTitle) }) {
                    val animeId = anime.id ?: continue
                    val lastNotifiedDate = prefs.getString("notified_$animeId", "")
                    
                    if (lastNotifiedDate != todayStr) {
                        sendNotification(anime.title)
                        
                        prefs.edit().putString("notified_$animeId", todayStr).apply()
                        notifiedCount++
                        
                        if (notifiedCount >= 3) {
                            break
                        }
                    }
                }
            }

            Log.d(TAG, "EpisodeSyncWorker completed successfully. Notified $notifiedCount new episodes.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in EpisodeSyncWorker", e)
            Result.retry()
        }
    }

    private fun sendNotification(animeTitle: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && 
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        createNotificationChannel()

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Make sure this icon exists
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText("¡Nuevo episodio disponible! El anime $animeTitle tiene un nuevo capítulo.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(animeTitle.hashCode(), builder.build())
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Nuevos Episodios"
            val descriptionText = "Notificaciones de nuevos episodios de tus animes en progreso."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
