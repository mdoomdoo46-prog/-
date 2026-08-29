package com.example.core.prayer

enum class PrayerType(val arabicName: String, val englishKey: String, val iconRes: String) {
    FAJR("الفجر", "fajr", "fajr"),
    SUNRISE("الشروق", "sunrise", "sunrise"),
    DHUHR("الظهر", "dhuhr", "dhuhr"),
    ASR("العصر", "asr", "asr"),
    MAGHRIB("المغرب", "maghrib", "maghrib"),
    ISHA("العشاء", "isha", "isha")
}

enum class PrayerStatus(val arabicLabel: String) {
    UNRECORDED("لم تسجل بعد"),
    CONGREGATION("جماعة"),
    INDIVIDUAL("منفردًا"),
    MISSED("لم أصلِّ")
}

data class DailyPrayerTimes(
    val dayKey: String,
    val cityName: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val fajrEpochMillis: Long,
    val sunriseEpochMillis: Long,
    val dhuhrEpochMillis: Long,
    val asrEpochMillis: Long,
    val maghribEpochMillis: Long,
    val ishaEpochMillis: Long
)

data class NextPrayerInfo(
    val currentPrayer: PrayerType?,
    val nextPrayer: PrayerType,
    val nextPrayerTimeFormatted: String,
    val timeRemainingFormatted: String,
    val remainingMinutes: Long
)

data class EgyptianCity(
    val nameArabic: String,
    val latitude: Double,
    val longitude: Double
)
