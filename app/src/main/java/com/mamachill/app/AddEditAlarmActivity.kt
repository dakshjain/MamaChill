package com.mamachill.app

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.mamachill.app.data.Alarm
import com.mamachill.app.databinding.ActivityAddEditAlarmBinding
import com.mamachill.app.viewmodel.AlarmViewModel
import kotlinx.coroutines.launch
import java.util.Locale

class AddEditAlarmActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditAlarmBinding
    private val viewModel: AlarmViewModel by viewModels()

    private var editAlarmId: Int = -1
    private var selectedHour = 8
    private var selectedMinute = 0
    private var selectedToneUri: Uri? = null
    private var selectedToneName = "Default"

    private val ringtoneLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            selectedToneUri = uri
            selectedToneName = if (uri != null) {
                RingtoneManager.getRingtone(this, uri)?.getTitle(this) ?: "Default"
            } else "Default"
            binding.btnTone.text = selectedToneName
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        editAlarmId = intent.getIntExtra("alarm_id", -1)

        if (editAlarmId != -1) {
            supportActionBar?.title = "Edit Alarm"
            lifecycleScope.launch {
                viewModel.getById(editAlarmId)?.let { populateFrom(it) }
            }
        } else {
            supportActionBar?.title = "New Alarm"
        }

        updateTimeDisplay()

        binding.cardTime.setOnClickListener { showTimePicker() }

        binding.btnTone.setOnClickListener {
            ringtoneLauncher.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                selectedToneUri?.let { putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, it) }
            })
        }

        binding.btnSave.setOnClickListener { saveAlarm() }
    }

    private fun populateFrom(alarm: Alarm) {
        selectedHour = alarm.hour
        selectedMinute = alarm.minute
        binding.etLabel.setText(alarm.label)
        selectedToneName = alarm.toneName
        selectedToneUri = alarm.toneUri.takeIf { it.isNotEmpty() }?.let { Uri.parse(it) }
        binding.btnTone.text = selectedToneName
        updateTimeDisplay()
        setRepeatDays(alarm.repeatDays)
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(selectedHour)
            .setMinute(selectedMinute)
            .setTitleText("Set alarm time")
            .build()
        picker.addOnPositiveButtonClickListener {
            selectedHour = picker.hour
            selectedMinute = picker.minute
            updateTimeDisplay()
        }
        picker.show(supportFragmentManager, "time_picker")
    }

    private fun updateTimeDisplay() {
        val hour12 = when {
            selectedHour == 0 -> 12
            selectedHour > 12 -> selectedHour - 12
            else -> selectedHour
        }
        binding.tvSelectedTime.text =
            String.format(Locale.getDefault(), "%d:%02d", hour12, selectedMinute)
        binding.tvAmPm.text = if (selectedHour < 12) "AM" else "PM"
    }

    private fun getRepeatDays(): Int {
        var mask = 0
        if (binding.chipMon.isChecked) mask = mask or (1 shl 0)
        if (binding.chipTue.isChecked) mask = mask or (1 shl 1)
        if (binding.chipWed.isChecked) mask = mask or (1 shl 2)
        if (binding.chipThu.isChecked) mask = mask or (1 shl 3)
        if (binding.chipFri.isChecked) mask = mask or (1 shl 4)
        if (binding.chipSat.isChecked) mask = mask or (1 shl 5)
        if (binding.chipSun.isChecked) mask = mask or (1 shl 6)
        return mask
    }

    private fun setRepeatDays(mask: Int) {
        binding.chipMon.isChecked = mask and (1 shl 0) != 0
        binding.chipTue.isChecked = mask and (1 shl 1) != 0
        binding.chipWed.isChecked = mask and (1 shl 2) != 0
        binding.chipThu.isChecked = mask and (1 shl 3) != 0
        binding.chipFri.isChecked = mask and (1 shl 4) != 0
        binding.chipSat.isChecked = mask and (1 shl 5) != 0
        binding.chipSun.isChecked = mask and (1 shl 6) != 0
    }

    private fun saveAlarm() {
        val alarm = Alarm(
            id = if (editAlarmId != -1) editAlarmId else 0,
            hour = selectedHour,
            minute = selectedMinute,
            label = binding.etLabel.text.toString().trim(),
            isEnabled = true,
            repeatDays = getRepeatDays(),
            toneUri = selectedToneUri?.toString() ?: "",
            toneName = selectedToneName
        )

        lifecycleScope.launch {
            val savedId = if (editAlarmId != -1) {
                viewModel.update(alarm)
                editAlarmId
            } else {
                viewModel.insert(alarm).toInt().also { /* id returned */ }
                // Re-fetch to get the auto-generated id
                -1
            }
            // Schedule via observer in MainActivity — finish and let it handle scheduling
        }
        AlarmScheduler.schedule(this, alarm)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
