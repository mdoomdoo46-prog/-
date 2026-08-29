package com.example

import com.example.core.datetime.EgyptDateTimeService
import com.example.core.prayer.EgyptPrayerTimesEngine
import com.example.core.prayer.PrayerType
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class EgyptPrayerTimesEngineTest {

    @Test
    fun testCairoPrayerTimesGeneration() {
        val times = EgyptPrayerTimesEngine.calculatePrayerTimes(
            dayKey = "2026-08-29",
            latitude = 30.0444,
            longitude = 31.2357,
            cityName = "القاهرة"
        )

        assertEquals("2026-08-29", times.dayKey)
        assertEquals("القاهرة", times.cityName)
        assertTrue(times.fajr.isNotEmpty())
        assertTrue(times.sunrise.isNotEmpty())
        assertTrue(times.dhuhr.isNotEmpty())
        assertTrue(times.asr.isNotEmpty())
        assertTrue(times.maghrib.isNotEmpty())
        assertTrue(times.isha.isNotEmpty())

        // Ensure chronological progression
        assertTrue(times.fajrEpochMillis < times.sunriseEpochMillis)
        assertTrue(times.sunriseEpochMillis < times.dhuhrEpochMillis)
        assertTrue(times.dhuhrEpochMillis < times.asrEpochMillis)
        assertTrue(times.asrEpochMillis < times.maghribEpochMillis)
        assertTrue(times.maghribEpochMillis < times.ishaEpochMillis)
    }

    @Test
    fun testNextPrayerDetermination() {
        val times = EgyptPrayerTimesEngine.calculatePrayerTimes("2026-08-29")

        // 1. Early morning before Fajr
        val earlyMorning = ZonedDateTime.of(
            LocalDate.of(2026, 8, 29),
            LocalTime.of(2, 0),
            EgyptDateTimeService.CAIRO_ZONE_ID
        )
        val info1 = EgyptPrayerTimesEngine.getNextPrayerInfo(times, earlyMorning)
        assertEquals(PrayerType.FAJR, info1.nextPrayer)

        // 2. Afternoon between Dhuhr and Asr
        val afternoon = java.time.Instant.ofEpochMilli(times.dhuhrEpochMillis + 30 * 60 * 1000)
            .atZone(EgyptDateTimeService.CAIRO_ZONE_ID)
        val info2 = EgyptPrayerTimesEngine.getNextPrayerInfo(times, afternoon)
        assertEquals(PrayerType.DHUHR, info2.currentPrayer)
        assertEquals(PrayerType.ASR, info2.nextPrayer)
    }

    @Test
    fun testEgyptianCitiesList() {
        val cities = EgyptPrayerTimesEngine.EGYPTIAN_CITIES
        assertTrue(cities.size >= 10)
        assertNotNull(cities.find { it.nameArabic == "القاهرة" })
        assertNotNull(cities.find { it.nameArabic == "الإسكندرية" })
        assertNotNull(cities.find { it.nameArabic == "أسوان" })
    }
}
