package com.nhathuy.nextmeet.di

import android.content.Context
import com.nhathuy.nextmeet.dao.AlarmHistoryDao
import com.nhathuy.nextmeet.dao.AppointmentDao
import com.nhathuy.nextmeet.dao.AppointmentPlusDao
import com.nhathuy.nextmeet.dao.ContactDao
import com.nhathuy.nextmeet.dao.CustomerDao
import com.nhathuy.nextmeet.dao.NoteDao
import com.nhathuy.nextmeet.dao.NoteImageDao
import com.nhathuy.nextmeet.dao.SearchHistoryDao
import com.nhathuy.nextmeet.dao.TransactionDao
import com.nhathuy.nextmeet.dao.UserDao
import com.nhathuy.nextmeet.repository.AlarmHistoryRepository
import com.nhathuy.nextmeet.repository.AppointmentPlusRepository
import com.nhathuy.nextmeet.repository.AppointmentRepository
import com.nhathuy.nextmeet.repository.ContactRepository
import com.nhathuy.nextmeet.repository.CustomerRepository
import com.nhathuy.nextmeet.repository.NoteRepository
import com.nhathuy.nextmeet.repository.NotificationRepository
import com.nhathuy.nextmeet.repository.SearchRepository
import com.nhathuy.nextmeet.repository.TransactionRepository
import com.nhathuy.nextmeet.repository.UserRepository
import com.nhathuy.nextmeet.utils.ImageManager
import com.nhathuy.nextmeet.utils.NotificationManagerService
import com.nhathuy.nextmeet.utils.SessionManager
import com.nhathuy.nextmeet.utils.UniversalSearchManager
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
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    @Singleton
    @Provides
    fun provideUserRepository(userDao: UserDao, sessionManager: SessionManager): UserRepository {
        return UserRepository(userDao, sessionManager)
    }

    @Singleton
    @Provides
    fun provideCustomerRepository(customerDao: CustomerDao): CustomerRepository {
        return CustomerRepository(customerDao)
    }

    @Singleton
    @Provides
    fun providerAppointmentRepository(appointmentDao: AppointmentDao): AppointmentRepository {
        return AppointmentRepository(appointmentDao)
    }

    @Singleton
    @Provides
    fun providerTransactionRepository(transactionDao: TransactionDao): TransactionRepository {
        return TransactionRepository(transactionDao)
    }

    @Singleton
    @Provides
    fun providerAlarmHistoryRepository(alarmHistoryDao: AlarmHistoryDao): AlarmHistoryRepository {
        return AlarmHistoryRepository(alarmHistoryDao)
    }

    @Singleton
    @Provides
    fun providerNoteRepository(noteDao: NoteDao, noteImageDao: NoteImageDao, imageManager: ImageManager): NoteRepository {
        return NoteRepository(noteDao, noteImageDao, imageManager)
    }

    @Singleton
    @Provides
    fun providerContactRepository(contactDao: ContactDao): ContactRepository {
        return ContactRepository(contactDao)
    }

    @Singleton
    @Provides
    fun providerAppointmentPlusRepository(
        appointmentDao: AppointmentPlusDao,
        contactDao: ContactDao
    ): AppointmentPlusRepository {
        return AppointmentPlusRepository(appointmentDao, contactDao)
    }

    @Provides
    @Singleton
    fun providerSearchHistoryRepository(
        @ApplicationContext context: Context,
        searchHistoryDao: SearchHistoryDao,
        contactDao: ContactDao,
        appointmentDao: AppointmentPlusDao,
        noteDao: NoteDao
    ): SearchRepository {
        return SearchRepository(context, searchHistoryDao, contactDao, appointmentDao, noteDao)
    }

    @Provides
    @Singleton
    fun providerUniversalManager(
        searchRepository: SearchRepository,
        @ApplicationContext context: Context
    ): UniversalSearchManager {
        return UniversalSearchManager(searchRepository,context)
    }

    @Provides
    @Singleton
    fun providerImageManager(
        @ApplicationContext context: Context
    ): ImageManager {
        return ImageManager(context)
    }

    @Provides
    @Singleton
    fun providerNotificationManagerService(
        @ApplicationContext context : Context,
        notificationRepository: NotificationRepository
    ) : NotificationManagerService{
        return NotificationManagerService(context,notificationRepository)
    }
}