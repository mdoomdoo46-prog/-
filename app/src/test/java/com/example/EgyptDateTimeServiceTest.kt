package com.example

import com.example.core.datetime.EgyptDateTimeService
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

class EgyptDateTimeServiceTest {

    @Test
    fun testCanonicalDayKeyFormat() {
        val date = LocalDate.of(2026, 8, 29)
        val dayKey = EgyptDateTimeService.toDayKey(date)
        assertEquals("2026-08-29", dayKey)

        val parsed = EgyptDateTimeService.parseDayKey(dayKey)
        assertEquals(date, parsed)
    }

    @Test
    fun testWeekKeyForFriday() {
        // Friday August 28, 2026
        val friday = LocalDate.of(2026, 8, 28)
        val weekKey = EgyptDateTimeService.getWeekKeyForDate(friday)
        assertEquals("2026-08-28", weekKey)
    }

    @Test
    fun testWeekKeyForMidWeekDays() {
        // Saturday August 29, 2026 -> belongs to week starting Friday Aug 28
        val saturday = LocalDate.of(2026, 8, 29)
        assertEquals("2026-08-28", EgyptDateTimeService.getWeekKeyForDate(saturday))

        // Tuesday September 1, 2026 -> belongs to week starting Friday Aug 28
        val tuesday = LocalDate.of(2026, 9, 1)
        assertEquals("2026-08-28", EgyptDateTimeService.getWeekKeyForDate(tuesday))

        // Thursday September 3, 2026 -> last day of week starting Friday Aug 28
        val thursday = LocalDate.of(2026, 9, 3)
        assertEquals("2026-08-28", EgyptDateTimeService.getWeekKeyForDate(thursday))
    }

    @Test
    fun testWeekKeyForNextFriday() {
        // Next Friday September 4, 2026 -> starts a NEW week
        val nextFriday = LocalDate.of(2026, 9, 4)
        assertEquals("2026-09-04", EgyptDateTimeService.getWeekKeyForDate(nextFriday))
    }

    @Test
    fun testWeekDaysSequence() {
        val weekKey = "2026-08-28" // Friday
        val days = EgyptDateTimeService.getDaysInWeek(weekKey)

        assertEquals(7, days.size)
        assertEquals("2026-08-28", days[0]) // Friday
        assertEquals("2026-08-29", days[1]) // Saturday
        assertEquals("2026-08-30", days[2]) // Sunday
        assertEquals("2026-08-31", days[3]) // Monday
        assertEquals("2026-09-01", days[4]) // Tuesday
        assertEquals("2026-09-02", days[5]) // Wednesday
        assertEquals("2026-09-03", days[6]) // Thursday

        assertEquals("2026-09-03", EgyptDateTimeService.getWeekEndForWeekKey(weekKey))
    }

    @Test
    fun testWeekExpirationAtFridayMidnight() {
        val weekKey = "2026-08-28" // Start: Friday Aug 28, 00:00 Cairo

        // Thursday 23:59:50 -> NOT expired
        val thursdayNight = ZonedDateTime.of(
            LocalDate.of(2026, 9, 3),
            LocalTime.of(23, 59, 50),
            EgyptDateTimeService.CAIRO_ZONE_ID
        )
        assertFalse(EgyptDateTimeService.isWeekExpired(weekKey, thursdayNight))

        // Friday 00:00:00 -> Expired (finalization boundary reached)
        val fridayMidnight = ZonedDateTime.of(
            LocalDate.of(2026, 9, 4),
            LocalTime.of(0, 0, 0),
            EgyptDateTimeService.CAIRO_ZONE_ID
        )
        assertTrue(EgyptDateTimeService.isWeekExpired(weekKey, fridayMidnight))

        // Friday 10:00 AM -> Expired
        val fridayMorning = ZonedDateTime.of(
            LocalDate.of(2026, 9, 4),
            LocalTime.of(10, 0, 0),
            EgyptDateTimeService.CAIRO_ZONE_ID
        )
        assertTrue(EgyptDateTimeService.isWeekExpired(weekKey, fridayMorning))
    }

    @Test
    fun testUnfinalizedWeeksDetectionAcrossAbsence() {
        val lastFinalized = "2026-08-07"
        val currentWeek = "2026-08-28" // 3 weeks later

        val unfinalized = EgyptDateTimeService.getUnfinalizedExpiredWeekKeys(lastFinalized, currentWeek)
        assertEquals(2, unfinalized.size)
        assertEquals("2026-08-14", unfinalized[0])
        assertEquals("2026-08-21", unfinalized[1])
    }

    @Test
    fun testArabicDayNames() {
        assertEquals("الجمعة", EgyptDateTimeService.getArabicDayName("2026-08-28"))
        assertEquals("السبت", EgyptDateTimeService.getArabicDayName("2026-08-29"))
        assertEquals("الأحد", EgyptDateTimeService.getArabicDayName("2026-08-30"))
        assertEquals("الإثنين", EgyptDateTimeService.getArabicDayName("2026-08-31"))
        assertEquals("الثلاثاء", EgyptDateTimeService.getArabicDayName("2026-09-01"))
        assertEquals("الأربعاء", EgyptDateTimeService.getArabicDayName("2026-09-02"))
        assertEquals("الخميس", EgyptDateTimeService.getArabicDayName("2026-09-03"))
    }
}
