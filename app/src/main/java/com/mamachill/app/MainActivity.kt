package com.mamachill.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.mamachill.app.databinding.ActivityMainBinding
import com.mamachill.app.viewmodel.AlarmViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: AlarmViewModel by viewModels()

    private val addEditLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* no action needed after result */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        requestNotificationPermission()
        requestExactAlarmPermission()
        requestBatteryOptimizationExemption()
        requestFullScreenIntentPermission()

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

    // Required at runtime on Android 13+ (API 33+) — without this notifications are silently blocked
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Android 14+ requires explicit user approval to use full-screen intents (lock-screen alarm UI)
    private fun requestFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= 34) {
            val nm = getSystemService(NotificationManager::class.java)
            if (!nm.canUseFullScreenIntent()) {
                startActivity(
                    Intent("android.settings.MANAGE_APP_USE_FULL_SCREEN_INTENTS").apply {
                        data = Uri.parse("package:$packageName")
                    }
                )
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(android.os.PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:$packageName")
            })
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
