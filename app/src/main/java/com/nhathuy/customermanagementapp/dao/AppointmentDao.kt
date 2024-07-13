package com.nhathuy.customermanagementapp.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.nhathuy.customermanagementapp.model.Appointment
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.model.Transaction

@Dao
interface AppointmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun register(appointment: Appointment)

    @Query("select * from appointments")
    fun  getAllAppointment(): LiveData<List<Appointment>>


    @Update
    suspend fun editAppointment(appointment: Appointment)

    @Delete
    suspend fun deleteAppointment(appointment: Appointment)

    @Query("delete from appointments")
    suspend fun deleteAllAppointments()
}