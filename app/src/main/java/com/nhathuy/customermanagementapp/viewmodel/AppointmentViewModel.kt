package com.nhathuy.customermanagementapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.nhathuy.customermanagementapp.database.AppDatabase
import com.nhathuy.customermanagementapp.model.Appointment
import com.nhathuy.customermanagementapp.repository.AppointmentRepository
import kotlinx.coroutines.launch

class AppointmentViewModel(application: Application):AndroidViewModel(application) {

    private val appointmentRepository:AppointmentRepository

    init {
        val appointmentDao=AppDatabase.getDatabase(application).appointmentDao()
        appointmentRepository= AppointmentRepository(appointmentDao)
    }

    fun register(appointment: Appointment) =viewModelScope.launch {
        appointmentRepository.register(appointment)
    }

    fun getAllAppointment(): LiveData<List<Appointment>>{
        return appointmentRepository.getAllAppointment()
    }
}