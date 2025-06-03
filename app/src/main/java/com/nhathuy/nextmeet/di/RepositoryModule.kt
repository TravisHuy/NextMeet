package com.nhathuy.nextmeet.di

import android.content.Context
import androidx.room.Room
import com.nhathuy.nextmeet.dao.AlarmHistoryDao
import com.nhathuy.nextmeet.dao.AppointmentDao
import com.nhathuy.nextmeet.dao.CustomerDao
import com.nhathuy.nextmeet.dao.NoteDao
import com.nhathuy.nextmeet.dao.TransactionDao
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
            "1_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    fun providerCustomerDao(database: AppDatabase): CustomerDao {
        return database.customerDao()
    }

    @Provides
    fun providerTransactionDao(database: AppDatabase): TransactionDao {
        return database.transactionDao()
    }

    @Provides
    fun providerAppointment(database: AppDatabase): AppointmentDao {
        return database.appointmentDao()
    }

    @Provides
    fun providerAlarmHistory(database: AppDatabase): AlarmHistoryDao {
        return database.alarmHistoryDao()
    }

    @Provides
    fun providerNoteDao(database: AppDatabase):NoteDao{
        return database.noteDao()
    }
}