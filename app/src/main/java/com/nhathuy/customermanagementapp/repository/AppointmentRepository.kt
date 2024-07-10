package com.nhathuy.customermanagementapp.repository

import androidx.lifecycle.LiveData
import com.nhathuy.customermanagementapp.dao.AppointmentDao
import com.nhathuy.customermanagementapp.model.Appointment

class AppointmentRepository(private val appointmentDao: AppointmentDao) {

    suspend fun register(appointment: Appointment){
        appointmentDao.register(appointment)
    }

    fun  getAllAppointment() : LiveData<List<Appointment>>{
        return appointmentDao.getAllAppointment()
    }
}