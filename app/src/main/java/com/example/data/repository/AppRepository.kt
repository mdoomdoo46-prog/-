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
    val dayRecordDao = db.dayRecordDao()

    /**
     * Initializes all records for the given day if not already initialized.
     * Guaranteed to be idempotent and never delete or reset existing records.
     */
    suspend fun ensureDayInitialized(dayKey: String, prayerTimes: DailyPrayerTimes) {
        // 0. Ensure DayRecord exists
        dayRecordDao.insertIfAbsent(
            DayRecordEntity(
                dayKey = dayKey,
                isFinalized = false,
                createdAt = System.currentTimeMillis()
            )
        )

        // 1. Prayers initialization - only insert missing
        val existingPrayers = prayerDao.getPrayersForDaySync(dayKey)
        val existingPrayerKeys = existingPrayers.map { it.prayer }.toSet()
        val defaultPrayers = listOf(
            PrayerType.FAJR to prayerTimes.fajr,
            PrayerType.DHUHR to prayerTimes.dhuhr,
            PrayerType.ASR to prayerTimes.asr,
            PrayerType.MAGHRIB to prayerTimes.maghrib,
            PrayerType.ISHA to prayerTimes.isha
        )
        val missingPrayers = defaultPrayers.filter { it.first.name !in existingPrayerKeys }.map { (type, time) ->
            PrayerRecordEntity(
                id = "${dayKey}_${type.name}",
                dayKey = dayKey,
                prayer = type.name,
                scheduledTime = time,
                status = PrayerStatus.UNRECORDED.name
            )
        }
        if (missingPrayers.isNotEmpty()) {
            prayerDao.insertAll(missingPrayers)
        }

        // 2. Daily Habits initialization - only insert missing
        val existingHabits = habitDao.getHabitsForDaySync(dayKey)
        val existingHabitKeys = existingHabits.map { it.habitKey }.toSet()
        val missingHabits = DefaultHabits.ALL_DAILY_HABITS.filter { it.key !in existingHabitKeys }.map { def ->
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
        if (missingHabits.isNotEmpty()) {
            habitDao.insertAll(missingHabits)
        }

        // 3. Counters initialization - only insert missing
        val existingCounters = counterDao.getCountersForDaySync(dayKey)
        val existingCounterKeys = existingCounters.map { it.counterKey }.toSet()
        val missingCounters = DefaultHabits.ALL_COUNTERS.filter { it.key !in existingCounterKeys }.map { def ->
            CounterRecordEntity(
                id = "${dayKey}_${def.key}",
                dayKey = dayKey,
                counterKey = def.key,
                titleArabic = def.titleArabic,
                count = 0,
                target = def.defaultTarget
            )
        }
        if (missingCounters.isNotEmpty()) {
            counterDao.insertAll(missingCounters)
        }

        // Update settings lastActiveDayKey
        val settings = userSettingsDao.getSettingsSync()
        if (settings != null) {
            userSettingsDao.saveSettings(settings.copy(lastActiveDayKey = dayKey))
        }
    }

    // DayRecord Operations
    fun getDayRecordFlow(dayKey: String): Flow<DayRecordEntity?> = dayRecordDao.getDayRecordFlow(dayKey)
    suspend fun getDayRecordSync(dayKey: String): DayRecordEntity? = dayRecordDao.getDayRecordSync(dayKey)

    data class PreviousDayReviewState(
        val dayKey: String,
        val dateFormattedArabic: String,
        val unrecordedPrayers: List<PrayerRecordEntity>,
        val incompleteHabits: List<HabitRecordEntity>,
        val needsReview: Boolean
    )

    suspend fun getPreviousDayReviewState(todayKey: String): PreviousDayReviewState? {
        val todayDate = try {
            EgyptDateTimeService.parseDayKey(todayKey)
        } catch (e: Exception) {
            return null
        }
        val yesterdayDate = todayDate.minusDays(1)
        val yesterdayKey = EgyptDateTimeService.toDayKey(yesterdayDate)

        val dayRecord = dayRecordDao.getDayRecordSync(yesterdayKey)
        val yesterdayPrayers = prayerDao.getPrayersForDaySync(yesterdayKey)
        val yesterdayHabits = habitDao.getHabitsForDaySync(yesterdayKey)

        // If yesterday has no records in DB at all (fresh install today), no review needed
        if (dayRecord == null && yesterdayPrayers.isEmpty() && yesterdayHabits.isEmpty()) {
            return null
        }

        // If yesterday is already marked finalized, no review needed
        if (dayRecord?.isFinalized == true) {
            return null
        }

        val unrecordedPrayers = yesterdayPrayers.filter { it.status == PrayerStatus.UNRECORDED.name }
        val incompleteHabits = yesterdayHabits.filter { !it.isCompleted }

        val needsReview = unrecordedPrayers.isNotEmpty() || incompleteHabits.isNotEmpty()
        if (!needsReview) {
            // Automatically mark finalized if everything was already recorded
            dayRecordDao.insertOrUpdate(
                DayRecordEntity(
                    dayKey = yesterdayKey,
                    isFinalized = true,
                    finalizedAt = System.currentTimeMillis()
                )
            )
            return null
        }

        val arabicDateStr = EgyptDateTimeService.formatArabicFullDate(yesterdayKey)
        return PreviousDayReviewState(
            dayKey = yesterdayKey,
            dateFormattedArabic = arabicDateStr,
            unrecordedPrayers = unrecordedPrayers,
            incompleteHabits = incompleteHabits,
            needsReview = true
        )
    }

    suspend fun finalizeDay(
        dayKey: String,
        prayerUpdates: Map<String, PrayerStatus> = emptyMap(),
        habitUpdates: Map<String, Boolean> = emptyMap()
    ) {
        // Apply prayer updates
        val existingPrayers = prayerDao.getPrayersForDaySync(dayKey)
        for (prayer in existingPrayers) {
            val updatedStatus = prayerUpdates[prayer.prayer]
            if (updatedStatus != null) {
                prayerDao.insertOrUpdate(
                    prayer.copy(
                        status = updatedStatus.name,
                        recordedAt = System.currentTimeMillis()
                    )
                )
            } else if (prayer.status == PrayerStatus.UNRECORDED.name) {
                prayerDao.insertOrUpdate(
                    prayer.copy(
                        status = PrayerStatus.MISSED.name,
                        reason = "لم تسجل في وقتها",
                        recordedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        // Apply habit updates
        val existingHabits = habitDao.getHabitsForDaySync(dayKey)
        for (habit in existingHabits) {
            val isCompleted = habitUpdates[habit.habitKey]
            if (isCompleted != null) {
                habitDao.insertOrUpdate(
                    habit.copy(
                        isCompleted = isCompleted,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }

        // Mark as finalized in daily_records
        val existingRecord = dayRecordDao.getDayRecordSync(dayKey)
        val finalizedRecord = (existingRecord ?: DayRecordEntity(dayKey = dayKey)).copy(
            isFinalized = true,
            finalizedAt = System.currentTimeMillis()
        )
        dayRecordDao.insertOrUpdate(finalizedRecord)
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

    suspend fun toggleNotifications(enabled: Boolean) {
        val current = userSettingsDao.getSettingsSync() ?: UserSettingsEntity()
        userSettingsDao.saveSettings(
            current.copy(notificationsEnabled = enabled)
        )
    }
}

