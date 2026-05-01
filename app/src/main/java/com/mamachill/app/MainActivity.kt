package com.mamachill.app

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.mamachill.app.data.Alarm
import com.mamachill.app.databinding.ActivityMainBinding
import com.mamachill.app.viewmodel.AlarmViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AlarmViewModel by viewModels()

    private val addEditLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        requestExactAlarmPermission()

        val adapter = AlarmAdapter(
            onToggle = { alarm, enabled ->
                val updated = alarm.copy(isEnabled = enabled)
                viewModel.update(updated)
                if (enabled) AlarmScheduler.schedule(this, updated)
                else AlarmScheduler.cancel(this, alarm.id)
            },
            onDelete = { alarm ->
                AlarmScheduler.cancel(this, alarm.id)
                viewModel.delete(alarm)
            },
            onClick = { alarm ->
                addEditLauncher.launch(
                    Intent(this, AddEditAlarmActivity::class.java).putExtra("alarm_id", alarm.id)
                )
            }
        )

        binding.recyclerAlarms.adapter = adapter

        viewModel.allAlarms.observe(this) { alarms ->
            adapter.submitList(alarms)
            binding.tvEmpty.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE
        }

        binding.fabAdd.setOnClickListener {
            addEditLauncher.launch(Intent(this, AddEditAlarmActivity::class.java))
        }
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val am = getSystemService(ALARM_SERVICE) as AlarmManager
            if (!am.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:$packageName")
                })
            }
        }
    }
}
