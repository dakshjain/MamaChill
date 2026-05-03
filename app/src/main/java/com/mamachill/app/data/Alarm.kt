package com.mamachill.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val hour: Int,
    val minute: Int,
    val label: String = "",
    val isEnabled: Boolean = true,
    // Bitmask: bit 0=Mon, bit 1=Tue, ..., bit 6=Sun. 0 = one-time alarm.
    val repeatDays: Int = 0,
    val toneUri: String = "",
    val toneName: String = "Default",
    val localAudioPath: String = ""   // Path to ElevenLabs-generated mp3, empty = use toneUri
)
