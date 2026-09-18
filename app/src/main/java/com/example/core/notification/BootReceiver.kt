package com.example.core.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = AppDatabase.getInstance(context)
                    val settings = db.userSettingsDao().getSettingsSync()
                    if (settings?.notificationsEnabled != false) {
                        val lat = settings?.cityLat ?: 30.0444
                        val lng = settings?.cityLng ?: 31.2357
                        PrayerAlarmScheduler.scheduleAllAlarms(context, lat, lng)
                    }
                } catch (e: Exception) {
                    // Ignore exceptions on boot
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
