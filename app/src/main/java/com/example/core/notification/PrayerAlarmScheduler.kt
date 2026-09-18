package com.example.core.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.core.datetime.EgyptDateTimeService
import com.example.core.prayer.EgyptPrayerTimesEngine
import com.example.core.prayer.PrayerType
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZonedDateTime

object PrayerAlarmScheduler {

    fun scheduleAllAlarms(
        context: Context,
        lat: Double = 30.0444,
        lng: Double = 31.2357
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val nowZdt = EgyptDateTimeService.getNowCairo()
        val nowMillis = nowZdt.toInstant().toEpochMilli()
        val todayDate = nowZdt.toLocalDate()
        val tomorrowDate = todayDate.plusDays(1)

        val todayKey = EgyptDateTimeService.toDayKey(todayDate)
        val tomorrowKey = EgyptDateTimeService.toDayKey(tomorrowDate)

        val todayTimes = EgyptPrayerTimesEngine.calculatePrayerTimes(todayKey, lat, lng)
        val tomorrowTimes = EgyptPrayerTimesEngine.calculatePrayerTimes(tomorrowKey, lat, lng)

        // 1. Schedule 5 Daily Prayers (15 minutes prior)
        scheduleSinglePrayer(
            context, alarmManager, PrayerType.FAJR.name,
            todayTimes.fajr, tomorrowTimes.fajr, todayDate, tomorrowDate,
            PrayerNotificationConstants.RC_FAJR, nowMillis
        )
        scheduleSinglePrayer(
            context, alarmManager, PrayerType.DHUHR.name,
            todayTimes.dhuhr, tomorrowTimes.dhuhr, todayDate, tomorrowDate,
            PrayerNotificationConstants.RC_DHUHR, nowMillis
        )
        scheduleSinglePrayer(
            context, alarmManager, PrayerType.ASR.name,
            todayTimes.asr, tomorrowTimes.asr, todayDate, tomorrowDate,
            PrayerNotificationConstants.RC_ASR, nowMillis
        )
        scheduleSinglePrayer(
            context, alarmManager, PrayerType.MAGHRIB.name,
            todayTimes.maghrib, tomorrowTimes.maghrib, todayDate, tomorrowDate,
            PrayerNotificationConstants.RC_MAGHRIB, nowMillis
        )
        scheduleSinglePrayer(
            context, alarmManager, PrayerType.ISHA.name,
            todayTimes.isha, tomorrowTimes.isha, todayDate, tomorrowDate,
            PrayerNotificationConstants.RC_ISHA, nowMillis
        )

        // 2. Schedule Night Prayer (قيام الليل)
        // In the last third of the night (60 minutes before upcoming Fajr)
        val upcomingFajrDate = if (nowMillis < toEpochMillis(todayDate, todayTimes.fajr)) todayDate else tomorrowDate
        val upcomingFajrTimes = if (upcomingFajrDate == todayDate) todayTimes else tomorrowTimes
        val fajrEpoch = toEpochMillis(upcomingFajrDate, upcomingFajrTimes.fajr)
        val nightPrayerTrigger = fajrEpoch - 60 * 60 * 1000 // 1 hour before Fajr

        if (nightPrayerTrigger > nowMillis && nightPrayerTrigger < fajrEpoch) {
            scheduleAlarm(
                context,
                alarmManager,
                PrayerNotificationConstants.TYPE_NIGHT_PRAYER,
                "NIGHT_PRAYER",
                EgyptDateTimeService.toDayKey(upcomingFajrDate),
                nightPrayerTrigger,
                fajrEpoch,
                PrayerNotificationConstants.RC_NIGHT_PRAYER
            )
        } else {
            // Schedule for tomorrow night
            val nextFajrDate = tomorrowDate.plusDays(1)
            val nextFajrKey = EgyptDateTimeService.toDayKey(nextFajrDate)
            val nextFajrTimes = EgyptPrayerTimesEngine.calculatePrayerTimes(nextFajrKey, lat, lng)
            val nextFajrEpoch = toEpochMillis(nextFajrDate, nextFajrTimes.fajr)
            val nextNightTrigger = nextFajrEpoch - 60 * 60 * 1000
            scheduleAlarm(
                context,
                alarmManager,
                PrayerNotificationConstants.TYPE_NIGHT_PRAYER,
                "NIGHT_PRAYER",
                nextFajrKey,
                nextNightTrigger,
                nextFajrEpoch,
                PrayerNotificationConstants.RC_NIGHT_PRAYER
            )
        }

        // 3. Evening Check Reminder at 21:30 Cairo time
        val todayEveningTrigger = toEpochMillis(todayDate, "21:30")
        val isEveningTodayInFuture = todayEveningTrigger > nowMillis
        val eveningTargetDate = if (isEveningTodayInFuture) todayDate else tomorrowDate
        val eveningTrigger = if (isEveningTodayInFuture) todayEveningTrigger else toEpochMillis(tomorrowDate, "21:30")

        scheduleAlarm(
            context,
            alarmManager,
            PrayerNotificationConstants.TYPE_EVENING_REMINDER,
            "EVENING_REMINDER",
            EgyptDateTimeService.toDayKey(eveningTargetDate),
            eveningTrigger,
            eveningTrigger,
            PrayerNotificationConstants.RC_EVENING_REMINDER
        )
    }

    private fun scheduleSinglePrayer(
        context: Context,
        alarmManager: AlarmManager,
        prayerName: String,
        todayTime: String,
        tomorrowTime: String,
        todayDate: LocalDate,
        tomorrowDate: LocalDate,
        requestCode: Int,
        nowMillis: Long
    ) {
        val todayPrayerMillis = toEpochMillis(todayDate, todayTime)
        val todayTrigger = todayPrayerMillis - 15 * 60 * 1000 // 15 minutes before

        val isTodayInFuture = todayTrigger > nowMillis
        val targetDate = if (isTodayInFuture) todayDate else tomorrowDate
        val targetPrayerMillis = if (isTodayInFuture) todayPrayerMillis else toEpochMillis(tomorrowDate, tomorrowTime)
        val triggerMillis = if (isTodayInFuture) todayTrigger else targetPrayerMillis - 15 * 60 * 1000

        scheduleAlarm(
            context,
            alarmManager,
            PrayerNotificationConstants.TYPE_PRAYER,
            prayerName,
            EgyptDateTimeService.toDayKey(targetDate),
            triggerMillis,
            targetPrayerMillis,
            requestCode
        )
    }

    private fun scheduleAlarm(
        context: Context,
        alarmManager: AlarmManager,
        type: String,
        prayerName: String,
        dayKey: String,
        triggerMillis: Long,
        targetTimeMillis: Long,
        requestCode: Int
    ) {
        val intent = Intent(context, PrayerAlarmReceiver::class.java).apply {
            putExtra(PrayerNotificationConstants.EXTRA_TYPE, type)
            putExtra(PrayerNotificationConstants.EXTRA_PRAYER_NAME, prayerName)
            putExtra(PrayerNotificationConstants.EXTRA_DAY_KEY, dayKey)
            putExtra(PrayerNotificationConstants.EXTRA_TARGET_MILLIS, targetTimeMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                } else {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
                }
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancelAllAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val requestCodes = listOf(
            PrayerNotificationConstants.RC_FAJR,
            PrayerNotificationConstants.RC_DHUHR,
            PrayerNotificationConstants.RC_ASR,
            PrayerNotificationConstants.RC_MAGHRIB,
            PrayerNotificationConstants.RC_ISHA,
            PrayerNotificationConstants.RC_NIGHT_PRAYER,
            PrayerNotificationConstants.RC_EVENING_REMINDER
        )
        for (rc in requestCodes) {
            val intent = Intent(context, PrayerAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                rc,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }
    }

    private fun toEpochMillis(date: LocalDate, timeStr: String): Long {
        val time = try {
            LocalTime.parse(timeStr)
        } catch (e: Exception) {
            LocalTime.of(0, 0)
        }
        val zdt = ZonedDateTime.of(date, time, EgyptDateTimeService.CAIRO_ZONE_ID)
        return zdt.toInstant().toEpochMilli()
    }
}
