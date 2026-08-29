package com.example.ui.screens.reports

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.datetime.EgyptDateTimeService
import com.example.data.local.AppDatabase
import com.example.data.local.entities.WeeklyReportEntity
import com.example.domain.engine.WeeklyFinalizationManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ReportsUiState(
    val currentWeekKey: String = "",
    val currentWeekRangeArabic: String = "",
    val currentWeekReport: WeeklyReportEntity? = null,
    val historicalReports: List<WeeklyReportEntity> = emptyList(),
    val selectedReport: WeeklyReportEntity? = null,
    val isLoading: Boolean = true
)

class ReportsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val finalizationManager = WeeklyFinalizationManager(db)

    private val _uiState = MutableStateFlow(ReportsUiState())
    val uiState: StateFlow<ReportsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val currentWeekKey = EgyptDateTimeService.getCurrentWeekKey()
            val rangeArabic = EgyptDateTimeService.formatArabicWeekRange(currentWeekKey)

            _uiState.update { it.copy(currentWeekKey = currentWeekKey, currentWeekRangeArabic = rangeArabic) }

            // Ensure any expired weeks are finalized
            finalizationManager.checkAndFinalizeExpiredWeeks()

            // Calculate live dynamic report for the current week
            val liveCurrentWeek = finalizationManager.calculateWeeklyReport(currentWeekKey, isFinalized = false)
            _uiState.update { it.copy(currentWeekReport = liveCurrentWeek) }

            // Collect historical reports
            db.weeklyReportDao().getAllWeeklyReports().collect { reports ->
                val historical = reports.filter { it.isImmutable && it.weekKey != currentWeekKey }
                _uiState.update { it.copy(historicalReports = historical, isLoading = false) }
            }
        }
    }

    fun selectReport(report: WeeklyReportEntity?) {
        _uiState.update { it.copy(selectedReport = report) }
    }

    fun refreshCurrentWeek() {
        viewModelScope.launch {
            val currentWeekKey = EgyptDateTimeService.getCurrentWeekKey()
            val live = finalizationManager.calculateWeeklyReport(currentWeekKey, isFinalized = false)
            _uiState.update { it.copy(currentWeekReport = live) }
        }
    }
}
