package com.example

import android.content.Context
import androidx.room.Room
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.core.prayer.EgyptPrayerTimesEngine
import com.example.core.prayer.PrayerStatus
import com.example.core.prayer.PrayerType
import com.example.data.local.AppDatabase
import com.example.data.local.entities.DayRecordEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DayRecordAuditVerificationTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: AppRepository
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = AppRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    /**
     * Test A: لا يوجد DayRecord لليوم الحالي.
     * Expected: إنشاء واحد فقط.
     */
    @Test
    fun testA_noDayRecord_createsExactlyOne() = runBlocking {
        val todayKey = "2026-09-18"
        val times = EgyptPrayerTimesEngine.calculatePrayerTimes(todayKey)

        val beforeRecord = repository.getDayRecordSync(todayKey)
        assertNull("Before init, record should not exist", beforeRecord)

        repository.ensureDayInitialized(todayKey, times)

        val afterRecord = repository.getDayRecordSync(todayKey)
        assertNotNull("After init, record must exist", afterRecord)
        assertEquals(todayKey, afterRecord?.dayKey)
        assertFalse("New record must not be finalized", afterRecord?.isFinalized == true)

        val prayers = repository.prayerDao.getPrayersForDaySync(todayKey)
        val habits = repository.habitDao.getHabitsForDaySync(todayKey)

        assertEquals(5, prayers.size)
        assertTrue(habits.any { it.habitKey == "night_prayer" })
    }

    /**
     * Test B: يوجد DayRecord لليوم الحالي.
     * Expected: استخدامه بدون إنشاء سجل جديد وبدون تعديل ما سُجِّل.
     */
    @Test
    fun testB_existingDayRecord_usesItWithoutReset() = runBlocking {
        val todayKey = "2026-09-18"
        val times = EgyptPrayerTimesEngine.calculatePrayerTimes(todayKey)

        // First initialization
        repository.ensureDayInitialized(todayKey, times)

        // User records Fajr = CONGREGATION and Quran = Completed
        repository.updatePrayerStatus(todayKey, PrayerType.FAJR.name, PrayerStatus.CONGREGATION)
        repository.toggleHabit(todayKey, "quran_wird", true)
        repository.incrementCounter(todayKey, "counter_istighfar", 50)

        // Re-run ensureDayInitialized (simulating re-opening app in the same day)
        repository.ensureDayInitialized(todayKey, times)

        val prayers = repository.prayerDao.getPrayersForDaySync(todayKey)
        val fajr = prayers.find { it.prayer == PrayerType.FAJR.name }
        assertEquals(PrayerStatus.CONGREGATION.name, fajr?.status)

        val habits = repository.habitDao.getHabitsForDaySync(todayKey)
        val quran = habits.find { it.habitKey == "quran_wird" }
        assertTrue(quran?.isCompleted == true)

        val counters = repository.counterDao.getCountersForDaySync(todayKey)
        val istighfar = counters.find { it.counterKey == "counter_istighfar" }
        assertEquals(50, istighfar?.count)
    }

    /**
     * Test C: انتقال التاريخ من 18 → 19.
     * Expected: 18 محفوظ + 19 يتم إنشاؤه.
     */
    @Test
    fun testC_transitionFrom18To19_preserves18AndCreates19() = runBlocking {
        val day18 = "2026-09-18"
        val times18 = EgyptPrayerTimesEngine.calculatePrayerTimes(day18)
        repository.ensureDayInitialized(day18, times18)

        // Record on day 18
        repository.updatePrayerStatus(day18, PrayerType.FAJR.name, PrayerStatus.CONGREGATION)
        repository.updatePrayerStatus(day18, PrayerType.DHUHR.name, PrayerStatus.INDIVIDUAL)
        repository.toggleHabit(day18, "quran_wird", true)

        // Midnight arrives -> Day 19
        val day19 = "2026-09-19"
        val times19 = EgyptPrayerTimesEngine.calculatePrayerTimes(day19)
        repository.ensureDayInitialized(day19, times19)

        // Check day 18 is intact!
        val record18 = repository.getDayRecordSync(day18)
        assertNotNull("Day 18 record must still exist", record18)
        val prayers18 = repository.prayerDao.getPrayersForDaySync(day18)
        assertEquals(PrayerStatus.CONGREGATION.name, prayers18.find { it.prayer == PrayerType.FAJR.name }?.status)
        assertEquals(PrayerStatus.INDIVIDUAL.name, prayers18.find { it.prayer == PrayerType.DHUHR.name }?.status)
        val habits18 = repository.habitDao.getHabitsForDaySync(day18)
        assertTrue(habits18.find { it.habitKey == "quran_wird" }?.isCompleted == true)

        // Check day 19 is freshly initialized with UNRECORDED prayers
        val record19 = repository.getDayRecordSync(day19)
        assertNotNull("Day 19 record must exist", record19)
        val prayers19 = repository.prayerDao.getPrayersForDaySync(day19)
        assertEquals(5, prayers19.size)
        assertTrue("Day 19 prayers start fresh", prayers19.all { it.status == PrayerStatus.UNRECORDED.name })
    }

    /**
     * Test D: 18 غير Finalized وله مهام غير مسجلة.
     * Expected: Review Yesterday تظهر.
     */
    @Test
    fun testD_unfinalizedYesterday_showsReviewBanner() = runBlocking {
        val day18 = "2026-09-18"
        val times18 = EgyptPrayerTimesEngine.calculatePrayerTimes(day18)
        repository.ensureDayInitialized(day18, times18)
        repository.updatePrayerStatus(day18, PrayerType.FAJR.name, PrayerStatus.CONGREGATION)
        // Dhuhr, Asr, Maghrib, Isha still unrecorded

        val day19 = "2026-09-19"
        val reviewState = repository.getPreviousDayReviewState(day19)

        assertNotNull("Review state must exist for day 18", reviewState)
        assertEquals(day18, reviewState?.dayKey)
        assertTrue("Needs review must be true", reviewState?.needsReview == true)
        assertEquals(4, reviewState?.unrecordedPrayers?.size)
    }

    /**
     * Test E: 18 تم عمل Finalize له.
     * Expected: لا تظهر Review Yesterday مرة أخرى بلا سبب، والسجل يظل محفوظاً (Finalized لا يعني Delete).
     */
    @Test
    fun testE_finalizedYesterday_doesNotShowReviewAgainAndKeepsData() = runBlocking {
        val day18 = "2026-09-18"
        val times18 = EgyptPrayerTimesEngine.calculatePrayerTimes(day18)
        repository.ensureDayInitialized(day18, times18)

        val day19 = "2026-09-19"
        val times19 = EgyptPrayerTimesEngine.calculatePrayerTimes(day19)
        repository.ensureDayInitialized(day19, times19)

        // Finalize day 18 with prayer updates
        val prayerUpdates = mapOf(
            PrayerType.FAJR.name to PrayerStatus.CONGREGATION,
            PrayerType.DHUHR.name to PrayerStatus.INDIVIDUAL,
            PrayerType.ASR.name to PrayerStatus.MISSED,
            PrayerType.MAGHRIB.name to PrayerStatus.INDIVIDUAL,
            PrayerType.ISHA.name to PrayerStatus.CONGREGATION
        )
        val habitUpdates = mapOf(
            "quran_wird" to true,
            "night_prayer" to true
        )
        repository.finalizeDay(day18, prayerUpdates, habitUpdates)

        // Day 18 record is NOT deleted, but is marked finalized
        val record18 = repository.getDayRecordSync(day18)
        assertNotNull(record18)
        assertTrue("Day 18 is now finalized", record18?.isFinalized == true)

        // Now check if review banner shows for day 19
        val reviewState = repository.getPreviousDayReviewState(day19)
        assertNull("Finalized yesterday must not require review banner", reviewState)

        // Confirm day 19 was unaffected
        val prayers19 = repository.prayerDao.getPrayersForDaySync(day19)
        assertTrue(prayers19.all { it.status == PrayerStatus.UNRECORDED.name })
    }

    /**
     * Test F: فتح التطبيق عدة مرات في نفس اليوم.
     * Expected: لا Reset للبيانات، ونفس DayRecord يُستخدم.
     */
    @Test
    fun testF_openMultipleTimesSameDay_noReset() = runBlocking {
        val day = "2026-09-18"
        val times = EgyptPrayerTimesEngine.calculatePrayerTimes(day)

        // Open 1
        repository.ensureDayInitialized(day, times)
        repository.updatePrayerStatus(day, PrayerType.ASR.name, PrayerStatus.CONGREGATION)
        repository.incrementCounter(day, "counter_tasbih", 33)

        // Open 2
        repository.ensureDayInitialized(day, times)

        // Open 3
        repository.ensureDayInitialized(day, times)

        val prayers = repository.prayerDao.getPrayersForDaySync(day)
        assertEquals(PrayerStatus.CONGREGATION.name, prayers.find { it.prayer == PrayerType.ASR.name }?.status)

        val counters = repository.counterDao.getCountersForDaySync(day)
        assertEquals(33, counters.find { it.counterKey == "counter_tasbih" }?.count)
    }

    /**
     * Test G: إغلاق التطبيق وفتحه بعد منتصف الليل.
     * Expected: لا فقدان للبيانات.
     */
    @Test
    fun testG_closeAndOpenAfterMidnight_preservesOldData() = runBlocking {
        val day18 = "2026-09-18"
        val times18 = EgyptPrayerTimesEngine.calculatePrayerTimes(day18)
        repository.ensureDayInitialized(day18, times18)
        repository.updatePrayerStatus(day18, PrayerType.MAGHRIB.name, PrayerStatus.CONGREGATION)
        repository.toggleHabit(day18, "night_prayer", true)

        // App closed, reopened next day
        val day19 = "2026-09-19"
        val times19 = EgyptPrayerTimesEngine.calculatePrayerTimes(day19)
        repository.ensureDayInitialized(day19, times19)

        val habits18 = repository.habitDao.getHabitsForDaySync(day18)
        val nightPrayer18 = habits18.find { it.habitKey == "night_prayer" }
        assertTrue(nightPrayer18?.isCompleted == true)
    }

    /**
     * Test H: مراجعة يوم قديم.
     * Expected: بياناته كما تم تسجيلها وبقائها مستقلة.
     */
    @Test
    fun testH_reviewOldDay_dataRemainsAccurate() = runBlocking {
        val day18 = "2026-09-18"
        val times18 = EgyptPrayerTimesEngine.calculatePrayerTimes(day18)
        repository.ensureDayInitialized(day18, times18)
        repository.updatePrayerStatus(day18, PrayerType.FAJR.name, PrayerStatus.CONGREGATION)

        val day19 = "2026-09-19"
        val reviewState = repository.getPreviousDayReviewState(day19)
        assertNotNull(reviewState)
        assertEquals("2026-09-18", reviewState?.dayKey)
        // Fajr was recorded, so only 4 unrecorded
        assertEquals(4, reviewState?.unrecordedPrayers?.size)
        assertFalse(reviewState?.unrecordedPrayers?.any { it.prayer == PrayerType.FAJR.name } == true)
    }

    /**
     * Test I: Qiyam al-layl defaults.
     * Expected: قيام الليل 3 ركعات نافلة، عبادة اختيارية مستقلة.
     */
    @Test
    fun testI_qiyamAlLaylDefaults() = runBlocking {
        val day = "2026-09-18"
        val times = EgyptPrayerTimesEngine.calculatePrayerTimes(day)
        repository.ensureDayInitialized(day, times)

        val habits = repository.habitDao.getHabitsForDaySync(day)
        val qiyam = habits.find { it.habitKey == "night_prayer" }
        assertNotNull(qiyam)
        assertEquals("قيام الليل", qiyam?.titleArabic)
        assertEquals(3, qiyam?.targetValue)
        assertEquals("ركعات", qiyam?.unitArabic)
        assertFalse(qiyam?.isCompleted == true)
    }
}
