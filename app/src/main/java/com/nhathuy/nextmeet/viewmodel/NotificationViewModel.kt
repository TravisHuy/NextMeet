package com.nhathuy.nextmeet.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.nextmeet.model.Notification
import com.nhathuy.nextmeet.model.NotificationType
import com.nhathuy.nextmeet.repository.NotificationRepository
import com.nhathuy.nextmeet.resource.NotificationUiState
import com.nhathuy.nextmeet.utils.NotificationManagerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationRepository: NotificationRepository,
    private val notificationManagerService: NotificationManagerService
) : ViewModel() {

    private val _notificationUiState =
        MutableStateFlow<NotificationUiState>(NotificationUiState.Idle)
    val notificationUiState: StateFlow<NotificationUiState> = _notificationUiState

    // current user id
    private val _currentUserId = MutableStateFlow(0)
    val currentUserId: StateFlow<Int> = _currentUserId.asStateFlow()

    // real-time-data-flows
    val allNotifications: StateFlow<List<Notification>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId != 0) {
                notificationRepository.getNotificationsByUserId(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingNotifications: StateFlow<List<Notification>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId != 0) {
                notificationRepository.getPendingNotifications(userId)
            } else {
                flowOf(emptyList())
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadNotifications: StateFlow<List<Notification>> = _currentUserId
        .flatMapLatest { userId ->
            if (userId != 0) {
                notificationRepository.getUnreadNotifications(userId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val unreadCount: StateFlow<Int> = unreadNotifications
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val pendingCount: StateFlow<Int> = pendingNotifications
        .map { it.size }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // dat user hien tai
    fun setCurrentUserId(userId: Int) {
        _currentUserId.value = userId
    }

    /**
     * Schedule notification cho appointment
     */
    fun scheduleAppointmentNotification(
        userId: Int,
        appointmentId: Int,
        title: String,
        description: String,
        appointmentTime: Long,
        location: String? = null,
        contactName: String? = null
    ) {
        viewModelScope.launch {
            try {
                Log.d("NotificationViewModel", "Starting notification scheduling...")
                Log.d("NotificationViewModel", "Parameters - UserId: $userId, AppointmentId: $appointmentId")
                Log.d("NotificationViewModel", "Title: $title, Contact: $contactName, Location: $location")
                
                _notificationUiState.value = NotificationUiState.Loading

                val success = notificationManagerService.scheduleAppointmentNotification(
                    userId = userId,
                    appointmentId = appointmentId,
                    title = title,
                    description = description,
                    appointmentTime = appointmentTime,
                    location = location,
                    contactName = contactName
                )

                if (success) {
                    Log.d("NotificationViewModel", "Notification scheduled successfully")
                    _notificationUiState.value =
                        NotificationUiState.NotificationScheduled("Đã đặt nhắc nhở cho cuộc hẹn 5 phút trước")
                } else {
                    Log.e("NotificationViewModel", "Failed to schedule notification")
                    _notificationUiState.value =
                        NotificationUiState.Error("Không thể đặt nhắc nhở. Kiểm tra quyền hoặc thời gian")
                }
            } catch (e: Exception) {
                Log.e("NotificationViewModel", "Exception during notification scheduling", e)
                _notificationUiState.value = NotificationUiState.Error(
                    "Loi dat nhac nho : ${e.message}"
                )
            }
        }
    }

    /**
     * Schedule notification cho note
     */
    fun scheduleNoteNotification(
        userId: Int,
        noteId: Int,
        title: String,
        content: String,
        noteTime: Long
    ) {
        viewModelScope.launch {
            try {
                _notificationUiState.value = NotificationUiState.Loading

                val success = notificationManagerService.scheduleNoteNotification(
                    userId = userId,
                    noteId = noteId,
                    title = title,
                    content = content,
                    noteTime = noteTime
                )

                if (success) {
                    _notificationUiState.value = NotificationUiState.NotificationScheduled(
                        "Đã đặt nhắc nhở cho ghi chú 5 phút trước"
                    )
                } else {
                    _notificationUiState.value = NotificationUiState.Error(
                        "Không thể đặt nhắc nhở. Kiểm tra quyền hoặc thời gian"
                    )
                }
            } catch (e: Exception) {
                _notificationUiState.value = NotificationUiState.Error(
                    "Lỗi đặt nhắc nhở: ${e.message}"
                )
            }
        }
    }

    /**
     * huy thong bao
     */
    fun cancelNotification(notificationId: Int) {
        viewModelScope.launch {
            try {
                notificationManagerService.cancelNotification(notificationId)
                _notificationUiState.value = NotificationUiState.NotificationCancelled("Da huy nhac nho")
            } catch (e: Exception) {
                _notificationUiState.value = NotificationUiState.Error(
                    "Lỗi hủy nhắc nhở: ${e.message}"
                )
            }
        }
    }

    /**
     * Cancel all notifications for appointment
     */
    fun cancelAppointmentNotifications(appointmentId: Int) {
        viewModelScope.launch {
            try {
                // Cancel from database first
                notificationRepository.deleteNotificationsByRelatedId(
                    appointmentId,
                    NotificationType.APPOINTMENT_REMINDER
                ).onSuccess {
                    _notificationUiState.value = NotificationUiState.NotificationCancelled(
                        "Đã hủy tất cả nhắc nhở cho cuộc hẹn"
                    )
                }.onFailure { e ->
                    _notificationUiState.value = NotificationUiState.Error(
                        "Lỗi hủy nhắc nhở: ${e.message}"
                    )
                }
            } catch (e: Exception) {
                _notificationUiState.value = NotificationUiState.Error(
                    "Lỗi hủy nhắc nhở: ${e.message}"
                )
            }
        }
    }

    /**
     * Load all notifications for current user
     */
    fun loadAllNotifications() {
        viewModelScope.launch {
            try {
                _notificationUiState.value = NotificationUiState.Loading
                val userId = _currentUserId.value

                if (userId != 0) {
                    // Notifications are automatically loaded via StateFlow
                    _notificationUiState.value = NotificationUiState.Success("Đã tải thông báo")
                } else {
                    _notificationUiState.value = NotificationUiState.Error("Chưa đăng nhập")
                }
            } catch (e: Exception) {
                _notificationUiState.value = NotificationUiState.Error(
                    "Lỗi tải thông báo: ${e.message}"
                )
            }
        }
    }

    /**
     * Load pending notifications only
     */
    fun loadPendingNotifications() {
        viewModelScope.launch {
            try {
                _notificationUiState.value = NotificationUiState.Loading
                val userId = _currentUserId.value

                if (userId != 0) {
                    // Notifications are automatically loaded via StateFlow
                    _notificationUiState.value = NotificationUiState.Success("Đã tải thông báo chờ")
                } else {
                    _notificationUiState.value = NotificationUiState.Error("Chưa đăng nhập")
                }
            } catch (e: Exception) {
                _notificationUiState.value = NotificationUiState.Error(
                    "Lỗi tải thông báo chờ: ${e.message}"
                )
            }
        }
    }

    /**
     * Load unread notifications only
     */
    fun loadUnreadNotifications() {
        viewModelScope.launch {
            try {
                _notificationUiState.value = NotificationUiState.Loading
                val userId = _currentUserId.value

                if (userId != 0) {
                    // Notifications are automatically loaded via StateFlow
                    _notificationUiState.value = NotificationUiState.Success("Đã tải thông báo chưa đọc")
                } else {
                    _notificationUiState.value = NotificationUiState.Error("Chưa đăng nhập")
                }
            } catch (e: Exception) {
                _notificationUiState.value = NotificationUiState.Error(
                    "Lỗi tải thông báo chưa đọc: ${e.message}"
                )
            }
        }
    }

    /**
     * Mark notification as read
     */
    fun markNotificationAsRead(notificationId: Int) {
        viewModelScope.launch {
            try {
                notificationRepository.markAsRead(notificationId).onSuccess {
                    _notificationUiState.value = NotificationUiState.Success("Đã đánh dấu đã đọc")
                }.onFailure { e ->
                    _notificationUiState.value = NotificationUiState.Error(
                        "Lỗi đánh dấu đã đọc: ${e.message}"
                    )
                }
            } catch (e: Exception) {
                _notificationUiState.value = NotificationUiState.Error(
                    "Lỗi đánh dấu đã đọc: ${e.message}"
                )
            }
        }
    }

    /**
     * Mark all notifications as read
     */
    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val userId = _currentUserId.value
                if (userId != 0) {
                    notificationRepository.markAllAsRead(userId).onSuccess {
                        _notificationUiState.value = NotificationUiState.Success(
                            "Đã đánh dấu tất cả thông báo là đã đọc"
                        )
                    }.onFailure { e ->
                        _notificationUiState.value = NotificationUiState.Error(
                            "Lỗi đánh dấu đã đọc: ${e.message}"
                        )
                    }
                } else {
                    _notificationUiState.value = NotificationUiState.Error("Chưa đăng nhập")
                }
            } catch (e: Exception) {
                _notificationUiState.value = NotificationUiState.Error(
                    "Lỗi đánh dấu đã đọc: ${e.message}"
                )
            }
        }
    }

    /**
     * Delete notification
     */
    fun deleteNotification(notificationId: Int) {
        viewModelScope.launch {
            try {
                // Cancel alarm first, then delete from database
                notificationManagerService.cancelNotification(notificationId)
                _notificationUiState.value = NotificationUiState.NotificationDeleted(
                    "Đã xóa thông báo"
                )
            } catch (e: Exception) {
                _notificationUiState.value = NotificationUiState.Error(
                    "Lỗi xóa thông báo: ${e.message}"
                )
            }
        }
    }

    /**
     * Clear all notifications for current user
     */
    fun clearAllNotifications() {
        viewModelScope.launch {
            try {
                val userId = _currentUserId.value
                if (userId != 0) {
                    // Clear from database
                    notificationRepository.clearAllNotifications(userId).onSuccess {
                        _notificationUiState.value = NotificationUiState.Success(
                            "Đã xóa tất cả thông báo"
                        )
                    }.onFailure { e ->
                        _notificationUiState.value = NotificationUiState.Error(
                            "Lỗi xóa thông báo: ${e.message}"
                        )
                    }
                } else {
                    _notificationUiState.value = NotificationUiState.Error("Chưa đăng nhập")
                }
            } catch (e: Exception) {
                _notificationUiState.value = NotificationUiState.Error(
                    "Lỗi xóa thông báo: ${e.message}"
                )
            }
        }
    }

    /**
     * Get notification by ID
     */
    fun getNotificationById(notificationId: Int) {
        viewModelScope.launch {
            try {
                _notificationUiState.value = NotificationUiState.Loading
                notificationRepository.getNotificationById(notificationId).onSuccess { notification ->
                    if (notification != null) {
                        _notificationUiState.value = NotificationUiState.Success("Đã tải thông báo")
                    } else {
                        _notificationUiState.value = NotificationUiState.Error("Không tìm thấy thông báo")
                    }
                }.onFailure { e ->
                    _notificationUiState.value = NotificationUiState.Error(
                        "Lỗi tải thông báo: ${e.message}"
                    )
                }
            } catch (e: Exception) {
                _notificationUiState.value = NotificationUiState.Error(
                    "Lỗi tải thông báo: ${e.message}"
                )
            }
        }
    }

    /**
     * Check notification permissions
     */
    fun checkNotificationPermissions(): Boolean {
        return notificationManagerService.hasExactAlarmPermission()
    }

    /**
     * Clean up expired notifications
     */
    fun cleanupExpiredNotifications() {
        viewModelScope.launch {
            try {
                notificationRepository.deleteExpiredNotifications().onSuccess {
                    _notificationUiState.value = NotificationUiState.Success(
                        "Đã dọn dẹp thông báo cũ"
                    )
                }.onFailure { e ->
                    _notificationUiState.value = NotificationUiState.Error(
                        "Lỗi dọn dẹp: ${e.message}"
                    )
                }
            } catch (e: Exception) {
                _notificationUiState.value = NotificationUiState.Error(
                    "Lỗi dọn dẹp: ${e.message}"
                )
            }
        }
    }

    /**
     * Reset UI state to idle
     */
    fun resetUiState() {
        _notificationUiState.value = NotificationUiState.Idle
    }

    /**
     * Check if there are any pending notifications
     */
    fun hasPendingNotifications(): Boolean {
        return pendingCount.value > 0
    }

    /**
     * Check if there are any unread notifications
     */
    fun hasUnreadNotifications(): Boolean {
        return unreadCount.value > 0
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup when ViewModel is destroyed
        notificationManagerService.cleanup()
    }
}