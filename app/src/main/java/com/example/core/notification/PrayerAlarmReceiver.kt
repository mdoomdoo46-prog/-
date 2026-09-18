package com.example.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(PrayerNotificationConstants.EXTRA_TYPE) ?: return
        val prayerName = intent.getStringExtra(PrayerNotificationConstants.EXTRA_PRAYER_NAME) ?: ""
        val dayKey = intent.getStringExtra(PrayerNotificationConstants.EXTRA_DAY_KEY) ?: ""
        val targetMillis = intent.getLongExtra(PrayerNotificationConstants.EXTRA_TARGET_MILLIS, 0L)

        val now = System.currentTimeMillis()

        // 1. Guard against stale triggers
        if (type == PrayerNotificationConstants.TYPE_PRAYER) {
            // Do not show if prayer time already passed by more than 15 minutes
            if (targetMillis > 0 && now > targetMillis + 15 * 60 * 1000) {
                return
            }
        } else if (type == PrayerNotificationConstants.TYPE_NIGHT_PRAYER) {
            // Do not show night prayer notification after Fajr entered!
            if (targetMillis > 0 && now >= targetMillis) {
                return
            }
        }

        // 2. Guard against duplicate notifications for the same day
        val prefs = context.getSharedPreferences(PrayerNotificationConstants.PREFS_NAME, Context.MODE_PRIVATE)
        val deduplicationKey = "notified_${type}_${prayerName}_${dayKey}"
        if (prefs.getBoolean(deduplicationKey, false)) {
            return
        }

        // 3. Check if notifications enabled in settings
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getInstance(context)
                val settings = db.userSettingsDao().getSettingsSync()
                val isEnabled = settings?.notificationsEnabled ?: true
                if (!isEnabled) {
                    return@launch
                }

                // If evening reminder, check if user actually has unrecorded tasks
                if (type == PrayerNotificationConstants.TYPE_EVENING_REMINDER) {
                    val prayers = db.prayerDao().getPrayersForDaySync(dayKey)
                    val habits = db.habitDao().getHabitsForDaySync(dayKey)
                    val hasUnrecorded = prayers.any { it.status == "UNRECORDED" } || habits.any { !it.isCompleted }
                    if (!hasUnrecorded) {
                        return@launch
                    }
                }

                // Mark as notified in SharedPreferences
                prefs.edit().putBoolean(deduplicationKey, true).apply()

                // Create channel
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        PrayerNotificationConstants.CHANNEL_ID,
                        PrayerNotificationConstants.CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = PrayerNotificationConstants.CHANNEL_DESCRIPTION
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                // Resolve text and IDs
                val (notifId, title, body) = when (type) {
                    PrayerNotificationConstants.TYPE_PRAYER -> {
                        when (prayerName) {
                            "FAJR" -> Triple(PrayerNotificationConstants.NOTIF_ID_FAJR, "الفجر اقترب 🕌", "لا تنسَ صلاة الفجر.")
                            "DHUHR" -> Triple(PrayerNotificationConstants.NOTIF_ID_DHUHR, "صلاة الظهر اقتربت 🕌", "استعد للصلاة.")
                            "ASR" -> Triple(PrayerNotificationConstants.NOTIF_ID_ASR, "صلاة العصر اقتربت 🕌", "استعد للصلاة.")
                            "MAGHRIB" -> Triple(PrayerNotificationConstants.NOTIF_ID_MAGHRIB, "المغرب اقترب 🕌", "لا تنسَ الصلاة.")
                            "ISHA" -> Triple(PrayerNotificationConstants.NOTIF_ID_ISHA, "صلاة العشاء اقتربت 🕌", "استعد للصلاة.")
                            else -> Triple(1000, "حان موعد الصلاة 🕌", "استعد للصلاة وذكر الله.")
                        }
                    }
                    PrayerNotificationConstants.TYPE_NIGHT_PRAYER -> {
                        Triple(
                            PrayerNotificationConstants.NOTIF_ID_NIGHT_PRAYER,
                            "شَرُفَ المسلم قيام ليله 🌙",
                            "إن استطعت، صلِّ قيام الليل ولو ثلاث ركعات."
                        )
                    }
                    PrayerNotificationConstants.TYPE_EVENING_REMINDER -> {
                        Triple(
                            PrayerNotificationConstants.NOTIF_ID_EVENING_REMINDER,
                            "أهل القرآن 🤍",
                            "ما تنساش تسجل اللي عملته النهارده."
                        )
                    }
                    else -> Triple(999, "تذكير يومي", "ذكر الله طمأنينة للقلوب")
                }

                // Launch Intent
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val contentPendingIntent = PendingIntent.getActivity(
                    context,
                    notifId,
                    openIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val notification = NotificationCompat.Builder(context, PrayerNotificationConstants.CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .setContentIntent(contentPendingIntent)
                    .build()

                notificationManager.notify(notifId, notification)

                // Reschedule future alarms
                val cityLat = settings?.cityLat ?: 30.0444
                val cityLng = settings?.cityLng ?: 31.2357
                PrayerAlarmScheduler.scheduleAllAlarms(context, cityLat, cityLng)

            } catch (e: Exception) {
                // Ignore background notification dispatch exceptions
            } finally {
                pendingResult.finish()
            }
        }
    }
}
