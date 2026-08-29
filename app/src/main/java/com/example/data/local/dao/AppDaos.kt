package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PrayerDao {
    @Query("SELECT * FROM prayer_records WHERE dayKey = :dayKey")
    fun getPrayersForDay(dayKey: String): Flow<List<PrayerRecordEntity>>

    @Query("SELECT * FROM prayer_records WHERE dayKey = :dayKey")
    suspend fun getPrayersForDaySync(dayKey: String): List<PrayerRecordEntity>

    @Query("SELECT * FROM prayer_records WHERE dayKey IN (:dayKeys)")
    suspend fun getPrayersForDaysSync(dayKeys: List<String>): List<PrayerRecordEntity>

    @Query("SELECT * FROM prayer_records WHERE id = :id LIMIT 1")
    suspend fun getPrayerById(id: String): PrayerRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: PrayerRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<PrayerRecordEntity>)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habit_records WHERE dayKey = :dayKey")
    fun getHabitsForDay(dayKey: String): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habit_records WHERE dayKey = :dayKey")
    suspend fun getHabitsForDaySync(dayKey: String): List<HabitRecordEntity>

    @Query("SELECT * FROM habit_records WHERE dayKey IN (:dayKeys)")
    suspend fun getHabitsForDaysSync(dayKeys: List<String>): List<HabitRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: HabitRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<HabitRecordEntity>)
}

@Dao
interface CounterDao {
    @Query("SELECT * FROM counter_records WHERE dayKey = :dayKey")
    fun getCountersForDay(dayKey: String): Flow<List<CounterRecordEntity>>

    @Query("SELECT * FROM counter_records WHERE dayKey = :dayKey")
    suspend fun getCountersForDaySync(dayKey: String): List<CounterRecordEntity>

    @Query("SELECT * FROM counter_records WHERE dayKey IN (:dayKeys)")
    suspend fun getCountersForDaysSync(dayKeys: List<String>): List<CounterRecordEntity>

    @Query("SELECT * FROM counter_records WHERE id = :id LIMIT 1")
    suspend fun getCounterById(id: String): CounterRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(record: CounterRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<CounterRecordEntity>)
}

@Dao
interface ReflectionDao {
    @Query("SELECT * FROM daily_reflections WHERE dayKey = :dayKey LIMIT 1")
    fun getReflectionForDay(dayKey: String): Flow<DailyReflectionEntity?>

    @Query("SELECT * FROM daily_reflections WHERE dayKey = :dayKey LIMIT 1")
    suspend fun getReflectionForDaySync(dayKey: String): DailyReflectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(reflection: DailyReflectionEntity)
}

@Dao
interface WeeklyReportDao {
    @Query("SELECT * FROM weekly_reports ORDER BY weekKey DESC")
    fun getAllWeeklyReports(): Flow<List<WeeklyReportEntity>>

    @Query("SELECT * FROM weekly_reports WHERE weekKey = :weekKey LIMIT 1")
    fun getWeeklyReport(weekKey: String): Flow<WeeklyReportEntity?>

    @Query("SELECT * FROM weekly_reports WHERE weekKey = :weekKey LIMIT 1")
    suspend fun getWeeklyReportSync(weekKey: String): WeeklyReportEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(report: WeeklyReportEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(report: WeeklyReportEntity)
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSync(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettingsEntity)
}
