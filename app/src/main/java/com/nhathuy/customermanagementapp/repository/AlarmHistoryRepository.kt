package com.nhathuy.customermanagementapp.repository

import androidx.lifecycle.LiveData
import com.nhathuy.customermanagementapp.dao.AlarmHistoryDao
import com.nhathuy.customermanagementapp.model.AlarmHistory

class AlarmHistoryRepository(private val alarmHistoryDao: AlarmHistoryDao) {

    val allAlarmHistory: LiveData<List<AlarmHistory>> = alarmHistoryDao.getAllAlarmHistory()

    suspend fun insertAlarmHistory(alarmHistory: AlarmHistory){
        alarmHistoryDao.insertAlarmHistory(alarmHistory)
    }

    fun getUnDisplayedAlarmHistory(): LiveData<List<AlarmHistory>>{
        return alarmHistoryDao.getUnDisplayedAlarmHistory()
    }

    suspend fun markAlarmAsDisplayed(id:Int){
        alarmHistoryDao.markAlarmAsDisplayed(id)
    }

    suspend fun markAllAlarmsAsDisplayed(){
        alarmHistoryDao.markAllAlarmsAsDisplayed()
    }

    suspend fun deleteAlarmHistory(alarmHistory: AlarmHistory){
        alarmHistoryDao.deleteAlarmHistory(alarmHistory)
    }
}