package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prayer_records",
    indices = [Index(value = ["dayKey"])]
)
data class PrayerRecordEntity(
    @PrimaryKey
    val id: String, // format: "2026-08-29_FAJR"
    val dayKey: String,
    val prayer: String, // FAJR, DHUHR, ASR, MAGHRIB, ISHA
    val scheduledTime: String,
    val status: String, // UNRECORDED, CONGREGATION, INDIVIDUAL, MISSED
    val reason: String? = null,
    val customReason: String? = null,
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habit_records",
    indices = [Index(value = ["dayKey"])]
)
data class HabitRecordEntity(
    @PrimaryKey
    val id: String, // format: "2026-08-29_QURAN_WIRD"
    val dayKey: String,
    val habitKey: String,
    val titleArabic: String,
    val isCompleted: Boolean = false,
    val currentValue: Int = 0,
    val targetValue: Int = 1,
    val unitArabic: String = "مرة",
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "counter_records",
    indices = [Index(value = ["dayKey"])]
)
data class CounterRecordEntity(
    @PrimaryKey
    val id: String, // format: "2026-08-29_ISTIGHFAR"
    val dayKey: String,
    val counterKey: String,
    val titleArabic: String,
    val count: Int = 0,
    val target: Int = 100,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "daily_reflections"
)
data class DailyReflectionEntity(
    @PrimaryKey
    val dayKey: String,
    val isCompleted: Boolean = false,
    val struggledHabit: String? = null,
    val struggleReason: String? = null,
    val customReason: String? = null,
    val note: String? = null,
    val recordedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "weekly_reports"
)
data class WeeklyReportEntity(
    @PrimaryKey
    val weekKey: String, // Friday YYYY-MM-DD
    val weekEnd: String, // Thursday YYYY-MM-DD
    val weeklyScore: Int = 0, // 0..100
    val totalPrayers: Int = 35,
    val recordedPrayers: Int = 0,
    val congregationPrayers: Int = 0,
    val individualPrayers: Int = 0,
    val missedPrayers: Int = 0,
    val unrecordedPrayers: Int = 35,
    val congregationRate: Float = 0f,
    val quranCompletionDays: Int = 0,
    val dhikrTotalCount: Int = 0,
    val habitsCompletionRate: Float = 0f,
    val mostMissedPrayer: String? = null,
    val mostCommonMissReason: String? = null,
    val mostCommonIndividualReason: String? = null,
    val patternInsight: String? = null,
    val recommendation: String? = null,
    val finalizedAt: Long = System.currentTimeMillis(),
    val isImmutable: Boolean = true
)

@Entity(
    tableName = "user_settings"
)
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val isOnboarded: Boolean = false,
    val selectedCity: String = "القاهرة",
    val cityLat: Double = 30.0444,
    val cityLng: Double = 31.2357,
    val notificationsEnabled: Boolean = true,
    val lastFinalizedWeekKey: String? = null,
    val lastActiveDayKey: String? = null,
    val isDarkMode: Boolean? = null
)
