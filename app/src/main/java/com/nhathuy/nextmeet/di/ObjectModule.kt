package com.nhathuy.nextmeet.di

import android.content.Context
import com.nhathuy.nextmeet.dao.AlarmHistoryDao
import com.nhathuy.nextmeet.dao.AppointmentDao
import com.nhathuy.nextmeet.dao.CustomerDao
import com.nhathuy.nextmeet.dao.TransactionDao
import com.nhathuy.nextmeet.dao.UserDao
import com.nhathuy.nextmeet.repository.AlarmHistoryRepository
import com.nhathuy.nextmeet.repository.AppointmentRepository
import com.nhathuy.nextmeet.repository.CustomerRepository
import com.nhathuy.nextmeet.repository.TransactionRepository
import com.nhathuy.nextmeet.repository.UserRepository
import com.nhathuy.nextmeet.utils.SessionManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ObjectModule {

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context):SessionManager{
        return SessionManager(context)
    }

    @Singleton
    @Provides
    fun provideUserRepository(userDao: UserDao,sessionManager: SessionManager): UserRepository {
        return UserRepository(userDao,sessionManager)
    }

    @Singleton
    @Provides
    fun provideCustomerRepository(customerDao: CustomerDao): CustomerRepository {
        return CustomerRepository(customerDao)
    }

    @Singleton
    @Provides
    fun providerAppointmentRepository(appointmentDao: AppointmentDao) : AppointmentRepository {
        return AppointmentRepository(appointmentDao)
    }

    @Singleton
    @Provides
    fun providerTransactionRepository(transactionDao: TransactionDao) : TransactionRepository {
        return TransactionRepository(transactionDao)
    }

    @Singleton
    @Provides
    fun providerAlarmHistoryRepository(alarmHistoryDao: AlarmHistoryDao) : AlarmHistoryRepository{
        return AlarmHistoryRepository(alarmHistoryDao)
    }

}