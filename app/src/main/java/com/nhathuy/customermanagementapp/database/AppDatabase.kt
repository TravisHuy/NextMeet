package com.nhathuy.customermanagementapp.database

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nhathuy.customermanagementapp.dao.AlarmHistoryDao
import com.nhathuy.customermanagementapp.dao.AppointmentDao
import com.nhathuy.customermanagementapp.dao.CustomerDao
import com.nhathuy.customermanagementapp.dao.TransactionDao
import com.nhathuy.customermanagementapp.dao.UserDao
import com.nhathuy.customermanagementapp.model.AlarmHistory
import com.nhathuy.customermanagementapp.model.Appointment
import com.nhathuy.customermanagementapp.model.Customer
import com.nhathuy.customermanagementapp.model.Transaction
import com.nhathuy.customermanagementapp.model.User

@Database(entities = [User::class,Customer::class,Appointment::class,Transaction::class,AlarmHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase:RoomDatabase(){
    abstract fun userDao():UserDao
    abstract fun customerDao():CustomerDao
    abstract fun appointmentDao() : AppointmentDao
    abstract fun transactionDao() : TransactionDao
    abstract fun alarmHistoryDao() : AlarmHistoryDao
}