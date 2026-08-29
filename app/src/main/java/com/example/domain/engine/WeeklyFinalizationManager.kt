package com.example.domain.engine

import com.example.core.datetime.EgyptDateTimeService
import com.example.core.prayer.PrayerStatus
import com.example.data.local.AppDatabase
import com.example.data.local.entities.PrayerRecordEntity
import com.example.data.local.entities.WeeklyReportEntity
import kotlin.math.roundToInt

class WeeklyFinalizationManager(private val db: AppDatabase) {

    /**
     * Checks and finalizes any expired weeks that have not yet been snapshotted.
     * Guaranteed to be idempotent and safe to run multiple times.
     */
    suspend fun checkAndFinalizeExpiredWeeks() {
        val settings = db.userSettingsDao().getSettingsSync()
        val currentWeekKey = EgyptDateTimeService.getCurrentWeekKey()
        val lastFinalizedWeekKey = settings?.lastFinalizedWeekKey

        // If this is the first week or previous week is already finalized, check if current week's predecessor needs finalization
        val previousWeekKey = EgyptDateTimeService.toDayKey(
            EgyptDateTimeService.parseDayKey(currentWeekKey).minusWeeks(1)
        )

        // Check all historical unfinalized weeks
        val unfinalizedKeys = if (lastFinalizedWeekKey != null) {
            EgyptDateTimeService.getUnfinalizedExpiredWeekKeys(lastFinalizedWeekKey, currentWeekKey)
        } else {
            // First time running or no recorded lastFinalizedWeekKey:
            // Check if there are records from previous week that need snapshot
            listOf(previousWeekKey)
        }

        for (weekKey in unfinalizedKeys) {
            finalizeWeekIdempotent(weekKey)
        }

        // Always check previousWeekKey if it has passed Friday 00:00 Cairo
        if (EgyptDateTimeService.isWeekExpired(previousWeekKey)) {
            finalizeWeekIdempotent(previousWeekKey)
        }
    }

    /**
     * Finalizes a specific weekKey (Friday YYYY-MM-DD) into an immutable snapshot.
     */
    suspend fun finalizeWeekIdempotent(weekKey: String): WeeklyReportEntity {
        // Idempotency check: if report already exists and is immutable, return it directly
        val existing = db.weeklyReportDao().getWeeklyReportSync(weekKey)
        if (existing != null && existing.isImmutable) {
            return existing
        }

        val report = calculateWeeklyReport(weekKey, isFinalized = true)
        db.weeklyReportDao().insertOrUpdate(report)

        // Update settings last finalized week
        val currentSettings = db.userSettingsDao().getSettingsSync()
        if (currentSettings != null) {
            db.userSettingsDao().saveSettings(
                currentSettings.copy(lastFinalizedWeekKey = weekKey)
            )
        }

        return report
    }

    /**
     * Calculates the weekly report and score.
     * Can be called dynamically for the active current week (isFinalized=false)
     * or for finalizing an expired week (isFinalized=true).
     */
    suspend fun calculateWeeklyReport(weekKey: String, isFinalized: Boolean = false): WeeklyReportEntity {
        val daysInWeek = EgyptDateTimeService.getDaysInWeek(weekKey)
        val weekEnd = EgyptDateTimeService.getWeekEndForWeekKey(weekKey)

        val prayerRecords = db.prayerDao().getPrayersForDaysSync(daysInWeek)
        val habitRecords = db.habitDao().getHabitsForDaysSync(daysInWeek)
        val counterRecords = db.counterDao().getCountersForDaysSync(daysInWeek)

        val totalPrayers = 35 // 5 prayers * 7 days
        val recordedPrayers = prayerRecords.count { it.status != PrayerStatus.UNRECORDED.name }
        val congregationPrayers = prayerRecords.count { it.status == PrayerStatus.CONGREGATION.name }
        val individualPrayers = prayerRecords.count { it.status == PrayerStatus.INDIVIDUAL.name }
        val missedPrayers = prayerRecords.count { it.status == PrayerStatus.MISSED.name }
        val unrecordedPrayers = totalPrayers - recordedPrayers

        val congregationRate = if (totalPrayers > 0) (congregationPrayers.toFloat() / totalPrayers.toFloat()) * 100f else 0f

        // Quran stats
        val quranRecords = habitRecords.filter { it.habitKey == "quran_wird" }
        val quranCompletionDays = quranRecords.count { it.isCompleted }

        // Dhikr total count
        val dhikrTotalCount = counterRecords.sumOf { it.count }

        // Habits completion rate
        val nonQuranHabits = habitRecords.filter { it.habitKey != "quran_wird" }
        val habitsCompletedCount = nonQuranHabits.count { it.isCompleted }
        val totalExpectedHabits = 4 * 7 // 4 other daily habits across 7 days
        val habitsCompletionRate = if (totalExpectedHabits > 0) (habitsCompletedCount.toFloat() / totalExpectedHabits.toFloat()) * 100f else 0f

        // Deterministic Weekly Score Calculation
        // 1. Prayer Score (45 points max)
        val prayerPoints = (congregationPrayers * 1.0 + individualPrayers * 0.75) / totalPrayers.toDouble() * 45.0

        // 2. Quran Score (20 points max)
        val quranPoints = (quranCompletionDays.toDouble() / 7.0) * 20.0

        // 3. Dhikr Score (20 points max)
        val targetDhikrPerDay = 200 + 33 + 33 + 33 + 10 + 10 // 319 daily target
        val expectedDhikrWeek = targetDhikrPerDay * 7
        val dhikrRatio = if (expectedDhikrWeek > 0) (dhikrTotalCount.toDouble() / expectedDhikrWeek).coerceAtMost(1.0) else 0.0
        val dhikrPoints = dhikrRatio * 20.0

        // 4. Sunan & Other Habits (15 points max)
        val sunanRatio = (habitsCompletedCount.toDouble() / totalExpectedHabits.toDouble()).coerceAtMost(1.0)
        val sunanPoints = sunanRatio * 15.0

        val totalRawScore = (prayerPoints + quranPoints + dhikrPoints + sunanPoints).roundToInt().coerceIn(0, 100)

        // Reasons & Patterns analysis
        val missedByPrayer = prayerRecords.filter { it.status == PrayerStatus.MISSED.name }
            .groupBy { it.prayer }
            .mapValues { it.value.size }

        val mostMissedPrayer = missedByPrayer.maxByOrNull { it.value }?.key

        val missedReasons = prayerRecords.filter { it.status == PrayerStatus.MISSED.name && !it.reason.isNullOrEmpty() }
            .groupBy { it.reason!! }
            .mapValues { it.value.size }

        val mostCommonMissReason = missedReasons.maxByOrNull { it.value }?.key

        val individualReasons = prayerRecords.filter { it.status == PrayerStatus.INDIVIDUAL.name && !it.reason.isNullOrEmpty() }
            .groupBy { it.reason!! }
            .mapValues { it.value.size }

        val mostCommonIndividualReason = individualReasons.maxByOrNull { it.value }?.key

        // Deterministic insights & recommendations
        val insight = generatePatternInsight(
            congregationPrayers = congregationPrayers,
            individualPrayers = individualPrayers,
            missedPrayers = missedPrayers,
            mostMissedPrayer = mostMissedPrayer,
            mostCommonMissReason = mostCommonMissReason,
            mostCommonIndividualReason = mostCommonIndividualReason,
            quranDays = quranCompletionDays,
            score = totalRawScore,
            recordedPrayers = recordedPrayers
        )

        val recommendation = generateRecommendation(
            mostMissedPrayer = mostMissedPrayer,
            mostCommonMissReason = mostCommonMissReason,
            mostCommonIndividualReason = mostCommonIndividualReason,
            quranDays = quranCompletionDays,
            congregationRate = congregationRate
        )

        return WeeklyReportEntity(
            weekKey = weekKey,
            weekEnd = weekEnd,
            weeklyScore = if (recordedPrayers == 0 && quranCompletionDays == 0 && dhikrTotalCount == 0) 0 else totalRawScore,
            totalPrayers = totalPrayers,
            recordedPrayers = recordedPrayers,
            congregationPrayers = congregationPrayers,
            individualPrayers = individualPrayers,
            missedPrayers = missedPrayers,
            unrecordedPrayers = unrecordedPrayers,
            congregationRate = congregationRate,
            quranCompletionDays = quranCompletionDays,
            dhikrTotalCount = dhikrTotalCount,
            habitsCompletionRate = habitsCompletionRate,
            mostMissedPrayer = mostMissedPrayer,
            mostCommonMissReason = mostCommonMissReason,
            mostCommonIndividualReason = mostCommonIndividualReason,
            patternInsight = insight,
            recommendation = recommendation,
            finalizedAt = if (isFinalized) System.currentTimeMillis() else 0L,
            isImmutable = isFinalized
        )
    }

    private fun generatePatternInsight(
        congregationPrayers: Int,
        individualPrayers: Int,
        missedPrayers: Int,
        mostMissedPrayer: String?,
        mostCommonMissReason: String?,
        mostCommonIndividualReason: String?,
        quranDays: Int,
        score: Int,
        recordedPrayers: Int
    ): String {
        if (recordedPrayers == 0 && quranDays == 0) {
            return "لم يتم تسجيل عبادات كافية لهذا الأسبوع. ابدأ بتسجيل صلواتك ووردك خطوة بخطوة."
        }

        val parts = mutableListOf<String>()

        if (congregationPrayers >= 20) {
            parts.add("ما شاء الله، ثبات رائع على صلاة الجماعة (${congregationPrayers} صلاة في المسجد).")
        } else if (congregationPrayers > 0) {
            parts.add("سجلت $congregationPrayers صلاة في الجماعة هذا الأسبوع.")
        }

        if (mostMissedPrayer != null) {
            val prayerAr = when (mostMissedPrayer) {
                "FAJR" -> "الفجر"
                "DHUHR" -> "الظهر"
                "ASR" -> "العصر"
                "MAGHRIB" -> "المغرب"
                "ISHA" -> "العشاء"
                else -> mostMissedPrayer
            }
            parts.add("صلاة $prayerAr كانت أكثر صلاة واجهت فيها صعوبة.")
        }

        if (mostCommonIndividualReason != null) {
            parts.add("أكثر سبب لأداء الصلاة منفردًا كان: \"$mostCommonIndividualReason\".")
        }

        if (quranDays >= 5) {
            parts.add("محافظة ممتازة على ورد القرآن الكريم في $quranDays أيام.")
        }

        return parts.joinToString(" ")
    }

    private fun generateRecommendation(
        mostMissedPrayer: String?,
        mostCommonMissReason: String?,
        mostCommonIndividualReason: String?,
        quranDays: Int,
        congregationRate: Float
    ): String {
        if (mostMissedPrayer == "FAJR" && mostCommonMissReason?.contains("نائم") == true) {
            return "جرّب التبكير في النوم قليلًا وضبط منبه ثانٍ بعيد عن السرير لصلاة الفجر."
        }
        if (mostCommonMissReason?.contains("نسيت") == true) {
            return "تفعيل التنبيهات قبل مواقيت الأذان بعشر دقائق يساعد في تقليل النسيان."
        }
        if (mostCommonIndividualReason?.contains("خارج المنزل") == true) {
            return "حدد أقرب مسجد لمكان عملك أو دراستك للحرص على صلاة الجماعة أثناء التواجد بالخارج."
        }
        if (quranDays < 3) {
            return "ابدأ بصفحة واحدة فقط يوميًا بعد صلاة الفجر أو المغرب لتثبيت ورد القرآن."
        }
        if (congregationRate < 40f) {
            return "اختر صلاة واحدة ثابتة في اليوم (كالظهر أو المغرب) والتزم بأدائها في المسجد كخطوة أولى."
        }
        return "الحمد لله على ما وفقت إليه هذا الأسبوع، استمر على هذه الهمة المباركة."
    }
}
