package com.nhathuy.customermanagementapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.nhathuy.customermanagementapp.dao.AlarmHistoryDao
import com.nhathuy.customermanagementapp.database.AppDatabase
import com.nhathuy.customermanagementapp.model.AlarmHistory
import com.nhathuy.customermanagementapp.repository.AlarmHistoryRepository
import com.nhathuy.customermanagementapp.repository.AppointmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmHistoryViewModel(application: Application): AndroidViewModel(application) {

    private val repository: AlarmHistoryRepository
    val allAlarmHistory: LiveData<List<AlarmHistory>>

     init {
        val alarmHistoryDao=AppDatabase.getDatabase(application).alarmHistoryDao()
        repository= AlarmHistoryRepository(alarmHistoryDao)
        allAlarmHistory= repository.allAlarmHistory
     }

    fun insertAlarmHistory(alarmHistory: AlarmHistory) = viewModelScope.launch {
         repository.insertAlarmHistory(alarmHistory)
    }

    fun markAllAlarmsAsDisplayed() = viewModelScope.launch {
        repository.markAllAlarmsAsDisplayed()
    }

    fun getUnDisplayedAlarmHistory(): LiveData<List<AlarmHistory>> {
        return repository.getUnDisplayedAlarmHistory()
    }

    fun markAlarmAsDisplayed(id: Int) = viewModelScope.launch {
        repository.markAlarmAsDisplayed(id)
    }

    fun deleteAlarmHistory(alarmHistory: AlarmHistory){
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAlarmHistory(alarmHistory)
        }
    }

}