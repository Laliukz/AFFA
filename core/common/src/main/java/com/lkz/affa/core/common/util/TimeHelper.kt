package com.lkz.affa.core.common.util

import com.lkz.affa.domain.model.EpisodeTimeCategory
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeHelper {

    private val spanishDayNames = mapOf(
        "lunes" to 1, "martes" to 2, "miércoles" to 3,
        "jueves" to 4, "viernes" to 5, "sábado" to 6, "domingo" to 0
    )

    fun classifyByInstant(instant: Instant?, zone: ZoneId = ZoneId.systemDefault()): EpisodeTimeCategory {
        if (instant == null) return EpisodeTimeCategory.OLDER
        val episodeDate = instant.atZone(zone).toLocalDate()
        val today = LocalDate.now(zone)
        return when (episodeDate) {
            today -> EpisodeTimeCategory.TODAY
            today.minusDays(1) -> EpisodeTimeCategory.YESTERDAY
            else -> EpisodeTimeCategory.OLDER
        }
    }

    fun parseJkAnimeLabel(label: String): EpisodeTimeCategory {
        val lower = label.lowercase(java.util.Locale.forLanguageTag("es")).trim()
        return when {
            lower == "hoy" -> EpisodeTimeCategory.TODAY
            lower == "ayer" -> EpisodeTimeCategory.YESTERDAY
            else -> parseWeekdayLabel(lower)
        }
    }

    private fun parseWeekdayLabel(label: String): EpisodeTimeCategory {
        val parts = label.split(" ")
        if (parts.size < 2) return EpisodeTimeCategory.OLDER

        val dayName = parts[0]
        val dayNumber = parts[1].toIntOrNull() ?: return EpisodeTimeCategory.OLDER

        val targetDayOfWeek = spanishDayNames[dayName] ?: return EpisodeTimeCategory.OLDER

        val today = LocalDate.now()
        val currentDayOfWeek = today.dayOfWeek.value % 7

        var daysAgo = currentDayOfWeek - targetDayOfWeek
        if (daysAgo < 0) daysAgo += 7
        if (daysAgo > 6) daysAgo += 7

        val targetDate = today.minusDays(daysAgo.toLong())
        return if (targetDate.dayOfMonth == dayNumber) {
            when (daysAgo) {
                0 -> EpisodeTimeCategory.TODAY
                1 -> EpisodeTimeCategory.YESTERDAY
                else -> EpisodeTimeCategory.OLDER
            }
        } else {
            EpisodeTimeCategory.OLDER
        }
    }
}
