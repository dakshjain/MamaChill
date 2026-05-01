package com.mamachill.app

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val label = intent.getStringExtra("alarm_label") ?: ""
        val toneUri = intent.getStringExtra("alarm_tone") ?: ""

        // Stop the ringing service and dismiss the notification
        context.stopService(Intent(context, AlarmService::class.java))
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.cancel(alarmId)

        if (intent.action == ACTION_SNOOZE) {
            AlarmScheduler.scheduleSnooze(context, alarmId, label, toneUri)
        }
    }

    companion object {
        const val ACTION_STOP = "com.mamachill.app.ACTION_STOP"
        const val ACTION_SNOOZE = "com.mamachill.app.ACTION_SNOOZE"
    }
}
