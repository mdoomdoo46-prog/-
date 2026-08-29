package com.example.ui.screens.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.datetime.EgyptDateTimeService
import com.example.core.prayer.*
import com.example.data.local.AppDatabase
import com.example.data.local.entities.CounterRecordEntity
import com.example.data.local.entities.DailyReflectionEntity
import com.example.data.local.entities.HabitRecordEntity
import com.example.data.local.entities.PrayerRecordEntity
import com.example.data.repository.AppRepository
import com.example.domain.engine.WeeklyFinalizationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class HomeUiState(
    val dayKey: String = "",
    val fullArabicDate: String = "",
    val cityName: String = "القاهرة",
    val prayerTimes: DailyPrayerTimes? = null,
    val nextPrayerInfo: NextPrayerInfo? = null,
    val prayers: List<PrayerRecordEntity> = emptyList(),
    val habits: List<HabitRecordEntity> = emptyList(),
    val counters: List<CounterRecordEntity> = emptyList(),
    val reflection: DailyReflectionEntity? = null,
    val dailyProgressPercent: Int = 0,
    val encouragementMessage: String = "السلام عليكم 🤍 لنبدأ يومنا بذكر الله وطاعته",
    val nextStepSuggestion: String = "أكمل صلواتك ووردك اليوم",
    val showCelebration: Boolean = false,
    val celebrationText: String = "ما شاء الله 🤍",
    val isLoading: Boolean = true
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    val repository = AppRepository(db)
    private val finalizationManager = WeeklyFinalizationManager(db)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Check and finalize any expired weeks upon startup
            finalizationManager.checkAndFinalizeExpiredWeeks()
            loadTodayData()
        }

        // Periodic timer for live prayer countdown update
        viewModelScope.launch {
            while (true) {
                delay(30_000) // update countdown every 30s
                updatePrayerCountdown()
            }
        }
    }

    fun loadTodayData() {
        viewModelScope.launch {
            val todayKey = EgyptDateTimeService.getTodayKey()
            val fullDate = EgyptDateTimeService.formatArabicFullDate(todayKey)

            val settings = repository.userSettingsDao.getSettingsSync()
            val cityName = settings?.selectedCity ?: "القاهرة"
            val lat = settings?.cityLat ?: 30.0444
            val lng = settings?.cityLng ?: 31.2357

            val times = EgyptPrayerTimesEngine.calculatePrayerTimes(todayKey, lat, lng, cityName)
            val nextInfo = EgyptPrayerTimesEngine.getNextPrayerInfo(times)

            // Ensure today is initialized in database
            repository.ensureDayInitialized(todayKey, times)

            // Combine flows for reactive updates
            combine(
                repository.getPrayersFlow(todayKey),
                repository.getHabitsFlow(todayKey),
                repository.getCountersFlow(todayKey),
                repository.getReflectionFlow(todayKey)
            ) { prayers, habits, counters, reflection ->
                val progress = calculateDayProgress(prayers, habits, counters)
                val suggestion = determineNextStep(prayers, habits, counters)
                val encouragement = getEncouragement(progress)

                _uiState.update { current ->
                    current.copy(
                        dayKey = todayKey,
                        fullArabicDate = fullDate,
                        cityName = cityName,
                        prayerTimes = times,
                        nextPrayerInfo = nextInfo,
                        prayers = prayers,
                        habits = habits,
                        counters = counters,
                        reflection = reflection,
                        dailyProgressPercent = progress,
                        encouragementMessage = encouragement,
                        nextStepSuggestion = suggestion,
                        isLoading = false
                    )
                }
            }.collect()
        }
    }

    private fun updatePrayerCountdown() {
        val times = _uiState.value.prayerTimes ?: return
        val nextInfo = EgyptPrayerTimesEngine.getNextPrayerInfo(times)
        _uiState.update { it.copy(nextPrayerInfo = nextInfo) }
    }

    fun logPrayer(prayer: String, status: PrayerStatus, reason: String?, customReason: String?) {
        viewModelScope.launch {
            val dayKey = _uiState.value.dayKey
            val scheduledTime = when (prayer) {
                "FAJR" -> _uiState.value.prayerTimes?.fajr ?: ""
                "DHUHR" -> _uiState.value.prayerTimes?.dhuhr ?: ""
                "ASR" -> _uiState.value.prayerTimes?.asr ?: ""
                "MAGHRIB" -> _uiState.value.prayerTimes?.maghrib ?: ""
                "ISHA" -> _uiState.value.prayerTimes?.isha ?: ""
                else -> ""
            }

            repository.updatePrayerStatus(
                dayKey = dayKey,
                prayer = prayer,
                status = status,
                reason = reason,
                customReason = customReason,
                scheduledTime = scheduledTime
            )

            if (status == PrayerStatus.CONGREGATION) {
                triggerCelebration("تقبل الله 🕌", "صليت في جماعة، هنيئًا لك الأجر المضاعف")
            }
        }
    }

    fun toggleHabit(habitKey: String, isCompleted: Boolean, notes: String? = null) {
        viewModelScope.launch {
            repository.toggleHabit(_uiState.value.dayKey, habitKey, isCompleted, notes)
            if (isCompleted) {
                if (habitKey == "quran_wird") {
                    triggerCelebration("ما شاء الله 📖", "أتممت وردك من كتاب الله المبارك")
                } else {
                    triggerCelebration("الحمد لله 🌿", "كتب الله أجرك وثبتك")
                }
            }
        }
    }

    fun quickIncrementCounter(counterKey: String) {
        viewModelScope.launch {
            val updated = repository.incrementCounter(_uiState.value.dayKey, counterKey, 1)
            if (updated.count >= updated.target && (updated.count - 1) < updated.target) {
                triggerCelebration("ما شاء الله 🤍", "أكملت ${updated.titleArabic}")
            }
        }
    }

    fun saveDailyReflection(struggledHabit: String?, reason: String?, customReason: String?, note: String?) {
        viewModelScope.launch {
            repository.saveReflection(_uiState.value.dayKey, struggledHabit, reason, customReason, note)
            triggerCelebration("تقبل الله منك 🤍", "حفظت محاسبة اليوم، ونسأل الله التوفيق للغد")
        }
    }

    private fun triggerCelebration(title: String, subtitle: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(showCelebration = true, celebrationText = title) }
            delay(3500)
            _uiState.update { it.copy(showCelebration = false) }
        }
    }

    private fun calculateDayProgress(
        prayers: List<PrayerRecordEntity>,
        habits: List<HabitRecordEntity>,
        counters: List<CounterRecordEntity>
    ): Int {
        var totalWeight = 0.0
        var completedWeight = 0.0

        // 5 Prayers (Weight = 50%)
        val totalPrayers = 5.0
        val prayerRecordedScore = prayers.sumOf {
            when (it.status) {
                PrayerStatus.CONGREGATION.name -> 1.0
                PrayerStatus.INDIVIDUAL.name -> 0.75
                else -> 0.0
            }
        }
        totalWeight += 50.0
        completedWeight += (prayerRecordedScore / totalPrayers) * 50.0

        // Habits (Weight = 25%)
        if (habits.isNotEmpty()) {
            val habitsScore = habits.count { it.isCompleted }.toDouble() / habits.size.toDouble()
            totalWeight += 25.0
            completedWeight += habitsScore * 25.0
        }

        // Counters (Weight = 25%)
        if (counters.isNotEmpty()) {
            val countersRatio = counters.map {
                (it.count.toDouble() / it.target.toDouble()).coerceAtMost(1.0)
            }.average()
            totalWeight += 25.0
            completedWeight += countersRatio * 25.0
        }

        return if (totalWeight > 0) (completedWeight / totalWeight * 100.0).roundToInt().coerceIn(0, 100) else 0
    }

    private fun determineNextStep(
        prayers: List<PrayerRecordEntity>,
        habits: List<HabitRecordEntity>,
        counters: List<CounterRecordEntity>
    ): String {
        val unrecordedPrayer = prayers.find { it.status == PrayerStatus.UNRECORDED.name }
        if (unrecordedPrayer != null) {
            val prayerAr = when (unrecordedPrayer.prayer) {
                "FAJR" -> "الفجر"
                "DHUHR" -> "الظهر"
                "ASR" -> "العصر"
                "MAGHRIB" -> "المغرب"
                "ISHA" -> "العشاء"
                else -> unrecordedPrayer.prayer
            }
            return "سجّل كيف صليت $prayerAr اليوم."
        }

        val quranHabit = habits.find { it.habitKey == "quran_wird" && !it.isCompleted }
        if (quranHabit != null) {
            return "أكمل وردك من القرآن الكريم اليوم."
        }

        val istighfar = counters.find { it.counterKey == "counter_istighfar" && it.count < it.target }
        if (istighfar != null) {
            return "أكمل استغفار اليوم (${istighfar.count} / ${istighfar.target})."
        }

        val incompleteHabit = habits.find { !it.isCompleted }
        if (incompleteHabit != null) {
            return "بقي لك ${incompleteHabit.titleArabic} اليوم."
        }

        return "الحمد لله، أكملت جلّ عباداتك اليوم! بارك الله فيك."
    }

    private fun getEncouragement(progress: Int): String {
        return when {
            progress >= 90 -> "ما شاء الله، إنجاز مبارك ويوم عامر بالطاعات 🤍"
            progress >= 70 -> "أحسنت، اقتربت من إكمال يومك بخطوات ثابتة 🌿"
            progress >= 40 -> "خطوة جميلة، نكمل باقي عبادات اليوم بهمة واطمئنان 🤍"
            progress > 0 -> "بداية طيبة، استمر خطوة بخطوة 🌿"
            else -> "السلام عليكم 🤍 لنبدأ يومنا بذكر الله وطاعته"
        }
    }
}
