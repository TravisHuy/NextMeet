package com.nhathuy.nextmeet.repository

import com.nhathuy.nextmeet.dao.AppointmentDao
import com.nhathuy.nextmeet.model.Appointment
import com.nhathuy.nextmeet.resource.Resource
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