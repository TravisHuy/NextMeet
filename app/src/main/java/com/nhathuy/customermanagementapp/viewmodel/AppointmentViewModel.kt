package com.nhathuy.customermanagementapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.customermanagementapp.model.Appointment
import com.nhathuy.customermanagementapp.repository.AppointmentRepository
import com.nhathuy.customermanagementapp.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppointmentViewModel @Inject constructor(
    private val repository: AppointmentRepository
) : ViewModel() {

    private val _addAppointmentState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val addAppointmentState: StateFlow<Resource<Boolean>> = _addAppointmentState

    private val _allAppointmentsState = MutableStateFlow<Resource<List<Appointment>>>(Resource.Loading())
    val allAppointmentsState: StateFlow<Resource<List<Appointment>>> = _allAppointmentsState

    private val _editAppointmentState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val editAppointmentState: StateFlow<Resource<Boolean>> = _editAppointmentState

    private val _deleteAppointmentState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val deleteAppointmentState: StateFlow<Resource<Boolean>> = _deleteAppointmentState

    private val _deleteAllAppointmentState = MutableStateFlow<Resource<Boolean>>(Resource.Loading())
    val deleteAllAppointmentState: StateFlow<Resource<Boolean>> = _deleteAllAppointmentState

    fun register(appointment: Appointment) {
        viewModelScope.launch {
            repository.register(appointment).collectLatest {
                _addAppointmentState.value = it
            }
        }
    }

    fun getAllAppointments() {
        viewModelScope.launch {
            repository.getAllAppointment().collectLatest {
                _allAppointmentsState.value = it
            }
        }
    }

    fun editAppointment(appointment: Appointment) {
        viewModelScope.launch {
            repository.editAppointment(appointment).collectLatest {
                _editAppointmentState.value = it
            }
        }
    }

    fun deleteAppointment(appointment: Appointment) {
        viewModelScope.launch {
            repository.deleteAppointment(appointment).collectLatest {
                _deleteAppointmentState.value = it
            }
        }
    }

    fun deleteAllAppointments() {
        viewModelScope.launch {
            repository.deleteAllTransactions().collectLatest {
                _deleteAllAppointmentState.value = it
            }
        }
    }
}
