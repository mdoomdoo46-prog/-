package com.example.core.notification

object PrayerNotificationConstants {
    const val CHANNEL_ID = "ahl_alquran_prayer_channel"
    const val CHANNEL_NAME = "مواقيت الصلاة وقيام الليل"
    const val CHANNEL_DESCRIPTION = "تنبيهات هادئة قبل الصلوات ولقيام الليل وتذكير الورد"

    const val EXTRA_TYPE = "extra_notification_type"
    const val EXTRA_PRAYER_NAME = "extra_prayer_name"
    const val EXTRA_DAY_KEY = "extra_day_key"
    const val EXTRA_SCHEDULED_TIME = "extra_scheduled_time"
    const val EXTRA_TARGET_MILLIS = "extra_target_millis"

    const val TYPE_PRAYER = "PRAYER"
    const val TYPE_NIGHT_PRAYER = "NIGHT_PRAYER"
    const val TYPE_EVENING_REMINDER = "EVENING_REMINDER"

    // Request codes
    const val RC_FAJR = 101
    const val RC_DHUHR = 102
    const val RC_ASR = 103
    const val RC_MAGHRIB = 104
    const val RC_ISHA = 105
    const val RC_NIGHT_PRAYER = 201
    const val RC_EVENING_REMINDER = 301

    // Notification IDs
    const val NOTIF_ID_FAJR = 1001
    const val NOTIF_ID_DHUHR = 1002
    const val NOTIF_ID_ASR = 1003
    const val NOTIF_ID_MAGHRIB = 1004
    const val NOTIF_ID_ISHA = 1005
    const val NOTIF_ID_NIGHT_PRAYER = 2001
    const val NOTIF_ID_EVENING_REMINDER = 3001

    const val PREFS_NAME = "prayer_notifications_prefs"
}
