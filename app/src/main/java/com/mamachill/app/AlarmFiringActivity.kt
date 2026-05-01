package com.mamachill.app

import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.mamachill.app.databinding.ActivityAlarmFiringBinding

class AlarmFiringActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlarmFiringBinding
    private var ringtone: Ringtone? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Show on lock screen and wake the display
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

        val alarmId = intent.getIntExtra("alarm_id", -1)
        val label = intent.getStringExtra("alarm_label") ?: "Alarm"
        val toneUri = intent.getStringExtra("alarm_tone") ?: ""

        binding.tvAlarmLabel.text = label.ifEmpty { "Alarm" }

        playTone(toneUri)

        binding.btnDismiss.setOnClickListener {
            stopTone()
            finish()
        }

        binding.btnSnooze.setOnClickListener {
            stopTone()
            AlarmScheduler.scheduleSnooze(this, alarmId, label, toneUri)
            finish()
        }
    }

    private fun playTone(toneUri: String) {
        val uri = if (toneUri.isNotEmpty()) Uri.parse(toneUri)
                  else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ringtone = RingtoneManager.getRingtone(this, uri)
        ringtone?.play()
    }

    private fun stopTone() {
        ringtone?.stop()
        ringtone = null
    }

    override fun onDestroy() {
        stopTone()
        super.onDestroy()
    }
}
