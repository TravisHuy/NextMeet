package com.nhathuy.nextmeet.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhathuy.nextmeet.model.Appointment
import com.nhathuy.nextmeet.model.AppointmentPlus
import com.nhathuy.nextmeet.model.AppointmentStatus
import com.nhathuy.nextmeet.model.Contact
import com.nhathuy.nextmeet.model.NotificationType
import com.nhathuy.nextmeet.model.TransportMode
import com.nhathuy.nextmeet.repository.AppointmentPlusRepository
import com.nhathuy.nextmeet.repository.ContactRepository
import com.nhathuy.nextmeet.resource.AppointmentUiState
import com.nhathuy.nextmeet.utils.AppointmentStatusManager
import com.nhathuy.nextmeet.utils.NotificationManagerService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class AppointmentPlusViewModel @Inject constructor(
    private val appointmentRepository: AppointmentPlusRepository,
    private val contactRepository: ContactRepository,
    private val notificationManagerService: NotificationManagerService
) : ViewModel() {

    private val _appointmentUiState = MutableStateFlow<AppointmentUiState>(AppointmentUiState.Idle)
    val appointmentUiState: StateFlow<AppointmentUiState> = _appointmentUiState

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestion: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private var allAppointments: List<AppointmentPlus> = emptyList()


    private val statusManager = AppointmentStatusManager()
    private var statusUpdateJob: Job? = null
    private var isActive = false


    /**
     * Tạo cuộc hẹn mới
     */
    fun createAppointment(
        appointment: AppointmentPlus,
        shouldSetReminder: Boolean = false,
    ) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                if (appointment.contactId == null) {
                    _appointmentUiState.value =
                        AppointmentUiState.Error("Vui lòng chọn liên hệ cho cuộc hẹn")
                    return@launch
                }
                val result = appointmentRepository.createAppointment(
                    userId = appointment.userId,
                    contactId = appointment.contactId,
                    title = appointment.title,
                    description = appointment.description,
                    startDateTime = appointment.startDateTime,
                    endDateTime = appointment.endDateTime,
                    location = appointment.location,
                    latitude = appointment.latitude,
                    longitude = appointment.longitude,
                    status = appointment.status,
                    color = appointment.color,
                    travelTimeMinutes = appointment.travelTimeMinutes,
                    isPinned = appointment.isPinned
                )

                if (result.isSuccess) {
                    val createdAppointmentId = result.getOrThrow()

                    // hẹn cuộc hẹn
                    val appointmentWithId = appointment.copy(id = createdAppointmentId.toInt())
                    if (shouldSetReminder) {
                        scheduleAppointmentNotification(
                            appointmentWithId,
                            appointment.contactId
                        )
                    }

                    _appointmentUiState.value = AppointmentUiState.AppointmentCreated(
                        result.getOrThrow(),
                        "Cuộc hẹn đã được tạo thành công"
                    )
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi không xác định"
                    )
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi tạo cuộc hẹn"
                )
            }
        }
    }

    /**
     * Lấy tất cả cuộc hẹn của người dùng với các bộ lọc
     */
    fun getAllAppointments(
        userId: Int,
        searchQuery: String = "",
        showPinnedOnly: Boolean = false,
        status: AppointmentStatus? = null
    ) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                appointmentRepository.getAllAppointmentsWithFilter(
                    userId, searchQuery, showPinnedOnly, status
                ).collect { appointments ->
                    allAppointments = appointments
                    _appointmentUiState.value = AppointmentUiState.AppointmentsLoaded(appointments)
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi tải danh sách cuộc hẹn"
                )
            }
        }
    }

    /**
     * Toggle pin/unpin cuộc hẹn
     */
    fun togglePin(appointmentId: Int) {
        viewModelScope.launch {
            try {
                appointmentRepository.togglePin(appointmentId)
                    .onSuccess { isPinned ->
                        val message = if (isPinned) "Đã pin cuộc hẹn" else "Đã bỏ pin cuộc hẹn"
                        _appointmentUiState.value = AppointmentUiState.PinToggled(isPinned, message)
                    }
                    .onFailure { error ->
                        _appointmentUiState.value = AppointmentUiState.Error(
                            error.message ?: "Lỗi khi pin cuộc hẹn"
                        )
                    }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi pin cuộc hẹn"
                )
            }
        }
    }

    /**
     * Cập nhật trạng thái cuộc hẹn
     */
    fun updateAppointmentStatus(appointmentId: Int, status: AppointmentStatus) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val result = appointmentRepository.updateAppointmentStatus(appointmentId, status)
                if (result.isSuccess) {

                    allAppointments = allAppointments.map { appointment ->
                        if (appointment.id == appointmentId) {
                            appointment.copy(status = status, updateAt = System.currentTimeMillis())
                        } else {
                            appointment
                        }
                    }

                    _appointmentUiState.value = AppointmentUiState.StatusUpdated(
                        status,
                        "Trạng thái cuộc hẹn đã được cập nhật"
                    )

                    _appointmentUiState.value =
                        AppointmentUiState.AppointmentsLoaded(allAppointments)
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi khi cập nhật trạng thái"
                    )
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi cập nhật trạng thái cuộc hẹn"
                )
            }
        }
    }

    /**
     * Xóa cuộc hẹn theo ID
     */
    fun deleteAppointment(appointmentId: Int) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                // Hủy notifications đầu tiên
                cancelAppointmentNotification(appointmentId)

                val result = appointmentRepository.deleteAppointment(appointmentId)
                if (result.isSuccess) {
                    _appointmentUiState.value = AppointmentUiState.AppointmentDeleted(
                        "Cuộc hẹn đã được xóa thành công"
                    )
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi khi xóa cuộc hẹn"
                    )
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi xóa cuộc hẹn"
                )
            }
        }
    }

    /**
     * Lấy cuộc hẹn theo ID
     */
    fun getAppointmentById(appointmentId: Int) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val result = appointmentRepository.getAppointmentById(appointmentId)
                if (result.isSuccess) {
                    _appointmentUiState.value = AppointmentUiState.AppointmentLoaded(
                        result.getOrThrow()
                    )
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi khi tải cuộc hẹn"
                    )
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi tải cuộc hẹn"
                )
            }
        }
    }

    /**
     * Cập nhật cuộc hẹn
     */
    fun updateAppointment(
        appointment: AppointmentPlus,
        shouldSetReminder: Boolean = false,
    ) {
        viewModelScope.launch {
            _appointmentUiState.value = AppointmentUiState.Loading
            try {
                val result = appointmentRepository.updateAppointment(appointment)

                if (result.isSuccess) {
                    // Xóa notification cũ trước (nếu có)
                    cancelAppointmentNotification(appointment.id)

                    // Tạo notification mới nếu user chọn reminder
                    if (shouldSetReminder && appointment.contactId != null) {
                        scheduleAppointmentNotification(
                            appointment,
                            appointment.contactId
                        )
                    }

                    _appointmentUiState.value = AppointmentUiState.AppointmentUpdated(
                        "Cuộc hẹn đã được cập nhật thành công"
                    )
                } else {
                    _appointmentUiState.value = AppointmentUiState.Error(
                        result.exceptionOrNull()?.message ?: "Lỗi không xác định"
                    )
                }
            } catch (e: Exception) {
                _appointmentUiState.value = AppointmentUiState.Error(
                    e.message ?: "Lỗi khi cập nhật cuộc hẹn"
                )
            }
        }
    }

    /**
     * Lên lịch thông báo cho cuộc hẹn
     */
    fun scheduleAppointmentNotification(
        appointment: AppointmentPlus,
        contactId: Int
    ) {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()

                if (appointment.startDateTime > now) {
                    // Lấy thông tin contact để hiển thị trong notification
                    val contactResult = contactRepository.getContactById(contactId)
                    val contactName = if (contactResult.isSuccess) {
                        contactResult.getOrNull()?.name ?: "Không rõ"
                    } else {
                        "Không rõ"
                    }

                    val success = notificationManagerService.scheduleAppointmentNotification(
                        userId = appointment.userId,
                        appointmentId = appointment.id,
                        title = appointment.title,
                        description = appointment.description ?: "",
                        appointmentTime = appointment.startDateTime,
                        location = appointment.location,
                        contactName = contactName
                    )

                    if (!success) {
                        // Log lỗi nhưng không fail toàn bộ process tạo appointment
                        Log.w(
                            "AppointmentViewModel",
                            "Không thể tạo notification cho cuộc hẹn ${appointment.id}"
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Lỗi khi tạo notification", e)
            }
        }
    }


    /**
     * Hủy thông báo cho cuộc hẹn
     */
    private suspend fun cancelAppointmentNotification(appointmentId: Int) {
        try {
            // Xóa tất cả notification liên quan đến appointment này
            notificationManagerService.cancelNotificationsByRelatedId(
                appointmentId,
                NotificationType.APPOINTMENT_REMINDER
            )
        } catch (e: Exception) {
            Log.e("AppointmentViewModel", "Lỗi khi hủy notification", e)
        }
    }

    /**
     * Bắt đầu điều hướng đến cuộc hẹn
     */
    fun updateNavigationStatus(appointmentId: Int, hasStartedNavigation: Boolean) {
        viewModelScope.launch {
            try {
                val result = appointmentRepository.updateNavigationStatus(
                    appointmentId,
                    hasStartedNavigation
                )
                if (result.isSuccess) {
                    checkAppointmentStatus(appointmentId)
                }
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error updating navigation status", e)
            }
        }
    }

    /**
     * tự động cập nhật trạng thái dựa trên thời gian hiện tại
     */
    fun updateAppointmentBasedOnTime(appointmentId: Int) {
        viewModelScope.launch {
            try {
                val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)
                if (appointmentResult.isSuccess) {
                    val appointment = appointmentResult.getOrThrow()
                    val currentTime = System.currentTimeMillis()

                    Log.d("AppointmentViewModel", "Current time: $currentTime")
                    Log.d("AppointmentViewModel", "Start time: ${appointment.startDateTime}")
                    Log.d("AppointmentViewModel", "End time: ${appointment.endDateTime}")
                    Log.d("AppointmentViewModel", "Current status: ${appointment.status}")

                    val newStatus = statusManager.calculateNewStatus(
                        appointment = appointment,
                        currentTime = currentTime,
                        hasStartedNavigation = appointment.navigationStarted
                    )

                    if (statusManager.shouldUpdateStatus(appointment.status, newStatus)) {
                        Log.d(
                            "AppointmentViewModel",
                            "Updating status from ${appointment.status} to $newStatus"
                        )

                        // Cập nhật status
                        updateAppointmentStatus(appointmentId, newStatus)

//                        // Gửi notification nếu cần
//                        val message = statusManager.getStatusTransitionMessage(appointment.status, newStatus)
//                        sendStatusUpdateNotification(appointment, newStatus, message)
                    } else {
                        Log.d(
                            "AppointmentViewModel",
                            "No status change needed: ${appointment.status}"
                        )
                    }
                } else {
                    Log.w("AppointmentViewModel", "Appointment not found for ID: $appointmentId")
                }
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error updating appointment status: ${e.message}", e)
            }
        }
    }

    /**
     *  cập nhật thời gian di chuyển cho cuộn hẹn
     */
    fun updateTravelTime(appointmentId: Int, travelTimeMinutes: Int) {
        viewModelScope.launch {
            try {
                Log.d(
                    "AppointmentViewModel",
                    "Updating travel time for appointment $appointmentId to $travelTimeMinutes minutes"
                )

                allAppointments = allAppointments.map { appointment ->
                    if (appointment.id == appointmentId) {
                        appointment.copy(
                            travelTimeMinutes = travelTimeMinutes,
                            updateAt = System.currentTimeMillis()
                        )
                    } else {
                        appointment
                    }
                }
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error updating travel time", e)
            }
        }
    }

    /**
     *
     */
    fun updateAppointmentWithRouteInfo(
        appointmentId: Int,
        travelTimeMinutes: Int,
        distance: Double,
        transportMode: TransportMode
    ) {
        viewModelScope.launch {
            try {
                val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)
                if (appointmentResult.isSuccess) {
                    val appointment = appointmentResult.getOrThrow()

                    // Cập nhật appointment với travel time mới
                    val updatedAppointment = appointment.copy(
                        travelTimeMinutes = travelTimeMinutes,
                        updateAt = System.currentTimeMillis()
                    )

                    val result = appointmentRepository.updateAppointment(updatedAppointment)
                    if (result.isSuccess) {
                        Log.d(
                            "AppointmentViewModel",
                            "Updated appointment $appointmentId: travel time = $travelTimeMinutes min, distance = $distance km"
                        )

                        // Cập nhật local cache
                        allAppointments = allAppointments.map { appt ->
                            if (appt.id == appointmentId) updatedAppointment else appt
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AppointmentViewModel", "Error updating appointment with route info", e)
            }
        }
    }

    /**
     * Lấy gợi ý tìm kiếm
     */

    /**
     * Reset UI state về Idle
     */
    fun resetUiState() {
        _appointmentUiState.value = AppointmentUiState.Idle
    }

    /**
     * Bắt đầu theo dõi trạng thái cuộc hẹn
     */
    fun startStatus(userId: Int) {
        if (isActive) return

        isActive = true
        statusUpdateJob = viewModelScope.launch {
            while (isActive) {
                try {
                    updateActiveAppointments(userId)
                    delay(5 * 60 * 1000) // Cập nhật mỗi 5 phút
                } catch (e: Exception) {
                    Log.e("StatusMonitor", "Error: ${e.message}")
                    delay(10 * 60 * 1000)
                }
            }
        }
    }

    /**
     * Dừng theo dõi
     */
    fun stopStatus() {
        isActive = false
        statusUpdateJob?.cancel()
        statusUpdateJob = null
    }

    private suspend fun updateActiveAppointments(userId: Int) {
        try {
            val activeAppointments = appointmentRepository.getAllActiveAppointments(userId)
            var updatedCount = 0

            activeAppointments.forEach { appointment ->
                val newStatus = statusManager.calculateNewStatus(
                    appointment = appointment,
                    currentTime = System.currentTimeMillis(),
                    hasStartedNavigation = appointment.navigationStarted
                )

                if (statusManager.shouldUpdateStatus(appointment.status, newStatus)) {
                    val result =
                        appointmentRepository.updateAppointmentStatus(appointment.id, newStatus)
                    if (result.isSuccess) {
                        updatedCount++

                        if (shouldNotifyUser(newStatus)) {
                            sendImportantNotification(appointment, newStatus)
                        }

                        updateLocalCache(appointment.id, newStatus)
                    }
                }
            }
            if (updatedCount > 0) {
                Log.d("StatusMonitor", "Quietly updated $updatedCount appointments")
            }
        } catch (e: Exception) {
            Log.e("StatusMonitor", "Error updating appointments", e)
        }
    }

    private fun shouldNotifyUser(status: AppointmentStatus): Boolean {
        return when (status) {
            AppointmentStatus.MISSED -> true
            AppointmentStatus.DELAYED -> true
            else -> false
        }
    }

    private suspend fun sendImportantNotification(
        appointment: AppointmentPlus,
        newStatus: AppointmentStatus
    ) {
        try {
            val (title, message) = when (newStatus) {
                AppointmentStatus.DELAYED -> {
                    "⚠️ Sắp trễ cuộc hẹn" to "Cuộc hẹn '${appointment.title}' sắp diễn ra. Bạn nên khởi hành ngay!"
                }

                AppointmentStatus.MISSED -> {
                    "❌ Đã bỏ lỡ cuộc hẹn" to "Cuộc hẹn '${appointment.title}' đã bắt đầu và bạn chưa đến."
                }

                else -> return
            }
            notificationManagerService.sendSimpleNotification(
                appointmentId = appointment.id,
                title = title,
                message = message
            )
            Log . d ("StatusMonitor", "Sent important notification: $title")

        } catch (e: Exception) {
            Log.e("StatusMonitor", "Error sending notification", e)
        }
    }

    private fun updateLocalCache(appointmentId:Int, newStatus: AppointmentStatus){
        allAppointments = allAppointments.map { appointment ->
            if (appointment.id == appointmentId) {
                appointment.copy(status = newStatus, updateAt = System.currentTimeMillis())
            } else {
                appointment
            }
        }
    }
    //kiểm tra cuộc hẹn với trạng thái
    fun checkAppointmentStatus(appointmentId: Int) {
        viewModelScope.launch {
            try {
                val appointmentResult = appointmentRepository.getAppointmentById(appointmentId)
                if(appointmentResult.isSuccess){
                    val appointment = appointmentResult.getOrThrow()
                    val newStatus = statusManager.calculateNewStatus(
                        appointment = appointment,
                        currentTime = System.currentTimeMillis(),
                        hasStartedNavigation = appointment.navigationStarted
                    )

                    if (statusManager.shouldUpdateStatus(appointment.status, newStatus)) {
                        appointmentRepository.updateAppointmentStatus(appointment.id, newStatus)
                        updateLocalCache(appointment.id, newStatus)

                        Log.d("StatusMonitor", "Updated appointment $appointmentId: ${appointment.status} -> $newStatus")
                    }
                }
            }
            catch (e: Exception){
                Log.e("StatusMonitor", "Error checking appointment status", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopStatus()
    }
}