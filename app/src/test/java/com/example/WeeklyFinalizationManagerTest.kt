package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.core.prayer.PrayerStatus
import com.example.core.prayer.PrayerType
import com.example.data.local.AppDatabase
import com.example.data.local.entities.HabitRecordEntity
import com.example.data.local.entities.PrayerRecordEntity
import com.example.domain.engine.WeeklyFinalizationManager
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WeeklyFinalizationManagerTest {

    private lateinit var db: AppDatabase
    private lateinit var manager: WeeklyFinalizationManager

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        manager = WeeklyFinalizationManager(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testEmptyWeekDoesNotInventActivity() = runBlocking {
        val weekKey = "2026-08-21"
        val report = manager.finalizeWeekIdempotent(weekKey)

        assertEquals(0, report.recordedPrayers)
        assertEquals(0, report.congregationPrayers)
        assertEquals(0, report.weeklyScore)
        assertEquals(35, report.unrecordedPrayers)
        assertTrue(report.isImmutable)
    }

    @Test
    fun testScoreCalculationAndImmutability() = runBlocking {
        val weekKey = "2026-08-21"
        val day1 = "2026-08-21"

        // Insert 5 congregation prayers for day 1
        val prayers = listOf(
            PrayerRecordEntity("${day1}_FAJR", day1, PrayerType.FAJR.name, "04:30", PrayerStatus.CONGREGATION.name),
            PrayerRecordEntity("${day1}_DHUHR", day1, PrayerType.DHUHR.name, "12:00", PrayerStatus.CONGREGATION.name),
            PrayerRecordEntity("${day1}_ASR", day1, PrayerType.ASR.name, "15:30", PrayerStatus.CONGREGATION.name),
            PrayerRecordEntity("${day1}_MAGHRIB", day1, PrayerType.MAGHRIB.name, "18:15", PrayerStatus.CONGREGATION.name),
            PrayerRecordEntity("${day1}_ISHA", day1, PrayerType.ISHA.name, "19:45", PrayerStatus.CONGREGATION.name)
        )
        db.prayerDao().insertAll(prayers)

        // Insert Quran completed
        db.habitDao().insertOrUpdate(
            HabitRecordEntity("${day1}_quran_wird", day1, "quran_wird", "ورد القرآن", isCompleted = true)
        )

        // Finalize week
        val snapshot = manager.finalizeWeekIdempotent(weekKey)
        val initialScore = snapshot.weeklyScore

        assertTrue(initialScore > 0)
        assertEquals(5, snapshot.congregationPrayers)
        assertEquals(5, snapshot.recordedPrayers)
        assertEquals(1, snapshot.quranCompletionDays)
        assertTrue(snapshot.isImmutable)

        // Add more prayers to the same day after finalization (simulating retroactive edits)
        db.prayerDao().insertOrUpdate(
            PrayerRecordEntity("2026-08-22_FAJR", "2026-08-22", PrayerType.FAJR.name, "04:30", PrayerStatus.CONGREGATION.name)
        )

        // Query again using idempotency check
        val recheckSnapshot = manager.finalizeWeekIdempotent(weekKey)
        // Score MUST remain immutable and identical
        assertEquals(initialScore, recheckSnapshot.weeklyScore)
        assertEquals(5, recheckSnapshot.congregationPrayers)
    }

    @Test
    fun testIdempotencyDoesNotDuplicateReports() = runBlocking {
        val weekKey = "2026-08-14"

        val report1 = manager.finalizeWeekIdempotent(weekKey)
        val report2 = manager.finalizeWeekIdempotent(weekKey)

        assertEquals(report1.weekKey, report2.weekKey)
        assertEquals(report1.finalizedAt, report2.finalizedAt)

        val allReports = db.weeklyReportDao().getWeeklyReportSync(weekKey)
        assertNotNull(allReports)
    }
}
