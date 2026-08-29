package com.example.ui.screens.counter

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.datetime.EgyptDateTimeService
import com.example.data.local.AppDatabase
import com.example.data.local.entities.CounterRecordEntity
import com.example.data.repository.AppRepository
import com.example.domain.models.DefaultHabits
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CounterUiState(
    val dayKey: String = "",
    val counters: List<CounterRecordEntity> = emptyList(),
    val selectedCounterKey: String = DefaultHabits.ISTIGHFAR_200.key,
    val showCelebration: Boolean = false,
    val celebrationTitle: String = "ما شاء الله 🤍"
)

class CounterViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = AppRepository(db)

    private val _uiState = MutableStateFlow(CounterUiState())
    val uiState: StateFlow<CounterUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val todayKey = EgyptDateTimeService.getTodayKey()
            _uiState.update { it.copy(dayKey = todayKey) }

            repository.getCountersFlow(todayKey).collect { list ->
                _uiState.update { current ->
                    current.copy(counters = list)
                }
            }
        }
    }

    fun selectCounter(key: String) {
        _uiState.update { it.copy(selectedCounterKey = key) }
    }

    fun increment() {
        viewModelScope.launch {
            val currentKey = _uiState.value.selectedCounterKey
            val dayKey = _uiState.value.dayKey
            val updated = repository.incrementCounter(dayKey, currentKey, 1)

            if (updated.count == updated.target) {
                _uiState.update { it.copy(showCelebration = true, celebrationTitle = "أتممت ${updated.titleArabic} 🤍") }
                delay(3000)
                _uiState.update { it.copy(showCelebration = false) }
            }
        }
    }

    fun reset() {
        viewModelScope.launch {
            val currentKey = _uiState.value.selectedCounterKey
            val dayKey = _uiState.value.dayKey
            repository.resetCounter(dayKey, currentKey)
        }
    }
}
