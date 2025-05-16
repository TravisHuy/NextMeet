package com.nhathuy.customermanagementapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.customermanagementapp.model.AlarmHistory
import com.nhathuy.customermanagementapp.repository.AlarmHistoryRepository
import com.nhathuy.customermanagementapp.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmHistoryViewModel @Inject constructor(
    private val repository: AlarmHistoryRepository
) : ViewModel() {

    private val _insertState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val insertState: StateFlow<Resource<Boolean>> = _insertState

    private val _unDisplayedAlarms = MutableStateFlow<Resource<List<AlarmHistory>>>(Resource.Loading())
    val unDisplayedAlarms: StateFlow<Resource<List<AlarmHistory>>> = _unDisplayedAlarms

    private val _deleteState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val deleteState: StateFlow<Resource<Boolean>> = _deleteState

    private val _markOneDisplayedState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val markOneDisplayedState: StateFlow<Resource<Boolean>> = _markOneDisplayedState

    private val _markAllDisplayedState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val markAllDisplayedState: StateFlow<Resource<Boolean>> = _markAllDisplayedState

    fun insertAlarmHistory(alarmHistory: AlarmHistory) {
        viewModelScope.launch {
            repository.insertAlarmHistory(alarmHistory).collectLatest {
                _insertState.value = it
            }
        }
    }

    fun getUnDisplayedAlarmHistory() {
        viewModelScope.launch {
            repository.getUnDisplayedAlarmHistory().collectLatest {
                _unDisplayedAlarms.value = it
            }
        }
    }

    fun deleteAlarmHistory(alarmHistory: AlarmHistory) {
        viewModelScope.launch {
            repository.deleteAlarmHistory(alarmHistory).collectLatest {
                _deleteState.value = it
            }
        }
    }

    fun markAlarmAsDisplayed(id: Int) {
        viewModelScope.launch {
            repository.markAlarmAsDisplayed(id).collectLatest {
                _markOneDisplayedState.value = it
            }
        }
    }

    fun markAllAlarmsAsDisplayed() {
        viewModelScope.launch {
            repository.markAllAlarmsAsDisplayed().collectLatest {
                _markAllDisplayedState.value = it
            }
        }
    }
}
