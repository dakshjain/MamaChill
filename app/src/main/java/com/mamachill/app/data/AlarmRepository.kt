package com.mamachill.app.data

import kotlinx.coroutines.flow.Flow

class AlarmRepository(private val dao: AlarmDao) {
    val allAlarms: Flow<List<Alarm>> = dao.getAllAlarms()

    suspend fun insert(alarm: Alarm): Long = dao.insert(alarm)
    suspend fun update(alarm: Alarm) = dao.update(alarm)
    suspend fun delete(alarm: Alarm) = dao.delete(alarm)
    suspend fun getById(id: Int): Alarm? = dao.getById(id)
}
