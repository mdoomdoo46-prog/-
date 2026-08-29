package com.example.core.prayer

import com.example.core.datetime.EgyptDateTimeService
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.*

/**
 * Deterministic, offline calculation engine for Islamic Prayer Times in Egypt,
 * implementing the exact parameters of the Egyptian General Authority of Survey (الهيئة العامة المصرية للمساحة):
 * - Fajr angle: 19.5°
 * - Isha angle: 17.5°
 * - Dhuhr: Solar transit + small 1-min buffer
 * - Asr: Standard Shafi'i (shadow ratio = 1)
 * - Maghrib: Sunset
 */
object EgyptPrayerTimesEngine {

    val EGYPTIAN_CITIES = listOf(
        EgyptianCity("القاهرة", 30.0444, 31.2357),
        EgyptianCity("الإسكندرية", 31.2001, 29.9187),
        EgyptianCity("الجيزة", 30.0131, 31.2089),
        EgyptianCity("المنصورة", 31.0409, 31.3785),
        EgyptianCity("طنطا", 30.7865, 31.0004),
        EgyptianCity("الزقازيق", 30.5877, 31.5020),
        EgyptianCity("بورسعيد", 31.2653, 32.3019),
        EgyptianCity("السويس", 29.9668, 32.5498),
        EgyptianCity("الإسماعيلية", 30.5965, 32.2715),
        EgyptianCity("أسيوط", 27.1783, 31.1859),
        EgyptianCity("سوهاج", 26.5569, 31.6948),
        EgyptianCity("قنا", 26.1551, 32.7160),
        EgyptianCity("الأقصر", 25.6872, 32.6396),
        EgyptianCity("أسوان", 24.0889, 32.8998),
        EgyptianCity("الفيوم", 29.3084, 30.8428),
        EgyptianCity("بني سويف", 29.0661, 31.0994),
        EgyptianCity("المنيا", 28.1099, 30.7503),
        EgyptianCity("الغردقة", 27.2579, 33.8116),
        EgyptianCity("شرم الشيخ", 27.9158, 34.3299),
        EgyptianCity("مرسى مطروح", 31.3543, 27.2373)
    )

    fun getDefaultCity(): EgyptianCity = EGYPTIAN_CITIES[0]

    fun findCityByName(name: String): EgyptianCity {
        return EGYPTIAN_CITIES.find { it.nameArabic == name } ?: getDefaultCity()
    }

    /**
     * Calculates prayer times for a specific dayKey (YYYY-MM-DD) and city coordinates.
     */
    fun calculatePrayerTimes(
        dayKey: String,
        latitude: Double = 30.0444,
        longitude: Double = 31.2357,
        cityName: String = "القاهرة"
    ): DailyPrayerTimes {
        val localDate = EgyptDateTimeService.parseDayKey(dayKey)
        val dayOfYear = localDate.dayOfYear
        val year = localDate.year

        // Solar calculations
        val d = (367.0 * year - (7.0 * (year + ((localDate.monthValue + 9) / 12))) / 4 + (275 * localDate.monthValue) / 9 + localDate.dayOfMonth - 730530).toDouble()
        val w = 282.9404 + 4.70935E-5 * d
        val a = 1.000000
        val e = 0.016709 - 1.151E-9 * d
        val M = fixAngle(356.0470 + 0.9856002585 * d)
        val L = fixAngle(w + M)
        val oblecl = 23.4393 - 3.563E-7 * d
        val v = fixAngle(M + (360.0 / Math.PI) * e * sin(rad(M)))
        val sunLon = fixAngle(v + w)
        val x = cos(rad(sunLon))
        val y = cos(rad(oblecl)) * sin(rad(sunLon))
        val z = sin(rad(oblecl)) * sin(rad(sunLon))
        val r = sqrt(x * x + y * y)
        val ra = fixAngle(deg(atan2(y, x)))
        val decl = deg(atan2(z, r))

        // Equation of Time (in hours) & Solar Noon
        val eqt = (L - ra) / 15.0
        val timezoneOffset = 2.0 // Standard Egypt timezone offset (or dynamically computed)
        // Check if Cairo ZonedDateTime has DST offset
        val zdt = ZonedDateTime.of(localDate, LocalTime.NOON, EgyptDateTimeService.CAIRO_ZONE_ID)
        val actualOffsetHours = zdt.offset.totalSeconds / 3600.0

        val noon = 12.0 + actualOffsetHours - (longitude / 15.0) - eqt

        // Sun angles for Egyptian General Authority of Survey
        val fajrAngle = 19.5
        val ishaAngle = 17.5
        val sunAltitude = -0.8333

        val sunriseHour = noon - sunAngleHours(sunAltitude, latitude, decl)
        val sunsetHour = noon + sunAngleHours(sunAltitude, latitude, decl)
        val fajrHour = noon - sunAngleHours(-fajrAngle, latitude, decl)
        val ishaHour = noon + sunAngleHours(-ishaAngle, latitude, decl)

        // Asr calculation (Shafi'i shadow factor = 1)
        val asrAltitude = deg(atan(1.0 / (1.0 + tan(rad(abs(latitude - decl))))))
        val asrHour = noon + sunAngleHours(asrAltitude, latitude, decl)

        // Convert hours to ZonedDateTime millis and Arabic strings
        val fajrZdt = toZonedDateTime(localDate, fajrHour)
        val sunriseZdt = toZonedDateTime(localDate, sunriseHour)
        val dhuhrZdt = toZonedDateTime(localDate, noon + 0.016) // +1 min buffer for Dhuhr
        val asrZdt = toZonedDateTime(localDate, asrHour)
        val maghribZdt = toZonedDateTime(localDate, sunsetHour)
        val ishaZdt = toZonedDateTime(localDate, ishaHour)

        return DailyPrayerTimes(
            dayKey = dayKey,
            cityName = cityName,
            fajr = formatTime(fajrZdt),
            sunrise = formatTime(sunriseZdt),
            dhuhr = formatTime(dhuhrZdt),
            asr = formatTime(asrZdt),
            maghrib = formatTime(maghribZdt),
            isha = formatTime(ishaZdt),
            fajrEpochMillis = fajrZdt.toInstant().toEpochMilli(),
            sunriseEpochMillis = sunriseZdt.toInstant().toEpochMilli(),
            dhuhrEpochMillis = dhuhrZdt.toInstant().toEpochMilli(),
            asrEpochMillis = asrZdt.toInstant().toEpochMilli(),
            maghribEpochMillis = maghribZdt.toInstant().toEpochMilli(),
            ishaEpochMillis = ishaZdt.toInstant().toEpochMilli()
        )
    }

    /**
     * Determines next prayer and remaining time countdown in Cairo.
     */
    fun getNextPrayerInfo(dailyPrayerTimes: DailyPrayerTimes, now: ZonedDateTime = EgyptDateTimeService.getNowCairo()): NextPrayerInfo {
        val nowMillis = now.toInstant().toEpochMilli()

        val list = listOf(
            PrayerType.FAJR to dailyPrayerTimes.fajrEpochMillis,
            PrayerType.SUNRISE to dailyPrayerTimes.sunriseEpochMillis,
            PrayerType.DHUHR to dailyPrayerTimes.dhuhrEpochMillis,
            PrayerType.ASR to dailyPrayerTimes.asrEpochMillis,
            PrayerType.MAGHRIB to dailyPrayerTimes.maghribEpochMillis,
            PrayerType.ISHA to dailyPrayerTimes.ishaEpochMillis
        )

        var current: PrayerType? = null
        var nextPrayer: PrayerType = PrayerType.FAJR
        var targetMillis: Long = dailyPrayerTimes.fajrEpochMillis

        for (i in list.indices) {
            val (prayer, millis) = list[i]
            if (nowMillis >= millis) {
                current = prayer
            } else {
                nextPrayer = prayer
                targetMillis = millis
                break
            }
        }

        // If after Isha, next is tomorrow's Fajr
        if (nowMillis >= dailyPrayerTimes.ishaEpochMillis) {
            val tomorrowDate = now.toLocalDate().plusDays(1)
            val tomorrowDayKey = EgyptDateTimeService.toDayKey(tomorrowDate)
            val tomorrowTimes = calculatePrayerTimes(tomorrowDayKey, cityName = dailyPrayerTimes.cityName)
            nextPrayer = PrayerType.FAJR
            targetMillis = tomorrowTimes.fajrEpochMillis
            current = PrayerType.ISHA
        }

        val diffMillis = max(0L, targetMillis - nowMillis)
        val diffMinutes = ChronoUnit.MINUTES.between(now.toInstant(), java.time.Instant.ofEpochMilli(targetMillis))
        val hours = diffMinutes / 60
        val mins = diffMinutes % 60

        val formattedRemaining = if (hours > 0) {
            "متبقي $hours ساعة و $mins دقيقة"
        } else {
            "متبقي $mins دقيقة"
        }

        val nextTimeStr = when (nextPrayer) {
            PrayerType.FAJR -> dailyPrayerTimes.fajr
            PrayerType.SUNRISE -> dailyPrayerTimes.sunrise
            PrayerType.DHUHR -> dailyPrayerTimes.dhuhr
            PrayerType.ASR -> dailyPrayerTimes.asr
            PrayerType.MAGHRIB -> dailyPrayerTimes.maghrib
            PrayerType.ISHA -> dailyPrayerTimes.isha
        }

        return NextPrayerInfo(
            currentPrayer = current,
            nextPrayer = nextPrayer,
            nextPrayerTimeFormatted = nextTimeStr,
            timeRemainingFormatted = formattedRemaining,
            remainingMinutes = diffMinutes
        )
    }

    private fun sunAngleHours(altitude: Double, lat: Double, decl: Double): Double {
        val cosH = (sin(rad(altitude)) - sin(rad(lat)) * sin(rad(decl))) / (cos(rad(lat)) * cos(rad(decl)))
        val clampedCosH = cosH.coerceIn(-1.0, 1.0)
        return deg(acos(clampedCosH)) / 15.0
    }

    private fun toZonedDateTime(date: LocalDate, fractionalHour: Double): ZonedDateTime {
        var hour = fractionalHour.toInt()
        var remMin = (fractionalHour - hour) * 60.0
        var min = remMin.toInt()
        var sec = ((remMin - min) * 60.0).toInt()

        if (hour >= 24) hour %= 24
        if (hour < 0) hour = (hour % 24 + 24) % 24
        min = min.coerceIn(0, 59)
        sec = sec.coerceIn(0, 59)

        return ZonedDateTime.of(date, LocalTime.of(hour, min, sec), EgyptDateTimeService.CAIRO_ZONE_ID)
    }

    private fun formatTime(zdt: ZonedDateTime): String {
        return zdt.format(EgyptDateTimeService.TIME_FORMATTER)
    }

    private fun rad(d: Double): Double = d * Math.PI / 180.0
    private fun deg(r: Double): Double = r * 180.0 / Math.PI
    private fun fixAngle(a: Double): Double = ((a % 360.0) + 360.0) % 360.0
}
