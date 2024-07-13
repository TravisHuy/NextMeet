package com.nhathuy.customermanagementapp.repository

import androidx.lifecycle.LiveData
import com.nhathuy.customermanagementapp.dao.AppointmentDao
import com.nhathuy.customermanagementapp.model.Appointment
import com.nhathuy.customermanagementapp.model.Transaction

class AppointmentRepository(private val appointmentDao: AppointmentDao) {

    suspend fun register(appointment: Appointment){
        appointmentDao.register(appointment)
    }

    fun  getAllAppointment() : LiveData<List<Appointment>>{
        return appointmentDao.getAllAppointment()
    }

    suspend fun editAppointment(appointment: Appointment){
        appointmentDao.editAppointment(appointment)
    }

    suspend fun deleteAppointment(appointment: Appointment){
        appointmentDao.deleteAppointment(appointment)
    }


    suspend fun deleteAllTransactions(){
        appointmentDao.deleteAllAppointments()
    }
}