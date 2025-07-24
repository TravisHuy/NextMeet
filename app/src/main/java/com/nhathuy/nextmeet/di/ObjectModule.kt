package com.nhathuy.nextmeet.di

import android.content.Context
import com.nhathuy.nextmeet.dao.AlarmHistoryDao
import com.nhathuy.nextmeet.dao.AppointmentPlusDao
import com.nhathuy.nextmeet.dao.ContactDao
import com.nhathuy.nextmeet.dao.NoteDao
import com.nhathuy.nextmeet.dao.NoteImageDao
import com.nhathuy.nextmeet.dao.SearchHistoryDao
import com.nhathuy.nextmeet.dao.UserDao
import com.nhathuy.nextmeet.repository.AlarmHistoryRepository
import com.nhathuy.nextmeet.repository.AppointmentPlusRepository
import com.nhathuy.nextmeet.repository.ContactRepository
import com.nhathuy.nextmeet.repository.NoteRepository
import com.nhathuy.nextmeet.repository.NotificationRepository
import com.nhathuy.nextmeet.repository.SearchRepository
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
    fun provideUserRepository(userDao: UserDao, sessionManager: SessionManager,@ApplicationContext context: Context): UserRepository {
        return UserRepository(userDao, sessionManager,context)
    }


    @Singleton
    @Provides
    fun providerAlarmHistoryRepository(alarmHistoryDao: AlarmHistoryDao): AlarmHistoryRepository {
        return AlarmHistoryRepository(alarmHistoryDao)
    }

    @Singleton
    @Provides
    fun providerNoteRepository(noteDao: NoteDao, noteImageDao: NoteImageDao, imageManager: ImageManager,@ApplicationContext context: Context): NoteRepository {
        return NoteRepository(noteDao, noteImageDao, imageManager,context)
    }

    @Singleton
    @Provides
    fun providerContactRepository(contactDao: ContactDao,@ApplicationContext context: Context): ContactRepository {
        return ContactRepository(contactDao,context)
    }

    @Singleton
    @Provides
    fun providerAppointmentPlusRepository(
        appointmentDao: AppointmentPlusDao,
        contactDao: ContactDao,
        @ApplicationContext context: Context
    ): AppointmentPlusRepository {
        return AppointmentPlusRepository(appointmentDao, contactDao,context)
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