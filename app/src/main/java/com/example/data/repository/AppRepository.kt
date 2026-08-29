package com.example.data.repository

import com.example.core.datetime.EgyptDateTimeService
import com.example.core.prayer.DailyPrayerTimes
import com.example.core.prayer.EgyptPrayerTimesEngine
import com.example.core.prayer.EgyptianCity
import com.example.core.prayer.PrayerStatus
import com.example.core.prayer.PrayerType
import com.example.data.local.AppDatabase
import com.example.data.local.entities.*
import com.example.domain.models.DefaultHabits
import kotlinx.coroutines.flow.Flow

class AppRepository(private val db: AppDatabase) {

    val prayerDao = db.prayerDao()
    val habitDao = db.habitDao()
    val counterDao = db.counterDao()
    val reflectionDao = db.reflectionDao()
    val weeklyReportDao = db.weeklyReportDao()
    val userSettingsDao = db.userSettingsDao()

    /**
     * Initializes all records for today if not already initialized.
     */
    suspend fun ensureDayInitialized(dayKey: String, prayerTimes: DailyPrayerTimes) {
        // 1. Prayers initialization
        val existingPrayers = prayerDao.getPrayersForDaySync(dayKey)
        if (existingPrayers.isEmpty()) {
            val defaultPrayers = listOf(
                PrayerRecordEntity(
                    id = "${dayKey}_FAJR",
                    dayKey = dayKey,
                    prayer = PrayerType.FAJR.name,
                    scheduledTime = prayerTimes.fajr,
                    status = PrayerStatus.UNRECORDED.name
                ),
                PrayerRecordEntity(
                    id = "${dayKey}_DHUHR",
                    dayKey = dayKey,
                    prayer = PrayerType.DHUHR.name,
                    scheduledTime = prayerTimes.dhuhr,
                    status = PrayerStatus.UNRECORDED.name
                ),
                PrayerRecordEntity(
                    id = "${dayKey}_ASR",
                    dayKey = dayKey,
                    prayer = PrayerType.ASR.name,
                    scheduledTime = prayerTimes.asr,
                    status = PrayerStatus.UNRECORDED.name
                ),
                PrayerRecordEntity(
                    id = "${dayKey}_MAGHRIB",
                    dayKey = dayKey,
                    prayer = PrayerType.MAGHRIB.name,
                    scheduledTime = prayerTimes.maghrib,
                    status = PrayerStatus.UNRECORDED.name
                ),
                PrayerRecordEntity(
                    id = "${dayKey}_ISHA",
                    dayKey = dayKey,
                    prayer = PrayerType.ISHA.name,
                    scheduledTime = prayerTimes.isha,
                    status = PrayerStatus.UNRECORDED.name
                )
            )
            prayerDao.insertAll(defaultPrayers)
        }

        // 2. Daily Habits initialization
        val existingHabits = habitDao.getHabitsForDaySync(dayKey)
        if (existingHabits.isEmpty()) {
            val habits = DefaultHabits.ALL_DAILY_HABITS.map { def ->
                HabitRecordEntity(
                    id = "${dayKey}_${def.key}",
                    dayKey = dayKey,
                    habitKey = def.key,
                    titleArabic = def.titleArabic,
                    isCompleted = false,
                    currentValue = 0,
                    targetValue = def.defaultTarget,
                    unitArabic = def.unitArabic
                )
            }
            habitDao.insertAll(habits)
        }

        // 3. Counters initialization
        val existingCounters = counterDao.getCountersForDaySync(dayKey)
        if (existingCounters.isEmpty()) {
            val counters = DefaultHabits.ALL_COUNTERS.map { def ->
                CounterRecordEntity(
                    id = "${dayKey}_${def.key}",
                    dayKey = dayKey,
                    counterKey = def.key,
                    titleArabic = def.titleArabic,
                    count = 0,
                    target = def.defaultTarget
                )
            }
            counterDao.insertAll(counters)
        }

        // Update settings lastActiveDayKey
        val settings = userSettingsDao.getSettingsSync()
        if (settings != null) {
            userSettingsDao.saveSettings(settings.copy(lastActiveDayKey = dayKey))
        }
    }

    // Prayer Operations
    fun getPrayersFlow(dayKey: String): Flow<List<PrayerRecordEntity>> = prayerDao.getPrayersForDay(dayKey)

    suspend fun updatePrayerStatus(
        dayKey: String,
        prayer: String,
        status: PrayerStatus,
        reason: String? = null,
        customReason: String? = null,
        scheduledTime: String = ""
    ) {
        val id = "${dayKey}_$prayer"
        val record = PrayerRecordEntity(
            id = id,
            dayKey = dayKey,
            prayer = prayer,
            scheduledTime = scheduledTime,
            status = status.name,
            reason = reason,
            customReason = customReason,
            recordedAt = System.currentTimeMillis()
        )
        prayerDao.insertOrUpdate(record)
    }

    // Habit Operations
    fun getHabitsFlow(dayKey: String): Flow<List<HabitRecordEntity>> = habitDao.getHabitsForDay(dayKey)

    suspend fun toggleHabit(dayKey: String, habitKey: String, isCompleted: Boolean, notes: String? = null) {
        val id = "${dayKey}_$habitKey"
        val existing = habitDao.getHabitsForDaySync(dayKey).find { it.habitKey == habitKey }
        val updated = existing?.copy(
            isCompleted = isCompleted,
            notes = notes ?: existing.notes,
            updatedAt = System.currentTimeMillis()
        ) ?: HabitRecordEntity(
            id = id,
            dayKey = dayKey,
            habitKey = habitKey,
            titleArabic = DefaultHabits.ALL_DAILY_HABITS.find { it.key == habitKey }?.titleArabic ?: habitKey,
            isCompleted = isCompleted,
            notes = notes
        )
        habitDao.insertOrUpdate(updated)
    }

    // Counter Operations
    fun getCountersFlow(dayKey: String): Flow<List<CounterRecordEntity>> = counterDao.getCountersForDay(dayKey)

    suspend fun incrementCounter(dayKey: String, counterKey: String, amount: Int = 1): CounterRecordEntity {
        val id = "${dayKey}_$counterKey"
        val existing = counterDao.getCounterById(id)
        val def = DefaultHabits.ALL_COUNTERS.find { it.key == counterKey }
        val target = existing?.target ?: (def?.defaultTarget ?: 100)
        val title = existing?.titleArabic ?: (def?.titleArabic ?: counterKey)
        val newCount = (existing?.count ?: 0) + amount

        val updated = CounterRecordEntity(
            id = id,
            dayKey = dayKey,
            counterKey = counterKey,
            titleArabic = title,
            count = newCount,
            target = target,
            updatedAt = System.currentTimeMillis()
        )
        counterDao.insertOrUpdate(updated)
        return updated
    }

    suspend fun resetCounter(dayKey: String, counterKey: String) {
        val id = "${dayKey}_$counterKey"
        val existing = counterDao.getCounterById(id) ?: return
        counterDao.insertOrUpdate(existing.copy(count = 0, updatedAt = System.currentTimeMillis()))
    }

    // Reflection Operations
    fun getReflectionFlow(dayKey: String): Flow<DailyReflectionEntity?> = reflectionDao.getReflectionForDay(dayKey)

    suspend fun saveReflection(
        dayKey: String,
        struggledHabit: String?,
        struggleReason: String?,
        customReason: String?,
        note: String?
    ) {
        val entity = DailyReflectionEntity(
            dayKey = dayKey,
            isCompleted = true,
            struggledHabit = struggledHabit,
            struggleReason = struggleReason,
            customReason = customReason,
            note = note,
            recordedAt = System.currentTimeMillis()
        )
        reflectionDao.insertOrUpdate(entity)
    }

    // Weekly Report Operations
    fun getAllWeeklyReportsFlow(): Flow<List<WeeklyReportEntity>> = weeklyReportDao.getAllWeeklyReports()

    fun getWeeklyReportFlow(weekKey: String): Flow<WeeklyReportEntity?> = weeklyReportDao.getWeeklyReport(weekKey)

    // User Settings Operations
    fun getUserSettingsFlow(): Flow<UserSettingsEntity?> = userSettingsDao.getSettings()

    suspend fun completeOnboarding(cityName: String = "القاهرة", cityLat: Double = 30.0444, cityLng: Double = 31.2357) {
        val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity()
        userSettingsDao.saveSettings(
            current.copy(
                isOnboarded = true,
                selectedCity = cityName,
                cityLat = cityLat,
                cityLng = cityLng
            )
        )
    }

    suspend fun updateCity(city: EgyptianCity) {
        val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity()
        userSettingsDao.saveSettings(
            current.copy(
                selectedCity = city.nameArabic,
                cityLat = city.latitude,
                cityLng = city.longitude
            )
        )
    }

    suspend fun toggleDarkMode(isDark: Boolean?) {
        val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity()
        userSettingsDao.saveSettings(
            current.copy(isDarkMode = isDark)
        )
    }
}
