package com.nhathuy.nextmeet.resource

import com.nhathuy.nextmeet.model.Notification

sealed class NotificationUiState {
    object Idle : NotificationUiState()
    object Loading: NotificationUiState()
    data class NotificationCreated(val notificationId: Int, val message: String) : NotificationUiState()
    data class NotificationLoaded(val notifications: List<Notification>): NotificationUiState()
    data class NotificationDeleted(val message: String): NotificationUiState()
    data class NotificationCancelled(val message: String): NotificationUiState()
    data class NotificationScheduled(val message: String): NotificationUiState()
    data class Success(val message: String): NotificationUiState()
    data class Error(val message: String) : NotificationUiState()
}