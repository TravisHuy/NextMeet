package com.nhathuy.nextmeet.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nhathuy.nextmeet.dao.AlarmHistoryDao
import com.nhathuy.nextmeet.dao.AppointmentPlusDao
import com.nhathuy.nextmeet.dao.ContactDao
import com.nhathuy.nextmeet.dao.NoteDao
import com.nhathuy.nextmeet.dao.UserDao
import com.nhathuy.nextmeet.dao.NoteImageDao
import com.nhathuy.nextmeet.dao.NotificationDao
import com.nhathuy.nextmeet.dao.SearchHistoryDao
import com.nhathuy.nextmeet.model.AlarmHistory
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.Note
import com.nhathuy.nextmeet.model.User
import com.nhathuy.nextmeet.model.NoteImage
import com.nhathuy.nextmeet.model.Notification
import com.nhathuy.nextmeet.model.SearchHistory
import com.nhathuy.nextmeet.utils.AppointmentStatusConverter

@Database(entities = [User::class,AlarmHistory::class, Note::class, NoteImage::class, Contact::class, AppointmentPlus::class,SearchHistory::class, Notification::class], version = 3, exportSchema = false)
@TypeConverters(AppointmentStatusConverter::class)
abstract class AppDatabase:RoomDatabase(){
    abstract fun userDao():UserDao
    abstract fun alarmHistoryDao() : AlarmHistoryDao
    abstract fun noteDao() : NoteDao
    abstract fun noteImageDao(): NoteImageDao
    abstract fun contactDao() : ContactDao
    abstract fun appointmentPlusDao() : AppointmentPlusDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun notificationDao() : NotificationDao
}
