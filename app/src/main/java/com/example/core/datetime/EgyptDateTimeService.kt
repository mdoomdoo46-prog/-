package com.example.core.datetime

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import java.util.Locale

/**
 * Centralized Date & Time Service strictly operating in "Africa/Cairo" timezone.
 * All application logic (day reset at 00:00, week reset at Friday 00:00,
 * canonical day keys, and weekly boundary detection) MUST use this service.
 */
object EgyptDateTimeService {
    val CAIRO_ZONE_ID: ZoneId = ZoneId.of("Africa/Cairo")
    val DATE_KEY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.US)
    val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a", Locale("ar"))
    val FULL_DATE_FORMATTER_AR: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE، d MMMM yyyy", Locale("ar"))
    val SHORT_DATE_FORMATTER_AR: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale("ar"))

    /**
     * Gets the current ZonedDateTime in Africa/Cairo.
     */
    fun getNowCairo(): ZonedDateTime {
        return ZonedDateTime.now(CAIRO_ZONE_ID)
    }

    /**
     * Returns the canonical DayKey for today in Africa/Cairo (e.g. "2026-08-29").
     */
    fun getTodayKey(): String {
        return getNowCairo().format(DATE_KEY_FORMATTER)
    }

    /**
     * Converts a LocalDate or ZonedDateTime to canonical DayKey string.
     */
    fun toDayKey(date: LocalDate): String {
        return date.format(DATE_KEY_FORMATTER)
    }

    fun toDayKey(zonedDateTime: ZonedDateTime): String {
        val cairoTime = zonedDateTime.withZoneSameInstant(CAIRO_ZONE_ID)
        return cairoTime.format(DATE_KEY_FORMATTER)
    }

    /**
     * Parses a canonical DayKey back to LocalDate.
     */
    fun parseDayKey(dayKey: String): LocalDate {
        return LocalDate.parse(dayKey, DATE_KEY_FORMATTER)
    }

    /**
     * Calculates the Canonical WeekKey (Friday 00:00 start) for a given date in Africa/Cairo.
     * Week definition:
     * - Starts Friday 00:00:00 Africa/Cairo
     * - Ends Thursday 23:59:59 Africa/Cairo
     * If the date is Friday, weekKey is that Friday.
     * If the date is Saturday, Sunday, ..., Thursday, weekKey is the preceding Friday.
     */
    fun getWeekKeyForDate(date: LocalDate): String {
        val friday = if (date.dayOfWeek == DayOfWeek.FRIDAY) {
            date
        } else {
            date.with(TemporalAdjusters.previous(DayOfWeek.FRIDAY))
        }
        return toDayKey(friday)
    }

    /**
     * Returns the WeekKey for today in Africa/Cairo.
     */
    fun getCurrentWeekKey(): String {
        return getWeekKeyForDate(getNowCairo().toLocalDate())
    }

    /**
     * Returns the end date (Thursday) of a given weekKey.
     */
    fun getWeekEndForWeekKey(weekKey: String): String {
        val startFriday = parseDayKey(weekKey)
        val endThursday = startFriday.plusDays(6)
        return toDayKey(endThursday)
    }

    /**
     * Returns the 7 canonical day keys (Friday through Thursday) for a given weekKey.
     */
    fun getDaysInWeek(weekKey: String): List<String> {
        val startFriday = parseDayKey(weekKey)
        return (0..6).map { offset ->
            toDayKey(startFriday.plusDays(offset.toLong()))
        }
    }

    /**
     * Returns the Friday 00:00 ZonedDateTime representing the start of a given weekKey.
     */
    fun getWeekStartZonedDateTime(weekKey: String): ZonedDateTime {
        val localDate = parseDayKey(weekKey)
        return ZonedDateTime.of(localDate, LocalTime.MIN, CAIRO_ZONE_ID)
    }

    /**
     * Returns the next Friday 00:00 ZonedDateTime representing the end boundary of the week.
     */
    fun getNextFridayMidnight(weekKey: String): ZonedDateTime {
        val startZdt = getWeekStartZonedDateTime(weekKey)
        return startZdt.plusWeeks(1)
    }

    /**
     * Checks if a given weekKey is expired relative to current Cairo time (i.e. next Friday 00:00 has passed).
     */
    fun isWeekExpired(weekKey: String, nowCairo: ZonedDateTime = getNowCairo()): Boolean {
        val nextFridayMidnight = getNextFridayMidnight(weekKey)
        return nowCairo.isAfter(nextFridayMidnight) || nowCairo.isEqual(nextFridayMidnight)
    }

    /**
     * Identifies all expired week keys between a last known week and current week that have passed.
     * Never skips missed weeks even if the user didn't open the app for months.
     */
    fun getUnfinalizedExpiredWeekKeys(lastFinalizedWeekKey: String?, currentWeekKey: String): List<String> {
        if (lastFinalizedWeekKey.isNullOrEmpty()) {
            // First time or no previous week, no historical week before current to finalize
            return emptyList()
        }

        val result = mutableListOf<String>()
        var cursor = parseDayKey(lastFinalizedWeekKey).plusWeeks(1)
        val currentWeekDate = parseDayKey(currentWeekKey)

        while (cursor.isBefore(currentWeekDate)) {
            val key = toDayKey(cursor)
            result.add(key)
            cursor = cursor.plusWeeks(1)
        }

        // Also check if lastFinalizedWeekKey itself was recorded or if we should check it
        return result
    }

    /**
     * Formats an Arabic day name for a given DayKey (e.g. "الجمعة", "السبت").
     */
    fun getArabicDayName(dayKey: String): String {
        val date = parseDayKey(dayKey)
        return when (date.dayOfWeek) {
            DayOfWeek.FRIDAY -> "الجمعة"
            DayOfWeek.SATURDAY -> "السبت"
            DayOfWeek.SUNDAY -> "الأحد"
            DayOfWeek.MONDAY -> "الإثنين"
            DayOfWeek.TUESDAY -> "الثلاثاء"
            DayOfWeek.WEDNESDAY -> "الأربعاء"
            DayOfWeek.THURSDAY -> "الخميس"
        }
    }

    /**
     * Returns full formatted Arabic date string for today or a given dayKey.
     */
    fun formatArabicFullDate(dayKey: String): String {
        val date = parseDayKey(dayKey)
        val zdt = ZonedDateTime.of(date, LocalTime.NOON, CAIRO_ZONE_ID)
        return zdt.format(FULL_DATE_FORMATTER_AR)
    }

    /**
     * Returns short formatted Arabic date range for a weekKey (e.g. "29 أغسطس - 4 سبتمبر").
     */
    fun formatArabicWeekRange(weekKey: String): String {
        val start = parseDayKey(weekKey)
        val end = start.plusDays(6)
        val startZdt = ZonedDateTime.of(start, LocalTime.NOON, CAIRO_ZONE_ID)
        val endZdt = ZonedDateTime.of(end, LocalTime.NOON, CAIRO_ZONE_ID)
        return "${startZdt.format(SHORT_DATE_FORMATTER_AR)} - ${endZdt.format(SHORT_DATE_FORMATTER_AR)}"
    }

    /**
     * Formats timestamp in milliseconds to Arabic time.
     */
    fun formatArabicTimeFromMillis(millis: Long): String {
        val instant = Instant.ofEpochMilli(millis)
        val zdt = instant.atZone(CAIRO_ZONE_ID)
        return zdt.format(TIME_FORMATTER)
    }
}
