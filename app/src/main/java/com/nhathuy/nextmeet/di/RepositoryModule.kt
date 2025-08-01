package com.nhathuy.nextmeet.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nhathuy.nextmeet.dao.AlarmHistoryDao
import com.nhathuy.nextmeet.dao.AppointmentPlusDao
import com.nhathuy.nextmeet.dao.ContactDao
import com.nhathuy.nextmeet.dao.NoteDao
import com.nhathuy.nextmeet.dao.NoteImageDao
import com.nhathuy.nextmeet.dao.NotificationDao
import com.nhathuy.nextmeet.dao.SearchHistoryDao
import com.nhathuy.nextmeet.dao.UserDao
import com.nhathuy.nextmeet.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context, AppDatabase::class.java,
            "note_track_database"
        )
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }


    @Provides
    fun providerAlarmHistory(database: AppDatabase): AlarmHistoryDao {
        return database.alarmHistoryDao()
    }

    @Provides
    fun providerNoteDao(database: AppDatabase): NoteDao {
        return database.noteDao()
    }

    @Provides
    fun providerNoteImageDao(database: AppDatabase): NoteImageDao {
        return database.noteImageDao()
    }

    @Provides
    fun providerContactDao(database: AppDatabase): ContactDao {
        return database.contactDao()
    }

    @Provides
    fun providerAppointmentPlusDao(database: AppDatabase): AppointmentPlusDao {
        return database.appointmentPlusDao()
    }

    @Provides
    fun providerSearchHistoryDao(database: AppDatabase): SearchHistoryDao {
        return database.searchHistoryDao()
    }

    @Provides
    fun providerNotificationDao(database: AppDatabase): NotificationDao {
        return database.notificationDao()
    }
}