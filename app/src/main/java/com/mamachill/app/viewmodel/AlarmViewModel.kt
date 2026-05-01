package com.mamachill.app.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.mamachill.app.data.Alarm
import com.mamachill.app.data.AlarmDatabase
import com.mamachill.app.data.AlarmRepository
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AlarmRepository(AlarmDatabase.getInstance(application).alarmDao())
    val allAlarms: LiveData<List<Alarm>> = repository.allAlarms.asLiveData()

    fun insert(alarm: Alarm) = viewModelScope.launch { repository.insert(alarm) }
    fun update(alarm: Alarm) = viewModelScope.launch { repository.update(alarm) }
    fun delete(alarm: Alarm) = viewModelScope.launch { repository.delete(alarm) }

    suspend fun getById(id: Int): Alarm? = repository.getById(id)
}
