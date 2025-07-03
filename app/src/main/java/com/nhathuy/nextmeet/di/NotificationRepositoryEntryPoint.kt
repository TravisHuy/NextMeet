package com.nhathuy.nextmeet.di

import com.nhathuy.nextmeet.repository.NotificationRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface NotificationRepositoryEntryPoint {
    fun notificationRepository(): NotificationRepository
}
