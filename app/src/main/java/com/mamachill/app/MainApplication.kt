package com.mamachill.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            AlarmReceiver.CHANNEL_ID,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm notifications"
            enableVibration(true)
            setSound(alarmSound, audioAttributes)
            // Delete existing channel so settings refresh on reinstall
        }
        val nm = getSystemService(NotificationManager::class.java)
        // Delete old channel to pick up new sound/vibration settings
        nm.deleteNotificationChannel(AlarmReceiver.CHANNEL_ID)
        nm.createNotificationChannel(channel)
    }

    companion object {
        lateinit var instance: MainApplication
            private set
    }
}
