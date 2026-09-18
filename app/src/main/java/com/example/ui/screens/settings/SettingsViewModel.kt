package com.example.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.notification.PrayerAlarmScheduler
import com.example.core.prayer.EgyptPrayerTimesEngine
import com.example.core.prayer.EgyptianCity
import com.example.data.local.AppDatabase
import com.example.data.local.entities.UserSettingsEntity
import com.example.data.repository.AppRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedCity: String = "القاهرة",
    val isOnboarded: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val isDarkMode: Boolean? = null
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getInstance(application)
    private val repository = AppRepository(db)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getUserSettingsFlow().collect { settings ->
                if (settings != null) {
                    _uiState.update {
                        it.copy(
                            selectedCity = settings.selectedCity,
                            isOnboarded = settings.isOnboarded,
                            notificationsEnabled = settings.notificationsEnabled,
                            isDarkMode = settings.isDarkMode
                        )
                    }
                }
            }
        }
    }

    fun completeOnboarding(city: EgyptianCity) {
        viewModelScope.launch {
            repository.completeOnboarding(city.nameArabic, city.latitude, city.longitude)
            PrayerAlarmScheduler.scheduleAllAlarms(getApplication(), city.latitude, city.longitude)
        }
    }

    fun updateCity(city: EgyptianCity) {
        viewModelScope.launch {
            repository.updateCity(city)
            val settings = repository.userSettingsDao.getSettingsSync()
            if (settings?.notificationsEnabled != false) {
                PrayerAlarmScheduler.scheduleAllAlarms(getApplication(), city.latitude, city.longitude)
            }
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            repository.toggleNotifications(enabled)
            val settings = repository.userSettingsDao.getSettingsSync()
            val lat = settings?.cityLat ?: 30.0444
            val lng = settings?.cityLng ?: 31.2357
            if (enabled) {
                PrayerAlarmScheduler.scheduleAllAlarms(getApplication(), lat, lng)
            } else {
                PrayerAlarmScheduler.cancelAllAlarms(getApplication())
            }
        }
    }

    fun toggleDarkMode(isDark: Boolean?) {
        viewModelScope.launch {
            repository.toggleDarkMode(isDark)
        }
    }
}
