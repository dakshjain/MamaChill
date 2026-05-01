package com.mamachill.app

import android.app.NotificationManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.mamachill.app.databinding.ActivityAlarmFiringBinding

class AlarmFiringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmFiringBinding
    private var alarmId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        binding = ActivityAlarmFiringBinding.inflate(layoutInflater)
        setContentView(binding.root)

        alarmId = intent.getIntExtra("alarm_id", -1)
        val label = intent.getStringExtra("alarm_label") ?: "Alarm"
        val toneUri = intent.getStringExtra("alarm_tone") ?: ""

        binding.tvAlarmLabel.text = label.ifEmpty { "Alarm" }

        binding.btnDismiss.setOnClickListener {
            stopAlarm()
            finish()
        }

        binding.btnSnooze.setOnClickListener {
            stopAlarm()
            AlarmScheduler.scheduleSnooze(this, alarmId, label, toneUri)
            finish()
        }
    }

    private fun stopAlarm() {
        // Stop the foreground service (which stops the ringtone)
        stopService(Intent(this, AlarmService::class.java))
        // Dismiss the notification
        if (alarmId != -1) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.cancel(alarmId)
        }
    }

    // If user presses back, treat it as dismiss
    override fun onBackPressed() {
        stopAlarm()
        super.onBackPressed()
    }
}
