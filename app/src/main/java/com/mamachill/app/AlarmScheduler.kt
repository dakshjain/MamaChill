package com.mamachill.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.mamachill.app.data.Alarm
import java.util.Calendar

object AlarmScheduler {

    fun schedule(context: Context, alarm: Alarm) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerMillis = nextAlarmMillis(alarm.hour, alarm.minute, alarm.repeatDays)

        val alarmIntent = PendingIntent.getBroadcast(
            context, alarm.id,
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarm.id)
                putExtra("alarm_label", alarm.label)
                putExtra("alarm_tone", alarm.toneUri)
                putExtra("alarm_repeat", alarm.repeatDays)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Show intent — tapping alarm icon in status bar opens the app
        val showIntent = PendingIntent.getActivity(
            context, alarm.id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        // setAlarmClock is the correct API for user-visible alarms:
        // - Highest scheduling priority, bypasses Doze mode completely
        // - Does not require SCHEDULE_EXACT_ALARM user approval
        // - Shows clock icon in status bar
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(triggerMillis, showIntent),
            alarmIntent
        )
    }

    fun cancel(context: Context, alarmId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = PendingIntent.getBroadcast(
            context, alarmId,
            Intent(context, AlarmReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    fun scheduleSnooze(context: Context, alarmId: Int, label: String, toneUri: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val snoozeIntent = PendingIntent.getBroadcast(
            context, alarmId + 100_000,
            Intent(context, AlarmReceiver::class.java).apply {
                putExtra("alarm_id", alarmId)
                putExtra("alarm_label", label)
                putExtra("alarm_tone", toneUri)
                putExtra("alarm_repeat", 0)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val showIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.setAlarmClock(
            AlarmManager.AlarmClockInfo(System.currentTimeMillis() + 5 * 60 * 1000L, showIntent),
            snoozeIntent
        )
    }

    fun nextAlarmMillis(hour: Int, minute: Int, repeatDays: Int): Long {
        val now = Calendar.getInstance()
        val base = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (repeatDays == 0) {
            if (base.timeInMillis <= now.timeInMillis) base.add(Calendar.DATE, 1)
            return base.timeInMillis
        }

        for (daysAhead in 0..6) {
            val candidate = (base.clone() as Calendar).apply {
                if (daysAhead > 0) add(Calendar.DATE, daysAhead)
            }
            val calDay = candidate.get(Calendar.DAY_OF_WEEK)
            val bit = (calDay - 2 + 7) % 7
            if (repeatDays and (1 shl bit) != 0 && candidate.timeInMillis > now.timeInMillis) {
                return candidate.timeInMillis
            }
        }
        return base.timeInMillis + 7 * 24 * 60 * 60 * 1000L
    }
}
