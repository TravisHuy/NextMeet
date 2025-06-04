package com.nhathuy.nextmeet.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nhathuy.nextmeet.dao.AlarmHistoryDao
import com.nhathuy.nextmeet.dao.AppointmentDao
import com.nhathuy.nextmeet.dao.CustomerDao
import com.nhathuy.nextmeet.dao.NoteDao
import com.nhathuy.nextmeet.dao.TransactionDao
import com.nhathuy.nextmeet.dao.UserDao
import com.nhathuy.nextmeet.dao.NoteImageDao
import com.nhathuy.nextmeet.model.AlarmHistory
import com.nhathuy.nextmeet.model.Appointment
import com.nhathuy.nextmeet.model.Customer
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.Transaction
import com.nhathuy.nextmeet.model.User
import com.nhathuy.nextmeet.model.NoteImage

@Database(entities = [User::class,Customer::class,Appointment::class,Transaction::class,AlarmHistory::class, Note::class, NoteImage::class], version = 3, exportSchema = false)
abstract class AppDatabase:RoomDatabase(){
    abstract fun userDao():UserDao
    abstract fun customerDao():CustomerDao
    abstract fun appointmentDao() : AppointmentDao
    abstract fun transactionDao() : TransactionDao
    abstract fun alarmHistoryDao() : AlarmHistoryDao
    abstract fun noteDao() : NoteDao
    abstract fun noteImageDao(): NoteImageDao
}
