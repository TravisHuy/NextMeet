package com.nhathuy.customermanagementapp.repository

import androidx.lifecycle.LiveData
import com.nhathuy.customermanagementapp.dao.AppointmentDao
import com.nhathuy.customermanagementapp.model.Appointment
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.model.Transaction
import com.nhathuy.customermanagementapp.resource.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppointmentRepository @Inject constructor(private val appointmentDao: AppointmentDao) {

    suspend fun register(appointment: Appointment): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            appointmentDao.register(appointment)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi thêm cuộc hẹn"))
        }
    }

    fun getAllAppointment(): Flow<Resource<List<Appointment>>> = flow {
        emit(Resource.Loading())
        try {
            appointmentDao.getAllAppointment().collect { appointments ->
                emit(Resource.Success(appointments))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi không lấy tất cả cuộc hẹn"))
        }
    }

    suspend fun editAppointment(appointment: Appointment): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            appointmentDao.editAppointment(appointment)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi chỉnh sửa cuộc hẹn"))
        }
    }

    suspend fun deleteAppointment(appointment: Appointment): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            appointmentDao.deleteAppointment(appointment)
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi xóa cuộc hẹn"))
        }
    }


    suspend fun deleteAllTransactions(): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        try {
            appointmentDao.deleteAllAppointments()
            emit(Resource.Success(true))
        } catch (e: Exception) {
            emit(Resource.Error(e.message ?: "Lỗi xóa tất cả cuộc hẹn"))
        }
    }
}