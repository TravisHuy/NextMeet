package com.nhathuy.customermanagementapp.di

import com.nhathuy.customermanagementapp.dao.AlarmHistoryDao
import com.nhathuy.customermanagementapp.dao.AppointmentDao
import com.nhathuy.customermanagementapp.dao.CustomerDao
import com.nhathuy.customermanagementapp.dao.TransactionDao
import com.nhathuy.customermanagementapp.dao.UserDao
import com.nhathuy.customermanagementapp.repository.AlarmHistoryRepository
import com.nhathuy.customermanagementapp.repository.AppointmentRepository
import com.nhathuy.customermanagementapp.repository.CustomerRepository
import com.nhathuy.customermanagementapp.repository.TransactionRepository
import com.nhathuy.customermanagementapp.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ObjectModule {

    @Singleton
    @Provides
    fun provideUserRepository(userDao: UserDao): UserRepository {
        return UserRepository(userDao)
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