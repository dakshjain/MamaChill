package com.mamachill.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.mamachill.app.data.AlarmDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra("alarm_id", -1)
        val label = intent.getStringExtra("alarm_label") ?: "Alarm"
        val toneUri = intent.getStringExtra("alarm_tone") ?: ""
        val repeatDays = intent.getIntExtra("alarm_repeat", 0)

        // Reschedule repeating alarm before showing the screen
        if (repeatDays != 0) {
            val pending = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val alarm = AlarmDatabase.getInstance(context).alarmDao().getById(alarmId)
                    alarm?.let { if (it.isEnabled) AlarmScheduler.schedule(context, it) }
                } finally {
                    pending.finish()
                }
            }
        }

        createNotificationChannel(context)

        val fullScreenIntent = Intent(context, AlarmFiringActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("alarm_id", alarmId)
            putExtra("alarm_label", label)
            putExtra("alarm_tone", toneUri)
        }
        val fullScreenPending = PendingIntent.getActivity(
            context, alarmId, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(label.ifEmpty { "Alarm" })
            .setContentText("Tap to dismiss")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPending, true)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(alarmId, notification)

        context.startActivity(fullScreenIntent)
    }

    private fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL_ID, "Alarms", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Alarm notifications"
        }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "alarm_channel"
    }
}
