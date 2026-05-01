package com.mamachill.app

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat

class AlarmService : Service() {

    private var ringtone: Ringtone? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val alarmId = intent?.getIntExtra("alarm_id", -1) ?: -1
        val label = intent?.getStringExtra("alarm_label") ?: "Alarm"
        val toneUri = intent?.getStringExtra("alarm_tone") ?: ""
        val notifId = alarmId.takeIf { it != -1 } ?: 1

        val fullScreenPending = PendingIntent.getActivity(
            this, alarmId,
            Intent(this, AlarmFiringActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("alarm_id", alarmId)
                putExtra("alarm_label", label)
                putExtra("alarm_tone", toneUri)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopPending = PendingIntent.getBroadcast(
            this, alarmId,
            Intent(this, AlarmActionReceiver::class.java).apply {
                action = AlarmActionReceiver.ACTION_STOP
                putExtra("alarm_id", alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozePending = PendingIntent.getBroadcast(
            this, alarmId + 50_000,
            Intent(this, AlarmActionReceiver::class.java).apply {
                action = AlarmActionReceiver.ACTION_SNOOZE
                putExtra("alarm_id", alarmId)
                putExtra("alarm_label", label)
                putExtra("alarm_tone", toneUri)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, AlarmReceiver.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(label.ifEmpty { "Alarm" })
            .setContentText("Alarm is ringing")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPending, true)
            .setOngoing(true)
            .addAction(0, "Stop", stopPending)
            .addAction(0, "Snooze 5 min", snoozePending)
            .build()

        // Must call startForeground immediately — before any other work
        ServiceCompat.startForeground(
            this,
            notifId,
            notification,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            else 0
        )

        // Play ringtone after foreground is established
        val uri = if (toneUri.isNotEmpty()) Uri.parse(toneUri)
                  else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone?.play()

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        ringtone?.stop()
        ringtone = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
